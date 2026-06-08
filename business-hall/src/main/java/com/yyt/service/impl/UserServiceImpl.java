package com.yyt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yyt.entity.User;
import com.yyt.mapper.UserMapper;
import com.yyt.service.UserService;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public User login(String username, String password) {
        User user = getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user != null) {
            // 检查密码是否匹配
            if (passwordEncoder.matches(password, user.getPassword())) {
                return user;
            }
        }
        return null;
    }

    @Override
    @Transactional
    public boolean register(User user) {
        // 检查用户名是否已存在
        if (getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername())) != null) {
            return false;
        }
        // 强制设置角色为user，防止前端篡改
        user.setRole("user");
        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        return save(user);
    }

    @Override
    public boolean updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = getById(userId);
        if (user != null && passwordEncoder.matches(oldPassword, user.getPassword())) {
            user.setPassword(passwordEncoder.encode(newPassword));
            return updateById(user);
        }
        return false;
    }

    @Override
    @Transactional
    public Map<String, String> addClerk(String realName, String phone) {
        // 用户名：手机号
        // 密码：姓名首字母小写 + 手机号后4位
        String pinyinInitials = getPinyinInitials(realName);
        String phoneSuffix = phone.substring(phone.length() - 4);
        String password = pinyinInitials + phoneSuffix;

        // 创建用户
        User user = new User();
        user.setUsername(phone);
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(realName);
        user.setPhone(phone);
        user.setRole("clerk");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());

        if (save(user)) {
            Map<String, String> result = new HashMap<>();
            result.put("username", phone);
            result.put("password", password);
            return result;
        }
        return null;
    }

    // 使用pinyin4j获取汉字拼音首字母
    private String getPinyinInitials(String name) {
        if (name == null || name.isEmpty()) {
            return "emp";
        }
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        
        StringBuilder sb = new StringBuilder();
        for (char c : name.toCharArray()) {
            try {
                String[] pinyins = PinyinHelper.toHanyuPinyinStringArray(c, format);
                if (pinyins != null && pinyins.length > 0) {
                    sb.append(pinyins[0].charAt(0));
                } else {
                    sb.append(c);
                }
            } catch (BadHanyuPinyinOutputFormatCombination e) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    @Override
    @Transactional
    public boolean updateRole(Long userId, String role) {
        User user = getById(userId);
        if (user != null) {
            user.setRole(role);
            return updateById(user);
        }
        return false;
    }

    @Override
    public User getUserByUsername(String username) {
        return getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }
}