package com.yyt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yyt.entity.Appointment;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface AppointmentMapper extends BaseMapper<Appointment> {

    @Select("SELECT COUNT(*) FROM appointment WHERE verification_code = #{code} AND DATE(create_time) = #{date}")
    int countByVerificationCodeAndDate(@Param("code") String code, @Param("date") String date);
}