package com.group1.cloudlibrary.controller;

import com.group1.cloudlibrary.common.Result;
import com.group1.cloudlibrary.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 通用接口：图片上传（图书封面等）
 */
@RestController
@RequestMapping("/api/common")
@RequiredArgsConstructor
public class CommonController {

    private final FileService fileService;

    /** 上传图书封面 */
    @PostMapping("/upload/cover")
    public Result<String> uploadCover(@RequestParam("file") MultipartFile file) {
        return Result.success("上传成功", fileService.uploadImage(file, "cover"));
    }

    /** 通用图片上传 */
    @PostMapping("/upload/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file,
                                      @RequestParam(defaultValue = "common") String type) {
        return Result.success("上传成功", fileService.uploadImage(file, type));
    }
}
