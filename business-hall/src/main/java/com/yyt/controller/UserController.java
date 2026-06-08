package com.yyt.controller;

import com.yyt.entity.User;
import com.yyt.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    public UserController(UserService userService, AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginData, HttpServletRequest request) {
        String username = loginData.get("username");
        String password = loginData.get("password");

        try {
            // 使用Spring Security进行认证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
    
            // 将认证信息存入SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 创建Session
            HttpSession session = request.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            // 获取用户信息
            User user = userService.login(username, password);
            
            // 清除密码后再返回
            if (user != null) {
                user.setPassword(null);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("user", user);
            response.put("message", "登录成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("用户名或密码错误");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        // 清除SecurityContext
        SecurityContextHolder.clearContext();

        // 使Session失效
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        return ResponseEntity.ok("退出登录成功");
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (userService.register(user)) {
            return ResponseEntity.ok("注册成功");
        } else {
            return ResponseEntity.badRequest().body("用户名已存在");
        }
    }

    @PutMapping("/password")
    public ResponseEntity<?> updatePassword(@RequestParam Long userId,
                                           @RequestParam String oldPassword,
                                           @RequestParam String newPassword) {
        if (userService.updatePassword(userId, oldPassword, newPassword)) {
            return ResponseEntity.ok("密码更新成功");
        } else {
            return ResponseEntity.badRequest().body("原密码错误");
        }
    }

    @GetMapping("/info")
    public ResponseEntity<?> getUserInfo(@RequestParam Long userId) {
        User user = userService.getById(userId);
        if (user != null) {
            // 清除密码后再返回
            user.setPassword(null);
            return ResponseEntity.ok(user);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/list")
    public ResponseEntity<?> getUserList() {
        List<User> users = userService.list();
        // 清除所有用户的密码
        users.forEach(user -> user.setPassword(null));
        return ResponseEntity.ok(users);
    }

    @PostMapping("/save")
    public ResponseEntity<?> saveUser(@RequestBody User user) {
        if (userService.saveOrUpdate(user)) {
            return ResponseEntity.ok("保存成功");
        } else {
            return ResponseEntity.badRequest().body("保存失败");
        }
    }

    /**
     * 用户更新个人信息（只能修改自己的信息）
     */
    @PutMapping("/update")
    public ResponseEntity<?> updateUserProfile(@RequestBody Map<String, Object> updateData) {
        try {
            Long userId = Long.valueOf(updateData.get("id").toString());
            
            // 获取当前登录用户信息进行验证
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(401).body("未登录");
            }
            
            String currentUsername = auth.getName();
            User currentUser = userService.getUserByUsername(currentUsername);
            
            // 验证用户只能修改自己的信息
            if (currentUser == null || !currentUser.getId().equals(userId)) {
                return ResponseEntity.status(403).body("无权修改此用户信息");
            }
            
            // 更新用户信息
            User user = userService.getById(userId);
            if (user != null) {
                if (updateData.containsKey("realName")) {
                    user.setRealName((String) updateData.get("realName"));
                }
                if (updateData.containsKey("phone")) {
                    user.setPhone((String) updateData.get("phone"));
                }
                if (updateData.containsKey("email")) {
                    user.setEmail((String) updateData.get("email"));
                }
                
                if (userService.updateById(user)) {
                    user.setPassword(null); // 清除密码后返回
                    Map<String, Object> result = new HashMap<>();
                    result.put("message", "更新成功");
                    result.put("user", user);
                    return ResponseEntity.ok(result);
                }
            }
            return ResponseEntity.badRequest().body("更新失败");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("参数错误");
        }
    }

    @PutMapping("/status")
    public ResponseEntity<?> updateUserStatus(@RequestParam Long userId, @RequestParam Integer status) {
        User user = userService.getById(userId);
        if (user != null) {
            user.setStatus(status);
            if (userService.updateById(user)) {
                return ResponseEntity.ok("状态更新成功");
            }
        }
        return ResponseEntity.badRequest().body("状态更新失败");
    }

    @GetMapping("/count")
    public ResponseEntity<?> getUserCount() {
        return ResponseEntity.ok(userService.count());
    }

    @PostMapping("/addClerk")
    public ResponseEntity<?> addClerk(@RequestBody Map<String, String> data) {
        String realName = data.get("realName");
        String phone = data.get("phone");

        if (realName == null || phone == null) {
            return ResponseEntity.badRequest().body("参数不完整");
        }

        Map<String, String> result = userService.addClerk(realName, phone);
        if (result != null) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body("添加营业员失败");
    }

    @PutMapping("/role")
    public ResponseEntity<?> updateUserRole(@RequestParam Long userId, @RequestParam String role) {
        if (userService.updateRole(userId, role)) {
            return ResponseEntity.ok("角色更新成功");
        }
        return ResponseEntity.badRequest().body("角色更新失败");
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteUser(@RequestParam Long userId) {
        if (userService.removeById(userId)) {
            return ResponseEntity.ok("删除成功");
        }
        return ResponseEntity.badRequest().body("删除失败");
    }
}
