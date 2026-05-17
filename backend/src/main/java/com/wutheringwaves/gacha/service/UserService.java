package com.wutheringwaves.gacha.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wutheringwaves.gacha.mapper.UserMapper;
import com.wutheringwaves.gacha.model.User;
import com.wutheringwaves.gacha.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public Map<String, Object> register(String username, String password) {
        Map<String, Object> result = new HashMap<>();

        // 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        if (userMapper.selectOne(wrapper) != null) {
            result.put("success", false);
            result.put("message", "用户名已存在");
            return result;
        }

        // 创建新用户
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setStarlight(100000);  // 初始星声
        user.setRole("user");
        userMapper.insert(user);

        // 生成 JWT
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        result.put("success", true);
        result.put("message", "注册成功");
        result.put("token", token);
        result.put("user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "role", user.getRole(),
                "starlight", user.getStarlight()
        ));

        return result;
    }

    public Map<String, Object> login(String username, String password) {
        Map<String, Object> result = new HashMap<>();

        // 查找用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);

        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            result.put("success", false);
            result.put("message", "用户名或密码错误");
            return result;
        }

        // 生成 JWT
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        result.put("success", true);
        result.put("message", "登录成功");
        result.put("token", token);
        result.put("user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "role", user.getRole(),
                "starlight", user.getStarlight()
        ));

        return result;
    }

    public User getUserById(Long userId) {
        return userMapper.selectById(userId);
    }

    public void updateStarlight(Long userId, int delta) {
        userMapper.updateStarlight(userId, delta);
    }

    public void updateSelectedWeaponUp(Long userId, Long weaponId) {
        userMapper.updateSelectedWeaponUp(userId, weaponId);
    }

    public Long getSelectedWeaponUp(Long userId) {
        User user = getUserById(userId);
        return user != null ? user.getSelectedStandardWeaponUp() : null;
    }
}
