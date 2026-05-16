package com.wutheringwaves.gacha.controller;

import com.wutheringwaves.gacha.service.UserService;
import com.wutheringwaves.gacha.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "用户名和密码不能为空"));
        }

        if (username.length() < 3 || username.length() > 20) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "用户名长度需在3-20之间"));
        }

        if (password.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "密码长度不能少于6位"));
        }

        return ResponseEntity.ok(userService.register(username, password));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "用户名和密码不能为空"));
        }

        return ResponseEntity.ok(userService.login(username, password));
    }

    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> getUserInfo(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            Long userId = getUserIdFromToken(token);
            var user = userService.getUserById(userId);

            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("success", false, "message", "用户不存在"));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "user", Map.of(
                            "id", user.getId(),
                            "username", user.getUsername(),
                            "starlight", user.getStarlight(),
                            "starshards", user.getStarshards()
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "认证失败"));
        }
    }

    private Long getUserIdFromToken(String token) {
        return jwtUtil.getUserIdFromToken(token);
    }
}
