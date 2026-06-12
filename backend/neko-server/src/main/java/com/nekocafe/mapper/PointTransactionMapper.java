package com.nekocafe.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nekocafe.entity.PointTransaction;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface PointTransactionMapper extends BaseMapper<PointTransaction> {

    /** 用户积分明细，最新变更在前。 */
    @Select("""
            SELECT id, user_id, delta, balance_after, source_type, source_id, reason,
                   DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS created_at
            FROM point_transactions
            WHERE user_id = #{userId}
            ORDER BY created_at DESC, id DESC
            """)
    List<Map<String, Object>> listByUser(@Param("userId") Long userId);
}
