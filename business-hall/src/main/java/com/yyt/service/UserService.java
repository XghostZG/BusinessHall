package com.yyt.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yyt.entity.User;

import java.util.Map;

public interface UserService extends IService<User> {
    User login(String username, String password);
    boolean register(User user);
    boolean updatePassword(Long userId, String oldPassword, String newPassword);
    Map<String, String> addClerk(String realName, String phone);
    boolean updateRole(Long userId, String role);
    User getUserByUsername(String username);
}