package com.yyt.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yyt.entity.User;
import com.yyt.mapper.UserMapper;
import com.yyt.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private UserMapper userMapper;

    /**
     * 打卡
     */
    @PostMapping("/checkin")
    public Map<String, Object> checkIn(
            @RequestParam String type,
            @RequestParam(required = false) String remark,
            Authentication authentication) {
        Map<String, Object> result = new HashMap<>();
        Long userId = getCurrentUserId(authentication);
        Map<String, Object> checkResult = attendanceService.checkIn(userId, type, remark);
        result.put("code", (Boolean) checkResult.get("success") ? 200 : 500);
        result.put("message", checkResult.get("message"));
        result.put("data", checkResult.get("data"));
        return result;
    }

    /**
     * 获取我的考勤记录
     */
    @GetMapping("/my")
    public Map<String, Object> getMyAttendance(
            @RequestParam String month,
            Authentication authentication) {
        Map<String, Object> result = new HashMap<>();
        Long userId = getCurrentUserId(authentication);
        List<?> records = attendanceService.getMyAttendance(userId, month);
        result.put("code", 200);
        result.put("data", records);
        return result;
    }

    /**
     * 获取考勤统计
     */
    @GetMapping("/stats")
    public Map<String, Object> getStats(
            @RequestParam String month,
            Authentication authentication) {
        Map<String, Object> result = new HashMap<>();
        Long userId = getCurrentUserId(authentication);
        Map<String, Object> stats = attendanceService.getStats(userId, month);
        result.put("code", 200);
        result.put("data", stats);
        return result;
    }

    /**
     * 获取今日考勤状态
     */
    @GetMapping("/today")
    public Map<String, Object> getToday(Authentication authentication) {
        Map<String, Object> result = new HashMap<>();
        Long userId = getCurrentUserId(authentication);
        Object attendance = attendanceService.getTodayAttendance(userId);
        result.put("code", 200);
        result.put("data", attendance);
        return result;
    }

    /**
     * 获取所有营业员考勤（管理员）
     */
    @GetMapping("/all")
    public Map<String, Object> getAllAttendance(@RequestParam String month) {
        Map<String, Object> result = new HashMap<>();
        List<?> records = attendanceService.getAllAttendance(month);
        result.put("code", 200);
        result.put("data", records);
        return result;
    }

    private Long getCurrentUserId(Authentication authentication) {
        if (authentication != null) {
            String username = authentication.getName();
            User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, username));
            if (user != null) {
                return user.getId();
            }
        }
        return null;
    }
}
