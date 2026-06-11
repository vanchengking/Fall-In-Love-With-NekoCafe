package com.nekocafe.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DashboardMapper {

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

    @Select("""
            SELECT DATE_FORMAT(r.reservation_date, '%Y-%m-%d') AS biz_date,
                   COUNT(*) AS reservation_count,
                   SUM(CASE WHEN r.status = 'finished' THEN 1 ELSE 0 END) AS finished_count,
                   SUM(CASE WHEN r.status IN ('created', 'booked', 'seated', 'dining') THEN 1 ELSE 0 END) AS active_count
            FROM reservations r
            WHERE r.reservation_date >= DATE_SUB(CURRENT_DATE, INTERVAL #{daysBack} DAY)
              AND (#{storeId} IS NULL OR r.store_id = #{storeId})
            GROUP BY r.reservation_date
            ORDER BY r.reservation_date
            """)
    List<Map<String, Object>> reservationTrend(@Param("storeId") Long storeId,
                                               @Param("daysBack") int daysBack);

    @Select("""
            SELECT DATE_FORMAT(DATE(o.created_at), '%Y-%m-%d') AS biz_date,
                   COUNT(*) AS order_count,
                   COALESCE(SUM(o.total_cents), 0) AS revenue_cents
            FROM orders o
            WHERE DATE(o.created_at) >= DATE_SUB(CURRENT_DATE, INTERVAL #{daysBack} DAY)
              AND o.payment_status = 'sandbox_paid'
              AND (#{storeId} IS NULL OR o.store_id = #{storeId})
            GROUP BY DATE(o.created_at)
            ORDER BY DATE(o.created_at)
            """)
    List<Map<String, Object>> revenueTrend(@Param("storeId") Long storeId,
                                           @Param("daysBack") int daysBack);

    @Select("""
            SELECT s.id AS store_id,
                   s.name AS store_name,
                   COALESCE(tb.table_count, 0) AS table_count,
                   COALESCE(tb.total_seats, 0) AS total_seats,
                   COALESCE(rs.today_reservations, 0) AS today_reservations,
                   COALESCE(rs.finished_count, 0) AS finished_count,
                   COALESCE(rv.revenue_cents, 0) AS revenue_cents
            FROM stores s
            LEFT JOIN (
              SELECT store_id, COUNT(*) AS table_count, COALESCE(SUM(seats), 0) AS total_seats
              FROM dining_tables
              GROUP BY store_id
            ) tb ON tb.store_id = s.id
            LEFT JOIN (
              SELECT store_id,
                     COUNT(*) AS today_reservations,
                     SUM(CASE WHEN status = 'finished' THEN 1 ELSE 0 END) AS finished_count
              FROM reservations
              WHERE reservation_date = CURRENT_DATE
              GROUP BY store_id
            ) rs ON rs.store_id = s.id
            LEFT JOIN (
              SELECT store_id, COALESCE(SUM(total_cents), 0) AS revenue_cents
              FROM orders
              WHERE payment_status = 'sandbox_paid'
                AND DATE(created_at) = CURRENT_DATE
              GROUP BY store_id
            ) rv ON rv.store_id = s.id
            WHERE (#{storeId} IS NULL OR s.id = #{storeId})
            ORDER BY revenue_cents DESC, s.id
            """)
    List<Map<String, Object>> storeOverview(@Param("storeId") Long storeId);
}
