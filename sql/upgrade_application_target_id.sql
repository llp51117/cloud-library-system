-- =============================================================================
-- 增量升级脚本：operation 申请表的 target_id 改为「字符串 + 允许为空」
-- 背景：老库里 application.target_id 是 BIGINT，提交申请时填了文字就会报
--       Data truncated for column 'target_id'。
--       这里统一改成 VARCHAR(255) DEFAULT NULL：
--       1. 「批量删除图书」可以存 "1,2,3" 这种逗号分隔的多个 ID
--       2. 「其他」类型不填目标时存 NULL，不会报错
-- 适用：已经在运行的老库（cloud_library），可重复执行（幂等）
-- 执行：mysql -u root -p cloud_library < sql/upgrade_application_target_id.sql
-- =============================================================================

USE `cloud_library`;

SET @db_name = DATABASE();

-- -----------------------------------------------------------------------------
-- application.target_id → VARCHAR(255) DEFAULT NULL
-- -----------------------------------------------------------------------------
SET @need_modify = (SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = @db_name
                      AND TABLE_NAME = 'application'
                      AND COLUMN_NAME = 'target_id'
                      AND (DATA_TYPE <> 'varchar' OR IS_NULLABLE <> 'YES'));
SET @modify_sql = IF(@need_modify = 1,
    'ALTER TABLE `application` MODIFY COLUMN `target_id` VARCHAR(255) DEFAULT NULL COMMENT ''操作目标ID（批量操作时多个ID用英文逗号分隔）''',
    'SELECT 1');
PREPARE stmt FROM @modify_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
