package com.yyt.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yyt.entity.Notification;

import java.util.List;

public interface NotificationService extends IService<Notification> {
    void sendNotification(String title, String content, String type, Long relatedId, Long userId);
    void sendNotificationToRole(String title, String content, String type, Long relatedId, String role);
    List<Notification> getUserNotifications(Long userId);
    void markAsRead(Long notificationId, Long userId);
    void markAllAsRead(Long userId);
    int getUnreadCount(Long userId);
    void deleteNotification(Long notificationId, Long userId);
}