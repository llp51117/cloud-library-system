package com.group1.cloudlibrary.controller;

import com.group1.cloudlibrary.common.Result;
import com.group1.cloudlibrary.dto.PasswordVerifyDTO;
import com.group1.cloudlibrary.dto.UserThemeSaveDTO;
import com.group1.cloudlibrary.dto.UpdateBgPaletteDTO;
import com.group1.cloudlibrary.dto.UpdateEmailDTO;
import com.group1.cloudlibrary.dto.UpdateNicknameDTO;
import com.group1.cloudlibrary.dto.UpdatePasswordDTO;
import com.group1.cloudlibrary.dto.UpdateThemeDTO;
import com.group1.cloudlibrary.service.FileService;
import com.group1.cloudlibrary.service.UserBgService;
import com.group1.cloudlibrary.service.UserHomepageService;
import com.group1.cloudlibrary.service.UserService;
import com.group1.cloudlibrary.service.UserThemeService;
import com.group1.cloudlibrary.util.UserContext;
import com.group1.cloudlibrary.vo.ThemeAnalyzeVO;
import com.group1.cloudlibrary.vo.UserBgVO;
import com.group1.cloudlibrary.vo.UserHomepageVO;
import com.group1.cloudlibrary.vo.UserThemeVO;
import com.group1.cloudlibrary.vo.UserVO;
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

import java.util.List;

/**
 * 个人信息接口（用户端）
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserHomepageService userHomepageService;
    private final UserThemeService userThemeService;
    private final UserBgService userBgService;
    private final FileService fileService;

    /** 获取当前登录用户信息（邮箱只返回打码值，完整邮箱需验证密码后才能拿到） */
    @GetMapping("/profile")
    public Result<UserVO> profile() {
        UserVO vo = userService.profile(UserContext.requireUserId());
        vo.setEmail(vo.getEmailMasked());
        return Result.success(vo);
    }

    /**
     * 用户个人主页（评论区点击头像 / 昵称进入）
     * 只返回头像、昵称、注册时间、账号状态、身份标签、正在阅读、已收藏
     */
    @GetMapping("/homepage/{id}")
    public Result<UserHomepageVO> homepage(@PathVariable Long id) {
        return Result.success(userHomepageService.homepage(id));
    }

    /** 上传头像 */
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String url = fileService.uploadImage(file, "avatar");
        return Result.success("头像上传成功", userService.updateAvatar(UserContext.requireUserId(), url));
    }

    /** 修改昵称（留空则恢复为邮箱前缀） */
    @PutMapping("/nickname")
    public Result<String> updateNickname(@Valid @RequestBody UpdateNicknameDTO dto) {
        return Result.success("昵称已更新", userService.updateNickname(UserContext.requireUserId(), dto.getNickname()));
    }

    /** 修改邮箱第一步：验证登录密码，通过后返回当前完整邮箱 */
    @PostMapping("/email/verify-password")
    public Result<String> verifyEmailPassword(@Valid @RequestBody PasswordVerifyDTO dto) {
        return Result.success("身份验证通过",
                userService.verifyPasswordAndGetEmail(UserContext.requireUserId(), dto.getPassword()));
    }

    /** 修改邮箱第二步：向当前（旧）邮箱发送验证码 */
    @PostMapping("/email/send-code")
    public Result<Void> sendEmailChangeCode() {
        userService.sendEmailChangeCode(UserContext.requireUserId());
        return Result.success("验证码已发送到当前邮箱，请查收邮件", null);
    }

    /** 修改邮箱：校验旧邮箱验证码后保存新邮箱，并给旧邮箱发变更通知 */
    @PutMapping("/email")
    public Result<Void> updateEmail(@Valid @RequestBody UpdateEmailDTO dto) {
        userService.updateEmail(UserContext.requireUserId(), dto);
        return Result.success("邮箱修改成功", null);
    }

    /**
     * 查询当前用户的主题（皮肤）：GET /api/user/theme
     * 没设置过（老数据）时返回默认主题 quantum-blue
     */
    @GetMapping("/theme")
    public Result<String> getTheme() {
        return Result.success(userService.getTheme(UserContext.requireUserId()));
    }

    /**
     * 修改当前用户的主题（皮肤）：PUT /api/user/theme，参数 theme
     * 主题存数据库、与账号绑定，不用 localStorage
     */
    @PutMapping("/theme")
    public Result<String> updateTheme(@RequestBody UpdateThemeDTO dto) {
        String theme = userService.updateTheme(UserContext.requireUserId(), dto == null ? null : dto.getTheme());
        return Result.success("主题已切换", theme);
    }

    /* ==================== 「光影幻境」主题（magic-bg）的背景图 ==================== */

    /**
     * 上传背景图：存图 + 自动取色，返回 图片URL 和这套配色（用户确认后才应用主题）
     */
    @PostMapping("/bg")
    public Result<UserBgVO> uploadBg(@RequestParam("file") MultipartFile file) {
        return Result.success("背景图已上传，确认配色后即可应用",
                userBgService.upload(UserContext.requireUserId(), file));
    }

    /** 保存用户确认的配色（主色 / 深色 / 浅色） */
    @PutMapping("/bg/palette")
    public Result<String> updateBgPalette(@Valid @RequestBody UpdateBgPaletteDTO dto) {
        return Result.success("配色已保存", userBgService.updatePalette(UserContext.requireUserId(),
                dto.getPrimaryColor(), dto.getDarkColor(), dto.getLightColor()));
    }

    /** 删除背景图：回落到默认的「白底 + 黑色星星」背景 */
    @DeleteMapping("/bg")
    public Result<Void> clearBg() {
        userBgService.clear(UserContext.requireUserId());
        return Result.success("背景图已删除", null);
    }

    /* ==================== 自定义主题（图片取色） ==================== */

    /** 查询当前用户的全部自定义主题 */
    @GetMapping("/themes")
    public Result<List<UserThemeVO>> myThemes() {
        return Result.success(userThemeService.list(UserContext.requireUserId()));
    }

    /** 保存一套自定义主题（可以无限制添加，每个用户最多 50 套） */
    @PostMapping("/themes")
    public Result<UserThemeVO> saveMyTheme(@Valid @RequestBody UserThemeSaveDTO dto) {
        return Result.success("自定义主题已保存", userThemeService.save(UserContext.requireUserId(), dto));
    }

    /** 删除自定义主题（只能删自己的；删的是正在用的主题时，账号主题回退到默认） */
    @DeleteMapping("/themes/{id}")
    public Result<Void> deleteMyTheme(@PathVariable Long id) {
        userThemeService.delete(UserContext.requireUserId(), id);
        return Result.success("自定义主题已删除", null);
    }

    /**
     * 上传图片 → 分析配色 → 返回一套「主色 / 深色 / 浅色」建议（不落库，用户命名后才保存）
     */
    @PostMapping("/themes/analyze")
    public Result<ThemeAnalyzeVO> analyzeTheme(@RequestParam("file") MultipartFile file) {
        return Result.success(userThemeService.analyze(file));
    }

    /** 修改密码 */
    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody UpdatePasswordDTO dto) {
        userService.updatePassword(UserContext.requireUserId(), dto);
        return Result.success("密码修改成功，请重新登录", null);
    }
}
