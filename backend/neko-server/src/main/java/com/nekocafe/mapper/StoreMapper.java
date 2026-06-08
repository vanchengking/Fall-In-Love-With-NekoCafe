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
public interface StoreMapper {

    @Select("""
            SELECT s.id, s.name, s.city, s.address, s.phone,
                   DATE_FORMAT(s.open_time, '%H:%i') AS open_time,
                   DATE_FORMAT(s.close_time, '%H:%i') AS close_time,
                   s.latitude, s.longitude,
                   s.business_hours_text, s.photo_url, s.equipment_desc, s.area_detail,
                   COUNT(t.id) AS table_count,
                   COALESCE(SUM(t.seats), 0) AS total_seats
            FROM stores s
            LEFT JOIN dining_tables t ON t.store_id = s.id
            GROUP BY s.id, s.name, s.city, s.address, s.phone, s.open_time, s.close_time,
                     s.latitude, s.longitude, s.business_hours_text, s.photo_url, s.equipment_desc, s.area_detail
            ORDER BY s.id
            """)
    List<Map<String, Object>> listStores();

    @Select("""
            SELECT s.id, s.name, s.city, s.address, s.phone,
                   DATE_FORMAT(s.open_time, '%H:%i') AS open_time,
                   DATE_FORMAT(s.close_time, '%H:%i') AS close_time,
                   s.latitude, s.longitude,
                   s.business_hours_text, s.photo_url, s.equipment_desc, s.area_detail,
                   (SELECT COUNT(*) FROM dining_tables t WHERE t.store_id = s.id) AS table_count,
                   (SELECT COALESCE(SUM(t.seats), 0) FROM dining_tables t WHERE t.store_id = s.id) AS total_seats,
                   (SELECT COUNT(*) FROM cats c WHERE c.store_id = s.id) AS cat_count
            FROM stores s
            WHERE s.id = #{id}
            """)
    Map<String, Object> getStoreDetail(@Param("id") Long id);

    @Insert("""
            INSERT INTO stores (
              name, city, address, phone, open_time, close_time, latitude, longitude,
              business_hours_text, photo_url, equipment_desc, area_detail
            ) VALUES (
              #{name}, #{city}, #{address}, #{phone},
              STR_TO_DATE(#{open_time}, '%H:%i'),
              STR_TO_DATE(#{close_time}, '%H:%i'),
              #{latitude}, #{longitude},
              #{business_hours_text}, #{photo_url}, #{equipment_desc}, #{area_detail}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertStore(Map<String, Object> store);

    @Update("""
            UPDATE stores
            SET name = #{name},
                city = #{city},
                address = #{address},
                phone = #{phone},
                open_time = STR_TO_DATE(#{open_time}, '%H:%i'),
                close_time = STR_TO_DATE(#{close_time}, '%H:%i'),
                latitude = #{latitude},
                longitude = #{longitude},
                business_hours_text = #{business_hours_text},
                photo_url = #{photo_url},
                equipment_desc = #{equipment_desc},
                area_detail = #{area_detail}
            WHERE id = #{id}
            """)
    int updateStore(Map<String, Object> store);

    @Delete("DELETE FROM stores WHERE id = #{id}")
    int deleteStore(@Param("id") Long id);
}
