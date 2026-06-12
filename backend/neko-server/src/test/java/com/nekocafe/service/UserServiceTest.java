package com.nekocafe.service;

import com.nekocafe.common.ApiException;
import com.nekocafe.entity.PointTransaction;
import com.nekocafe.entity.User;
import com.nekocafe.mapper.PointTransactionMapper;
import com.nekocafe.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户服务单元测试（FR-MEMBER-001 / FR-MEMBER-002）：
 * 资料维护（姓名/手机号/偏好 + 唯一性校验）与积分变更落流水。
 */
class UserServiceTest {

    private UserMapper userMapper;
    private PointTransactionMapper pointTransactionMapper;
    private UserService service;

    @BeforeEach
    void setup() {
        userMapper = mock(UserMapper.class);
        pointTransactionMapper = mock(PointTransactionMapper.class);
        service = new UserService(userMapper, pointTransactionMapper);
    }

    private User user(long id) {
        User u = new User();
        u.setId(id);
        u.setName("林小满");
        u.setMobileNumber("13800000001");
        u.setRole("customer");
        u.setMemberLevel("gold");
        u.setPoints(1280);
        u.setPreferences(new ArrayList<>(List.of("quiet")));
        return u;
    }

    @Test
    @DisplayName("更新资料：姓名/手机号（归一化）/偏好生效，role 与积分不受影响")
    void updateProfileUpdatesNameMobileAndPreferences() {
        User u = user(1L);
        when(userMapper.selectById(1L)).thenReturn(u);
        when(userMapper.selectCount(any())).thenReturn(0L);

        Map<String, Object> out = service.updateProfile(1L, "新名字", "139-0000-0009", List.of("window"));

        assertEquals("新名字", u.getName());
        assertEquals("13900000009", u.getMobileNumber());
        assertEquals(List.of("window"), u.getPreferences());
        // 规则字段不可被资料接口篡改
        assertEquals("customer", u.getRole());
        assertEquals(1280, u.getPoints());
        assertEquals("gold", u.getMemberLevel());
        verify(userMapper).updateById(u);
        assertEquals("13900000009", out.get("mobile_number"));
    }

    @Test
    @DisplayName("手机号被他人占用：抛 409，不写库")
    void updateProfileMobileConflictRejected() {
        when(userMapper.selectById(1L)).thenReturn(user(1L));
        when(userMapper.selectCount(any())).thenReturn(1L);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.updateProfile(1L, null, "13800000002", null));
        assertEquals(409, ex.getStatus());
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    @DisplayName("手机号不足 8 位数字：抛 400")
    void updateProfileInvalidMobileRejected() {
        when(userMapper.selectById(1L)).thenReturn(user(1L));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.updateProfile(1L, null, "123", null));
        assertEquals(400, ex.getStatus());
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    @DisplayName("手机号与本人当前号码一致：不触发唯一性查询")
    void updateProfileSameMobileSkipsUniqueCheck() {
        User u = user(1L);
        when(userMapper.selectById(1L)).thenReturn(u);

        service.updateProfile(1L, null, "138 0000 0001", null);

        verify(userMapper, never()).selectCount(any());
        assertEquals("13800000001", u.getMobileNumber());
    }

    @Test
    @DisplayName("changePoints：更新积分与等级，并写入带余额的流水")
    void changePointsPersistsTransaction() {
        User u = user(1L);
        u.setPoints(995);
        u.setMemberLevel("bronze");
        when(userMapper.selectById(1L)).thenReturn(u);

        service.changePoints(1L, 10, "reservation_finished", 5L, "预约完成奖励");

        assertEquals(1005, u.getPoints());
        assertEquals("silver", u.getMemberLevel());
        ArgumentCaptor<PointTransaction> captor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(pointTransactionMapper).insert(captor.capture());
        PointTransaction txn = captor.getValue();
        assertEquals(1L, txn.getUserId());
        assertEquals(10, txn.getDelta());
        assertEquals(1005, txn.getBalanceAfter());
        assertEquals("reservation_finished", txn.getSourceType());
        assertEquals(5L, txn.getSourceId());
        assertEquals("预约完成奖励", txn.getReason());
    }

    @Test
    @DisplayName("changePoints：余额不足抛 400，不更新用户、不写流水")
    void changePointsInsufficientBalanceRejected() {
        User u = user(1L);
        u.setPoints(5);
        when(userMapper.selectById(1L)).thenReturn(u);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.changePoints(1L, -10, "order_cancelled", 8L, null));
        assertEquals(400, ex.getStatus());
        verify(userMapper, never()).updateById(any(User.class));
        verify(pointTransactionMapper, never()).insert(any(PointTransaction.class));
    }

    @Test
    @DisplayName("changePoints：同一来源重复发放命中唯一约束抛 409（事务回滚）")
    void changePointsDuplicateSourceRejected() {
        when(userMapper.selectById(1L)).thenReturn(user(1L));
        when(pointTransactionMapper.insert(any(PointTransaction.class)))
                .thenThrow(new DuplicateKeyException("uq_point_transactions_source"));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.changePoints(1L, 10, "reservation_finished", 5L, "预约完成奖励"));
        assertEquals(409, ex.getStatus());
    }

    @Test
    @DisplayName("积分明细按用户查询透传 mapper 结果")
    void pointsHistoryDelegatesToMapper() {
        when(pointTransactionMapper.listByUser(1L)).thenReturn(List.of(Map.of("delta", 10)));

        List<Map<String, Object>> history = service.getPointsHistory(1L);

        assertEquals(1, history.size());
        assertEquals(10, history.get(0).get("delta"));
    }
}
