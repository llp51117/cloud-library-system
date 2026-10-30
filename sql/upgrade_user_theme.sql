-- =============================================================================
-- 增量升级脚本：用户自定义主题（图片取色）
-- 适用：已经在运行的老库（cloud_library），可重复执行（表已存在就跳过）
-- 执行：mysql -u root -p cloud_library < sql/upgrade_user_theme.sql
-- =============================================================================

USE `cloud_library`;

-- -----------------------------------------------------------------------------
-- 1. 用户自定义主题表 user_theme
--    用户上传图片 → 后端分析配色 → 命名后存到这里，按 user_id 隔离，只本人可见
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user_theme` (
    `id`            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主题ID',
    `user_id`       BIGINT      NOT NULL COMMENT '用户ID（主题归属，只本人可见）',
    `name`          VARCHAR(30) NOT NULL COMMENT '用户命名的主题名，如「我的夏日」',
    `primary_color` VARCHAR(20) NOT NULL COMMENT '主色，如 #00D4FF',
    `dark_color`    VARCHAR(20) NOT NULL COMMENT '深色（主色加深）',
    `light_color`   VARCHAR(20) NOT NULL COMMENT '浅色（主色变浅）',
    `created_at`    DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_theme_user` (`user_id`),
    CONSTRAINT `fk_user_theme_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户自定义主题表';

-- -----------------------------------------------------------------------------
-- 2. 已下线的内置主题「超新星爆闪黄」：老数据回退到默认主题「量子脉冲蓝」
--    （和「熔核爆裂橙」太像，已从主题列表移除）
-- -----------------------------------------------------------------------------
UPDATE `user` SET `theme` = 'quantum-blue' WHERE `theme` = 'supernova-yellow';

-- -----------------------------------------------------------------------------
-- 3. 自定义主题新增 palette 字段：把图片里提取出的 8~12 个主要颜色，
--    按角色（主色 / 深色 / 浅色 / 按钮 / 边框 / Tab选中 / 文字高亮 / 卡片边框）
--    分配好后整套存下来，下次应用这套主题时每个部位都用它自己的颜色。
--    逗号分隔的 #RRGGBB，可重复执行（字段已存在就跳过）。
-- -----------------------------------------------------------------------------
SET @exist_palette := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user_theme'
      AND COLUMN_NAME = 'palette'
);
SET @sql_palette := IF(@exist_palette = 0,
    'ALTER TABLE `user_theme` ADD COLUMN `palette` VARCHAR(240) DEFAULT NULL COMMENT ''整套配色：逗号分隔的 #RRGGBB（主色,深色,浅色,按钮,边框,Tab选中,文字高亮,卡片边框）'' AFTER `light_color`',
    'SELECT 1');
PREPARE stmt_palette FROM @sql_palette;
EXECUTE stmt_palette;
DEALLOCATE PREPARE stmt_palette;

-- -----------------------------------------------------------------------------
-- 4. 新用户默认主题改成「极光圣殿白」（aurora-white）
--    只改列的默认值，老用户已保存的主题不动。
-- -----------------------------------------------------------------------------
ALTER TABLE `user` ALTER COLUMN `theme` SET DEFAULT 'aurora-white';
