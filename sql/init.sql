-- =============================================================================
-- 云图书管理系统 - 数据库初始化脚本
-- 对应设计文档 6.1.2 核心表结构，字段与后端 entity 包保持一致
--
-- 执行方式（任选其一）：
--   1) 命令行：mysql -u root -p < sql/init.sql
--   2) Navicat / DataGrip 等工具：新建查询窗口，粘贴全部内容后执行
--   3) IDEA：打开本文件，选择数据库连接后点击执行
--
-- 注意：脚本包含 DROP TABLE，重复执行会清空已有数据，请谨慎用于生产环境。
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `cloud_library`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE `cloud_library`;

-- 按外键依赖倒序清理，保证脚本可重复执行
DROP TABLE IF EXISTS `admin_operation_log`;
DROP TABLE IF EXISTS `application`;
DROP TABLE IF EXISTS `search_log`;
DROP TABLE IF EXISTS `announcement_read`;
DROP TABLE IF EXISTS `announcement`;
DROP TABLE IF EXISTS `verification_code`;
DROP TABLE IF EXISTS `notification`;
DROP TABLE IF EXISTS `feedback`;
DROP TABLE IF EXISTS `user_theme`;
DROP TABLE IF EXISTS `comment_report`;
DROP TABLE IF EXISTS `comment_dislike`;
DROP TABLE IF EXISTS `comment_like`;
DROP TABLE IF EXISTS `comment`;
DROP TABLE IF EXISTS `favorite`;
DROP TABLE IF EXISTS `reservation`;
DROP TABLE IF EXISTS `borrow`;
DROP TABLE IF EXISTS `book`;
DROP TABLE IF EXISTS `user`;

-- -----------------------------------------------------------------------------
-- 1. 用户表 user
-- -----------------------------------------------------------------------------
CREATE TABLE `user` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `email`          VARCHAR(100) NOT NULL COMMENT '邮箱（登录账号）',
    `nickname`       VARCHAR(50)  DEFAULT NULL COMMENT '昵称，默认取邮箱 @ 前面的部分',
    `password`       VARCHAR(255) NOT NULL COMMENT '密码（BCrypt 加密存储）',
    `avatar`         VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    `role`           TINYINT      DEFAULT 0 COMMENT '角色：0普通用户/1管理员',
    `is_super_admin` TINYINT      DEFAULT 0 COMMENT '是否最高管理员：0否/1是',
    `pending_admin`  TINYINT      DEFAULT 0 COMMENT '待激活管理员：0否/1是',
    `pending_admin_deadline` DATETIME DEFAULT NULL COMMENT '待激活截止时间（10 天内未归还全部图书则转换失败）',
    `admin_title`    VARCHAR(50)  DEFAULT NULL COMMENT '管理员封号',
    `theme`          VARCHAR(50)  DEFAULT 'aurora-white' COMMENT '用户主题（皮肤），新用户默认极光圣殿白',
    `bg_url`         VARCHAR(500) DEFAULT NULL COMMENT '「光影幻境」主题的背景图URL（theme=magic-bg 时生效）',
    `bg_palette`     VARCHAR(200) DEFAULT NULL COMMENT '「光影幻境」主题的配色：主色,深色,浅色（背景图自动取色）',
    `status`         TINYINT      DEFAULT 1 COMMENT '状态：0注销/1正常/2禁用',
    `ban_reason`     VARCHAR(255) DEFAULT NULL COMMENT '禁用原因',
    `ban_days`       INT          DEFAULT 0 COMMENT '禁用天数',
    `ban_start_time` DATETIME     DEFAULT NULL COMMENT '禁用开始时间',
    `created_at`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `updated_at`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_email` (`email`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户表';

-- -----------------------------------------------------------------------------
-- 1.1 操作申请表 application
--     普通管理员提交「删除用户 / 注销用户 / 批量删除图书」等操作申请，由最高管理员审批
-- -----------------------------------------------------------------------------
CREATE TABLE `application` (
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

-- -----------------------------------------------------------------------------
-- 2. 图书表 book
-- -----------------------------------------------------------------------------
CREATE TABLE `book` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '图书ID',
    `title`          VARCHAR(200)  NOT NULL COMMENT '书名',
    `author`         VARCHAR(100)  NOT NULL COMMENT '作者',
    `publisher`      VARCHAR(200)  DEFAULT NULL COMMENT '出版社',
    `platform`       VARCHAR(50)   DEFAULT NULL COMMENT '平台（起点/晋江/番茄等）',
    `price`          DECIMAL(10, 2) DEFAULT NULL COMMENT '价格',
    `publish_date`   DATE          DEFAULT NULL COMMENT '出版时间',
    `cover_url`      VARCHAR(500)  DEFAULT NULL COMMENT '封面图URL',
    `cover_card_crop`   VARCHAR(500) DEFAULT NULL COMMENT '封面裁剪参数（首页卡片横向），JSON 字符串，空表示不裁剪',
    `cover_detail_crop` VARCHAR(500) DEFAULT NULL COMMENT '封面裁剪参数（详情页竖向），JSON 字符串，空表示不裁剪',
    `description`    TEXT          COMMENT '简介',
    `is_finished`    TINYINT       DEFAULT 0 COMMENT '是否完结：0连载中/1已完结',
    `stock`          INT           DEFAULT 10 COMMENT '库存数量（可借余量）：借阅成功 -1，归还成功 +1，为 0 时只能预约',
    `borrow_count`   INT           DEFAULT 0 COMMENT '借阅次数',
    `favorite_count` INT           DEFAULT 0 COMMENT '收藏人数',
    `search_count`   INT           DEFAULT 0 COMMENT '搜索次数',
    `comment_pinned` TINYINT       DEFAULT 0 COMMENT '评论管理置顶：0否/1是',
    `created_at`     DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '录入时间',
    `updated_at`     DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_book_title` (`title`),
    KEY `idx_book_platform` (`platform`),
    KEY `idx_book_favorite_count` (`favorite_count`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '图书表';
-- 说明：设计文档中的 embedding_vector（语义向量）由 Milvus 侧存储，当前实现未落库，故不建该字段。

-- -----------------------------------------------------------------------------
-- 3. 借阅记录表 borrow
-- -----------------------------------------------------------------------------
CREATE TABLE `borrow` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id`     BIGINT   NOT NULL COMMENT '用户ID',
    `book_id`     BIGINT   NOT NULL COMMENT '图书ID',
    `borrow_date` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '借阅时间',
    `due_date`    DATETIME DEFAULT NULL COMMENT '应还时间（借阅日期+借阅期限天数，当前配置10天）',
    `return_date` DATETIME DEFAULT NULL COMMENT '实际归还时间',
    `status`      TINYINT  DEFAULT 0 COMMENT '状态：0借阅中/1已归还/2逾期',
    `return_type` TINYINT  DEFAULT 0 COMMENT '归还方式：0用户自己归还/1管理员强制归还',
    `renew_count` INT      DEFAULT 0 COMMENT '续借次数',
    `created_at`  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_borrow_user` (`user_id`),
    KEY `idx_borrow_book` (`book_id`),
    KEY `idx_borrow_status` (`status`),
    CONSTRAINT `fk_borrow_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_borrow_book` FOREIGN KEY (`book_id`) REFERENCES `book` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '借阅记录表';

-- -----------------------------------------------------------------------------
-- 4. 预约记录表 reservation
-- -----------------------------------------------------------------------------
CREATE TABLE `reservation` (
    `id`               BIGINT   NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id`          BIGINT   NOT NULL COMMENT '用户ID',
    `book_id`          BIGINT   NOT NULL COMMENT '图书ID',
    `reservation_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '预约时间',
    `available_time`   DATETIME DEFAULT NULL COMMENT '变为可借阅的时间',
    `status`           TINYINT  DEFAULT 0 COMMENT '状态：0等待借阅/1可借阅/2已借阅/3已取消/4超时取消',
    `created_at`       DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_reservation_user` (`user_id`),
    KEY `idx_reservation_book` (`book_id`),
    KEY `idx_reservation_status` (`status`),
    CONSTRAINT `fk_reservation_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_reservation_book` FOREIGN KEY (`book_id`) REFERENCES `book` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '预约记录表';

-- -----------------------------------------------------------------------------
-- 5. 收藏表 favorite
-- -----------------------------------------------------------------------------
CREATE TABLE `favorite` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id`    BIGINT   NOT NULL COMMENT '用户ID',
    `book_id`    BIGINT   NOT NULL COMMENT '图书ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_favorite_user_book` (`user_id`, `book_id`),
    KEY `idx_favorite_book` (`book_id`),
    CONSTRAINT `fk_favorite_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_favorite_book` FOREIGN KEY (`book_id`) REFERENCES `book` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '收藏表';

-- -----------------------------------------------------------------------------
-- 6. 评论表 comment
-- -----------------------------------------------------------------------------
CREATE TABLE `comment` (
    `id`            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '评论ID',
    `user_id`       BIGINT   NOT NULL COMMENT '用户ID',
    `book_id`       BIGINT   NOT NULL COMMENT '图书ID',
    `content`       TEXT     NOT NULL COMMENT '评论内容',
    `like_count`    INT      DEFAULT 0 COMMENT '点赞数',
    `dislike_count` INT      DEFAULT 0 COMMENT '踩数',
    `parent_id`     BIGINT   DEFAULT NULL COMMENT '父评论ID（楼中楼回复，只支持一层）',
    `status`        TINYINT  DEFAULT 0 COMMENT '状态：0正常/1系统删除/2管理员删除/3管理员恢复',
    `is_reported`   TINYINT  DEFAULT 0 COMMENT '是否被@管理员标记：0否/1是',
    `created_at`    DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
    PRIMARY KEY (`id`),
    KEY `idx_comment_user` (`user_id`),
    KEY `idx_comment_book` (`book_id`),
    KEY `idx_comment_status` (`status`),
    KEY `idx_comment_reported` (`is_reported`),
    KEY `idx_comment_parent` (`parent_id`),
    CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_comment_book` FOREIGN KEY (`book_id`) REFERENCES `book` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '评论表';

-- -----------------------------------------------------------------------------
-- 6.1 评论点赞记录表 comment_like
--     一个用户对同一条评论只能点赞一次，唯一索引保证去重；再点一次取消点赞即删除记录
-- -----------------------------------------------------------------------------
CREATE TABLE `comment_like` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id`    BIGINT   NOT NULL COMMENT '点赞用户ID',
    `comment_id` BIGINT   NOT NULL COMMENT '评论ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_comment_like` (`user_id`, `comment_id`),
    KEY `idx_comment_like_comment` (`comment_id`),
    CONSTRAINT `fk_comment_like_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_comment_like_comment` FOREIGN KEY (`comment_id`) REFERENCES `comment` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '评论点赞记录表';

-- -----------------------------------------------------------------------------
-- 6.1.1 评论点踩记录表 comment_dislike
--     一个用户对同一条评论只能踩一次，唯一索引保证去重；再点一次取消踩即删除记录。
--     与点赞互相独立（同一条评论可以既点赞又点踩）
-- -----------------------------------------------------------------------------
CREATE TABLE `comment_dislike` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id`    BIGINT   NOT NULL COMMENT '点踩用户ID',
    `comment_id` BIGINT   NOT NULL COMMENT '评论ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点踩时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_comment_dislike` (`user_id`, `comment_id`),
    KEY `idx_comment_dislike_comment` (`comment_id`),
    CONSTRAINT `fk_comment_dislike_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_comment_dislike_comment` FOREIGN KEY (`comment_id`) REFERENCES `comment` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '评论点踩记录表';

-- -----------------------------------------------------------------------------
-- 6.2 评论举报记录表 comment_report
--     一个用户对同一条评论只能 @管理员 一次（重复 @ 不重复计数），
--     多个用户举报同一条评论时行数累加，评论的举报次数按该表行数汇总
-- -----------------------------------------------------------------------------
CREATE TABLE `comment_report` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id`    BIGINT   NOT NULL COMMENT '举报用户ID',
    `comment_id` BIGINT   NOT NULL COMMENT '评论ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '举报时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_comment_report` (`user_id`, `comment_id`),
    KEY `idx_comment_report_comment` (`comment_id`),
    CONSTRAINT `fk_comment_report_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_comment_report_comment` FOREIGN KEY (`comment_id`) REFERENCES `comment` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '评论举报记录表';

-- -----------------------------------------------------------------------------
-- 7. 反馈表 feedback
-- -----------------------------------------------------------------------------
CREATE TABLE `feedback` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '反馈ID',
    `user_id`    BIGINT       NOT NULL COMMENT '用户ID',
    `type`       TINYINT      NOT NULL COMMENT '类型：0功能建议/1举报用户/2内容错误/3系统BUG/4其他',
    `title`      VARCHAR(200) NOT NULL COMMENT '标题',
    `content`    TEXT         NOT NULL COMMENT '内容',
    `status`     TINYINT      DEFAULT 0 COMMENT '状态：0待回复/1已回复',
    `reply`      TEXT         COMMENT '管理员回复内容',
    `reply_time` DATETIME     DEFAULT NULL COMMENT '回复时间',
    `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    PRIMARY KEY (`id`),
    KEY `idx_feedback_user` (`user_id`),
    KEY `idx_feedback_status` (`status`),
    CONSTRAINT `fk_feedback_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '反馈表';

-- -----------------------------------------------------------------------------
-- 7.1 用户自定义主题表 user_theme（上传图片取色后保存，只属于创建它的用户）
-- -----------------------------------------------------------------------------
CREATE TABLE `user_theme` (
    `id`            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主题ID',
    `user_id`       BIGINT      NOT NULL COMMENT '用户ID（主题归属，只本人可见）',
    `name`          VARCHAR(30) NOT NULL COMMENT '用户命名的主题名，如「我的夏日」',
    `primary_color` VARCHAR(20) NOT NULL COMMENT '主色，如 #00D4FF',
    `dark_color`    VARCHAR(20) NOT NULL COMMENT '深色（主色加深）',
    `light_color`   VARCHAR(20) NOT NULL COMMENT '浅色（主色变浅）',
    `palette`       VARCHAR(240) DEFAULT NULL COMMENT '整套配色：逗号分隔的 #RRGGBB（主色,深色,浅色,按钮,边框,Tab选中,文字高亮,卡片边框）',
    `created_at`    DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_theme_user` (`user_id`),
    CONSTRAINT `fk_user_theme_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户自定义主题表';

-- -----------------------------------------------------------------------------
-- 8. 通知表 notification
-- -----------------------------------------------------------------------------
CREATE TABLE `notification` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '通知ID',
    `user_id`    BIGINT       DEFAULT NULL COMMENT '用户ID（NULL 表示系统公告，所有用户可见）',
    `type`       TINYINT      NOT NULL COMMENT '类型：0系统公告/1借阅成功/2预约轮到/3预约超时/4还书成功/5逾期提醒/6反馈回复/7催还提醒',
    `title`      VARCHAR(200) NOT NULL COMMENT '标题',
    `content`    TEXT         NOT NULL COMMENT '内容',
    `is_read`    TINYINT      DEFAULT 0 COMMENT '是否已读：0未读/1已读',
    `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    PRIMARY KEY (`id`),
    KEY `idx_notification_user_read` (`user_id`, `is_read`),
    CONSTRAINT `fk_notification_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '通知表';

-- -----------------------------------------------------------------------------
-- 9. 验证码表 verification_code
-- -----------------------------------------------------------------------------
CREATE TABLE `verification_code` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `email`      VARCHAR(100) NOT NULL COMMENT '邮箱',
    `code`       VARCHAR(10)  NOT NULL COMMENT '验证码',
    `type`       TINYINT      DEFAULT 0 COMMENT '类型：0注册/1找回密码',
    `expired_at` DATETIME     DEFAULT NULL COMMENT '过期时间（有效期5分钟）',
    `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_code_email_type` (`email`, `type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '验证码表';

-- -----------------------------------------------------------------------------
-- 10. 公告表 announcement
-- -----------------------------------------------------------------------------
CREATE TABLE `announcement` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '公告ID',
    `title`      VARCHAR(200) NOT NULL COMMENT '标题',
    `content`    TEXT         NOT NULL COMMENT '内容',
    `admin_id`   BIGINT       DEFAULT NULL COMMENT '发布管理员ID',
    `status`     TINYINT      DEFAULT 1 COMMENT '状态：0已删除/1正常',
    `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    `updated_at` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_announcement_admin` (`admin_id`),
    KEY `idx_announcement_status` (`status`),
    CONSTRAINT `fk_announcement_admin` FOREIGN KEY (`admin_id`) REFERENCES `user` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '公告表';

-- -----------------------------------------------------------------------------
-- 公告已读记录表 announcement_read
--     公告是全员可见的，无法记录「已读」字段，因此单独用一张表记录
--     每个用户已读过的公告，用于计算「系统公告」Tab 的未读数量
-- -----------------------------------------------------------------------------
CREATE TABLE `announcement_read` (
    `id`              BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id`         BIGINT NOT NULL COMMENT '用户ID',
    `announcement_id` BIGINT NOT NULL COMMENT '公告ID',
    `created_at`      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '已读时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_announcement_read_user` (`user_id`, `announcement_id`),
    KEY `idx_announcement_read_announcement` (`announcement_id`),
    CONSTRAINT `fk_announcement_read_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_announcement_read_announcement` FOREIGN KEY (`announcement_id`) REFERENCES `announcement` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '公告已读记录表';

-- -----------------------------------------------------------------------------
-- 11. 搜索记录表 search_log
--     用于首页「热门搜索」关键词统计：个人高频词 + 全平台高频词
-- -----------------------------------------------------------------------------
CREATE TABLE `search_log` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id`    BIGINT       DEFAULT NULL COMMENT '搜索用户ID（未登录为NULL）',
    `keyword`    VARCHAR(100) NOT NULL COMMENT '搜索关键词',
    `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '搜索时间',
    PRIMARY KEY (`id`),
    KEY `idx_search_user_keyword` (`user_id`, `keyword`),
    KEY `idx_search_keyword` (`keyword`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '搜索记录表';

-- =============================================================================
-- 初始化数据
-- =============================================================================

-- 管理员账号：admin@library.com / admin123
-- 普通用户账号：user@library.com / 123456
-- （密码为 BCrypt 密文，由 spring-security-crypto 6.2.4 生成）
INSERT INTO `user` (`email`, `nickname`, `password`, `role`, `status`, `created_at`, `updated_at`)
VALUES ('admin@library.com', 'admin', '$2a$10$ZMqbVsgyRFaBfWrYgcatnePcfKCwIg9G6Mxg8xYF7iza7A1rDKfhq', 1, 1, NOW(), NOW()),
       ('user@library.com', 'user', '$2a$10$AcrRp6xXd0yBlYLG7Z3bPecV5Rql3CXH2ggDxJBO0V4nJVVEfGknW', 0, 1, NOW(), NOW());

-- 最高管理员（超管）：只有他能设置 / 取消其他管理员、审批操作申请
UPDATE `user` SET `is_super_admin` = 1 WHERE `email` = 'admin@library.com';

-- 已存在数据的库：补 nickname 列，并把历史用户的昵称回填为邮箱 @ 前缀
-- 下面这段是幂等的（列已存在就跳过 ALTER），全新库和老库都可以直接整份执行
SET @db_name = DATABASE();
SET @nickname_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
                        WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'user' AND COLUMN_NAME = 'nickname');
SET @add_nickname = IF(@nickname_exists = 0,
    'ALTER TABLE `user` ADD COLUMN `nickname` VARCHAR(50) DEFAULT NULL COMMENT ''昵称，默认取邮箱 @ 前面的部分'' AFTER `email`',
    'SELECT 1');
PREPARE stmt FROM @add_nickname;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
UPDATE `user`
SET `nickname` = SUBSTRING_INDEX(`email`, '@', 1)
WHERE `nickname` IS NULL OR `nickname` = '';

-- 老库补「最高管理员 / 待激活管理员 / 待激活截止时间」三列（幂等，已有列就跳过）
SET @super_col = (SELECT COUNT(*) FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'user' AND COLUMN_NAME = 'is_super_admin');
SET @add_super_col = IF(@super_col = 0,
    'ALTER TABLE `user` ADD COLUMN `is_super_admin` TINYINT DEFAULT 0 COMMENT ''是否最高管理员：0否/1是'' AFTER `role`',
    'SELECT 1');
PREPARE stmt FROM @add_super_col;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @pending_col = (SELECT COUNT(*) FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'user' AND COLUMN_NAME = 'pending_admin');
SET @add_pending_col = IF(@pending_col = 0,
    'ALTER TABLE `user` ADD COLUMN `pending_admin` TINYINT DEFAULT 0 COMMENT ''待激活管理员：0否/1是'' AFTER `is_super_admin`',
    'SELECT 1');
PREPARE stmt FROM @add_pending_col;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @deadline_col = (SELECT COUNT(*) FROM information_schema.COLUMNS
                     WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'user' AND COLUMN_NAME = 'pending_admin_deadline');
SET @add_deadline_col = IF(@deadline_col = 0,
    'ALTER TABLE `user` ADD COLUMN `pending_admin_deadline` DATETIME DEFAULT NULL COMMENT ''待激活截止时间'' AFTER `pending_admin`',
    'SELECT 1');
PREPARE stmt FROM @add_deadline_col;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 老库补「管理员封号」列（幂等，已有列就跳过）
SET @admin_title_col = (SELECT COUNT(*) FROM information_schema.COLUMNS
                        WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'user' AND COLUMN_NAME = 'admin_title');
SET @add_admin_title_col = IF(@admin_title_col = 0,
    'ALTER TABLE `user` ADD COLUMN `admin_title` VARCHAR(50) DEFAULT NULL COMMENT ''管理员封号'' AFTER `pending_admin_deadline`',
    'SELECT 1');
PREPARE stmt FROM @add_admin_title_col;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 老库补「用户主题（皮肤）」列（幂等，已有列就跳过）
SET @theme_col = (SELECT COUNT(*) FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'user' AND COLUMN_NAME = 'theme');
SET @add_theme_col = IF(@theme_col = 0,
    'ALTER TABLE `user` ADD COLUMN `theme` VARCHAR(50) DEFAULT ''aurora-white'' COMMENT ''用户主题（皮肤）'' AFTER `admin_title`',
    'SELECT 1');
PREPARE stmt FROM @add_theme_col;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 老库补「管理员操作记录表」（幂等，表已存在就跳过）
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

-- 确保 admin@library.com 是最高管理员（上面 ALTER 补列后老库也要生效）
UPDATE `user` SET `is_super_admin` = 1 WHERE `email` = 'admin@library.com';

-- 老库补「操作申请表」（幂等，表已存在就跳过）
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

-- 老库补封面裁剪参数列（同样是幂等的，已有列就跳过）
SET @card_crop_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
                         WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'book' AND COLUMN_NAME = 'cover_card_crop');
SET @add_card_crop = IF(@card_crop_exists = 0,
    'ALTER TABLE `book` ADD COLUMN `cover_card_crop` VARCHAR(500) DEFAULT NULL COMMENT ''封面裁剪参数（首页卡片横向），JSON 字符串，空表示不裁剪'' AFTER `cover_url`',
    'SELECT 1');
PREPARE stmt FROM @add_card_crop;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @detail_crop_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
                           WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'book' AND COLUMN_NAME = 'cover_detail_crop');
SET @add_detail_crop = IF(@detail_crop_exists = 0,
    'ALTER TABLE `book` ADD COLUMN `cover_detail_crop` VARCHAR(500) DEFAULT NULL COMMENT ''封面裁剪参数（详情页竖向），JSON 字符串，空表示不裁剪'' AFTER `cover_card_crop`',
    'SELECT 1');
PREPARE stmt FROM @add_detail_crop;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 老库补「评论管理置顶」列（幂等，已有列就跳过）
SET @comment_pinned_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
                              WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'book' AND COLUMN_NAME = 'comment_pinned');
SET @add_comment_pinned = IF(@comment_pinned_exists = 0,
    'ALTER TABLE `book` ADD COLUMN `comment_pinned` TINYINT DEFAULT 0 COMMENT ''评论管理置顶：0否/1是'' AFTER `search_count`',
    'SELECT 1');
PREPARE stmt FROM @add_comment_pinned;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 老库补「评论踩数」列（幂等，已有列就跳过）
SET @dislike_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
                       WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'comment' AND COLUMN_NAME = 'dislike_count');
SET @add_dislike = IF(@dislike_exists = 0,
    'ALTER TABLE `comment` ADD COLUMN `dislike_count` INT DEFAULT 0 COMMENT ''踩数'' AFTER `like_count`',
    'SELECT 1');
PREPARE stmt FROM @add_dislike;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 老库补「楼中楼父评论」列 + 索引（幂等，已有列就跳过）
SET @parent_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'comment' AND COLUMN_NAME = 'parent_id');
SET @add_parent = IF(@parent_exists = 0,
    'ALTER TABLE `comment` ADD COLUMN `parent_id` BIGINT DEFAULT NULL COMMENT ''父评论ID（楼中楼回复，只支持一层）'' AFTER `dislike_count`',
    'SELECT 1');
PREPARE stmt FROM @add_parent;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @parent_idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
                          WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'comment' AND INDEX_NAME = 'idx_comment_parent');
SET @add_parent_idx = IF(@parent_idx_exists = 0,
    'ALTER TABLE `comment` ADD INDEX `idx_comment_parent` (`parent_id`)',
    'SELECT 1');
PREPARE stmt FROM @add_parent_idx;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 老库补「评论点踩」记录表（幂等，表已存在就跳过）
CREATE TABLE IF NOT EXISTS `comment_dislike` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id`    BIGINT   NOT NULL COMMENT '点踩用户ID',
    `comment_id` BIGINT   NOT NULL COMMENT '评论ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点踩时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_comment_dislike` (`user_id`, `comment_id`),
    KEY `idx_comment_dislike_comment` (`comment_id`),
    CONSTRAINT `fk_comment_dislike_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_comment_dislike_comment` FOREIGN KEY (`comment_id`) REFERENCES `comment` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '评论点踩记录表';

-- 老库补「评论点赞 / 评论举报」记录表（幂等，表已存在就跳过）
CREATE TABLE IF NOT EXISTS `comment_like` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id`    BIGINT   NOT NULL COMMENT '点赞用户ID',
    `comment_id` BIGINT   NOT NULL COMMENT '评论ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_comment_like` (`user_id`, `comment_id`),
    KEY `idx_comment_like_comment` (`comment_id`),
    CONSTRAINT `fk_comment_like_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_comment_like_comment` FOREIGN KEY (`comment_id`) REFERENCES `comment` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '评论点赞记录表';

CREATE TABLE IF NOT EXISTS `comment_report` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id`    BIGINT   NOT NULL COMMENT '举报用户ID',
    `comment_id` BIGINT   NOT NULL COMMENT '评论ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '举报时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_comment_report` (`user_id`, `comment_id`),
    KEY `idx_comment_report_comment` (`comment_id`),
    CONSTRAINT `fk_comment_report_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_comment_report_comment` FOREIGN KEY (`comment_id`) REFERENCES `comment` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '评论举报记录表';

-- 示例图书（用于首页浏览、热门排行、个性化推荐与语义搜索演示）
INSERT INTO `book` (`title`, `author`, `publisher`, `platform`, `price`, `publish_date`, `description`, `is_finished`,
                    `stock`, `borrow_count`, `favorite_count`, `search_count`)
VALUES ('三体', '刘慈欣', '重庆出版社', '起点', 23.00, '2008-01-01',
        '地球文明向宇宙发出的一声啼鸣，引来了三体世界的目光。宏大的宇宙社会学构想，中国科幻的里程碑之作。', 1, 10, 12, 8, 25),
       ('活着', '余华', '作家出版社', '番茄', 20.00, '2012-08-01',
        '福贵的一生，是一个人一生的苦难与坚韧。以最朴素的笔触写出生命的重量。', 1, 5, 9, 6, 18),
       ('许三观卖血记', '余华', '作家出版社', '番茄', 22.00, '2017-06-01',
        '许三观一次次卖血撑起一个家，荒诞中透着温情，平凡人的生存智慧。', 1, 3, 5, 3, 9),
       ('明朝那些事儿', '当年明月', '浙江人民出版社', '起点', 55.00, '2011-01-01',
        '以轻松幽默的笔法讲述明朝三百年历史，让历史变得好看又好读。', 1, 8, 15, 11, 32),
       ('斗破苍穹', '天蚕土豆', '湖北少年儿童出版社', '起点', 32.00, '2010-05-01',
        '少年萧炎的逆袭之路，斗气大陆的热血成长故事，网络文学的经典之作。', 1, 10, 21, 14, 40),
       ('知否知否应是绿肥红瘦', '关心则乱', '湖南文艺出版社', '晋江', 68.00, '2018-03-01',
        '从闺阁少女到侯门主母，一部古代家庭生活的细腻长卷。', 1, 2, 7, 9, 16),
       ('隐秘的角落', '紫金陈', '湖南文艺出版社', '晋江', 42.00, '2020-06-01',
        '三个孩子的一次意外目击，牵出一段步步惊心的推理故事。', 0, 0, 4, 5, 11),
       ('时间简史', '史蒂芬·霍金', '湖南科学技术出版社', '豆瓣', 45.00, '2010-04-01',
        '从宇宙大爆炸到黑洞，用通俗语言讲述宇宙的起源与命运。', 1, 6, 3, 7, 13);

-- 示例评论（演示：正常评论 / 含敏感词被系统自动删除 / 被 @管理员 举报 / 楼中楼回复）
-- 敏感词库见后端 CommentService.SENSITIVE_WORDS：广告、代购、赌博、诈骗、色情、暴力、刷单、加微信、私聊、违禁、傻逼、垃圾
INSERT INTO `comment` (`user_id`, `book_id`, `content`, `status`, `like_count`, `dislike_count`, `is_reported`, `created_at`)
VALUES ((SELECT `id` FROM `user` WHERE `email` = 'user@library.com'),
        (SELECT `id` FROM `book` WHERE `title` = '三体'),
        '宏大世界观震撼人心，强烈推荐！', 0, 3, 1, 0, NOW() - INTERVAL 3 DAY),
       ((SELECT `id` FROM `user` WHERE `email` = 'user@library.com'),
        (SELECT `id` FROM `book` WHERE `title` = '三体'),
        '这作者真是个傻逼，写的什么破书', 1, 0, 5, 0, NOW() - INTERVAL 2 DAY),
       ((SELECT `id` FROM `user` WHERE `email` = 'user@library.com'),
        (SELECT `id` FROM `book` WHERE `title` = '活着'),
        '加微信低价代购本书，需要的私聊', 1, 0, 2, 0, NOW() - INTERVAL 2 DAY),
       ((SELECT `id` FROM `user` WHERE `email` = 'user@library.com'),
        (SELECT `id` FROM `book` WHERE `title` = '斗破苍穹'),
        '越到后面越无聊，剧情全是重复套路', 0, 2, 3, 1, NOW() - INTERVAL 1 DAY);

-- 楼中楼回复（只支持一层）：管理员以管理员身份回复上面的正常评论
INSERT INTO `comment` (`user_id`, `book_id`, `parent_id`, `content`, `status`, `like_count`, `dislike_count`, `created_at`)
VALUES ((SELECT `id` FROM `user` WHERE `email` = 'admin@library.com'),
        (SELECT `id` FROM `book` WHERE `title` = '三体'),
        (SELECT c.`id` FROM `comment` c WHERE c.`content` = '宏大世界观震撼人心，强烈推荐！' LIMIT 1),
        '感谢书友的推荐，欢迎借阅三体系列～', 0, 1, 0, NOW() - INTERVAL 2 DAY);

-- 上面那条被举报的评论：两个用户（user + admin）各举报一次，演示「举报次数累加」
INSERT INTO `comment_report` (`user_id`, `comment_id`)
SELECT u.`id`, c.`id`
FROM `comment` c
         JOIN `user` u ON u.`email` = 'user@library.com'
WHERE c.`content` = '越到后面越无聊，剧情全是重复套路'
UNION ALL
SELECT u.`id`, c.`id`
FROM `comment` c
         JOIN `user` u ON u.`email` = 'admin@library.com'
WHERE c.`content` = '越到后面越无聊，剧情全是重复套路';

-- 示例公告
INSERT INTO `announcement` (`title`, `content`, `admin_id`, `status`, `created_at`, `updated_at`)
VALUES ('系统上线公告',
        '云图书管理系统正式上线啦！现在你可以搜索图书、在线借阅、预约热门书籍、发表书评，还能使用智能问答助手快速了解借阅规则。祝阅读愉快！',
        (SELECT `id` FROM `user` WHERE `email` = 'admin@library.com'), 1, NOW(), NOW());

-- 示例搜索记录（首页「热门搜索」的数据来源）
-- 第一部分：user_id 为 NULL，属于全平台公共搜索，参与「全平台热门关键词」统计
INSERT INTO `search_log` (`user_id`, `keyword`, `created_at`)
VALUES (NULL, '三体', NOW()),
       (NULL, '三体', NOW()),
       (NULL, '三体', NOW()),
       (NULL, '斗破苍穹', NOW()),
       (NULL, '斗破苍穹', NOW()),
       (NULL, '明朝那些事儿', NOW()),
       (NULL, '明朝那些事儿', NOW()),
       (NULL, '刘慈欣', NOW()),
       (NULL, '余华', NOW()),
       (NULL, '推理悬疑', NOW());

-- 第二部分：绑定到普通用户，用于演示「个人高频搜索词」分支
-- user@library.com 登录后，个人高频词为：科幻小说(3次) > 刘慈欣(2次) > 晋江(1次)
SET @normal_user_id := (SELECT `id` FROM `user` WHERE `email` = 'user@library.com');

INSERT INTO `search_log` (`user_id`, `keyword`, `created_at`)
VALUES (@normal_user_id, '科幻小说', NOW()),
       (@normal_user_id, '科幻小说', NOW()),
       (@normal_user_id, '科幻小说', NOW()),
       (@normal_user_id, '刘慈欣', NOW()),
       (@normal_user_id, '刘慈欣', NOW()),
       (@normal_user_id, '晋江', NOW());

-- =============================================================================
-- 初始化完成
-- 验证：执行下面的语句应能查到 2 个用户、8 本图书、1 条公告
-- =============================================================================
-- SELECT COUNT(*) AS users  FROM `user`;
-- SELECT COUNT(*) AS books  FROM `book`;
-- SELECT COUNT(*) AS notices FROM `announcement`;
