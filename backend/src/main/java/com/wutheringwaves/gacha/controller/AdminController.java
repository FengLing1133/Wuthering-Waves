package com.wutheringwaves.gacha.controller;

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
            Path uploadPath = Paths.get(uploadDir);
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
