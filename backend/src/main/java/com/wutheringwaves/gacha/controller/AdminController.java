package com.wutheringwaves.gacha.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wutheringwaves.gacha.mapper.GachaItemMapper;
import com.wutheringwaves.gacha.model.GachaItem;
import com.wutheringwaves.gacha.model.GachaPool;
import com.wutheringwaves.gacha.model.ItemCategory;
import com.wutheringwaves.gacha.model.ItemTheme;
import com.wutheringwaves.gacha.dto.CopyThemeRequest;
import com.wutheringwaves.gacha.dto.CreateThemeRequest;
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

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final AdminService adminService;
    private final GachaItemMapper gachaItemMapper;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    // ========== 卡池管理 ==========

    @GetMapping("/pools")
    public ResponseEntity<Map<String, Object>> listPools(
            @RequestParam(required = false) String poolType,
            @RequestParam(required = false) String status) {
        List<GachaPool> pools = adminService.listPools(poolType, status);
        Map<Long, List<Map<String, Object>>> allUpItems = adminService.batchGetPoolUpItems(pools);

        List<Map<String, Object>> result = new ArrayList<>();
        for (GachaPool pool : pools) {
            Map<String, Object> poolData = new HashMap<>();
            poolData.put("id", pool.getId());
            poolData.put("name", pool.getName());
            poolData.put("poolType", pool.getPoolType());
            poolData.put("description", pool.getDescription());
            poolData.put("bgImageUrl", pool.getBgImageUrl());
            poolData.put("thumbnailUrl", pool.getThumbnailUrl());
            poolData.put("fivestarUp", pool.getFivestarUp());
            poolData.put("fourstarUp", pool.getFourstarUp());
            poolData.put("upItems", allUpItems.getOrDefault(pool.getId(), List.of()));
            poolData.put("fiveStarRate", pool.getFiveStarRate());
            poolData.put("fourStarRate", pool.getFourStarRate());
            poolData.put("maxPity", pool.getMaxPity());
            poolData.put("softPityStart", pool.getSoftPityStart());
            poolData.put("softPityIncrement", pool.getSoftPityIncrement());
            poolData.put("startTime", pool.getStartTime());
            poolData.put("endTime", pool.getEndTime());
            poolData.put("status", pool.getStatus());
            poolData.put("sidebarVisible", pool.getSidebarVisible());
            poolData.put("sidebarOrder", pool.getSidebarOrder());
            poolData.put("createdAt", pool.getCreatedAt());
            poolData.put("updatedAt", pool.getUpdatedAt());
            result.add(poolData);
        }

        return ResponseEntity.ok(Map.of("success", true, "pools", result));
    }

    @GetMapping("/pools/{id}")
    public ResponseEntity<Map<String, Object>> getPool(@PathVariable Long id) {
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
        poolData.put("fivestarUp", pool.getFivestarUp());
        poolData.put("fourstarUp", pool.getFourstarUp());
        poolData.put("upItems", adminService.getPoolUpItems(id));
        poolData.put("fiveStarRate", pool.getFiveStarRate());
        poolData.put("fourStarRate", pool.getFourStarRate());
        poolData.put("maxPity", pool.getMaxPity());
        poolData.put("softPityStart", pool.getSoftPityStart());
        poolData.put("softPityIncrement", pool.getSoftPityIncrement());
        poolData.put("startTime", pool.getStartTime());
        poolData.put("endTime", pool.getEndTime());
        poolData.put("status", pool.getStatus());
        poolData.put("sidebarVisible", pool.getSidebarVisible());
        poolData.put("sidebarOrder", pool.getSidebarOrder());
        poolData.put("createdAt", pool.getCreatedAt());
        poolData.put("updatedAt", pool.getUpdatedAt());
        return ResponseEntity.ok(Map.of("success", true, "pool", poolData));
    }

    @PostMapping("/pools")
    public ResponseEntity<Map<String, Object>> createPool(@RequestBody GachaPool pool) {
        processPoolImages(pool);
        GachaPool created = adminService.createPool(pool);
        return ResponseEntity.ok(Map.of("success", true, "pool", created));
    }

    @PutMapping("/pools/{id}")
    public ResponseEntity<Map<String, Object>> updatePool(@PathVariable Long id, @RequestBody GachaPool pool) {
        pool.setId(id);
        processPoolImages(pool);
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
        ItemCategory created = adminService.createCategory(category);
        if (created == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "主题不存在"));
        }
        return ResponseEntity.ok(Map.of(
                "success", true,
                "category", created
        ));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<Map<String, Object>> updateCategory(@PathVariable Long id, @RequestBody ItemCategory category) {
        ItemCategory updated = adminService.updateCategory(id, category);
        if (updated == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "分类不存在或主题无效"));
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
    public ResponseEntity<Map<String, Object>> createTheme(@RequestBody CreateThemeRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "主题名称不能为空"));
        }
        ItemTheme theme = adminService.createTheme(request.name(), request.description(), request.generateCategories());
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
    public ResponseEntity<Map<String, Object>> copyTheme(@RequestBody CopyThemeRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "主题名称不能为空"));
        }
        if (request.sourceThemeId() == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "源主题ID不能为空"));
        }
        ItemTheme theme = adminService.copyTheme(request.name(), request.description(), request.sourceThemeId());
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

        // 后缀白名单校验
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }
        Set<String> allowedExtensions = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".mp4", ".webm");
        if (!allowedExtensions.contains(extension)) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "不支持的文件格式"));
        }

        try {
            String filename = UUID.randomUUID().toString() + extension;

            String subDir = isVideo ? "videos" : "images";
            Path uploadPath = Paths.get(uploadDir, subDir);
            Files.createDirectories(uploadPath);
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 图片自动压缩
            if (isImage) {
                long originalSize = Files.size(filePath);
                compressImage(filePath);
                // 压缩后文件可能变成 .jpg
                String baseName = filename.replaceFirst("\\.[^.]+$", "");
                Path jpgPath = uploadPath.resolve(baseName + ".jpg");
                if (Files.exists(jpgPath)) {
                    filePath = jpgPath;
                    filename = baseName + ".jpg";
                }
                long compressedSize = Files.size(filePath);
                log.info("图片压缩完成: {} -> {} ({}KB -> {}KB)",
                        originalFilename, filename, originalSize / 1024, compressedSize / 1024);
            }

            // 视频自动压缩
            if (isVideo) {
                compressVideo(filePath);
            }

            String url = "/uploads/" + subDir + "/" + filename;
            return ResponseEntity.ok(Map.of("success", true, "url", url));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "上传失败"));
        }
    }

    private void processPoolImages(GachaPool pool) {
        if (pool.getBgImageUrl() != null && pool.getBgImageUrl().startsWith("data:")) {
            String url = saveBase64Image(pool.getBgImageUrl(), "pool_" + System.currentTimeMillis() + "_bg");
            if (url != null) pool.setBgImageUrl(url);
        }
        if (pool.getThumbnailUrl() != null && pool.getThumbnailUrl().startsWith("data:")) {
            String url = saveBase64Image(pool.getThumbnailUrl(), "pool_" + System.currentTimeMillis() + "_thumb");
            if (url != null) pool.setThumbnailUrl(url);
        }
    }

    private String saveBase64Image(String dataUrl, String baseName) {
        try {
            String[] parts = dataUrl.split(",", 2);
            String header = parts[0];
            String b64data = parts[1];

            String ext = header.contains("png") ? "png" : "jpg";
            byte[] bytes = Base64.getDecoder().decode(b64data);

            Path dir = Paths.get(uploadDir, "pools");
            Files.createDirectories(dir);
            Path filePath = dir.resolve(baseName + "." + ext);
            Files.write(filePath, bytes);

            // 压缩：> 300KB 时缩放到 1920 宽并转 JPEG
            if (bytes.length > 300 * 1024) {
                compressImage(filePath);
                // 压缩后文件可能变成 .jpg
                Path jpgPath = filePath.resolveSibling(baseName + ".jpg");
                if (Files.exists(jpgPath)) {
                    filePath = jpgPath;
                }
            }

            return "/uploads/pools/" + filePath.getFileName();
        } catch (Exception e) {
            return null;
        }
    }

    private void compressImage(Path imagePath) {
        try {
            BufferedImage img = ImageIO.read(imagePath.toFile());
            if (img == null) return;

            int maxW = 1920;
            if (img.getWidth() > maxW) {
                int newH = (int) ((long) img.getHeight() * maxW / img.getWidth());
                BufferedImage resized = new BufferedImage(maxW, newH, BufferedImage.TYPE_INT_RGB);
                resized.getGraphics().drawImage(img.getScaledInstance(maxW, newH, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
                img = resized;
            } else if (img.getType() != BufferedImage.TYPE_INT_RGB) {
                BufferedImage rgb = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
                rgb.getGraphics().drawImage(img, 0, 0, null);
                img = rgb;
            }

            Path jpgPath = imagePath.resolveSibling(imagePath.getFileName().toString().replaceFirst("\\.[^.]+$", ".jpg"));
            ImageIO.write(img, "jpg", jpgPath.toFile());

            if (!jpgPath.equals(imagePath)) {
                Files.delete(imagePath);
            }
        } catch (Exception e) {
            // 压缩失败保留原文件
        }
    }

    private boolean ffmpegAvailable = true;

    private void compressVideo(Path videoPath) {
        if (!ffmpegAvailable) return;

        try {
            long size = Files.size(videoPath);
            // 跳过小于 1MB 的视频
            if (size < 1024 * 1024) {
                log.info("视频小于 1MB，跳过压缩: {}", videoPath.getFileName());
                return;
            }

            // 检查 ffmpeg 是否可用
            try {
                Process check = new ProcessBuilder(ffmpegPath, "-version").redirectErrorStream(true).start();
                if (check.waitFor() != 0) {
                    log.warn("ffmpeg 不可用（exitCode={}），跳过视频压缩。请确保 ffmpeg 已安装并在 PATH 中，或在 application.yml 中配置 app.ffmpeg.path", check.waitFor());
                    ffmpegAvailable = false;
                    return;
                }
            } catch (IOException e) {
                log.warn("ffmpeg 未找到（{}），跳过视频压缩。请安装 ffmpeg 并配置 PATH，或在 application.yml 中设置 app.ffmpeg.path", e.getMessage());
                ffmpegAvailable = false;
                return;
            }

            String fileName = videoPath.getFileName().toString();
            String baseName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
            Path tempPath = videoPath.resolveSibling(baseName + "_small.mp4");
            log.info("开始压缩视频: {} ({}KB)", fileName, size / 1024);
            ProcessBuilder pb = new ProcessBuilder(
                ffmpegPath, "-i", videoPath.toString(),
                "-vcodec", "libx264", "-crf", "28", "-preset", "fast",
                "-vf", "scale=1280:-2",
                "-y", tempPath.toString()
            );
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            Process process = pb.start();
            // 必须消费输出流避免缓冲区满导致死锁（Redirect.DISCARD 已处理）
            int exitCode = process.waitFor();

            if (exitCode == 0 && Files.exists(tempPath)) {
                long compressedSize = Files.size(tempPath);
                Files.delete(videoPath);
                Files.move(tempPath, videoPath);
                log.info("视频压缩完成: {} ({}KB -> {}KB)", videoPath.getFileName(), size / 1024, compressedSize / 1024);
            } else {
                Files.deleteIfExists(tempPath);
                log.warn("视频压缩失败 (exitCode={})，保留原文件: {}", exitCode, videoPath.getFileName());
            }
        } catch (IOException | InterruptedException e) {
            log.error("视频压缩异常，保留原文件: {}", videoPath.getFileName(), e);
        }
    }
}
