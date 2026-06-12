package com.nekocafe.web;

import com.nekocafe.config.SecurityConfig;
import com.nekocafe.security.JwtAuthFilter;
import com.nekocafe.security.JwtService;
import com.nekocafe.service.DashboardService;
import com.nekocafe.service.ReservationService;
import com.nekocafe.service.StoreAdminService;
import com.nekocafe.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RBAC 接口级安全测试（FR-AUTH-003）：
 * 顾客（customer）携带有效 JWT 访问管理接口必须返回 403，后台角色放行，匿名 401；
 * 鉴权由后端 SecurityConfig 强制，不依赖前端路由守卫。
 */
@WebMvcTest(controllers = {
        ReservationController.class, DashboardController.class, StoreController.class, UserController.class})
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtService.class})
@TestPropertySource(properties = {
        "neko.jwt.secret=test-secret-0123456789abcdef-0123456789abcdef",
        "neko.jwt.ttl-seconds=3600"
})
class RbacSecurityTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @MockBean
    private ReservationService reservationService;
    @MockBean
    private DashboardService dashboardService;
    @MockBean
    private StoreAdminService storeAdminService;
    @MockBean
    private UserService userService;

    private String bearer(long userId, String role) {
        return "Bearer " + jwtService.issue(userId, role, "测试用户", "13800000099");
    }

    @Test
    @DisplayName("customer 流转预约状态：403")
    void customerCannotTransitionReservation() throws Exception {
        mockMvc.perform(patch("/api/reservations/1/status")
                        .header("Authorization", bearer(1L, "customer"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"seated\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("staff 流转预约状态：放行")
    void staffCanTransitionReservation() throws Exception {
        when(reservationService.updateStatus(anyLong(), any(), any())).thenReturn(Map.of("id", 1));
        mockMvc.perform(patch("/api/reservations/1/status")
                        .header("Authorization", bearer(2L, "staff"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"seated\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("匿名流转预约状态：401")
    void anonymousTransitionRejected() throws Exception {
        mockMvc.perform(patch("/api/reservations/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"seated\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("customer 访问管理看板：403")
    void customerCannotReadAdminDashboard() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/summary")
                        .header("Authorization", bearer(1L, "customer")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("manager 访问管理看板：放行")
    void managerCanReadAdminDashboard() throws Exception {
        when(dashboardService.summary(any())).thenReturn(Map.of());
        mockMvc.perform(get("/api/admin/dashboard/summary")
                        .header("Authorization", bearer(3L, "manager")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("customer 访问门店管理接口（读/写）：403")
    void customerCannotAccessAdminStores() throws Exception {
        mockMvc.perform(get("/api/admin/stores")
                        .header("Authorization", bearer(1L, "customer")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/stores")
                        .header("Authorization", bearer(1L, "customer"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("operator 访问门店管理接口：放行")
    void operatorCanAccessAdminStores() throws Exception {
        when(storeAdminService.listStores()).thenReturn(List.of());
        mockMvc.perform(get("/api/admin/stores")
                        .header("Authorization", bearer(4L, "operator")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("customer 访问桌位管理接口：403（/api/admin/** 兜底规则）")
    void customerCannotAccessAdminTables() throws Exception {
        mockMvc.perform(get("/api/admin/tables")
                        .header("Authorization", bearer(1L, "customer")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("customer 写猫咪健康记录：403（鉴权先于路由，无需控制器参与）")
    void customerCannotCreateCatHealthRecord() throws Exception {
        mockMvc.perform(post("/api/cat-health-records")
                        .header("Authorization", bearer(1L, "customer"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("匿名读取个人资料：401（/api/users/me 不再落入 GET permitAll）")
    void anonymousProfileRejected() throws Exception {
        mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users/me/points/history")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("customer 读取本人资料与积分明细：放行")
    void customerCanReadOwnProfileAndPoints() throws Exception {
        when(userService.getProfile(42L)).thenReturn(Map.of("id", 42));
        when(userService.getPointsHistory(42L)).thenReturn(List.of());

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", bearer(42L, "customer")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/users/me/points/history")
                        .header("Authorization", bearer(42L, "customer")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("customer 更新本人资料：放行，姓名/手机号/偏好透传服务层")
    void customerCanUpdateOwnProfile() throws Exception {
        when(userService.updateProfile(eq(42L), any(), any(), any())).thenReturn(Map.of("id", 42));

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", bearer(42L, "customer"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"新名字\",\"mobile_number\":\"13900000009\"}"))
                .andExpect(status().isOk());

        verify(userService).updateProfile(eq(42L), eq("新名字"), eq("13900000009"), isNull());
    }

    @Test
    @DisplayName("公开读取接口不受影响：匿名 GET /api/reservations 放行")
    void publicReadStillOpen() throws Exception {
        when(reservationService.list(any(), any(), any(), any())).thenReturn(List.of());
        mockMvc.perform(get("/api/reservations")).andExpect(status().isOk());
    }
}
