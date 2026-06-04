package com.nekocafe.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.nekocafe.common.ApiException;
import com.nekocafe.common.Normalizer;
import com.nekocafe.domain.ReservationStateMachine;
import com.nekocafe.domain.ReservationStatus;
import com.nekocafe.domain.ReservationValidator;
import com.nekocafe.dto.ReservationRequest;
import com.nekocafe.entity.Reservation;
import com.nekocafe.entity.ReservationEvent;
import com.nekocafe.entity.User;
import com.nekocafe.mapper.CatalogMapper;
import com.nekocafe.mapper.ReservationEventMapper;
import com.nekocafe.mapper.ReservationMapper;
import com.nekocafe.mapper.UserMapper;
import com.nekocafe.security.AuthUser;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ReservationService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final ReservationMapper reservationMapper;
    private final ReservationEventMapper reservationEventMapper;
    private final CatalogMapper catalogMapper;
    private final UserMapper userMapper;

    public ReservationService(ReservationMapper reservationMapper,
                              ReservationEventMapper reservationEventMapper,
                              CatalogMapper catalogMapper,
                              UserMapper userMapper) {
        this.reservationMapper = reservationMapper;
        this.reservationEventMapper = reservationEventMapper;
        this.catalogMapper = catalogMapper;
        this.userMapper = userMapper;
    }

    public List<Map<String, Object>> list(String date, String mobileNumber, Long storeId, String status) {
        String mobile = (mobileNumber == null || mobileNumber.isBlank())
                ? null : ReservationValidator.normalizeMobile(mobileNumber);
        return catalogMapper.listReservations(emptyToNull(date), mobile, storeId, emptyToNull(status));
    }

    @Transactional
    public Map<String, Object> create(ReservationRequest request) {
        ReservationValidator.Normalized r = ReservationValidator.validate(request, LocalDateTime.now(ZONE));

        User user = upsertCustomer(r);

        Map<String, Object> table = (r.tableId() != null)
                ? catalogMapper.findTableByIdForUpdate(r.tableId(), r.storeId(), r.partySize())
                : catalogMapper.findAvailableTableForUpdate(r.storeId(), r.partySize(), r.reservationDate(), r.reservationTime());

        if (table == null) {
            throw ApiException.conflict("no available table for the selected slot and party size");
        }
        Long tableId = Normalizer.toLong(table.get("id"));
        String tableCode = String.valueOf(table.get("code"));

        if (catalogMapper.countActiveOnSlot(tableId, r.reservationDate(), r.reservationTime()) > 0) {
            throw ApiException.conflict("selected table is already reserved for this time slot");
        }

        Reservation reservation = new Reservation();
        reservation.setUserId(user.getId());
        reservation.setStoreId(r.storeId());
        reservation.setTableId(tableId);
        reservation.setRecommendedCatId(r.recommendedCatId());
        reservation.setReservationDate(r.reservationDate());
        reservation.setReservationTime(r.reservationTime());
        reservation.setPartySize(r.partySize());
        reservation.setStatus(ReservationStatus.BOOKED.value());
        reservation.setNote(r.note());

        try {
            reservationMapper.insert(reservation);
        } catch (DuplicateKeyException ex) {
            // 命中 table-slot 或 user-store-slot 唯一约束（并发/重复预约）
            throw ApiException.conflict("this slot is already reserved (duplicate booking)");
        }

        reservationEventMapper.insert(new ReservationEvent(
                reservation.getId(), ReservationStatus.CREATED.value(), ReservationStatus.BOOKED.value(),
                "customer", user.getId(), "预约创建"));

        Map<String, Object> detail = catalogMapper.getReservationDetail(reservation.getId());
        if (detail != null) {
            detail.putIfAbsent("table_code", tableCode);
        }
        return detail;
    }

    @Transactional
    public Map<String, Object> updateStatus(Long id, String nextStatusRaw, AuthUser actor) {
        Reservation current = reservationMapper.selectForUpdate(id);
        if (current == null) {
            throw ApiException.notFound("reservation " + id + " not found");
        }

        ReservationStatus from = ReservationStatus.from(current.getStatus())
                .orElseThrow(() -> ApiException.badRequest("unknown current status: " + current.getStatus()));
        ReservationStatus to = ReservationStatus.from(nextStatusRaw)
                .orElseThrow(() -> ApiException.badRequest("unknown reservation status: " + nextStatusRaw));

        if (!ReservationStateMachine.canTransition(from, to)) {
            throw ApiException.badRequest(
                    "reservation cannot transition from " + from.value() + " to " + to.value());
        }

        reservationMapper.update(null, new UpdateWrapper<Reservation>()
                .eq("id", id)
                .set("status", to.value()));

        reservationEventMapper.insert(new ReservationEvent(
                id, from.value(), to.value(),
                actor == null ? null : actor.role(),
                actor == null ? null : actor.id(),
                "状态变更"));

        if (to == ReservationStatus.SEATED) {
            catalogMapper.insertSeatedAlert(current.getStoreId(), id);
        }

        return catalogMapper.getReservationDetail(id);
    }

    public List<Map<String, Object>> events(Long reservationId) {
        return new ArrayList<>(reservationEventMapper.selectList(
                new QueryWrapper<ReservationEvent>()
                        .eq("reservation_id", reservationId)
                        .orderByAsc("created_at", "id"))
                .stream()
                .map(e -> {
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("id", e.getId());
                    m.put("reservation_id", e.getReservationId());
                    m.put("from_status", e.getFromStatus());
                    m.put("to_status", e.getToStatus());
                    m.put("actor_role", e.getActorRole());
                    m.put("actor_user_id", e.getActorUserId());
                    m.put("note", e.getNote());
                    return m;
                })
                .toList());
    }

    private User upsertCustomer(ReservationValidator.Normalized r) {
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("mobile_number", r.mobileNumber()));
        if (user == null) {
            User created = new User();
            created.setName(r.customerName());
            created.setMobileNumber(r.mobileNumber());
            created.setRole("customer");
            created.setMemberLevel("silver");
            created.setPoints(10);
            created.setPreferences(new ArrayList<>(r.preferences()));
            userMapper.insert(created);
            return created;
        }
        user.setName(r.customerName());
        user.setPoints((user.getPoints() == null ? 0 : user.getPoints()) + 10);
        if (!r.preferences().isEmpty()) {
            user.setPreferences(new ArrayList<>(r.preferences()));
        }
        userMapper.updateById(user);
        return user;
    }

    private static String emptyToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
