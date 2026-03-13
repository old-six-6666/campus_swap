package com.itcodai.campus_swap.controller;

import com.itcodai.campus_swap.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 文件上传接口（图片）
 */
@Slf4j
@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5MB
    private static final java.util.Set<String> ALLOWED_TYPES = java.util.Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    @PostMapping
    public Result<String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return Result.fail(400, "文件不能为空");
        }
        if (file.getSize() > MAX_SIZE) {
            return Result.fail(400, "图片大小不能超过 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            return Result.fail(400, "只支持 JPG/PNG/GIF/WebP 格式图片");
        }

        // 生成唯一文件名，保留原始扩展名
        String originalName = file.getOriginalFilename();
        String ext = (originalName != null && originalName.contains("."))
                ? originalName.substring(originalName.lastIndexOf("."))
                : ".jpg";
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;

        // 解析为绝对路径，确保目录存在（transferTo 使用相对路径会定向到 Servlet 临时目录）
        Path destDir = Paths.get(uploadDir).toAbsolutePath();
        Files.createDirectories(destDir);

        Path dest = destDir.resolve(filename);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }
        log.debug("文件上传成功: {}", dest);

        // 返回可访问的相对 URL
        return Result.success("/uploads/" + filename);
    }
}
