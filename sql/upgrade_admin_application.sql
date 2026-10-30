-- =============================================================================
-- 增量升级脚本：管理员权限升级 + 待激活管理员 + 操作申请审批
-- 适用：已经在运行的老库（cloud_library），可重复执行（幂等，列/表已存在就跳过）
-- 执行：mysql -u root -p cloud_library < sql/upgrade_admin_application.sql
-- =============================================================================

USE `cloud_library`;

SET @db_name = DATABASE();

-- -----------------------------------------------------------------------------
-- 1. user 表：新增「最高管理员 / 待激活管理员 / 待激活截止时间」三个字段
-- -----------------------------------------------------------------------------
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'user' AND COLUMN_NAME = 'is_super_admin');
SET @add_super = IF(@col_exists = 0,
    'ALTER TABLE `user` ADD COLUMN `is_super_admin` TINYINT DEFAULT 0 COMMENT ''是否最高管理员：0否/1是'' AFTER `role`',
    'SELECT 1');
PREPARE stmt FROM @add_super;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'user' AND COLUMN_NAME = 'pending_admin');
SET @add_pending = IF(@col_exists = 0,
    'ALTER TABLE `user` ADD COLUMN `pending_admin` TINYINT DEFAULT 0 COMMENT ''待激活管理员：0否/1是'' AFTER `is_super_admin`',
    'SELECT 1');
PREPARE stmt FROM @add_pending;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'user' AND COLUMN_NAME = 'pending_admin_deadline');
SET @add_deadline = IF(@col_exists = 0,
    'ALTER TABLE `user` ADD COLUMN `pending_admin_deadline` DATETIME DEFAULT NULL COMMENT ''待激活截止时间（10 天内未归还全部图书则转换失败）'' AFTER `pending_admin`',
    'SELECT 1');
PREPARE stmt FROM @add_deadline;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 2. 把 admin@library.com 设为最高管理员（超管）
-- -----------------------------------------------------------------------------
UPDATE `user` SET `is_super_admin` = 1 WHERE `email` = 'admin@library.com';

-- -----------------------------------------------------------------------------
-- 3. 操作申请表 application
--    普通管理员提交「删除用户 / 注销用户 / 批量删除图书」等操作申请，由超管审批
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `application` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '申请ID',
    `applicant_id` BIGINT       NOT NULL COMMENT '申请人ID（普通管理员）',
    `type`         VARCHAR(30)  NOT NULL COMMENT '申请类型：delete_user/batch_delete_book/cancel_user/other',
    `target_id`    VARCHAR(255) DEFAULT NULL COMMENT '操作目标ID（批量操作时多个ID用英文逗号分隔）',
    `content`      VARCHAR(500) DEFAULT NULL COMMENT '申请理由',
    `status`       TINYINT      DEFAULT 0 COMMENT '状态：0待审批/1已同意/2已拒绝',
    `reply`        VARCHAR(500) DEFAULT NULL COMMENT '最高管理员的回复',
    `created_at`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `reply_time`   DATETIME     DEFAULT NULL COMMENT '审批时间',
    PRIMARY KEY (`id`),
    KEY `idx_application_applicant` (`applicant_id`),
    KEY `idx_application_status` (`status`),
    CONSTRAINT `fk_application_applicant` FOREIGN KEY (`applicant_id`) REFERENCES `user` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '操作申请表';
