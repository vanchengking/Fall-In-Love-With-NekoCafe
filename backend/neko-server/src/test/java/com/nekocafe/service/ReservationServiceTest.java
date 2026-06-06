package com.nekocafe.service;

import com.nekocafe.common.ApiException;
import com.nekocafe.entity.Reservation;
import com.nekocafe.entity.ReservationEvent;
import com.nekocafe.mapper.ReservationEventMapper;
import com.nekocafe.mapper.ReservationMapper;
import com.nekocafe.mapper.UserMapper;
import com.nekocafe.security.AuthUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ReservationService 状态流转单元测试：合法跳转写库+审计，非法跳转不写库不插事件，取消校验本人权限。
 */
class ReservationServiceTest {

    private ReservationMapper reservationMapper;
    private ReservationEventMapper reservationEventMapper;
    private UserMapper userMapper;
    private ReservationService service;

    @BeforeEach
    void setup() {
        reservationMapper = mock(ReservationMapper.class);
        reservationEventMapper = mock(ReservationEventMapper.class);
        userMapper = mock(UserMapper.class);
        service = new ReservationService(reservationMapper, reservationEventMapper, userMapper);
    }

    private Reservation reservation(long id, long userId, String status) {
        Reservation r = new Reservation();
        r.setId(id);
        r.setUserId(userId);
        r.setStoreId(1L);
        r.setStatus(status);
        return r;
    }

    @Test
    @DisplayName("合法跳转 booked->seated：写库、插事件并生成入座提醒")
    void legalTransitionPersistsAndAudits() {
        when(reservationMapper.selectForUpdate(1L)).thenReturn(reservation(1L, 9L, "booked"));
        when(reservationMapper.getReservationDetail(1L)).thenReturn(new HashMap<>());

        service.updateStatus(1L, "seated", new AuthUser(2L, "店员", "138", "staff"));

        verify(reservationMapper, times(1)).update(isNull(), any());
        verify(reservationEventMapper, times(1)).insert(any(ReservationEvent.class));
        verify(reservationMapper, times(1)).insertSeatedAlert(eq(1L), eq(1L));
    }

    @Test
    @DisplayName("非法跳转 booked->finished：抛 400，不写库、不插事件")
    void illegalTransitionRejected() {
        when(reservationMapper.selectForUpdate(1L)).thenReturn(reservation(1L, 9L, "booked"));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.updateStatus(1L, "finished", new AuthUser(2L, "店员", "138", "staff")));
        assertEquals(400, ex.getStatus());

        verify(reservationMapper, never()).update(any(), any());
        verify(reservationEventMapper, never()).insert(any(ReservationEvent.class));
    }

    @Test
    @DisplayName("预约不存在：抛 404")
    void notFound() {
        when(reservationMapper.selectForUpdate(99L)).thenReturn(null);
        ApiException ex = assertThrows(ApiException.class,
                () -> service.updateStatus(99L, "seated", new AuthUser(2L, "店员", "138", "staff")));
        assertEquals(404, ex.getStatus());
    }

    @Test
    @DisplayName("顾客取消本人 booked 预约：成功写库并插事件")
    void ownerCancelsOwnReservation() {
        when(reservationMapper.selectForUpdate(1L)).thenReturn(reservation(1L, 9L, "booked"));
        when(reservationMapper.getReservationDetail(1L)).thenReturn(new HashMap<>());

        service.cancel(1L, new AuthUser(9L, "顾客", "138", "customer"));

        verify(reservationMapper, times(1)).update(isNull(), any());
        verify(reservationEventMapper, times(1)).insert(any(ReservationEvent.class));
    }

    @Test
    @DisplayName("顾客取消他人预约：抛 403，不写库")
    void nonOwnerCannotCancel() {
        when(reservationMapper.selectForUpdate(1L)).thenReturn(reservation(1L, 9L, "booked"));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.cancel(1L, new AuthUser(7L, "别人", "139", "customer")));
        assertEquals(403, ex.getStatus());

        verify(reservationMapper, never()).update(any(), any());
        verify(reservationEventMapper, never()).insert(any(ReservationEvent.class));
    }

    @Test
    @DisplayName("后台角色可代客取消他人预约")
    void staffCanCancelOnBehalf() {
        when(reservationMapper.selectForUpdate(1L)).thenReturn(reservation(1L, 9L, "booked"));
        when(reservationMapper.getReservationDetail(1L)).thenReturn(new HashMap<>());

        service.cancel(1L, new AuthUser(2L, "店员", "138", "staff"));

        verify(reservationMapper, times(1)).update(isNull(), any());
        verify(reservationEventMapper, times(1)).insert(any(ReservationEvent.class));
    }
}
