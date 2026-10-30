-- =============================================================================
-- 增量升级脚本：管理员封号（user.admin_title） + 管理员操作记录（admin_operation_log）
-- 适用：已经在运行的老库（cloud_library），可重复执行（幂等，列/表已存在就跳过）
-- 执行：mysql -u root -p cloud_library < sql/upgrade_admin_title_operation_log.sql
-- =============================================================================

USE `cloud_library`;

SET @db_name = DATABASE();

-- -----------------------------------------------------------------------------
-- 1. user 表：新增「管理员封号」字段
--    例：图书管理专员 / 用户管理专员 / 评论管理专员，由最高管理员设置
-- -----------------------------------------------------------------------------
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'user' AND COLUMN_NAME = 'admin_title');
SET @add_title = IF(@col_exists = 0,
    'ALTER TABLE `user` ADD COLUMN `admin_title` VARCHAR(50) DEFAULT NULL COMMENT ''管理员封号'' AFTER `pending_admin_deadline`',
    'SELECT 1');
PREPARE stmt FROM @add_title;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 2. 新增「管理员操作记录表」admin_operation_log
--    管理端删除评论 / 处理举报 / 编辑图书时写入一条，用于管理员主页展示操作统计
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `admin_operation_log` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `admin_id`   BIGINT       NOT NULL COMMENT '操作管理员ID',
    `type`       TINYINT      NOT NULL COMMENT '操作类型：1删除评论/2处理举报/3编辑图书',
    `target_id`  BIGINT       DEFAULT NULL COMMENT '操作对象ID（评论ID / 图书ID）',
    `detail`     VARCHAR(255) DEFAULT NULL COMMENT '操作说明',
    `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_admin_type` (`admin_id`, `type`),
    KEY `idx_created_at` (`created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '管理员操作记录表';

-- =============================================================================
-- 升级完成
-- 验证：
--   SHOW COLUMNS FROM `user` LIKE 'admin_title';
--   SHOW TABLES LIKE 'admin_operation_log';
-- =============================================================================
