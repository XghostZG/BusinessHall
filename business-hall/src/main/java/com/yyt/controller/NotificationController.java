package com.yyt.controller;

import com.yyt.entity.User;
import com.yyt.mapper.UserMapper;
import com.yyt.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/notification")
public class NotificationController {
    
    @Resource
    private NotificationService notificationService;
    
    @Resource
    private UserMapper userMapper;
    
    // 获取当前登录用户的ID
    private Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        return user != null ? user.getId() : null;
    }
    
    @GetMapping("/list")
    public ResponseEntity<?> getNotificationList() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(notificationService.getUserNotifications(userId));
    }
    
    @PutMapping("/read")
    public ResponseEntity<?> markAsRead(@RequestParam Long notificationId) {
        Long userId = getCurrentUserId();
        notificationService.markAsRead(notificationId, userId);
        return ResponseEntity.ok("标记已读成功");
    }
    
    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead() {
        Long userId = getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok("标记所有已读成功");
    }
    
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long notificationId) {
        Long userId = getCurrentUserId();
        notificationService.deleteNotification(notificationId, userId);
        return ResponseEntity.ok("删除成功");
    }
    
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(notificationService.getUnreadCount(userId));
    }
}