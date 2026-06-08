package com.yyt.controller;

import com.yyt.entity.ChatMessage;
import com.yyt.entity.ChatSession;
import com.yyt.entity.QuickReply;
import com.yyt.service.ChatSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatSessionController {

    @Autowired
    private ChatSessionService chatSessionService;

    /**
     * 创建或获取会话
     */
    @GetMapping("/session")
    public ResponseEntity<Map<String, Object>> getOrCreateSession(
            @RequestParam Long userId,
            @RequestParam String userName) {
        Map<String, Object> result = chatSessionService.getOrCreateSession(userId, userName);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取用户当前会话
     */
    @GetMapping("/session/current")
    public ResponseEntity<Map<String, Object>> getCurrentSession(@RequestParam Long userId) {
        Map<String, Object> result = chatSessionService.getUserCurrentSession(userId);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取用户历史会话列表
     */
    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> getUserHistory(@RequestParam Long userId) {
        List<Map<String, Object>> result = chatSessionService.getUserHistory(userId);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取客服会话列表
     */
    @GetMapping("/staff/list")
    public ResponseEntity<List<Map<String, Object>>> getStaffSessionList(@RequestParam Long staffId) {
        List<Map<String, Object>> result = chatSessionService.getStaffSessionList(staffId);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取会话消息历史
     */
    @GetMapping("/messages")
    public ResponseEntity<List<ChatMessage>> getMessages(@RequestParam Long sessionId) {
        List<ChatMessage> messages = chatSessionService.getSessionMessages(sessionId);
        return ResponseEntity.ok(messages);
    }

    /**
     * 标记消息已读
     */
    @PostMapping("/read")
    public ResponseEntity<?> markAsRead(
            @RequestParam Long sessionId,
            @RequestParam Long userId) {
        chatSessionService.markAsRead(sessionId, userId);
        return ResponseEntity.ok("标记成功");
    }

    /**
     * 结束会话
     */
    @PostMapping("/end")
    public ResponseEntity<?> endSession(
            @RequestParam Long sessionId,
            @RequestParam Long userId) {
        boolean success = chatSessionService.endSession(sessionId, userId);
        if (success) {
            return ResponseEntity.ok("会话已结束");
        }
        return ResponseEntity.badRequest().body("结束会话失败");
    }

    /**
     * 获取未读消息数
     */
    @GetMapping("/unread")
    public ResponseEntity<Integer> getUnreadCount(@RequestParam Long userId) {
        int count = chatSessionService.getUnreadCount(userId);
        return ResponseEntity.ok(count);
    }

    /**
     * 获取快捷回复列表
     */
    @GetMapping("/quick-replies")
    public ResponseEntity<List<QuickReply>> getQuickReplies() {
        List<QuickReply> replies = chatSessionService.getQuickReplies();
        return ResponseEntity.ok(replies);
    }

    /**
     * 获取会话统计
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics(@RequestParam Long staffId) {
        Map<String, Object> stats = chatSessionService.getSessionStatistics(staffId);
        return ResponseEntity.ok(stats);
    }

    /**
     * 获取会话详情
     */
    @GetMapping("/session/detail")
    public ResponseEntity<?> getSessionDetail(@RequestParam Long sessionId) {
        ChatSession session = chatSessionService.getById(sessionId);
        if (session != null) {
            return ResponseEntity.ok(session);
        }
        return ResponseEntity.notFound().build();
    }
}
