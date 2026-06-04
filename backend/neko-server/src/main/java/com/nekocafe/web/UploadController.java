package com.nekocafe.web;

import com.nekocafe.common.ApiException;
import com.nekocafe.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@RestController
@Tag(name = "Upload", description = "文件上传")
public class UploadController {

    @Value("${NEKO_UPLOAD_DIR:${neko.upload.dir:./uploads}}")
    private String uploadDir;

    @Value("${neko.upload.base-url:/uploads}")
    private String baseUrl;

    @Operation(summary = "上传图片", description = "上传图片文件，返回可访问的 URL")
    @PostMapping("/api/upload")
    public ApiResponse upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw ApiException.badRequest("文件不能为空");
        }

        String originalName = file.getOriginalFilename();
        String ext = ".jpg";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;

        Path dir = Paths.get(uploadDir).toAbsolutePath();
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        Path target = dir.resolve(filename);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        String url = baseUrl + "/" + filename;
        return ApiResponse.of(Map.of("url", url, "filename", filename));
    }
}
