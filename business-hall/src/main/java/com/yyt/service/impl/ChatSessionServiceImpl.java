package com.yyt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yyt.entity.ChatMessage;
import com.yyt.entity.ChatSession;
import com.yyt.entity.QuickReply;
import com.yyt.mapper.ChatMessageMapper;
import com.yyt.mapper.ChatSessionMapper;
import com.yyt.mapper.QuickReplyMapper;
import com.yyt.service.ChatSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession> implements ChatSessionService {

    @Autowired
    private ChatSessionMapper sessionMapper;

    @Autowired
    private ChatMessageMapper messageMapper;

    @Autowired
    private QuickReplyMapper quickReplyMapper;

    @Override
    public Map<String, Object> getOrCreateSession(Long userId, String userName) {
        // 首先查找用户是否有进行中的会话
        ChatSession existingSession = sessionMapper.selectOne(
                new QueryWrapper<ChatSession>()
                        .eq("user_id", userId)
                        .in("status", "waiting", "chatting")
                        .orderByDesc("create_time")
                        .last("LIMIT 1")
        );

        Map<String, Object> result = new HashMap<>();

        if (existingSession != null) {
            result.put("sessionId", existingSession.getId());
            result.put("sessionNo", existingSession.getSessionNo());
            result.put("status", existingSession.getStatus());
            result.put("staffId", existingSession.getStaffId());
            result.put("staffName", existingSession.getStaffName());
            result.put("isNew", false);
        } else {
            // 如果没有进行中的会话，查找最近5分钟内关闭的会话，允许恢复
            ChatSession recentClosedSession = sessionMapper.selectOne(
                    new QueryWrapper<ChatSession>()
                            .eq("user_id", userId)
                            .eq("status", "closed")
                            .ge("end_time", LocalDateTime.now().minusMinutes(5))
                            .orderByDesc("end_time")
                            .last("LIMIT 1")
            );

            if (recentClosedSession != null) {
                // 恢复最近关闭的会话
                // 如果会话有客服信息，设置为 chatting 状态，否则设置为 waiting
                String status = (recentClosedSession.getStaffId() != null) ? "chatting" : "waiting";
                recentClosedSession.setStatus(status);
                recentClosedSession.setEndTime(null);
                sessionMapper.updateById(recentClosedSession);

                result.put("sessionId", recentClosedSession.getId());
                result.put("sessionNo", recentClosedSession.getSessionNo());
                result.put("status", status);
                result.put("staffId", recentClosedSession.getStaffId());
                result.put("staffName", recentClosedSession.getStaffName());
                result.put("isNew", false);
            } else {
                // 创建新会话
                ChatSession newSession = new ChatSession();
                newSession.setSessionNo("LX" + System.currentTimeMillis());
                newSession.setUserId(userId);
                newSession.setUserName(userName);
                newSession.setStatus("waiting");
                newSession.setUnreadUser(0);
                newSession.setUnreadStaff(0);
                newSession.setStartTime(LocalDateTime.now());
                sessionMapper.insert(newSession);

                result.put("sessionId", newSession.getId());
                result.put("sessionNo", newSession.getSessionNo());
                result.put("status", newSession.getStatus());
                result.put("staffId", null);
                result.put("staffName", null);
                result.put("isNew", true);
            }
        }

        return result;
    }

    @Override
    public Map<String, Object> getUserCurrentSession(Long userId) {
        ChatSession session = sessionMapper.selectOne(
                new QueryWrapper<ChatSession>()
                        .eq("user_id", userId)
                        .in("status", "waiting", "chatting")
                        .orderByDesc("create_time")
                        .last("LIMIT 1")
        );

        if (session == null) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", session.getId());
        result.put("sessionNo", session.getSessionNo());
        result.put("status", session.getStatus());
        result.put("staffId", session.getStaffId());
        result.put("staffName", session.getStaffName());
        result.put("unreadCount", session.getUnreadUser());

        return result;
    }

    @Override
    public List<Map<String, Object>> getUserHistory(Long userId) {
        List<ChatSession> sessions = sessionMapper.selectList(
                new QueryWrapper<ChatSession>()
                        .eq("user_id", userId)
                        .orderByDesc("create_time")
                        .last("LIMIT 50")
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatSession session : sessions) {
            Map<String, Object> item = new HashMap<>();
            item.put("sessionId", session.getId());
            item.put("sessionNo", session.getSessionNo());
            item.put("status", session.getStatus());
            item.put("staffName", session.getStaffName());
            item.put("lastMessage", session.getLastMessage());
            item.put("lastMessageTime", session.getLastMessageTime());
            item.put("startTime", session.getStartTime());
            item.put("endTime", session.getEndTime());
            item.put("unreadCount", session.getUnreadUser());
            result.add(item);
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getStaffSessionList(Long staffId) {
        List<ChatSession> sessions = sessionMapper.selectList(
                new QueryWrapper<ChatSession>()
                        .eq("staff_id", staffId)
                        .orderByDesc("last_message_time")
                        .last("LIMIT 100")
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatSession session : sessions) {
            Map<String, Object> item = new HashMap<>();
            item.put("sessionId", session.getId());
            item.put("sessionNo", session.getSessionNo());
            item.put("userId", session.getUserId());
            item.put("userName", session.getUserName());
            item.put("status", session.getStatus());
            item.put("lastMessage", session.getLastMessage());
            item.put("lastMessageTime", session.getLastMessageTime());
            item.put("unreadCount", session.getUnreadStaff());
            item.put("startTime", session.getStartTime());
            result.add(item);
        }

        return result;
    }

    @Override
    public List<ChatMessage> getSessionMessages(Long sessionId) {
        return messageMapper.selectList(
                new QueryWrapper<ChatMessage>()
                        .eq("session_id", sessionId)
                        .orderByAsc("create_time")
        );
    }

    @Override
    public void markAsRead(Long sessionId, Long userId) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            return;
        }

        // 更新未读数
        if (session.getUserId().equals(userId)) {
            session.setUnreadUser(0);
        } else if (session.getStaffId() != null && session.getStaffId().equals(userId)) {
            session.setUnreadStaff(0);
        }
        sessionMapper.updateById(session);

        // 标记消息为已读
        messageMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .eq(ChatMessage::getSenderType, "user")
                        .set(ChatMessage::getIsRead, 1)
        );
    }

    @Override
    public boolean endSession(Long sessionId, Long userId) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            return false;
        }

        // 验证用户权限
        if (!session.getUserId().equals(userId) && !session.getStaffId().equals(userId)) {
            return false;
        }

        session.setStatus("closed");
        session.setEndTime(LocalDateTime.now());
        sessionMapper.updateById(session);

        // 插入系统消息
        ChatMessage endMsg = new ChatMessage();
        endMsg.setSessionId(sessionId);
        endMsg.setSenderType("system");
        endMsg.setSenderId(0L);
        endMsg.setSenderName("系统");
        endMsg.setContent("会话已结束");
        endMsg.setMessageType("text");
        endMsg.setIsRead(0);
        endMsg.setCreateTime(LocalDateTime.now());  // 手动设置创建时间
        messageMapper.insert(endMsg);

        return true;
    }

    @Override
    public int getUnreadCount(Long userId) {
        // 统计用户所有会话的未读数
        List<ChatSession> sessions = sessionMapper.selectList(
                new QueryWrapper<ChatSession>()
                        .eq("user_id", userId)
                        .in("status", "chatting")
        );

        int totalUnread = 0;
        for (ChatSession session : sessions) {
            totalUnread += session.getUnreadUser();
        }

        return totalUnread;
    }

    @Override
    public List<QuickReply> getQuickReplies() {
        return quickReplyMapper.selectList(
                new QueryWrapper<QuickReply>()
                        .eq("is_active", 1)
                        .orderByAsc("sort_order")
        );
    }

    @Override
    public Map<String, Object> getSessionStatistics(Long staffId) {
        Map<String, Object> stats = new HashMap<>();

        // 今日会话数
        long todayCount = sessionMapper.selectCount(
                new QueryWrapper<ChatSession>()
                        .eq("staff_id", staffId)
                        .ge("create_time", LocalDateTime.now().withHour(0).withMinute(0).withSecond(0))
        );

        // 进行中会话数
        long chattingCount = sessionMapper.selectCount(
                new QueryWrapper<ChatSession>()
                        .eq("staff_id", staffId)
                        .eq("status", "chatting")
        );

        // 已完成会话数
        long closedCount = sessionMapper.selectCount(
                new QueryWrapper<ChatSession>()
                        .eq("staff_id", staffId)
                        .eq("status", "closed")
        );

        stats.put("todayCount", todayCount);
        stats.put("chattingCount", chattingCount);
        stats.put("closedCount", closedCount);

        return stats;
    }
}
