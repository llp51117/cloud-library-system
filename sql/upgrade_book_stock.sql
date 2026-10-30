-- -----------------------------------------------------------------------------
-- 图书库存功能：book 表新增 stock 字段
-- 执行方式：USE 你的数据库; SOURCE sql/upgrade_book_stock.sql;
-- 说明：只在字段不存在时新增，可重复执行；已有图书统一初始化为默认库存 10。
-- -----------------------------------------------------------------------------

-- 1) 新增库存字段（MySQL 8 原生支持 IF NOT EXISTS；MariaDB 10 同样支持）
ALTER TABLE `book`
    ADD COLUMN IF NOT EXISTS `stock` INT DEFAULT 10 COMMENT '库存数量（可借余量）：借阅成功 -1，归还成功 +1，为 0 时只能预约';

-- 2) 老数据补默认值：NULL 或负数的一律改成 10
UPDATE `book` SET `stock` = 10 WHERE `stock` IS NULL OR `stock` < 0;
-- 这个没啥用，用下面这个
ALTER TABLE `book` ADD COLUMN `stock` INT DEFAULT 10 COMMENT '库存数量（可借余量）';

UPDATE `book` SET `stock` = 10 WHERE `stock` IS NULL OR `stock` < 0;