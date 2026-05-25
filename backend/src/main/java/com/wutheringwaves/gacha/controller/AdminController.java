package com.wutheringwaves.gacha.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wutheringwaves.gacha.mapper.GachaItemMapper;
import com.wutheringwaves.gacha.model.GachaItem;
import com.wutheringwaves.gacha.model.GachaPool;
import com.wutheringwaves.gacha.model.ItemCategory;
import com.wutheringwaves.gacha.model.ItemTheme;
import com.wutheringwaves.gacha.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import java.util.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final GachaItemMapper gachaItemMapper;

    @Value("${app.upload.dir:uploads}")
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

    // ========== 分类管理 ==========

    @GetMapping("/categories")
    public ResponseEntity<Map<String, Object>> listCategories() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "categories", adminService.listCategories()
        ));
    }

    @PostMapping("/categories")
    public ResponseEntity<Map<String, Object>> createCategory(@RequestBody ItemCategory category) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "category", adminService.createCategory(category)
        ));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<Map<String, Object>> updateCategory(@PathVariable Long id, @RequestBody ItemCategory category) {
        ItemCategory updated = adminService.updateCategory(id, category);
        if (updated == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "分类不存在"));
        }
        return ResponseEntity.ok(Map.of("success", true, "category", updated));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Map<String, Object>> deleteCategory(@PathVariable Long id) {
        boolean result = adminService.deleteCategory(id);
        if (!result) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "分类不存在或已被引用，无法删除"));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "分类已删除"));
    }

    // ========== 主题管理 ==========

    @GetMapping("/themes")
    public ResponseEntity<Map<String, Object>> listThemes() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "themes", adminService.listThemes()
        ));
    }

    @GetMapping("/themes/{id}")
    public ResponseEntity<Map<String, Object>> getTheme(@PathVariable Long id) {
        ItemTheme theme = adminService.getThemeById(id);
        if (theme == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "主题不存在"));
        }
        return ResponseEntity.ok(Map.of("success", true, "theme", theme));
    }

    @PostMapping("/themes")
    public ResponseEntity<Map<String, Object>> createTheme(@RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        @SuppressWarnings("unchecked")
        List<String> generateCategories = (List<String>) request.get("generateCategories");
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "主题名称不能为空"));
        }
        ItemTheme theme = adminService.createTheme(name, description, generateCategories);
        return ResponseEntity.ok(Map.of("success", true, "theme", theme));
    }

    @PutMapping("/themes/{id}")
    public ResponseEntity<Map<String, Object>> updateTheme(@PathVariable Long id, @RequestBody ItemTheme theme) {
        ItemTheme updated = adminService.updateTheme(id, theme);
        if (updated == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "主题不存在"));
        }
        return ResponseEntity.ok(Map.of("success", true, "theme", updated));
    }

    @DeleteMapping("/themes/{id}")
    public ResponseEntity<Map<String, Object>> deleteTheme(@PathVariable Long id) {
        boolean result = adminService.deleteTheme(id);
        if (!result) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "主题不存在或其下分类已被引用，无法删除"));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "主题已删除"));
    }

    @PostMapping("/themes/copy")
    public ResponseEntity<Map<String, Object>> copyTheme(@RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        Long sourceThemeId = request.get("sourceThemeId") != null
                ? ((Number) request.get("sourceThemeId")).longValue() : null;
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "主题名称不能为空"));
        }
        if (sourceThemeId == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "源主题ID不能为空"));
        }
        ItemTheme theme = adminService.copyTheme(name, description, sourceThemeId);
        if (theme == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "源主题不存在"));
        }
        return ResponseEntity.ok(Map.of("success", true, "theme", theme));
    }

    @GetMapping("/themes/{id}/categories")
    public ResponseEntity<Map<String, Object>> getThemeCategories(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "categories", adminService.getCategoriesByTheme(id)
        ));
    }

    // ========== 卡池物品管理 ==========

    @GetMapping("/items")
    public ResponseEntity<Map<String, Object>> listAllItems(
            @RequestParam(required = false) Integer rarity,
            @RequestParam(required = false) String itemType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        // 无分页参数时返回全量列表（兼容卡池管理中的物品选择）
        if (page == null && size == null && keyword == null && categoryId == null) {
            LambdaQueryWrapper<GachaItem> wrapper = new LambdaQueryWrapper<>();
            if (rarity != null) wrapper.eq(GachaItem::getRarity, rarity);
            if (itemType != null && !itemType.isEmpty()) wrapper.eq(GachaItem::getItemType, itemType);
            wrapper.orderByAsc(GachaItem::getId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "items", gachaItemMapper.selectList(wrapper)
            ));
        }
        return ResponseEntity.ok(Map.of(
                "success", true,
                "items", adminService.listItems(page != null ? page : 1, size != null ? size : 20,
                        rarity, itemType, keyword, categoryId)
        ));
    }

    @GetMapping("/items/{id}")
    public ResponseEntity<Map<String, Object>> getItem(@PathVariable Long id) {
        GachaItem item = adminService.getItemById(id);
        if (item == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "物品不存在"));
        }
        return ResponseEntity.ok(Map.of("success", true, "item", item));
    }

    @PostMapping("/items")
    public ResponseEntity<Map<String, Object>> createItem(@RequestBody GachaItem item) {
        GachaItem created = adminService.createItem(item);
        if (created == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "分类不存在"));
        }
        return ResponseEntity.ok(Map.of("success", true, "item", created));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<Map<String, Object>> updateItem(@PathVariable Long id, @RequestBody GachaItem item) {
        item.setId(id);
        GachaItem updated = adminService.updateItem(item);
        if (updated == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "物品不存在或分类无效"));
        }
        return ResponseEntity.ok(Map.of("success", true, "item", updated));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Map<String, Object>> deleteItem(@PathVariable Long id) {
        boolean result = adminService.deleteItem(id);
        if (!result) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "物品不存在或已被卡池引用，无法删除"));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "物品已删除"));
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

    // ========== 文件上传 ==========

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "文件为空"));
        }

        String contentType = file.getContentType();
        boolean isImage = contentType != null && contentType.startsWith("image/");
        boolean isVideo = contentType != null && contentType.startsWith("video/");
        if (!isImage && !isVideo) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "仅支持图片和视频文件"));
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + extension;

            String subDir = isVideo ? "videos" : "images";
            Path uploadPath = Paths.get(uploadDir, subDir);
            Files.createDirectories(uploadPath);
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String url = "/uploads/" + subDir + "/" + filename;
            return ResponseEntity.ok(Map.of("success", true, "url", url));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "上传失败: " + e.getMessage()));
        }
    }
}
