package com.yyt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yyt.entity.Notification;
import com.yyt.entity.User;
import com.yyt.mapper.NotificationMapper;
import com.yyt.mapper.UserMapper;
import com.yyt.service.NotificationService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

    @Resource
    private SimpMessagingTemplate messagingTemplate;

    @Resource
    private UserMapper userMapper;

    @Override
    public void sendNotification(String title, String content, String type, Long relatedId, Long userId) {
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setRelatedId(relatedId);
        notification.setUserId(userId);
        notification.setIsRead(0); // 未读
        notification.setCreateTime(LocalDateTime.now());

        save(notification);
        sendWebSocketNotification(notification);
    }

    @Override
    public void sendNotificationToRole(String title, String content, String type, Long relatedId, String role) {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getRole, role)
                .eq(User::getStatus, 1));

        for (User user : users) {
            Notification notification = new Notification();
            notification.setTitle(title);
            notification.setContent(content);
            notification.setType(type);
            notification.setRelatedId(relatedId);
            notification.setUserId(user.getId());
            notification.setIsRead(0);
            notification.setCreateTime(LocalDateTime.now());

            save(notification);
            sendWebSocketNotification(notification);
        }
    }

    private void sendWebSocketNotification(Notification notification) {
        if (notification.getUserId() != null) {
            messagingTemplate.convertAndSendToUser(
                String.valueOf(notification.getUserId()),
                "/queue/notifications",
                notification
            );
        } else {
            messagingTemplate.convertAndSend("/topic/notifications", notification);
        }
    }

    @Override
    public List<Notification> getUserNotifications(Long userId) {
        return list(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreateTime)
                .last("LIMIT 50"));
    }

    @Override
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = getById(notificationId);
        if (notification != null && (notification.getUserId() == null || notification.getUserId().equals(userId))) {
            notification.setIsRead(1);
            updateById(notification);
        }
    }

    @Override
    public int getUnreadCount(Long userId) {
        long count = count(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0));
        return (int) count;
    }

    @Override
    public void markAllAsRead(Long userId) {
        Notification notification = new Notification();
        notification.setIsRead(1);
        update(notification, new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0));
    }

    @Override
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = getById(notificationId);
        if (notification != null && (notification.getUserId() == null || notification.getUserId().equals(userId))) {
            removeById(notificationId);
        }
    }
}
