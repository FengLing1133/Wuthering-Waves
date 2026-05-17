package com.wutheringwaves.gacha.controller;

import com.wutheringwaves.gacha.service.GachaService;
import com.wutheringwaves.gacha.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/gacha")
@RequiredArgsConstructor
public class GachaController {

    private final GachaService gachaService;
    private final JwtUtil jwtUtil;

    private String normalizePoolType(String poolType) {
        if (poolType == null) return null;
        if (poolType.contains("character")) return "character";
        if (poolType.contains("weapon")) return "weapon";
        if (poolType.equals("limited")) return "limited";
        return poolType;
    }

    @PostMapping("/pull")
    public ResponseEntity<Map<String, Object>> pull(
            Authentication authentication,
            @RequestBody Map<String, Object> request) {

        Long userId = (Long) authentication.getPrincipal();
        String poolType = (String) request.get("poolType");
        Integer count = (Integer) request.get("count");

        if (poolType == null || count == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "参数错误"));
        }

        poolType = normalizePoolType(poolType);

        if (!poolType.equals("character") && !poolType.equals("weapon") && !poolType.equals("limited")) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "无效的池子类型"));
        }

        if (count != 1 && count != 10) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "抽卡次数只能是1或10"));
        }

        return ResponseEntity.ok(gachaService.pull(userId, poolType, count));
    }

    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "character") String poolType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        poolType = normalizePoolType(poolType);

        if (!poolType.equals("character") && !poolType.equals("weapon") && !poolType.equals("limited")) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "无效的池子类型"));
        }

        Long userId = (Long) authentication.getPrincipal();

        Map<String, Object> historyData = gachaService.getHistory(userId, poolType, page, size);
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("success", true);
        response.putAll(historyData);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(
            Authentication authentication,
            @RequestParam(defaultValue = "character") String poolType) {

        poolType = normalizePoolType(poolType);

        if (!poolType.equals("character") && !poolType.equals("weapon") && !poolType.equals("limited")) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "无效的池子类型"));
        }

        Long userId = (Long) authentication.getPrincipal();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "stats", gachaService.getStats(userId, poolType)
        ));
    }
}
