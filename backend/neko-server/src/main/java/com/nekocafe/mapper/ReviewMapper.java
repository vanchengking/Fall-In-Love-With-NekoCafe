package com.nekocafe.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nekocafe.entity.ReviewEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface ReviewMapper extends BaseMapper<ReviewEntity> {

    @Select("""
        SELECT r.*
        FROM reviews r
        WHERE r.store_id = #{storeId}
        ORDER BY r.created_at DESC
        LIMIT #{limit}
        """)
    List<Map<String, Object>> selectByStoreId(@Param("storeId") Long storeId, @Param("limit") Integer limit);

    @Select("""
        SELECT r.*, s.name AS store_name
        FROM reviews r
        LEFT JOIN stores s ON r.store_id = s.id
        WHERE r.user_id = #{userId}
        ORDER BY r.created_at DESC
        """)
    List<ReviewEntity> selectByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM reviews WHERE id = #{id} AND user_id = #{userId}")
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
