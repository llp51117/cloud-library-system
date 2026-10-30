package com.group1.cloudlibrary.controller.admin;

import com.group1.cloudlibrary.common.PageResult;
import com.group1.cloudlibrary.common.Result;
import com.group1.cloudlibrary.dto.BookSaveDTO;
import com.group1.cloudlibrary.dto.CoverCropDTO;
import com.group1.cloudlibrary.entity.Book;
import com.group1.cloudlibrary.service.BookService;
import com.group1.cloudlibrary.service.ExcelService;
import com.group1.cloudlibrary.vo.BatchCoverResultVO;
import com.group1.cloudlibrary.vo.BookImportResultVO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 图书管理接口（管理端）
 */
@RestController
@RequestMapping("/api/admin/books")
@RequiredArgsConstructor
public class AdminBookController {

    private final BookService bookService;
    private final ExcelService excelService;

    /** 图书列表（书名/作者/平台搜索） */
    @GetMapping
    public Result<PageResult<Book>> list(@RequestParam(defaultValue = "1") long page,
                                         @RequestParam(defaultValue = "10") long size,
                                         @RequestParam(required = false) String keyword) {
        return Result.success(bookService.adminPage(page, size, keyword));
    }

    /** 新增图书 */
    @PostMapping
    public Result<Long> add(@Valid @RequestBody BookSaveDTO dto) {
        return Result.success("新增成功", bookService.save(dto));
    }

    /** 编辑图书 */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody BookSaveDTO dto) {
        dto.setId(id);
        bookService.update(dto);
        return Result.success("修改成功", null);
    }

    /**
     * 单独保存封面裁剪参数
     *
     * <p>两套参数分开存：coverCardCrop 用于首页卡片（横向 3:2），coverDetailCrop 用于图书详情页（竖向 3:4）；
     * 传空字符串表示清空对应模式的裁剪，回到不裁剪的原图展示。</p>
     */
    @PutMapping("/{id}/cover-crop")
    public Result<Void> updateCoverCrop(@PathVariable Long id, @RequestBody CoverCropDTO dto) {
        bookService.updateCoverCrop(id, dto);
        return Result.success("封面裁剪已保存", null);
    }

    /** 删除图书 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        bookService.delete(id);
        return Result.success("删除成功", null);
    }

    /** 批量删除 */
    @DeleteMapping("/batch")
    public Result<Integer> batchDelete(@RequestBody Map<String, List<Long>> body) {
        List<Long> ids = body.get("ids");
        int count = bookService.batchDelete(ids);
        return Result.success("已删除 " + count + " 本图书", count);
    }

    /**
     * 批量导入（JSON 方式，前端预览后提交）
     *
     * <p>带查重：书名 + 作者相同的不新增，只补齐原有的空字段；
     * 完全没得补的跳过，跳过与补充清单放在返回结果的 duplicated / updated / repeatedInBatch 里。</p>
     */
    @PostMapping("/import")
    public Result<BookImportResultVO> batchImport(@RequestBody List<BookSaveDTO> list) {
        BookImportResultVO result = bookService.batchImport(list);
        StringBuilder message = new StringBuilder("成功导入 ").append(result.getImported()).append(" 本图书");
        if (result.getUpdatedCount() > 0) {
            message.append("，已更新 ").append(result.getUpdatedCount()).append(" 条空字段");
        }
        if (result.getDuplicatedCount() > 0) {
            message.append("，已跳过 ").append(result.getDuplicatedCount()).append(" 条重复图书");
        }
        if (result.getInvalid() > 0) {
            message.append("，已跳过 ").append(result.getInvalid()).append(" 条书名或作者为空的数据");
        }
        return Result.success(message.toString(), result);
    }

    /**
     * 批量上传封面：多张图片一次上传，按「图片文件名 = 书名」自动匹配并覆盖封面
     *
     * <p>例如「三体.jpg」会挂到书名《三体》上；匹配不到的图片会在返回结果里列出，不会写入磁盘。</p>
     */
    @PostMapping("/covers/batch")
    public Result<BatchCoverResultVO> batchUploadCovers(@RequestParam("files") List<MultipartFile> files) {
        BatchCoverResultVO result = bookService.batchUploadCovers(files);
        return Result.success(
                "上传完成：成功 " + result.getMatchedCount() + " 张，未匹配 " + result.getUnmatched().size()
                        + " 张，失败 " + result.getFailed().size() + " 张",
                result);
    }

    /** 批量导入（Excel 上传） */
    @PostMapping("/import-excel")
    public Result<List<BookSaveDTO>> importExcel(@RequestParam("file") MultipartFile file) {
        try {
            List<BookSaveDTO> list = excelService.parseBooks(file.getInputStream());
            return Result.success("解析成功，请确认后导入", list);
        } catch (IOException e) {
            return Result.error("文件读取失败：" + e.getMessage());
        }
    }

    /** 下载图书导入模板 */
    @GetMapping("/template")
    public void template(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String fileName = URLEncoder.encode("图书导入模板.xlsx", StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        excelService.writeBookTemplate(response.getOutputStream());
    }

    /** AI 辅助编目：自动生成图书摘要 */
    @PostMapping("/ai-summary")
    public Result<String> aiSummary(@RequestBody BookSaveDTO dto) {
        return Result.success(bookService.aiSummary(dto.getTitle(), dto.getAuthor(), dto.getDescription()));
    }
}
