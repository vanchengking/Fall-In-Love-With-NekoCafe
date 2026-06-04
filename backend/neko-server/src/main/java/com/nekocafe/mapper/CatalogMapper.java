package com.nekocafe.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 只读 / 联表查询集合。返回 Map（列名为 snake_case），与前端既有数据契约一致。
 * 日期/时间统一在 SQL 中格式化为字符串，避免 JDBC 时间类型序列化歧义。
 */
@Mapper
public interface CatalogMapper {

    @Select("""
            SELECT s.id, s.name, s.city, s.address, s.phone,
                   DATE_FORMAT(s.open_time, '%H:%i') AS open_time,
                   DATE_FORMAT(s.close_time, '%H:%i') AS close_time,
                   s.latitude, s.longitude,
                   COUNT(t.id) AS table_count,
                   COALESCE(SUM(t.seats), 0) AS total_seats
            FROM stores s
            LEFT JOIN dining_tables t ON t.store_id = s.id
            GROUP BY s.id, s.name, s.city, s.address, s.phone, s.open_time, s.close_time, s.latitude, s.longitude
            ORDER BY s.id
            """)
    List<Map<String, Object>> listStores();

    @Select("""
            SELECT t.id, t.store_id, t.code, t.seats, t.area, t.cat_zone, t.status,
                   NOT EXISTS (
                     SELECT 1 FROM reservations r
                     WHERE r.table_id = t.id
                       AND (#{date} IS NULL OR r.reservation_date = #{date})
                       AND (#{time} IS NULL OR r.reservation_time = #{time})
                       AND r.status IN ('created', 'booked', 'seated', 'dining')
                   ) AS available_for_slot
            FROM dining_tables t
            WHERE (#{storeId} IS NULL OR t.store_id = #{storeId})
              AND (#{partySize} IS NULL OR t.seats >= #{partySize})
            ORDER BY t.store_id, t.seats, t.code
            """)
    List<Map<String, Object>> listTables(@Param("storeId") Long storeId,
                                         @Param("date") String date,
                                         @Param("time") String time,
                                         @Param("partySize") Integer partySize);

    @Select("""
            SELECT id, store_id, name, category, price_cents, tags, status
            FROM menu_items
            WHERE (#{storeId} IS NULL OR store_id = #{storeId})
            ORDER BY category, price_cents
            """)
    List<Map<String, Object>> listMenuItems(@Param("storeId") Long storeId);

    @Select("""
            SELECT id, store_id, name, breed, photo_url,
                   DATE_FORMAT(birthday, '%Y-%m-%d') AS birthday,
                   personality_tags, health_status, interactive_status, weight_kg,
                   DATE_FORMAT(last_vaccine_at, '%Y-%m-%d') AS last_vaccine_at
            FROM cats
            WHERE (#{storeId} IS NULL OR store_id = #{storeId})
            ORDER BY store_id, name
            """)
    List<Map<String, Object>> listCats(@Param("storeId") Long storeId);

    @Select("""
            SELECT id, cat_id,
                   DATE_FORMAT(recorded_at, '%Y-%m-%d %H:%i') AS recorded_at,
                   weight_kg, vaccine_note, interaction_note
            FROM cat_health_records
            WHERE (#{catId} IS NULL OR cat_id = #{catId})
            ORDER BY recorded_at DESC
            LIMIT 50
            """)
    List<Map<String, Object>> listCatHealthRecords(@Param("catId") Long catId);

    @Select("""
            SELECT id, cat_id, vaccine_name,
                   DATE_FORMAT(vaccinated_at, '%Y-%m-%d') AS vaccinated_at,
                   DATE_FORMAT(next_due_at, '%Y-%m-%d') AS next_due_at, note
            FROM vaccine_records
            WHERE (#{catId} IS NULL OR cat_id = #{catId})
            ORDER BY vaccinated_at DESC
            """)
    List<Map<String, Object>> listVaccineRecords(@Param("catId") Long catId);

    @Select("""
            SELECT r.id, r.user_id, r.store_id, r.table_id, r.recommended_cat_id,
                   DATE_FORMAT(r.reservation_date, '%Y-%m-%d') AS reservation_date,
                   DATE_FORMAT(r.reservation_time, '%H:%i') AS reservation_time,
                   r.party_size, r.status, r.note,
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

    @Select("""
            SELECT id, store_id, code, seats, area, cat_zone, status
            FROM dining_tables
            WHERE id = #{tableId} AND store_id = #{storeId} AND seats >= #{partySize}
            FOR UPDATE
            """)
    Map<String, Object> findTableByIdForUpdate(@Param("tableId") Long tableId,
                                              @Param("storeId") Long storeId,
                                              @Param("partySize") Integer partySize);

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

    @Select("<script>SELECT id, price_cents FROM menu_items WHERE id IN "
            + "<foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<Map<String, Object>> selectMenuByIds(@Param("ids") List<Long> ids);

    @Select("""
            SELECT o.id, o.reservation_id, o.user_id, o.store_id, o.status, o.payment_status, o.total_cents,
                   DATE_FORMAT(o.created_at, '%Y-%m-%d %H:%i') AS created_at,
                   u.name AS customer_name,
                   DATE_FORMAT(r.reservation_date, '%Y-%m-%d') AS reservation_date,
                   DATE_FORMAT(r.reservation_time, '%H:%i') AS reservation_time
            FROM orders o
            JOIN users u ON u.id = o.user_id
            LEFT JOIN reservations r ON r.id = o.reservation_id
            WHERE (#{storeId} IS NULL OR o.store_id = #{storeId})
            ORDER BY o.created_at DESC
            """)
    List<Map<String, Object>> listOrders(@Param("storeId") Long storeId);

    @Select("""
            SELECT
              SUM(CASE WHEN r.reservation_date = CURRENT_DATE THEN 1 ELSE 0 END) AS today_reservations,
              SUM(CASE WHEN r.status = 'seated' THEN 1 ELSE 0 END) AS seated_count,
              SUM(CASE WHEN r.status = 'dining' THEN 1 ELSE 0 END) AS dining_count,
              SUM(CASE WHEN r.status = 'finished' THEN 1 ELSE 0 END) AS finished_count,
              COUNT(DISTINCT r.user_id) AS unique_customers
            FROM reservations r
            WHERE (#{storeId} IS NULL OR r.store_id = #{storeId})
            """)
    Map<String, Object> dashboardSummary(@Param("storeId") Long storeId);

    @Select("""
            SELECT COALESCE(SUM(total_cents), 0) AS revenue_cents
            FROM orders
            WHERE (#{storeId} IS NULL OR store_id = #{storeId})
              AND payment_status = 'sandbox_paid'
            """)
    Map<String, Object> dashboardRevenue(@Param("storeId") Long storeId);

    @Select("""
            SELECT id, store_id, level, title, detail, resolved,
                   DATE_FORMAT(created_at, '%Y-%m-%d %H:%i') AS created_at
            FROM operation_alerts
            WHERE (#{storeId} IS NULL OR store_id = #{storeId})
              AND resolved = FALSE
            ORDER BY created_at DESC
            LIMIT 5
            """)
    List<Map<String, Object>> listAlerts(@Param("storeId") Long storeId);

    @org.apache.ibatis.annotations.Insert("""
            INSERT INTO operation_alerts (store_id, level, title, detail)
            VALUES (#{storeId}, 'info', '顾客已入座', CONCAT('预约 #', #{reservationId}, ' 已确认入座。'))
            """)
    void insertSeatedAlert(@Param("storeId") Long storeId, @Param("reservationId") Long reservationId);
}
