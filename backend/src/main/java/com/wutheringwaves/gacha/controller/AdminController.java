package com.wutheringwaves.gacha.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wutheringwaves.gacha.mapper.GachaItemMapper;
import com.wutheringwaves.gacha.model.FourStarAvatar;
import com.wutheringwaves.gacha.model.GachaItem;
import com.wutheringwaves.gacha.model.GachaPool;
import com.wutheringwaves.gacha.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final GachaItemMapper gachaItemMapper;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    // ========== 卡池管理 ==========

    @GetMapping("/pools")
    public ResponseEntity<Map<String, Object>> listPools(
            @RequestParam(required = false) String poolType,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "pools", adminService.listPools(poolType, status)
        ));
    }

    @GetMapping("/pools/{id}")
    public ResponseEntity<Map<String, Object>> getPool(@PathVariable Long id) {
        GachaPool pool = adminService.getPoolById(id);
        if (pool == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "卡池不存在"));
        }
        return ResponseEntity.ok(Map.of("success", true, "pool", pool));
    }

    @PostMapping("/pools")
    public ResponseEntity<Map<String, Object>> createPool(@RequestBody GachaPool pool) {
        GachaPool created = adminService.createPool(pool);
        return ResponseEntity.ok(Map.of("success", true, "pool", created));
    }

    @PutMapping("/pools/{id}")
    public ResponseEntity<Map<String, Object>> updatePool(@PathVariable Long id, @RequestBody GachaPool pool) {
        pool.setId(id);
        GachaPool updated = adminService.updatePool(pool);
        if (updated == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "卡池不存在"));
        }
        return ResponseEntity.ok(Map.of("success", true, "pool", updated));
    }

    @PostMapping("/pools/{id}/toggle")
    public ResponseEntity<Map<String, Object>> togglePoolStatus(@PathVariable Long id) {
        boolean result = adminService.togglePoolStatus(id);
        if (!result) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "卡池不存在"));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "状态已切换"));
    }

    @DeleteMapping("/pools/{id}")
    public ResponseEntity<Map<String, Object>> deletePool(@PathVariable Long id) {
        boolean result = adminService.deletePool(id);
        if (!result) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "卡池不存在"));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "卡池已删除"));
    }

    // ========== 四星头像管理 ==========

    @GetMapping("/avatars")
    public ResponseEntity<Map<String, Object>> listAvatars() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "avatars", adminService.listAvatars()
        ));
    }

    @PostMapping("/avatars")
    public ResponseEntity<Map<String, Object>> createAvatar(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        String avatarUrl = request.get("avatarUrl");
        if (name == null || name.isEmpty() || avatarUrl == null || avatarUrl.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "名称和头像URL不能为空"));
        }
        FourStarAvatar avatar = new FourStarAvatar();
        avatar.setName(name);
        avatar.setAvatarUrl(avatarUrl);
        FourStarAvatar created = adminService.createAvatar(avatar);
        return ResponseEntity.ok(Map.of("success", true, "avatar", created));
    }

    @DeleteMapping("/avatars/{id}")
    public ResponseEntity<Map<String, Object>> deleteAvatar(@PathVariable Long id) {
        boolean result = adminService.deleteAvatar(id);
        if (!result) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "头像不存在"));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "已删除"));
    }

    @GetMapping("/pools/{id}/four-stars")
    public ResponseEntity<Map<String, Object>> getPoolFourStars(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "avatars", adminService.getPoolFourStarAvatars(id)
        ));
    }

    @PutMapping("/pools/{id}/four-stars")
    public ResponseEntity<Map<String, Object>> updatePoolFourStars(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Number> avatarIdNumbers = (List<Number>) request.get("avatarIds");
        List<Long> avatarIds = avatarIdNumbers != null
                ? avatarIdNumbers.stream().map(Number::longValue).toList()
                : null;
        boolean result = adminService.updatePoolFourStars(id, avatarIds);
        if (!result) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "更新失败（最多选择3个头像）"));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "已更新"));
    }

    // ========== 分类管理 ==========

    @GetMapping("/categories")
    public ResponseEntity<Map<String, Object>> listCategories() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "categories", adminService.listCategories()
        ));
    }

    // ========== 卡池物品管理 ==========

    @GetMapping("/items")
    public ResponseEntity<Map<String, Object>> listAllItems(
            @RequestParam(required = false) Integer rarity,
            @RequestParam(required = false) String itemType) {
        LambdaQueryWrapper<GachaItem> wrapper = new LambdaQueryWrapper<>();
        if (rarity != null) wrapper.eq(GachaItem::getRarity, rarity);
        if (itemType != null && !itemType.isEmpty()) wrapper.eq(GachaItem::getItemType, itemType);
        wrapper.orderByAsc(GachaItem::getRarity).orderByAsc(GachaItem::getName);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "items", gachaItemMapper.selectList(wrapper)
        ));
    }

    @GetMapping("/pools/{id}/items")
    public ResponseEntity<Map<String, Object>> getPoolItems(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "items", adminService.getPoolItems(id)
        ));
    }

    @GetMapping("/pools/{id}/up-items")
    public ResponseEntity<Map<String, Object>> getPoolUpItems(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "upItems", adminService.getPoolUpItems(id)
        ));
    }

    @PutMapping("/pools/{id}/config")
    public ResponseEntity<Map<String, Object>> updatePoolConfig(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Number> categoryNumbers = (List<Number>) request.get("categoryIds");
        List<Long> categoryIds = categoryNumbers != null
                ? categoryNumbers.stream().map(Number::longValue).toList() : null;
        Long fivestarUp = request.get("fivestarUp") != null
                ? ((Number) request.get("fivestarUp")).longValue() : null;
        @SuppressWarnings("unchecked")
        List<Number> fourstarUpNumbers = (List<Number>) request.get("fourstarUpIds");
        List<Long> fourstarUpIds = fourstarUpNumbers != null
                ? fourstarUpNumbers.stream().map(Number::longValue).toList() : null;
        adminService.updatePoolConfig(id, categoryIds, fivestarUp, fourstarUpIds);
        return ResponseEntity.ok(Map.of("success", true, "message", "已更新"));
    }

    // ========== 用户管理 ==========

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "users", adminService.listUsers(page, size, keyword)
        ));
    }

    @PutMapping("/users/{id}/resources")
    public ResponseEntity<Map<String, Object>> updateUserResources(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        Integer starlight = request.get("starlight") != null ?
                ((Number) request.get("starlight")).intValue() : null;

        boolean result = adminService.updateUserResources(id, starlight);
        if (!result) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "用户不存在"));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "资源已更新"));
    }

    @GetMapping("/users/{id}/records")
    public ResponseEntity<Map<String, Object>> getUserRecords(
            @PathVariable Long id,
            @RequestParam(required = false) String poolType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "records", adminService.getUserRecords(id, poolType, page, size)
        ));
    }

    // ========== 数据报表 ==========

    @GetMapping("/stats/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "stats", adminService.getDashboardStats()
        ));
    }

    @GetMapping("/stats/daily")
    public ResponseEntity<Map<String, Object>> getDailyStats(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "dailyStats", adminService.getDailyStats(days)
        ));
    }

    // ========== 图片上传 ==========

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "文件不能为空"));
        }

        try {
            // 确保上传目录存在
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".png";
            String filename = UUID.randomUUID().toString() + extension;

            // 保存文件
            Path filePath = uploadPath.resolve(filename);
            file.transferTo(filePath.toFile());

            String imageUrl = "/" + uploadDir + "/" + filename;

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "imageUrl", imageUrl,
                    "filename", filename
            ));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "上传失败：" + e.getMessage()
            ));
        }
    }
}
