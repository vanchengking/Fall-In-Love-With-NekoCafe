package com.nekocafe.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface TableMapper {

    /**
     * FR-TABLE-001 时段可用性口径：
     * 占用时段的活跃预约状态仅为 created/booked/seated/dining（cancelled/finished/no_show 不占用）；
     * 桌位自身 status 必须为 available（维护/停用桌位一律 available_for_slot=false）；
     * date/time 都为空时不做时段判断、只看桌位状态，只传其一时按该字段保守判占用，避免误判为可用。
     */
    @Select("""
            SELECT t.id, t.store_id, t.code, t.seats, t.area, t.cat_zone, t.status,
                   t.photo_url, t.area_detail, t.device_note,
                   (t.status = 'available' AND (
                     (#{date} IS NULL AND #{time} IS NULL)
                     OR NOT EXISTS (
                       SELECT 1 FROM reservations r
                       WHERE r.table_id = t.id
                         AND (#{date} IS NULL OR r.reservation_date = #{date})
                         AND (#{time} IS NULL OR r.reservation_time = #{time})
                         AND r.status IN ('created', 'booked', 'seated', 'dining')
                     )
                   )) AS available_for_slot
            FROM dining_tables t
            WHERE (#{storeId} IS NULL OR t.store_id = #{storeId})
              AND (#{partySize} IS NULL OR t.seats >= #{partySize})
            ORDER BY t.store_id, t.seats, t.code
            """)
    List<Map<String, Object>> listTables(@Param("storeId") Long storeId,
                                         @Param("date") String date,
                                         @Param("time") String time,
                                         @Param("partySize") Integer partySize);

    /** 单桌详情，available_for_slot 口径与 {@link #listTables} 一致。 */
    @Select("""
            SELECT t.id, t.store_id, t.code, t.seats, t.area, t.cat_zone, t.status,
                   t.photo_url, t.area_detail, t.device_note,
                   (t.status = 'available' AND (
                     (#{date} IS NULL AND #{time} IS NULL)
                     OR NOT EXISTS (
                       SELECT 1 FROM reservations r
                       WHERE r.table_id = t.id
                         AND (#{date} IS NULL OR r.reservation_date = #{date})
                         AND (#{time} IS NULL OR r.reservation_time = #{time})
                         AND r.status IN ('created', 'booked', 'seated', 'dining')
                     )
                   )) AS available_for_slot
            FROM dining_tables t
            WHERE t.id = #{id}
            """)
    Map<String, Object> getTableDetail(@Param("id") Long id,
                                       @Param("date") String date,
                                       @Param("time") String time);

    @Insert("""
            INSERT INTO dining_tables (
              store_id, code, seats, area, cat_zone, status, photo_url, area_detail, device_note
            ) VALUES (
              #{store_id}, #{code}, #{seats}, #{area}, #{cat_zone}, #{status},
              #{photo_url}, #{area_detail}, #{device_note}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertTable(Map<String, Object> table);

    @Update("""
            UPDATE dining_tables
            SET store_id = #{store_id},
                code = #{code},
                seats = #{seats},
                area = #{area},
                cat_zone = #{cat_zone},
                status = #{status},
                photo_url = #{photo_url},
                area_detail = #{area_detail},
                device_note = #{device_note}
            WHERE id = #{id}
            """)
    int updateTable(Map<String, Object> table);

    @Delete("DELETE FROM dining_tables WHERE id = #{id}")
    int deleteTable(@Param("id") Long id);
}
