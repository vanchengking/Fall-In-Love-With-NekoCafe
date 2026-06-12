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

    // 门店/桌位查询统一走 StoreMapper/TableMapper（含 V005 扩展字段），此处不再保留旧版查询，
    // 避免公开列表与管理列表出现两套字段契约。

    @Select("""
            SELECT id, store_id, name, category, price_cents, tags, status, photo_url
            FROM menu_items
            WHERE (#{storeId} IS NULL OR store_id = #{storeId})
              AND status = 'available'
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
              AND payment_status IN ('paid', 'sandbox_paid')
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

    @org.apache.ibatis.annotations.Update("UPDATE cats SET photo_url = #{photoUrl} WHERE id = #{id}")
    void updateCatPhoto(@Param("id") Long id, @Param("photoUrl") String photoUrl);

    @org.apache.ibatis.annotations.Update("UPDATE menu_items SET photo_url = #{photoUrl} WHERE id = #{id}")
    void updateMenuItemPhoto(@Param("id") Long id, @Param("photoUrl") String photoUrl);

    @Select("""
            SELECT o.id, o.user_id, o.store_id, o.status, o.payment_status,
                   o.total_cents, o.original_total_cents, o.discount_rate,
                   DATE_FORMAT(o.created_at, '%Y-%m-%d %H:%i') AS created_at,
                   u.name AS customer_name,
                   r.reservation_date, r.reservation_time
            FROM orders o
            JOIN users u ON u.id = o.user_id
            LEFT JOIN reservations r ON r.id = o.reservation_id
            WHERE o.id = #{orderId}
            """)
    Map<String, Object> getOrderDetail(@Param("orderId") Long orderId);

    @Select("""
            SELECT oi.id, oi.menu_item_id, oi.quantity, oi.unit_price_cents,
                   mi.name AS menu_item_name
            FROM order_items oi
            JOIN menu_items mi ON mi.id = oi.menu_item_id
            WHERE oi.order_id = #{orderId}
            ORDER BY oi.id
            """)
    List<Map<String, Object>> getOrderItems(@Param("orderId") Long orderId);
}
