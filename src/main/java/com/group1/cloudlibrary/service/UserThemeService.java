package com.group1.cloudlibrary.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.group1.cloudlibrary.common.BusinessException;
import com.group1.cloudlibrary.dto.UserThemeSaveDTO;
import com.group1.cloudlibrary.entity.User;
import com.group1.cloudlibrary.entity.UserTheme;
import com.group1.cloudlibrary.mapper.UserMapper;
import com.group1.cloudlibrary.mapper.UserThemeMapper;
import com.group1.cloudlibrary.vo.ThemeAnalyzeVO;
import com.group1.cloudlibrary.vo.UserThemeVO;
import com.group1.cloudlibrary.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 用户自定义主题：图片配色分析 + 自定义主题的增删查（存在 user_theme 表，按用户隔离）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserThemeService {

    /** 允许的图片后缀 */
    private static final List<String> ALLOW_EXT = List.of("jpg", "jpeg", "png", "gif", "webp", "bmp");
    /** 单张图片大小上限 */
    private static final long MAX_SIZE = 10 * 1024 * 1024L;
    /** 每个用户最多保存多少套自定义主题（防止无限刷） */
    private static final int MAX_PER_USER = 50;
    /** 颜色格式 #RRGGBB */
    private static final Pattern HEX = Pattern.compile("^#[0-9a-fA-F]{6}$");
    /** 分析时把图片压到多宽：越小越快，100×100 足够统计主要颜色 */
    private static final int ANALYZE_WIDTH = 100;
    /** 色差小于这个值就合并（RGB 直线距离） */
    private static final double MERGE_DISTANCE = 30.0;
    /** 最多提取几个主要颜色 */
    private static final int MAX_COLORS = 12;
    /** 至少提取几个主要颜色（纯色图会用深浅变体补齐） */
    private static final int MIN_COLORS = 8;
    /** 合并阈值放宽档：颜色不够时用它再合并一次，尽量凑够 8 个 */
    private static final double MERGE_DISTANCE_RELAXED = 20.0;
    /** 配色最多几个色（主色/深色/浅色/按钮/边框/Tab选中/文字高亮/卡片边框 …） */
    private static final int MAX_PALETTE = 12;

    private final UserThemeMapper userThemeMapper;
    private final UserMapper userMapper;

    /** 查询当前用户的全部自定义主题（按创建时间正序） */
    public List<UserThemeVO> list(Long userId) {
        List<UserTheme> list = userThemeMapper.selectList(Wrappers.<UserTheme>lambdaQuery()
                .eq(UserTheme::getUserId, userId)
                .orderByAsc(UserTheme::getCreatedAt)
                .orderByAsc(UserTheme::getId));
        return list.stream().map(UserThemeVO::from).toList();
    }

    /** 保存一套自定义主题（可以无限制添加，上限 50 套） */
    @Transactional(rollbackFor = Exception.class)
    public UserThemeVO save(Long userId, UserThemeSaveDTO dto) {
        String name = dto.getName() == null ? "" : dto.getName().trim();
        if (!StringUtils.hasText(name)) {
            throw BusinessException.of("请给这套颜色起个名字");
        }
        if (name.length() > 30) {
            throw BusinessException.of("主题名不能超过 30 个字符");
        }
        requireHex(dto.getPrimaryColor(), "主色");
        requireHex(dto.getDarkColor(), "深色");
        requireHex(dto.getLightColor(), "浅色");

        Long count = userThemeMapper.selectCount(Wrappers.<UserTheme>lambdaQuery()
                .eq(UserTheme::getUserId, userId));
        if (count != null && count >= MAX_PER_USER) {
            throw BusinessException.of("自定义主题最多保存 " + MAX_PER_USER + " 套，请先删除一些");
        }

        UserTheme entity = new UserTheme();
        entity.setUserId(userId);
        entity.setName(name);
        entity.setPrimaryColor(dto.getPrimaryColor().toUpperCase());
        entity.setDarkColor(dto.getDarkColor().toUpperCase());
        entity.setLightColor(dto.getLightColor().toUpperCase());
        // 整套配色（主色/深色/浅色/按钮/边框/Tab选中/文字高亮/卡片边框 …），逗号分隔存库
        entity.setPalette(joinPalette(dto.getPalette(), entity.getPrimaryColor(),
                entity.getDarkColor(), entity.getLightColor()));
        entity.setCreatedAt(LocalDateTime.now());
        userThemeMapper.insert(entity);
        return UserThemeVO.from(entity);
    }

    /**
     * 删除自定义主题：只能删自己的；
     * 如果删的正好是当前正在用的主题，把账号主题回退到默认「量子脉冲蓝」。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        UserTheme entity = userThemeMapper.selectOne(Wrappers.<UserTheme>lambdaQuery()
                .eq(UserTheme::getId, id)
                .eq(UserTheme::getUserId, userId));
        if (entity == null) {
            throw BusinessException.of("主题不存在或已被删除");
        }
        userThemeMapper.deleteById(id);

        User user = userMapper.selectById(userId);
        if (user != null && UserThemeVO.keyOf(id).equals(user.getTheme())) {
            user.setTheme(UserVO.DEFAULT_THEME);
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(user);
        }
    }

    /** 判断某个自定义主题标识是否属于该用户 */
    public boolean ownedByUser(Long userId, String key) {
        if (!StringUtils.hasText(key) || !key.startsWith(UserThemeVO.KEY_PREFIX)) {
            return false;
        }
        String idText = key.substring(UserThemeVO.KEY_PREFIX.length());
        try {
            Long id = Long.valueOf(idText);
            Long count = userThemeMapper.selectCount(Wrappers.<UserTheme>lambdaQuery()
                    .eq(UserTheme::getId, id)
                    .eq(UserTheme::getUserId, userId));
            return count != null && count > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // ==========================================================
    // 图片配色分析
    // ==========================================================

    /**
     * 分析一张图片的配色：
     * 缩放 100×100 → 按 5bit/通道量化统计 → 色差小于 30 的合并 → 取出现频率最高的 5~8 个主要颜色；
     * 再从这些颜色里「随机」挑 3 个，按明暗排成 深色 / 主色 / 浅色（每次上传 / 重新分析都换一套）。
     */
    public ThemeAnalyzeVO analyze(MultipartFile file) {
        BufferedImage image = readImage(file);
        BufferedImage small = scaleDown(image);

        Map<Integer, Bucket> buckets = new HashMap<>();
        for (int y = 0; y < small.getHeight(); y++) {
            for (int x = 0; x < small.getWidth(); x++) {
                int rgb = small.getRGB(x, y);
                int r = (rgb >> 16) & 255;
                int g = (rgb >> 8) & 255;
                int b = rgb & 255;
                // 纯黑噪点不算主要颜色（白色要保留，很多图的主色就是白）
                if (Math.max(r, Math.max(g, b)) < 12) {
                    continue;
                }
                // 每通道压到 5 位（32 级）做量化
                int key = ((r >> 3) << 10) | ((g >> 3) << 5) | (b >> 3);
                Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket());
                bucket.count++;
                bucket.r += r;
                bucket.g += g;
                bucket.b += b;
            }
        }
        if (buckets.isEmpty()) {
            throw BusinessException.of("无法识别该图片，请换一张试试");
        }

        // 出现频率从高到低
        List<int[]> ranked = buckets.values().stream()
                .sorted((a, b) -> Integer.compare(b.count, a.count))
                .map(b -> new int[]{b.r / b.count, b.g / b.count, b.b / b.count})
                .collect(Collectors.toList());

        // 色差小于 30 的合并（保留频率高的那个），最多留 12 个
        List<int[]> main = mergeSimilar(ranked, MERGE_DISTANCE, MAX_COLORS);
        // 颜色太少（纯色图 / 低对比图）：放宽合并阈值再提一次，尽量凑够 8 个
        if (main.size() < MIN_COLORS) {
            main = mergeSimilar(ranked, MERGE_DISTANCE_RELAXED, MAX_COLORS);
        }
        // 还是不够（比如纯色图）就用主色的深浅变体补齐到至少 8 个
        main = ensureMinSize(main, MIN_COLORS);

        List<int[]> trio = pickRandomTrio(main);

        ThemeAnalyzeVO vo = new ThemeAnalyzeVO();
        vo.setColors(main.stream().map(c -> hex(c)).toList());
        vo.setPrimaryColor(hex(trio.get(1)));
        vo.setDarkColor(hex(trio.get(0)));
        vo.setLightColor(hex(trio.get(2)));
        float[] hsl = rgbToHsl(trio.get(1)[0], trio.get(1)[1], trio.get(1)[2]);
        vo.setSuggestName(suggestName(hsl[0], hsl[1], hsl[2]));
        return vo;
    }

    /**
     * 合并相近颜色：和已有颜色的 RGB 直线距离小于 threshold 就合并进去（数量加权平均）
     * @param ranked 按出现频率排好的颜色
     * @param threshold 合并阈值（30）
     * @param max 最多保留几个
     */
    private static List<int[]> mergeSimilar(List<int[]> ranked, double threshold, int max) {
        List<int[]> result = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (int[] color : ranked) {
            int hit = -1;
            for (int i = 0; i < result.size(); i++) {
                if (distance(color, result.get(i)) < threshold) {
                    hit = i;
                    break;
                }
            }
            if (hit >= 0) {
                // 数量加权平均：这里用「已有次数 : 1」近似（本来就用桶的均值）
                int[] old = result.get(hit);
                int n = counts.get(hit);
                result.set(hit, new int[]{
                        (old[0] * n + color[0]) / (n + 1),
                        (old[1] * n + color[1]) / (n + 1),
                        (old[2] * n + color[2]) / (n + 1)
                });
                counts.set(hit, n + 1);
            } else {
                result.add(color);
                counts.add(1);
            }
            if (result.size() >= max) {
                break;
            }
        }
        return result;
    }

    /** 颜色不够时，用主色的深浅变体补齐（纯色图也能凑够 5 个） */
    private static List<int[]> ensureMinSize(List<int[]> colors, int min) {
        List<int[]> result = new ArrayList<>(colors);
        if (result.isEmpty()) {
            return result;
        }
        int[] base = result.get(0);
        float[] hsl = rgbToHsl(base[0], base[1], base[2]);
        double[] deltas = {-0.14, 0.14, -0.26, 0.26, -0.38, 0.38, -0.48, 0.48, 0.62, -0.62, 0.7};
        int i = 0;
        while (result.size() < min && i < deltas.length) {
            float l = (float) Math.min(0.96, Math.max(0.06, hsl[2] + deltas[i]));
            result.add(hslToRgb(hsl[0], Math.max(hsl[1], 0.28f), l));
            i++;
        }
        return result;
    }

    /**
     * 从主要颜色里随机挑 3 个，再按明暗排序成「深色 / 主色 / 浅色」
     * （随机 = 每次上传 / 重新分析都会换一套）
     */
    private static List<int[]> pickRandomTrio(List<int[]> colors) {
        List<int[]> pool = new ArrayList<>(colors);
        Collections.shuffle(pool, ThreadLocalRandom.current());
        List<int[]> picked = new ArrayList<>(pool.subList(0, Math.min(3, pool.size())));
        picked.sort(Comparator.comparingDouble(UserThemeService::luminance));
        if (picked.size() == 1) {
            return List.of(picked.get(0), picked.get(0), picked.get(0));
        }
        if (picked.size() == 2) {
            return List.of(picked.get(0), picked.get(0), picked.get(1));
        }
        return picked;
    }

    /** 视亮度：数值越大越亮 */
    private static double luminance(int[] rgb) {
        return 0.299 * rgb[0] + 0.587 * rgb[1] + 0.114 * rgb[2];
    }

    /** 两个颜色的 RGB 直线距离 */
    private static double distance(int[] a, int[] b) {
        int dr = a[0] - b[0];
        int dg = a[1] - b[1];
        int db = a[2] - b[2];
        return Math.sqrt(dr * dr + dg * dg + db * db);
    }

    // ==========================================================
    // 工具方法
    // ==========================================================

    /**
     * 把前端分配好的整套配色存成逗号分隔的字符串：
     * 顺序固定为 主色, 深色, 浅色, 按钮, 边框, Tab选中, 文字高亮, 卡片边框（后面还能接更多）
     * 不足 8 个时用主 / 深 / 浅 补齐，保证前端取回来一定能按角色还原。
     */
    private String joinPalette(List<String> palette, String... fallback) {
        List<String> valid = new ArrayList<>();
        if (palette != null) {
            for (String color : palette) {
                if (color == null) {
                    continue;
                }
                String hex = color.trim();
                if (HEX.matcher(hex).matches()) {
                    valid.add(hex.toUpperCase());
                }
                if (valid.size() >= MAX_PALETTE) {
                    break;
                }
            }
        }
        int i = 0;
        while (valid.size() < 8 && fallback.length > 0) {
            String f = fallback[i % fallback.length];
            if (f == null) {
                break;
            }
            valid.add(f.toUpperCase());
            i++;
        }
        return valid.isEmpty() ? null : String.join(",", valid);
    }

    /** 存库的 palette 字符串 → 颜色列表 */
    public static List<String> splitPalette(String palette) {
        if (!StringUtils.hasText(palette)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String part : palette.split(",")) {
            String hex = part.trim();
            if (HEX.matcher(hex).matches()) {
                result.add(hex.toUpperCase());
            }
        }
        return result;
    }

    private BufferedImage readImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.of("请选择一张图片");
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
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
            if (image == null) {
                throw BusinessException.of("无法识别该图片，请换一张试试");
            }
            return image;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("图片配色分析失败", e);
            throw BusinessException.of("无法识别该图片，请换一张试试");
        }
    }

    private BufferedImage scaleDown(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        if (w <= 0 || h <= 0) {
            throw BusinessException.of("无法识别该图片，请换一张试试");
        }
        int nw = Math.min(w, ANALYZE_WIDTH);
        int nh = Math.max(1, Math.round(nw * (float) h / w));
        BufferedImage small = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = small.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(src, 0, 0, nw, nh, null);
        } finally {
            g.dispose();
        }
        return small;
    }

    /** 统计色箱：同一箱内的像素累加后取平均 */
    private static class Bucket {
        int count;
        int r;
        int g;
        int b;
    }

    private static void requireHex(String color, String label) {
        if (color == null || !HEX.matcher(color.trim()).matches()) {
            throw BusinessException.of(label + "格式不正确");
        }
    }

    /** rgb → hsl：h 0-360，s / l 0-1 */
    private static float[] rgbToHsl(int r, int g, int b) {
        float rr = r / 255f;
        float gg = g / 255f;
        float bb = b / 255f;
        float max = Math.max(rr, Math.max(gg, bb));
        float min = Math.min(rr, Math.min(gg, bb));
        float l = (max + min) / 2f;
        float d = max - min;
        float h = 0f;
        float s = 0f;
        if (d > 0.0001f) {
            s = l > 0.5f ? d / (2f - max - min) : d / (max + min);
            if (max == rr) {
                h = (gg - bb) / d + (gg < bb ? 6 : 0);
            } else if (max == gg) {
                h = (bb - rr) / d + 2;
            } else {
                h = (rr - gg) / d + 4;
            }
            h *= 60f;
        }
        return new float[]{h, s, l};
    }

    /** hsl → rgb：h 0-360，s / l 0-1 */
    private static int[] hslToRgb(float h, float s, float l) {
        float hh = ((h % 360) + 360) % 360;
        float c = (1 - Math.abs(2 * l - 1)) * s;
        float x = c * (1 - Math.abs(((hh / 60f) % 2) - 1));
        float m = l - c / 2f;
        float r = 0;
        float g = 0;
        float b = 0;
        if (hh < 60) {
            r = c;
            g = x;
        } else if (hh < 120) {
            r = x;
            g = c;
        } else if (hh < 180) {
            g = c;
            b = x;
        } else if (hh < 240) {
            g = x;
            b = c;
        } else if (hh < 300) {
            r = x;
            b = c;
        } else {
            r = c;
            b = x;
        }
        return new int[]{
                Math.round((r + m) * 255),
                Math.round((g + m) * 255),
                Math.round((b + m) * 255)
        };
    }

    private static String hex(int[] rgb) {
        return String.format("#%02X%02X%02X",
                Math.min(255, Math.max(0, rgb[0])),
                Math.min(255, Math.max(0, rgb[1])),
                Math.min(255, Math.max(0, rgb[2])));
    }

    /** 按色相给个默认名字，用户可自行修改 */
    private static String suggestName(float h, float s, float l) {
        if (s < 0.15f) {
            return l > 0.6f ? "素雅白" : "静谧灰";
        }
        String[] names = {"赤焰红", "暖阳橙", "琥珀金", "青柠黄", "初芽绿",
                "薄荷青", "晴空蓝", "深海蓝", "星夜紫", "霓虹粉"};
        int idx = (int) (h / 36) % names.length;
        return names[idx];
    }
}
