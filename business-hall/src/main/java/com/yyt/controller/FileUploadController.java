package com.yyt.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "*", maxAge = 3600)
public class FileUploadController {

    private static final String UPLOAD_DIR = "uploads/complaints/";

    /**
     * 上传单个文件
     */
    @PostMapping(value = "/single", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadSingleFile(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        
        if (file == null || file.isEmpty()) {
            result.put("success", false);
            result.put("message", "请选择要上传的文件");
            return ResponseEntity.badRequest().body(result);
        }

        try {
            // 创建上传目录
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : "";
            String newFilename = UUID.randomUUID().toString() + extension;
            
            // 保存文件
            Path filePath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath);

            // 返回相对路径，前端根据环境配置拼接完整URL
            String fileUrl = "/api/upload/files/" + newFilename;
            
            result.put("success", true);
            result.put("message", "上传成功");
            result.put("fileName", originalFilename);
            result.put("fileUrl", fileUrl);
            result.put("storedName", newFilename);  
            
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            result.put("success", false);
            result.put("message", "上传失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
}