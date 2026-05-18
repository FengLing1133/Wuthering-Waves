package com.wutheringwaves.gacha.controller;

import com.wutheringwaves.gacha.service.AdminService;
import com.wutheringwaves.gacha.service.GachaService;
import com.wutheringwaves.gacha.service.UserService;
import com.wutheringwaves.gacha.model.GachaItem;
import com.wutheringwaves.gacha.model.GachaPool;
import com.wutheringwaves.gacha.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/gacha")
@RequiredArgsConstructor
public class GachaController {

    private final GachaService gachaService;
    private final AdminService adminService;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    private static final Set<String> VALID_POOL_TYPES = Set.of(
            "limited-character", "limited-weapon", "standard-character", "standard-weapon"
    );

    private String normalizePoolType(String poolType) {
        if (poolType == null) return null;
        String lower = poolType.toLowerCase();
        // 常驻池优先匹配
        if (lower.equals("standard-character") || lower.equals("standard_character")) return "standard-character";
        if (lower.equals("standard-weapon") || lower.equals("standard_weapon")) return "standard-weapon";
        // 限定角色池：character-1, character-2, limited-character 等
        if (lower.contains("character")) return "limited-character";
        // 限定武器池：weapon-1, weapon-2, limited-weapon 等
        if (lower.contains("weapon")) return "limited-weapon";
        return poolType;
    }

    @PostMapping("/pull")
    public ResponseEntity<Map<String, Object>> pull(
            Authentication authentication,
            @RequestBody Map<String, Object> request) {

        Long userId = (Long) authentication.getPrincipal();
        String poolType = (String) request.get("poolType");
        Integer count = (Integer) request.get("count");
        Number poolIdNum = (Number) request.get("poolId");

        if (poolType == null || count == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "参数错误"));
        }

        poolType = normalizePoolType(poolType);

        if (!VALID_POOL_TYPES.contains(poolType)) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "无效的池子类型"));
        }

        if (count != 1 && count != 10) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "抽卡次数只能是1或10"));
        }

        Long poolId = poolIdNum != null ? poolIdNum.longValue() : null;

        return ResponseEntity.ok(gachaService.pull(userId, poolType, poolId, count));
    }

    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getHistory(
            Authentication authentication,
            @RequestParam(defaultValue = "limited-character") String poolType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        poolType = normalizePoolType(poolType);

        if (!VALID_POOL_TYPES.contains(poolType)) {
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
            @RequestParam(defaultValue = "limited-character") String poolType) {

        poolType = normalizePoolType(poolType);

        if (!VALID_POOL_TYPES.contains(poolType)) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "无效的池子类型"));
        }

        Long userId = (Long) authentication.getPrincipal();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "stats", gachaService.getStats(userId, poolType)
        ));
    }

    // ========== 公开卡池查询（无需认证） ==========

    @GetMapping("/pools")
    public ResponseEntity<Map<String, Object>> listActivePools() {
        List<GachaPool> pools = adminService.listPools(null, "active");
        List<Map<String, Object>> visiblePools = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();
        for (GachaPool pool : pools) {
            // 检查是否侧栏可见
            if (!Boolean.TRUE.equals(pool.getSidebarVisible())) continue;
            // 检查时间范围
            if (pool.getStartTime() != null && pool.getStartTime().isAfter(now)) continue;
            if (pool.getEndTime() != null && pool.getEndTime().isBefore(now)) continue;

            Map<String, Object> poolData = new HashMap<>();
            poolData.put("id", pool.getId());
            poolData.put("name", pool.getName());
            poolData.put("poolType", pool.getPoolType());
            poolData.put("description", pool.getDescription());
            poolData.put("bgImageUrl", pool.getBgImageUrl());
            poolData.put("thumbnailUrl", pool.getThumbnailUrl());
            poolData.put("upItems", adminService.getPoolUpItems(pool.getId()));
            poolData.put("fiveStarRate", pool.getFiveStarRate());
            poolData.put("fourStarRate", pool.getFourStarRate());
            poolData.put("maxPity", pool.getMaxPity());
            poolData.put("softPityStart", pool.getSoftPityStart());
            poolData.put("softPityIncrement", pool.getSoftPityIncrement());
            poolData.put("startTime", pool.getStartTime());
            poolData.put("endTime", pool.getEndTime());
            poolData.put("sidebarOrder", pool.getSidebarOrder());

            // 关联的四星UP物品
            poolData.put("fourStarAvatars", adminService.getPoolFourStarItems(pool.getId()));

            visiblePools.add(poolData);
        }

        // 按 sidebarOrder 排序
        visiblePools.sort((a, b) -> {
            int orderA = a.get("sidebarOrder") != null ? (int) a.get("sidebarOrder") : 0;
            int orderB = b.get("sidebarOrder") != null ? (int) b.get("sidebarOrder") : 0;
            return Integer.compare(orderA, orderB);
        });

        return ResponseEntity.ok(Map.of("success", true, "pools", visiblePools));
    }

    @GetMapping("/pools/{id}")
    public ResponseEntity<Map<String, Object>> getPoolDetail(@PathVariable Long id) {
        GachaPool pool = adminService.getPoolById(id);
        if (pool == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "卡池不存在"));
        }

        Map<String, Object> poolData = new HashMap<>();
        poolData.put("id", pool.getId());
        poolData.put("name", pool.getName());
        poolData.put("poolType", pool.getPoolType());
        poolData.put("description", pool.getDescription());
        poolData.put("bgImageUrl", pool.getBgImageUrl());
        poolData.put("thumbnailUrl", pool.getThumbnailUrl());
        poolData.put("upItems", adminService.getPoolUpItems(pool.getId()));
        poolData.put("fiveStarRate", pool.getFiveStarRate());
        poolData.put("fourStarRate", pool.getFourStarRate());
        poolData.put("maxPity", pool.getMaxPity());
        poolData.put("softPityStart", pool.getSoftPityStart());
        poolData.put("softPityIncrement", pool.getSoftPityIncrement());
        poolData.put("startTime", pool.getStartTime());
        poolData.put("endTime", pool.getEndTime());
        poolData.put("fourStarAvatars", adminService.getPoolFourStarItems(pool.getId()));

        return ResponseEntity.ok(Map.of("success", true, "pool", poolData));
    }

    // ========== 常驻武器池UP自选 ==========

    @GetMapping("/standard-weapon-up")
    public ResponseEntity<Map<String, Object>> getStandardWeaponUpOptions(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();

        // 查找常驻武器池
        List<GachaPool> pools = adminService.listPools("standard-weapon", "active");
        if (pools.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "常驻武器池不存在"));
        }
        GachaPool pool = pools.get(0);

        // 获取池内所有五星武器
        List<GachaItem> allItems = adminService.getPoolItems(pool.getId());
        List<Map<String, Object>> fiveStarWeapons = allItems.stream()
                .filter(item -> item.getRarity() == 5 && "weapon".equals(item.getItemType()))
                .map(item -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", item.getId());
                    m.put("name", item.getName());
                    m.put("rarity", item.getRarity());
                    m.put("imageUrl", item.getImageUrl());
                    return m;
                })
                .toList();

        Long selectedId = userService.getSelectedWeaponUp(userId);
        // 如果用户未选择，使用池默认UP
        if (selectedId == null) {
            selectedId = pool.getFivestarUp();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("weapons", fiveStarWeapons);
        result.put("selectedId", selectedId);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/standard-weapon-up")
    public ResponseEntity<Map<String, Object>> setStandardWeaponUp(
            Authentication authentication,
            @RequestBody Map<String, Object> request) {
        Long userId = (Long) authentication.getPrincipal();

        Number weaponIdNum = (Number) request.get("weaponId");
        if (weaponIdNum == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "缺少武器ID"));
        }
        Long weaponId = weaponIdNum.longValue();

        // 验证武器存在且为五星武器
        GachaItem item = adminService.getItemById(weaponId);
        if (item == null || item.getRarity() != 5 || !"weapon".equals(item.getItemType())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "无效的武器ID"));
        }

        userService.updateSelectedWeaponUp(userId, weaponId);
        return ResponseEntity.ok(Map.of("success", true, "message", "UP武器已更新"));
    }

    // ========== 抽卡分析 ==========

    @GetMapping("/analysis")
    public ResponseEntity<Map<String, Object>> getAnalysis(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        Map<String, Object> analysis = gachaService.getAnalysis(userId);
        return ResponseEntity.ok(Map.of("success", true, "analysis", analysis));
    }
}
