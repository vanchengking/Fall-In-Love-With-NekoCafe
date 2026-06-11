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
import java.util.Set;

@Service
public class ReservationService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    /** 可代客取消/流转预约的后台角色。 */
    private static final Set<String> STAFF_ROLES = Set.of("staff", "manager", "operator", "admin");

    private final ReservationMapper reservationMapper;
    private final ReservationEventMapper reservationEventMapper;
    private final UserMapper userMapper;

    public ReservationService(ReservationMapper reservationMapper,
                              ReservationEventMapper reservationEventMapper,
                              UserMapper userMapper) {
        this.reservationMapper = reservationMapper;
        this.reservationEventMapper = reservationEventMapper;
        this.userMapper = userMapper;
    }

    public List<Map<String, Object>> list(String date, String mobileNumber, Long storeId, String status) {
        String mobile = (mobileNumber == null || mobileNumber.isBlank())
                ? null : ReservationValidator.normalizeMobile(mobileNumber);
        List<Map<String, Object>> rows =
                reservationMapper.listReservations(emptyToNull(date), mobile, storeId, emptyToNull(status));
        rows.forEach(ReservationService::decorate);
        return rows;
    }

    @Transactional
    public Map<String, Object> create(ReservationRequest request) {
        ReservationValidator.Normalized r = ReservationValidator.validate(request, LocalDateTime.now(ZONE));

        User user = upsertCustomer(r);

        boolean manual = r.tableId() != null;
        Map<String, Object> table = manual
                ? reservationMapper.findTableByIdForUpdate(r.tableId(), r.storeId(), r.partySize())
                : reservationMapper.findAvailableTableForUpdate(r.storeId(), r.partySize(), r.reservationDate(), r.reservationTime());

        if (table == null) {
            throw manual
                    ? ApiException.conflict("所选桌位不可用（不存在/容量不足/已停用），请重新选择桌位")
                    : ApiException.conflict("暂无满足该时段与人数的可用桌位，请调整时间或人数");
        }
        Long tableId = Normalizer.toLong(table.get("id"));
        String tableCode = String.valueOf(table.get("code"));

        if (reservationMapper.countActiveOnSlot(tableId, r.reservationDate(), r.reservationTime()) > 0) {
            throw ApiException.conflict("该桌位在该时段已被预约，请重新选择桌位或时间");
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
            // 命中 table-slot 或 user-store-slot 唯一约束（并发/重复预约），数据库兜底防重复
            throw ApiException.conflict("该时段已被预约，请重新选择桌位或时间");
        }

        // 按 D-01 报告口径写两条审计事件：null -> created -> booked
        reservationEventMapper.insert(new ReservationEvent(
                reservation.getId(), null, ReservationStatus.CREATED.value(),
                "customer", user.getId(), "预约已创建"));
        reservationEventMapper.insert(new ReservationEvent(
                reservation.getId(), ReservationStatus.CREATED.value(), ReservationStatus.BOOKED.value(),
                "customer", user.getId(), "桌位已分配，预约已确认"));

        Map<String, Object> detail = decorate(reservationMapper.getReservationDetail(reservation.getId()));
        if (detail != null) {
            detail.putIfAbsent("table_code", tableCode);
        }
        return detail;
    }

    /** 后台状态流转（staff/manager/operator/admin）。 */
    @Transactional
    public Map<String, Object> updateStatus(Long id, String nextStatusRaw, AuthUser actor) {
        Reservation current = reservationMapper.selectForUpdate(id);
        if (current == null) {
            throw ApiException.notFound("预约 " + id + " 不存在");
        }
        ReservationStatus to = ReservationStatus.from(nextStatusRaw)
                .orElseThrow(() -> ApiException.badRequest("无法识别的预约状态：" + nextStatusRaw));
        return applyTransition(current, to, actor, null);
    }

    /**
     * 取消预约：顾客可取消本人的 {@code created/booked} 预约，后台角色可代客取消。
     */
    @Transactional
    public Map<String, Object> cancel(Long id, AuthUser actor) {
        Reservation current = reservationMapper.selectForUpdate(id);
        if (current == null) {
            throw ApiException.notFound("预约 " + id + " 不存在");
        }
        boolean staff = isStaff(actor);
        boolean owner = actor != null && actor.id() != null && actor.id().equals(current.getUserId());
        if (!staff && !owner) {
            throw ApiException.forbidden("无权取消该预约");
        }

        ReservationStatus from = ReservationStatus.from(current.getStatus())
                .orElseThrow(() -> ApiException.badRequest("无法识别的当前状态：" + current.getStatus()));
        // 顾客仅能取消尚未入座的预约；后台角色由状态机决定可取消范围
        if (!staff && from != ReservationStatus.CREATED && from != ReservationStatus.BOOKED) {
            throw ApiException.badRequest("仅可取消「待确认」或「已预约」状态的预约");
        }

        String note = (owner && !staff) ? "顾客取消预约" : "取消预约";
        return applyTransition(current, ReservationStatus.CANCELLED, actor, note);
    }

    public List<Map<String, Object>> events(Long reservationId) {
        List<Map<String, Object>> rows = reservationMapper.listEvents(reservationId);
        for (Map<String, Object> row : rows) {
            row.put("from_status_label", ReservationStatus.labelOf(asString(row.get("from_status"))));
            row.put("to_status_label", ReservationStatus.labelOf(asString(row.get("to_status"))));
        }
        return rows;
    }

    /** 校验状态机后写库 + 审计事件 + 入座提醒，返回带中文标签的预约详情。 */
    private Map<String, Object> applyTransition(Reservation current, ReservationStatus to, AuthUser actor, String note) {
        ReservationStatus from = ReservationStatus.from(current.getStatus())
                .orElseThrow(() -> ApiException.badRequest("无法识别的当前状态：" + current.getStatus()));

        if (!ReservationStateMachine.canTransition(from, to)) {
            throw ApiException.badRequest(
                    "预约不能从「" + from.label() + "」流转到「" + to.label() + "」");
        }

        reservationMapper.update(null, new UpdateWrapper<Reservation>()
                .eq("id", current.getId())
                .set("status", to.value()));

        reservationEventMapper.insert(new ReservationEvent(
                current.getId(), from.value(), to.value(),
                actor == null ? null : actor.role(),
                actor == null ? null : actor.id(),
                note == null ? defaultNote(to) : note));

        if (to == ReservationStatus.SEATED) {
            reservationMapper.insertSeatedAlert(current.getStoreId(), current.getId());
        }

        return decorate(reservationMapper.getReservationDetail(current.getId()));
    }

    /** 按目标状态给出与动作一致的中文备注。 */
    private static String defaultNote(ReservationStatus to) {
        return switch (to) {
            case CREATED -> "预约已创建";
            case BOOKED -> "桌位已分配，预约已确认";
            case SEATED -> "顾客已入座";
            case DINING -> "开始用餐";
            case FINISHED -> "用餐完成";
            case CANCELLED -> "取消预约";
            case NO_SHOW -> "标记未到店";
        };
    }

    private static boolean isStaff(AuthUser actor) {
        return actor != null && actor.role() != null
                && STAFF_ROLES.contains(actor.role().toLowerCase());
    }

    /** 给预约 Map 补充中文状态标签（数据库仍存英文）。 */
    private static Map<String, Object> decorate(Map<String, Object> row) {
        if (row != null) {
            Object status = row.get("status");
            if (status != null) {
                row.put("status_label", ReservationStatus.labelOf(String.valueOf(status)));
            }
        }
        return row;
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
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
        // 预约不再增加积分，积分仅通过订单支付获得
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
