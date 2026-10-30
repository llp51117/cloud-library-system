-- ==========================================================
-- 增量升级：第 12 套特殊内置主题「光影幻境」（magic-bg）的背景图
-- 已运行的老库执行本脚本即可，新库直接用 init.sql（里面已有这两列）
-- 幂等：列已存在时不会重复添加
-- ==========================================================

SET @exist_bg_url := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'bg_url');
SET @sql_bg_url := IF(@exist_bg_url = 0,
    'ALTER TABLE `user` ADD COLUMN `bg_url` VARCHAR(500) DEFAULT NULL COMMENT ''「光影幻境」主题的背景图URL（theme=magic-bg 时生效）'' AFTER `theme`',
    'SELECT 1');
PREPARE s1 FROM @sql_bg_url; EXECUTE s1; DEALLOCATE PREPARE s1;

SET @exist_bg_palette := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'bg_palette');
SET @sql_bg_palette := IF(@exist_bg_palette = 0,
    'ALTER TABLE `user` ADD COLUMN `bg_palette` VARCHAR(200) DEFAULT NULL COMMENT ''「光影幻境」主题的配色：主色,深色,浅色（背景图自动取色）'' AFTER `bg_url`',
    'SELECT 1');
PREPARE s2 FROM @sql_bg_palette; EXECUTE s2; DEALLOCATE PREPARE s2;
