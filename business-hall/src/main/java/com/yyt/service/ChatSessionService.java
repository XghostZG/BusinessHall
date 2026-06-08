package com.yyt.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yyt.entity.ChatMessage;
import com.yyt.entity.ChatSession;
import com.yyt.entity.QuickReply;

import java.util.List;
import java.util.Map;

public interface ChatSessionService extends IService<ChatSession> {

    // 创建或获取会话
    Map<String, Object> getOrCreateSession(Long userId, String userName);

    // 获取用户当前会话
    Map<String, Object> getUserCurrentSession(Long userId);

    // 获取用户历史会话列表
    List<Map<String, Object>> getUserHistory(Long userId);

    // 获取客服会话列表
    List<Map<String, Object>> getStaffSessionList(Long staffId);

    // 获取会话消息历史
    List<ChatMessage> getSessionMessages(Long sessionId);

    // 标记消息已读
    void markAsRead(Long sessionId, Long userId);

    // 结束会话
    boolean endSession(Long sessionId, Long userId);

    // 获取会话未读数
    int getUnreadCount(Long userId);

    // 获取快捷回复列表
    List<QuickReply> getQuickReplies();

    // 获取会话统计
    Map<String, Object> getSessionStatistics(Long staffId);
}
