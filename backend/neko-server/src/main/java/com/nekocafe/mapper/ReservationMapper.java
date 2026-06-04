package com.nekocafe.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nekocafe.entity.Reservation;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ReservationMapper extends BaseMapper<Reservation> {

    /** 行锁读取预约，用于状态机更新时防止并发。 */
    @Select("SELECT * FROM reservations WHERE id = #{id} FOR UPDATE")
    Reservation selectForUpdate(@Param("id") Long id);
}
