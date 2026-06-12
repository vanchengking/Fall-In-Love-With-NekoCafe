package com.nekocafe.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nekocafe.entity.Reservation;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 预约相关查询集合：列表、详情、桌位锁定/可用性、时段计数、状态事件与入座提醒。
 * 从 {@code CatalogMapper} 拆分而来（CatalogMapper 仅保留目录型只读查询），
 * 日期/时间统一在 SQL 中格式化为字符串，避免 JDBC 时间类型序列化歧义。
 */
public interface ReservationMapper extends BaseMapper<Reservation> {

    /** 行锁读取预约，用于状态机更新时防止并发。 */
    @Select("SELECT * FROM reservations WHERE id = #{id} FOR UPDATE")
    Reservation selectForUpdate(@Param("id") Long id);

    @Select("""
            SELECT r.id, r.user_id, r.store_id, r.table_id, r.recommended_cat_id,
                   DATE_FORMAT(r.reservation_date, '%Y-%m-%d') AS reservation_date,
                   DATE_FORMAT(r.reservation_time, '%H:%i') AS reservation_time,
                   r.party_size, r.status, r.note,
                   DATE_FORMAT(r.created_at, '%Y-%m-%d %H:%i:%s') AS created_at,
                   u.name AS customer_name, u.mobile_number,
                   s.name AS store_name, t.code AS table_code, c.name AS cat_name
            FROM reservations r
            JOIN users u ON u.id = r.user_id
            JOIN stores s ON s.id = r.store_id
            LEFT JOIN dining_tables t ON t.id = r.table_id
            LEFT JOIN cats c ON c.id = r.recommended_cat_id
            WHERE (#{date} IS NULL OR r.reservation_date = #{date})
              AND (#{mobile} IS NULL OR REPLACE(REPLACE(REPLACE(REPLACE(u.mobile_number, '(', ''), ')', ''), ' ', ''), '-', '') LIKE CONCAT('%', #{mobile}, '%'))
              AND (#{storeId} IS NULL OR r.store_id = #{storeId})
              AND (#{status} IS NULL OR r.status = #{status})
            ORDER BY r.reservation_date, r.reservation_time
            """)
    List<Map<String, Object>> listReservations(@Param("date") String date,
                                               @Param("mobile") String mobile,
                                               @Param("storeId") Long storeId,
                                               @Param("status") String status);

    @Select("""
            SELECT r.id, r.user_id, r.store_id, r.table_id, r.recommended_cat_id,
                   DATE_FORMAT(r.reservation_date, '%Y-%m-%d') AS reservation_date,
                   DATE_FORMAT(r.reservation_time, '%H:%i') AS reservation_time,
                   r.party_size, r.status, r.note,
                   DATE_FORMAT(r.created_at, '%Y-%m-%d %H:%i:%s') AS created_at,
                   u.name AS customer_name, u.mobile_number,
                   s.name AS store_name, t.code AS table_code, c.name AS cat_name
            FROM reservations r
            JOIN users u ON u.id = r.user_id
            JOIN stores s ON s.id = r.store_id
            LEFT JOIN dining_tables t ON t.id = r.table_id
            LEFT JOIN cats c ON c.id = r.recommended_cat_id
            WHERE r.id = #{id}
            """)
    Map<String, Object> getReservationDetail(@Param("id") Long id);

    /**
     * 手动选桌：仅按 id 锁定桌位行，门店一致/容量/available 状态由服务层逐项校验，
     * 以便区分“不存在(404)/门店不符(400)/容量不足或停用(409)”，避免笼统失败或 500。
     */
    @Select("""
            SELECT id, store_id, code, seats, area, cat_zone, status
            FROM dining_tables
            WHERE id = #{tableId}
            FOR UPDATE
            """)
    Map<String, Object> lockTableById(@Param("tableId") Long tableId);

    /** 改约选桌：锁定桌位行，校验门店、容量与 available 状态。 */
    @Select("""
            SELECT id, store_id, code, seats, area, cat_zone, status
            FROM dining_tables
            WHERE id = #{tableId}
              AND store_id = #{storeId}
              AND seats >= #{partySize}
              AND status = 'available'
            FOR UPDATE
            """)
    Map<String, Object> findTableByIdForUpdate(@Param("tableId") Long tableId,
                                               @Param("storeId") Long storeId,
                                               @Param("partySize") Integer partySize);

    /** 自动分配：SKIP LOCKED 选 available 桌位，优先猫区、容量最小满足、桌号稳定排序。 */
    @Select("""
            SELECT t.id, t.store_id, t.code, t.seats, t.area, t.cat_zone, t.status
            FROM dining_tables t
            WHERE t.store_id = #{storeId}
              AND t.seats >= #{partySize}
              AND t.status = 'available'
              AND NOT EXISTS (
                SELECT 1 FROM reservations r
                WHERE r.table_id = t.id
                  AND r.reservation_date = #{date}
                  AND r.reservation_time = #{time}
                  AND r.status IN ('created', 'booked', 'seated', 'dining')
              )
            ORDER BY t.cat_zone DESC, t.seats ASC, t.code ASC
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """)
    Map<String, Object> findAvailableTableForUpdate(@Param("storeId") Long storeId,
                                                     @Param("partySize") Integer partySize,
                                                     @Param("date") String date,
                                                     @Param("time") String time);

    @Select("""
            SELECT COUNT(*) FROM reservations
            WHERE table_id = #{tableId}
              AND reservation_date = #{date}
              AND reservation_time = #{time}
              AND status IN ('created', 'booked', 'seated', 'dining')
            """)
    int countActiveOnSlot(@Param("tableId") Long tableId,
                          @Param("date") String date,
                          @Param("time") String time);

    @Select("""
            SELECT COUNT(*) FROM reservations
            WHERE id <> #{reservationId}
              AND table_id = #{tableId}
              AND reservation_date = #{date}
              AND reservation_time = #{time}
              AND status IN ('created', 'booked', 'seated', 'dining')
            """)
    int countActiveOnSlotExcludingReservation(@Param("reservationId") Long reservationId,
                                              @Param("tableId") Long tableId,
                                              @Param("date") String date,
                                              @Param("time") String time);

    @Update("""
            UPDATE reservations
            SET store_id = #{storeId},
                table_id = #{tableId},
                recommended_cat_id = #{recommendedCatId},
                reservation_date = #{reservationDate},
                reservation_time = #{reservationTime},
                party_size = #{partySize},
                note = #{note}
            WHERE id = #{id}
            """)
    int rescheduleReservation(Reservation reservation);

    /** 预约状态变更事件（含 created_at），按时间正序返回，供事件审计接口使用。 */
    @Select("""
            SELECT id, reservation_id, from_status, to_status, actor_role, actor_user_id, note,
                   DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS created_at
            FROM reservation_events
            WHERE reservation_id = #{reservationId}
            ORDER BY created_at ASC, id ASC
            """)
    List<Map<String, Object>> listEvents(@Param("reservationId") Long reservationId);

    @Insert("""
            INSERT INTO operation_alerts (store_id, level, title, detail)
            VALUES (#{storeId}, 'info', '顾客已入座', CONCAT('预约 #', #{reservationId}, ' 已确认入座。'))
            """)
    void insertSeatedAlert(@Param("storeId") Long storeId, @Param("reservationId") Long reservationId);
}
