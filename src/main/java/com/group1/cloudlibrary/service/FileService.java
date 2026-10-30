package com.group1.cloudlibrary.service;

import com.group1.cloudlibrary.common.BusinessException;
import com.group1.cloudlibrary.config.CloudLibraryProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 文件上传服务（本地存储）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private static final List<String> ALLOW_EXT = List.of("jpg", "jpeg", "png", "gif", "webp", "bmp");
    private static final long MAX_SIZE = 10 * 1024 * 1024L;

    private final CloudLibraryProperties properties;

    /**
     * 上传图片，返回可访问的 URL
     *
     * @param file   上传文件
     * @param subDir 子目录（avatar / cover）
     */
    public String uploadImage(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.of("请选择要上传的图片");
        }
        if (file.getSize() > MAX_SIZE) {
            throw BusinessException.of("图片过大，请控制在 10MB 以内");
        }
        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase()
                : "";
        if (!ALLOW_EXT.contains(ext)) {
            throw BusinessException.of("仅支持 jpg/png/gif/webp/bmp 格式的图片");
        }

        String day = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String dirName = (StringUtils.hasText(subDir) ? subDir : "common") + "/" + day;
        File dir = new File(properties.getUpload().getPath(), dirName);
        if (!dir.exists() && !dir.mkdirs()) {
            throw BusinessException.of("创建上传目录失败");
        }

        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        File target = new File(dir, fileName);
        try {
            file.transferTo(target.getAbsoluteFile());
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw BusinessException.of("文件上传失败：" + e.getMessage());
        }
        return properties.getUpload().getUrlPrefix() + dirName + "/" + fileName;
    }
}
