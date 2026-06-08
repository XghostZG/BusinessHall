package com.yyt.websocket;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yyt.entity.ChatMessage;
import com.yyt.entity.ChatSession;
import com.yyt.entity.User;
import com.yyt.mapper.ChatMessageMapper;
import com.yyt.mapper.ChatSessionMapper;
import com.yyt.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 用户ID -> Session 映射
    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    // SessionID -> 用户ID 映射
    private final Map<String, Long> sessionUserMap = new ConcurrentHashMap<>();
    // 用户ID -> 当前会话ID
    private final Map<Long, Long> userSessionMap = new ConcurrentHashMap<>();
    // 用户ID -> 角色
    private final Map<Long, String> userRoles = new ConcurrentHashMap<>();
    // clerk 断开重连保护：用户ID -> 断开时间
    private final Map<Long, Long> clerkReconnectTime = new ConcurrentHashMap<>();
    // 重连保护时间（毫秒）：5秒内重新连接视为重连
    private static final long RECONNECT_PROTECTION_MS = 5000;

    @Autowired
    private ChatSessionMapper sessionMapper;

    @Autowired
    private ChatMessageMapper messageMapper;

    @Autowired
    private UserMapper userMapper;
    
    private ScheduledExecutorService scheduler;
    
    @PostConstruct
    public void init() {
        // 启动定时任务，每秒检查超时的 clerk 重连保护
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                cleanupExpiredReconnectProtection();
            } catch (Exception e) {
                log.error("清理过期重连保护失败", e);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }
    
    @PreDestroy
    public void destroy() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
    
    /**
     * 清理超时的重连保护，超过保护期后 clerk 未重连，关闭其会话
     */
    private void cleanupExpiredReconnectProtection() {
        long now = System.currentTimeMillis();
        clerkReconnectTime.entrySet().removeIf(entry -> {
            Long userId = entry.getKey();
            Long disconnectTime = entry.getValue();
            if (now - disconnectTime >= RECONNECT_PROTECTION_MS) {
                log.info("Clerk 重连超时，关闭会话: userId={}", userId);
                // clerk 真正离线，关闭会话
                closeSessionForClerk(userId);
                userRoles.remove(userId);
                return true;
            }
            return false;
        });
    }
    
    /**
     * 关闭 clerk 的会话
     */
    private void closeSessionForClerk(Long clerkId) {
        Long chatSessionId = userSessionMap.get(clerkId);
        if (chatSessionId != null) {
            ChatSession chatSession = sessionMapper.selectById(chatSessionId);
            if (chatSession != null && "chatting".equals(chatSession.getStatus())) {
                // 查找用户并发送通知
                Long userId = chatSession.getUserId();
                WebSocketSession userSession = userSessions.get(userId);
                if (userSession != null && userSession.isOpen()) {
                    Map<String, Object> endMsg = new HashMap<>();
                    endMsg.put("type", "session_ended");
                    endMsg.put("content", "客服已离线，会话结束");
                    endMsg.put("time", LocalDateTime.now().toString());
                    try {
                        userSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(endMsg)));
                    } catch (IOException e) {
                        log.error("发送会话结束消息失败", e);
                    }
                }
                // 更新会话状态
                chatSession.setStatus("closed");
                chatSession.setEndTime(LocalDateTime.now());
                sessionMapper.updateById(chatSession);
            }
            userSessionMap.remove(clerkId);
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String query = session.getUri().getQuery();
        Long userId = getUserIdFromQuery(query);
        String userName = getUserNameFromQuery(query);
        String role = getRoleFromQuery(query);

        // 调试日志：打印连接请求详情
        log.debug("【WebSocket调试】接收到连接请求 - query: {}, userId: {}, userName: {}, role: {}", 
            query, userId, userName, role);

        if (userId == null) {
            log.warn("【WebSocket调试】连接失败 - 用户ID为空，关闭连接");
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        // 检查是否是 clerk 重连（5秒内）
        boolean isReconnect = false;
        if ("clerk".equals(role)) {
            Long disconnectTime = clerkReconnectTime.remove(userId);
            if (disconnectTime != null && System.currentTimeMillis() - disconnectTime < RECONNECT_PROTECTION_MS) {
                isReconnect = true;
                log.info("【WebSocket调试】Clerk 在{}秒内重连，恢复会话: userId={}", RECONNECT_PROTECTION_MS / 1000, userId);
            }
        }

        // 存储session和角色
        userSessions.put(userId, session);
        sessionUserMap.put(session.getId(), userId);
        userRoles.put(userId, role);

        // 调试日志：打印连接建立详情
        log.info("【WebSocket调试】连接建立成功 - userId={}, userName={}, role={}, isReconnect={}, sessionId={}", 
            userId, userName, role, isReconnect, session.getId());
        
        // 调试日志：打印当前在线用户数
        log.debug("【WebSocket调试】当前在线用户数: userSessions.size={}, userSessionMap.size={}", 
            userSessions.size(), userSessionMap.size());

        try {
            // 根据角色发送欢迎消息和处理
            if ("user".equals(role)) {
                // 用户连接：发送欢迎消息并分配客服
                Map<String, Object> welcomeMsg = new HashMap<>();
                welcomeMsg.put("type", "system");
                welcomeMsg.put("content", "欢迎连接客服系统，正在为您分配客服...");
                welcomeMsg.put("time", LocalDateTime.now().toString());
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(welcomeMsg)));
                
                allocateStaffForUser(userId, userName);
            } else if ("clerk".equals(role)) {
                // 营业员连接：发送欢迎消息
                Map<String, Object> welcomeMsg = new HashMap<>();
                welcomeMsg.put("type", "system");
                welcomeMsg.put("content", isReconnect ? "会话已恢复" : "客服系统已连接，等待分配客户...");
                welcomeMsg.put("time", LocalDateTime.now().toString());
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(welcomeMsg)));
                
                // 营业员上线后，尝试将自己分配给等待的用户
                // 包括新连接和重连情况
                User staff = new User();
                staff.setId(userId);
                staff.setUsername(userName);
                allocateSelfToWaitingUsers(staff);
            }
        } catch (Exception e) {
            log.error("处理连接建立消息失败: {}", e.getMessage(), e);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = sessionUserMap.get(session.getId());
        if (userId == null) {
            return;
        }

        try {
            Map<String, Object> data = objectMapper.readValue(message.getPayload(), Map.class);
            String msgType = (String) data.get("type");

            switch (msgType) {
                case "chat":
                    handleChatMessage(userId, data);
                    break;
                case "read":
                    handleReadMessage(userId, data);
                    break;
                case "ping":
                    // 心跳响应
                    Map<String, Object> pong = new HashMap<>();
                    pong.put("type", "pong");
                    pong.put("time", LocalDateTime.now().toString());
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(pong)));
                    break;
                case "end_session":
                    handleEndSession(userId);
                    break;
            }
        } catch (Exception e) {
            log.error("处理消息失败: {}", e.getMessage(), e);
            // 不关闭连接，只是记录错误
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = sessionUserMap.get(session.getId());
        if (userId != null) {
            userSessions.remove(session.getId());
            sessionUserMap.remove(session.getId());

            // 检查是否是 clerk，暂时不关闭会话，等待重连
            Long chatSessionId = userSessionMap.get(userId);
            String role = userRoles.get(userId);
            boolean isClerk = "clerk".equals(role);
            
            if (isClerk && chatSessionId != null) {
                // clerk 断开，记录重连保护时间
                clerkReconnectTime.put(userId, System.currentTimeMillis());
                log.info("Clerk WebSocket连接关闭，进入{}秒重连保护期: userId={}", RECONNECT_PROTECTION_MS / 1000, userId);
                // 移除 clerk 的 session，但保留 userSessionMap 映射以支持重连
                userSessions.remove(userId);
            } else {
                // 非 clerk（用户）断开，通知对方会话结束
                if (chatSessionId != null) {
                    ChatSession chatSession = sessionMapper.selectById(chatSessionId);
                    if (chatSession != null && "chatting".equals(chatSession.getStatus())) {
                        // 查找对方并发送通知
                        Long otherUserId = chatSession.getUserId().equals(userId)
                                ? chatSession.getStaffId() : chatSession.getUserId();
                        WebSocketSession otherSession = userSessions.get(otherUserId);
                        if (otherSession != null && otherSession.isOpen()) {
                            Map<String, Object> endMsg = new HashMap<>();
                            endMsg.put("type", "session_ended");
                            endMsg.put("content", "对方已离开会话");
                            endMsg.put("time", LocalDateTime.now().toString());
                            otherSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(endMsg)));
                        }

                        // 更新会话状态
                        chatSession.setStatus("closed");
                        chatSession.setEndTime(LocalDateTime.now());
                        sessionMapper.updateById(chatSession);
                    }
                    // 只移除用户的会话映射，不要移除营业员的映射
                    userSessionMap.remove(userId);
                    // 移除用户的 session
                    userSessions.remove(userId);
                } else {
                    // 没有活跃会话的用户断开，直接移除 session
                    userSessions.remove(userId);
                }
            }

            log.info("WebSocket连接关闭: userId={}", userId);
        }
    }

    /**
     * 处理聊天消息
     */
    @SuppressWarnings("unchecked")
    private void handleChatMessage(Long senderId, Map<String, Object> data) {
        Long sessionId = ((Number) data.get("sessionId")).longValue();
        String content = (String) data.get("content");
        String senderType = (String) data.get("senderType");
        String senderName = (String) data.get("senderName");

        if (sessionId == null || content == null || content.trim().isEmpty()) {
            return;
        }

        ChatSession chatSession = sessionMapper.selectById(sessionId);
        if (chatSession == null) {
            return;
        }

        // 保存消息到数据库
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSessionId(sessionId);
        chatMessage.setSenderType(senderType);
        chatMessage.setSenderId(senderId);
        chatMessage.setSenderName(senderName);
        chatMessage.setContent(content);
        chatMessage.setMessageType("text");
        chatMessage.setIsRead(0);
        chatMessage.setCreateTime(LocalDateTime.now());  // 手动设置创建时间
        messageMapper.insert(chatMessage);

        // 更新会话最后消息
        chatSession.setLastMessage(content);
        chatSession.setLastMessageTime(LocalDateTime.now());
        sessionMapper.updateById(chatSession);

        // 构造转发消息
        Map<String, Object> msgData = new HashMap<>();
        msgData.put("type", "chat");
        msgData.put("messageId", chatMessage.getId());
        msgData.put("sessionId", sessionId);
        msgData.put("senderType", senderType);
        msgData.put("senderId", senderId);
        msgData.put("senderName", senderName);
        msgData.put("content", content);
        msgData.put("time", chatMessage.getCreateTime().toString());

        // 发送给会话双方
        try {
            String msgJson = objectMapper.writeValueAsString(msgData);
            sendToUser(chatSession.getUserId(), msgJson);
            if (chatSession.getStaffId() != null) {
                sendToUser(chatSession.getStaffId(), msgJson);
            }
        } catch (IOException e) {
            log.error("发送消息失败", e);
        }
    }

    /**
     * 处理已读消息
     */
    private void handleReadMessage(Long userId, Map<String, Object> data) {
        Long sessionId = ((Number) data.get("sessionId")).longValue();
        if (sessionId == null) {
            return;
        }

        ChatSession chatSession = sessionMapper.selectById(sessionId);
        if (chatSession == null) {
            return;
        }

        // 更新未读数
        if (chatSession.getUserId().equals(userId)) {
            chatSession.setUnreadStaff(0);
        } else if (chatSession.getStaffId() != null && chatSession.getStaffId().equals(userId)) {
            chatSession.setUnreadUser(0);
        }
        sessionMapper.updateById(chatSession);

        // 通知对方
        Map<String, Object> readData = new HashMap<>();
        readData.put("type", "read_ack");
        readData.put("sessionId", sessionId);

        Long otherUserId = chatSession.getUserId().equals(userId)
                ? chatSession.getStaffId() : chatSession.getUserId();
        try {
            sendToUser(otherUserId, objectMapper.writeValueAsString(readData));
        } catch (IOException e) {
            log.error("发送已读确认失败", e);
        }
    }

    /**
     * 结束会话
     */
    private void handleEndSession(Long userId) {
        Long sessionId = userSessionMap.get(userId);
        if (sessionId == null) {
            return;
        }

        ChatSession chatSession = sessionMapper.selectById(sessionId);
        if (chatSession != null && "chatting".equals(chatSession.getStatus())) {
            chatSession.setStatus("closed");
            chatSession.setEndTime(LocalDateTime.now());
            sessionMapper.updateById(chatSession);

            // 通知对方
            Map<String, Object> endData = new HashMap<>();
            endData.put("type", "session_closed");
            endData.put("content", "会话已结束，感谢您的咨询");
            endData.put("time", LocalDateTime.now().toString());

            try {
                String endJson = objectMapper.writeValueAsString(endData);
                sendToUser(chatSession.getUserId(), endJson);
                if (chatSession.getStaffId() != null) {
                    sendToUser(chatSession.getStaffId(), endJson);
                }
            } catch (IOException e) {
                log.error("发送结束消息失败", e);
            }
        }

        // 只移除用户的会话映射，不要移除营业员的映射
        // 营业员的映射只在营业员断开连接时才移除
        userSessionMap.remove(userId);
    }

    /**
     * 为用户分配客服
     */
    private void allocateStaffForUser(Long userId, String userName) {
        // 调试日志：打印分配开始
        log.info("【客服分配调试】开始为用户分配客服 - userId={}, userName={}", userId, userName);
        
        // 检查用户是否已有进行中的会话
        var existingSession = sessionMapper.selectList(
            new QueryWrapper<ChatSession>()
                .eq("user_id", userId)
                .in("status", "waiting", "chatting")
                .orderByDesc("create_time")
                .last("LIMIT 1")
        );
        
        // 调试日志：打印查询结果
        log.debug("【客服分配调试】查询用户已有会话结果 - existingSession.size={}", existingSession.size());
        
        if (!existingSession.isEmpty()) {
            ChatSession session = existingSession.get(0);
            userSessionMap.put(userId, session.getId());
            
            // 调试日志：打印已有会话详情
            log.debug("【客服分配调试】发现已有会话 - sessionId={}, status={}, staffId={}, staffName={}", 
                session.getId(), session.getStatus(), session.getStaffId(), session.getStaffName());
            
            if ("chatting".equals(session.getStatus()) && session.getStaffId() != null) {
                // 已有进行中的会话，通知用户
                Map<String, Object> allocMsg = new HashMap<>();
                allocMsg.put("type", "allocated");
                allocMsg.put("sessionId", session.getId());
                allocMsg.put("staffName", session.getStaffName());
                allocMsg.put("content", "您已有进行中的会话，客服 " + session.getStaffName() + " 继续为您服务");
                allocMsg.put("time", LocalDateTime.now().toString());
                sendToUser(userId, toJson(allocMsg));
                
                // 将客服也加入会话映射
                userSessionMap.put(session.getStaffId(), session.getId());
                
                log.info("【客服分配调试】用户 {} 恢复已有会话 {} - 客服: {}", userId, session.getId(), session.getStaffName());
            } else if ("waiting".equals(session.getStatus())) {
                // 等待中的会话，通知用户
                Map<String, Object> waitMsg = new HashMap<>();
                waitMsg.put("type", "waiting");
                waitMsg.put("sessionId", session.getId());
                waitMsg.put("content", "您已在排队等待中，请稍候...");
                waitMsg.put("time", LocalDateTime.now().toString());
                sendToUser(userId, toJson(waitMsg));
                
                log.info("【客服分配调试】用户 {} 恢复等待会话 {}", userId, session.getId());
            }
            return;
        }
        
        // 查找空闲的营业员
        User staff = findAvailableStaff();
        if (staff != null) {
            // 创建新会话并分配客服
            ChatSession chatSession = new ChatSession();
            chatSession.setSessionNo("LX" + System.currentTimeMillis());
            chatSession.setUserId(userId);
            chatSession.setUserName(userName);
            chatSession.setStaffId(staff.getId());
            chatSession.setStaffName(staff.getUsername());
            chatSession.setStatus("chatting");
            chatSession.setUnreadUser(0);
            chatSession.setUnreadStaff(0);
            chatSession.setStartTime(LocalDateTime.now());
            sessionMapper.insert(chatSession);

            userSessionMap.put(userId, chatSession.getId());
            userSessionMap.put(staff.getId(), chatSession.getId());

            // 通知用户
            Map<String, Object> allocMsg = new HashMap<>();
            allocMsg.put("type", "allocated");
            allocMsg.put("sessionId", chatSession.getId());
            allocMsg.put("staffName", staff.getUsername());
            allocMsg.put("content", "已为您分配客服 " + staff.getUsername() + "，请开始咨询");
            allocMsg.put("time", LocalDateTime.now().toString());
            sendToUser(userId, toJson(allocMsg));

            // 通知客服
            Map<String, Object> staffMsg = new HashMap<>();
            staffMsg.put("type", "new_customer");
            staffMsg.put("sessionId", chatSession.getId());
            staffMsg.put("userId", userId);
            staffMsg.put("userName", userName);
            staffMsg.put("content", "新客户 " + userName + " 接入会话");
            staffMsg.put("time", LocalDateTime.now().toString());
            sendToUser(staff.getId(), toJson(staffMsg));

            log.info("为用户 {} 分配客服 {}", userId, staff.getId());
        } else {
            // 没有空闲客服，创建等待中的会话
            ChatSession chatSession = new ChatSession();
            chatSession.setSessionNo("LX" + System.currentTimeMillis());
            chatSession.setUserId(userId);
            chatSession.setUserName(userName);
            chatSession.setStatus("waiting");
            chatSession.setUnreadUser(0);
            chatSession.setUnreadStaff(0);
            chatSession.setStartTime(LocalDateTime.now());
            sessionMapper.insert(chatSession);

            userSessionMap.put(userId, chatSession.getId());

            // 通知用户等待
            Map<String, Object> waitMsg = new HashMap<>();
            waitMsg.put("type", "waiting");
            waitMsg.put("sessionId", chatSession.getId());
            waitMsg.put("content", "当前无空闲客服，您已进入排队等待，请稍候...");
            waitMsg.put("time", LocalDateTime.now().toString());
            sendToUser(userId, toJson(waitMsg));

            log.info("用户 {} 进入等待队列", userId);
        }
    }

    /**
     * 查找可用的营业员（优先选择当前会话数最少的客服）
     * 如果所有客服都有会话，返回最不忙的那个（让用户排队等待）
     */
    private User findAvailableStaff() {
        // 调试日志：开始查找可用客服
        log.debug("【客服分配调试】开始查找可用营业员");
        
        var staffList = userMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                .eq("role", "clerk")
        );
        
        // 调试日志：打印查询到的客服列表
        log.debug("【客服分配调试】查询到的营业员列表 - 总数: {}", staffList.size());
        for (User staff : staffList) {
            log.debug("【客服分配调试】营业员 - id={}, name={}", staff.getId(), staff.getUsername());
        }
        
        if (staffList.isEmpty()) {
            log.warn("【客服分配调试】未找到任何营业员");
            return null;
        }
        
        User selectedStaff = null;
        int minActiveSessions = Integer.MAX_VALUE;
        boolean hasOnlineStaff = false;
        
        for (User staff : staffList) {
            boolean isOnline = userSessions.containsKey(staff.getId());
            log.debug("【客服分配调试】检查营业员 - id={}, name={}, 在线状态={}", 
                staff.getId(), staff.getUsername(), isOnline);
            
            if (!isOnline) {
                log.debug("【客服分配调试】营业员 {} 不在线，跳过", staff.getId());
                continue;
            }
            
            hasOnlineStaff = true;
            
            int activeSessions = 0;
            var sessions = sessionMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ChatSession>()
                    .eq("staff_id", staff.getId())
                    .in("status", "chatting")
            );
            activeSessions = sessions.size();
            
            log.debug("【客服分配调试】营业员 {} 当前活跃会话数: {}", staff.getId(), activeSessions);
            
            if (activeSessions < minActiveSessions) {
                minActiveSessions = activeSessions;
                selectedStaff = staff;
                log.debug("【客服分配调试】更新最优客服 - id={}, name={}, 活跃会话数={}", 
                    selectedStaff.getId(), selectedStaff.getUsername(), minActiveSessions);
            }
        }
        
        // 调试日志：打印最终选择结果
        if (selectedStaff != null) {
            log.info("【客服分配调试】选择营业员 - id={}, name={}, 活跃会话数={}", 
                selectedStaff.getId(), selectedStaff.getUsername(), minActiveSessions);
        } else if (hasOnlineStaff) {
            log.warn("【客服分配调试】有在线客服但未选择到合适的（可能所有客服都有会话）");
        } else {
            log.warn("【客服分配调试】无在线客服");
        }
        
        // 如果有在线客服但所有客服都有会话，返回最不忙的那个
        // 让用户进入排队等待状态，而不是返回null导致无法匹配
        return hasOnlineStaff ? selectedStaff : null;
    }

    /**
     * 检查等待队列
     */
    private void checkWaitingUsers() {
        var waitingSessions = sessionMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ChatSession>()
                        .eq("status", "waiting")
                        .orderByAsc("create_time")
        );

        User staff = findAvailableStaff();
        if (staff == null || waitingSessions.isEmpty()) {
            return;
        }

        ChatSession waitingSession = waitingSessions.get(0);
        allocateStaffToSession(waitingSession, staff);
    }

    /**
     * 营业员上线时，将自己分配给等待的用户
     */
    private void allocateSelfToWaitingUsers(User staff) {
        var waitingSessions = sessionMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ChatSession>()
                        .eq("status", "waiting")
                        .orderByAsc("create_time")
                        .last("LIMIT 1")
        );

        if (waitingSessions.isEmpty()) {
            return;
        }

        allocateStaffToSession(waitingSessions.get(0), staff);
    }

    /**
     * 分配客服到指定会话
     */
    private void allocateStaffToSession(ChatSession chatSession, User staff) {
        chatSession.setStaffId(staff.getId());
        chatSession.setStaffName(staff.getUsername());
        chatSession.setStatus("chatting");
        sessionMapper.updateById(chatSession);

        userSessionMap.put(chatSession.getUserId(), chatSession.getId());
        userSessionMap.put(staff.getId(), chatSession.getId());

        // 通知用户
        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("type", "allocated");
        userMsg.put("sessionId", chatSession.getId());
        userMsg.put("staffName", staff.getUsername());
        userMsg.put("content", "已为您分配客服 " + staff.getUsername() + "，请开始咨询");
        userMsg.put("time", LocalDateTime.now().toString());
        sendToUser(chatSession.getUserId(), toJson(userMsg));

        // 通知客服
        Map<String, Object> staffMsg = new HashMap<>();
        staffMsg.put("type", "new_customer");
        staffMsg.put("sessionId", chatSession.getId());
        staffMsg.put("userId", chatSession.getUserId());
        staffMsg.put("userName", chatSession.getUserName());
        staffMsg.put("content", "新客户 " + chatSession.getUserName() + " 接入会话");
        staffMsg.put("time", LocalDateTime.now().toString());
        sendToUser(staff.getId(), toJson(staffMsg));

        // 发送系统消息给会话
        sendSystemMessage(chatSession.getId(), "客服 " + staff.getUsername() + " 已接入");

        log.info("会话 {} 分配客服 {}", chatSession.getId(), staff.getId());
    }

    /**
     * 广播客服上线
     */
    private void broadcastStaffOnline(Long staffId, String staffName) {
        // 通知等待的用户有新客服
        var waitingSessions = sessionMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ChatSession>()
                        .eq("status", "waiting")
                        .orderByAsc("create_time")
        );

        if (!waitingSessions.isEmpty()) {
            User staff = userMapper.selectById(staffId);
            allocateStaffToSession(waitingSessions.get(0), staff);
        }
    }

    /**
     * 发送系统消息
     */
    private void sendSystemMessage(Long sessionId, String content) {
        ChatMessage sysMsg = new ChatMessage();
        sysMsg.setSessionId(sessionId);
        sysMsg.setSenderType("system");
        sysMsg.setSenderId(0L);
        sysMsg.setSenderName("系统");
        sysMsg.setContent(content);
        sysMsg.setMessageType("text");
        sysMsg.setIsRead(0);
        sysMsg.setCreateTime(LocalDateTime.now());  // 手动设置创建时间
        messageMapper.insert(sysMsg);

        ChatSession chatSession = sessionMapper.selectById(sessionId);
        if (chatSession != null) {
            Map<String, Object> msgData = new HashMap<>();
            msgData.put("type", "system");
            msgData.put("messageId", sysMsg.getId());
            msgData.put("sessionId", sessionId);
            msgData.put("senderType", "system");
            msgData.put("senderName", "系统");
            msgData.put("content", content);
            msgData.put("time", sysMsg.getCreateTime().toString());

            sendToUser(chatSession.getUserId(), toJson(msgData));
            if (chatSession.getStaffId() != null) {
                sendToUser(chatSession.getStaffId(), toJson(msgData));
            }
        }
    }

    /**
     * 发送消息给指定用户
     */
    public void sendToUser(Long userId, String message) {
        // 调试日志：打印发送前的状态
        log.debug("【消息发送调试】准备发送消息 - userId={}, userSessions.containsKey={}", 
            userId, userSessions.containsKey(userId));
        
        WebSocketSession session = userSessions.get(userId);
        if (session == null) {
            log.warn("【消息发送调试】发送失败 - 用户session不存在, userId={}", userId);
            return;
        }
        
        boolean isOpen = session.isOpen();
        log.debug("【消息发送调试】检查session状态 - isOpen={}", isOpen);
        
        if (!isOpen) {
            log.warn("【消息发送调试】发送失败 - 用户session未开放, userId={}", userId);
            return;
        }
        
        // 解析消息类型用于日志
        String messageType = "unknown";
        try {
            com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(message);
            if (jsonNode.has("type")) {
                messageType = jsonNode.get("type").asText();
            }
        } catch (Exception e) {
            // 解析失败不影响发送
        }
        
        try {
            session.sendMessage(new TextMessage(message));
            log.info("【消息发送调试】发送成功 - userId={}, messageType={}", userId, messageType);
        } catch (IOException e) {
            log.error("【消息发送调试】发送失败 - userId={}, messageType={}, error={}", 
                userId, messageType, e.getMessage(), e);
        }
    }

    /**
     * Map转JSON字符串
     */
    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (IOException e) {
            log.error("JSON转换失败", e);
            return "{}";
        }
    }

    /**
     * 从查询字符串获取用户ID
     */
    private Long getUserIdFromQuery(String query) {
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] kv = param.split("=");
            if ("userId".equals(kv[0]) && kv.length > 1) {
                return Long.parseLong(kv[1]);
            }
        }
        return null;
    }

    /**
     * 从查询字符串获取用户名
     */
    private String getUserNameFromQuery(String query) {
        if (query == null) return "";
        for (String param : query.split("&")) {
            String[] kv = param.split("=");
            if ("userName".equals(kv[0]) && kv.length > 1) {
                return kv[1];
            }
        }
        return "";
    }

    /**
     * 从查询字符串获取角色
     */
    private String getRoleFromQuery(String query) {
        if (query == null) return "user";
        for (String param : query.split("&")) {
            String[] kv = param.split("=");
            if ("role".equals(kv[0]) && kv.length > 1) {
                return kv[1];
            }
        }
        return "user";
    }
}
