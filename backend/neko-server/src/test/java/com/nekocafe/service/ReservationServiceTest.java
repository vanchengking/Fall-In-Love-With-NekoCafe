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
 * ReservationService 状态流转单元测试：合法跳转写库+审计，非法跳转不写库不插事件，取消校验本人权限，
 * 完成预约发放积分（FR-MEMBER-002）。
 */
class ReservationServiceTest {

    private ReservationMapper reservationMapper;
    private ReservationEventMapper reservationEventMapper;
    private UserMapper userMapper;
    private UserService userService;
    private ReservationService service;

    @BeforeEach
    void setup() {
        reservationMapper = mock(ReservationMapper.class);
        reservationEventMapper = mock(ReservationEventMapper.class);
        userMapper = mock(UserMapper.class);
        userService = mock(UserService.class);
        service = new ReservationService(reservationMapper, reservationEventMapper, userMapper, userService);
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
    @DisplayName("合法跳转 booked->seated：写库、插事件并生成入座提醒，不发积分")
    void legalTransitionPersistsAndAudits() {
        when(reservationMapper.selectForUpdate(1L)).thenReturn(reservation(1L, 9L, "booked"));
        when(reservationMapper.getReservationDetail(1L)).thenReturn(new HashMap<>());

        service.updateStatus(1L, "seated", new AuthUser(2L, "店员", "138", "staff"));

        verify(reservationMapper, times(1)).update(isNull(), any());
        verify(reservationEventMapper, times(1)).insert(any(ReservationEvent.class));
        verify(reservationMapper, times(1)).insertSeatedAlert(eq(1L), eq(1L));
        verify(userService, never()).changePoints(any(), org.mockito.ArgumentMatchers.anyInt(), any(), any(), any());
    }

    @Test
    @DisplayName("dining->finished：状态写库 + 为预约用户发放完成积分并落流水")
    void finishAwardsPoints() {
        when(reservationMapper.selectForUpdate(1L)).thenReturn(reservation(1L, 9L, "dining"));
        when(reservationMapper.getReservationDetail(1L)).thenReturn(new HashMap<>());

        service.updateStatus(1L, "finished", new AuthUser(2L, "店员", "138", "staff"));

        verify(reservationMapper, times(1)).update(isNull(), any());
        verify(userService, times(1)).changePoints(
                eq(9L), eq(ReservationService.POINTS_PER_FINISHED_RESERVATION),
                eq("reservation_finished"), eq(1L), any());
    }

    @Test
    @DisplayName("finished 为终态：重复流转 finished 抛 400，不再重复发积分")
    void finishedIsTerminalNoDoubleAward() {
        when(reservationMapper.selectForUpdate(1L)).thenReturn(reservation(1L, 9L, "finished"));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.updateStatus(1L, "finished", new AuthUser(2L, "店员", "138", "staff")));
        assertEquals(400, ex.getStatus());

        verify(reservationMapper, never()).update(any(), any());
        verify(userService, never()).changePoints(any(), org.mockito.ArgumentMatchers.anyInt(), any(), any(), any());
    }

    @Test
    @DisplayName("积分发放失败：异常向上传播（事务回滚，状态不停留在 finished 而积分缺失）")
    void awardFailurePropagatesForRollback() {
        when(reservationMapper.selectForUpdate(1L)).thenReturn(reservation(1L, 9L, "dining"));
        when(userService.changePoints(any(), org.mockito.ArgumentMatchers.anyInt(), any(), any(), any()))
                .thenThrow(ApiException.conflict("该来源的积分已发放，不可重复入账"));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.updateStatus(1L, "finished", new AuthUser(2L, "店员", "138", "staff")));
        assertEquals(409, ex.getStatus());
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
