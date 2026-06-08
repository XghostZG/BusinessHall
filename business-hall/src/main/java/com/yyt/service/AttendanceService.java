package com.yyt.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yyt.entity.Attendance;
import java.util.List;
import java.util.Map;

public interface AttendanceService extends IService<Attendance> {

    /**
     * 打卡
     * @param userId 用户ID
     * @param type checkin-上班 checkout-下班
     * @param remark 备注
     * @return 打卡结果
     */
    Map<String, Object> checkIn(Long userId, String type, String remark);

    /**
     * 获取我的考勤记录
     * @param userId 用户ID
     * @param month 年月，格式：yyyy-MM
     * @return 考勤列表
     */
    List<Attendance> getMyAttendance(Long userId, String month);

    /**
     * 获取考勤统计
     * @param userId 用户ID
     * @param month 年月
     * @return 统计数据
     */
    Map<String, Object> getStats(Long userId, String month);

    /**
     * 获取今日考勤状态
     * @param userId 用户ID
     * @return 今日考勤
     */
    Attendance getTodayAttendance(Long userId);

    /**
     * 获取营业员考勤列表（管理员）
     * @param month 年月
     * @return 所有营业员考勤
     */
    List<Map<String, Object>> getAllAttendance(String month);
}
