-- ============================================================
-- 云图书管理系统 数据库建表脚本
-- 版本: V3.0
-- 编制: 一组（刘里萍、张语欣、朱文宣、许梓涵、王妍、孔令芬）
-- 日期: 2026年9月
-- ============================================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS cloud_library DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE cloud_library;

-- ============================================================
-- 1. 用户表（user）
-- ============================================================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `email` VARCHAR(100) NOT NULL COMMENT '邮箱（登录账号）',
  `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密存储）',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  `is_super_admin` TINYINT DEFAULT 0 COMMENT '是否最高管理员：0否/1是',
  `pending_admin` TINYINT DEFAULT 0 COMMENT '待激活管理员：0否/1是',
  `pending_admin_deadline` DATETIME DEFAULT NULL COMMENT '待激活截止时间',
  `admin_title` VARCHAR(50) DEFAULT NULL COMMENT '管理员封号',
  `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
  `theme` VARCHAR(50) DEFAULT 'aurora-white' COMMENT '用户主题',   -- 新增这一行
  `role` TINYINT DEFAULT 0 COMMENT '角色：0普通用户/1管理员',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0注销/1正常/2禁用',
  `ban_reason` VARCHAR(255) DEFAULT NULL COMMENT '禁用原因',
  `ban_days` INT DEFAULT 0 COMMENT '禁用天数',
  `ban_start_time` DATETIME DEFAULT NULL COMMENT '禁用开始时间',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================================
-- 2. 图书表（book）
-- ============================================================
DROP TABLE IF EXISTS `book`;
CREATE TABLE `book` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '图书ID',
  `title` VARCHAR(200) NOT NULL COMMENT '书名',
  `author` VARCHAR(100) NOT NULL COMMENT '作者',
  `publisher` VARCHAR(200) DEFAULT NULL COMMENT '出版社',
  `platform` VARCHAR(50) DEFAULT NULL COMMENT '平台（起点/晋江/番茄等）',
  `price` DECIMAL(10,2) DEFAULT NULL COMMENT '价格',
  `publish_date` DATE DEFAULT NULL COMMENT '出版时间',
  `cover_url` VARCHAR(500) DEFAULT NULL COMMENT '封面图URL',
  `description` TEXT COMMENT '简介',
  `is_finished` TINYINT DEFAULT 0 COMMENT '是否完结：0连载中/1已完结',
    `comment_pinned` TINYINT DEFAULT 0 COMMENT '评论是否置顶：0否/1是',
  `cover_card_crop` VARCHAR(500) DEFAULT NULL COMMENT '封面裁剪参数（首页卡片）',  
  `cover_detail_crop` VARCHAR(500) DEFAULT NULL COMMENT '封面裁剪参数（详情页）', 
  `borrow_count` INT DEFAULT 0 COMMENT '借阅次数',
  `favorite_count` INT DEFAULT 0 COMMENT '收藏人数',
  `search_count` INT DEFAULT 0 COMMENT '搜索次数',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '录入时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_title` (`title`),
  KEY `idx_author` (`author`),
  KEY `idx_platform` (`platform`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图书表';

INSERT INTO `book` 
(`title`, `author`, `publisher`, `platform`, `price`, `description`, `is_finished`, `borrow_count`, `favorite_count`, `search_count`) 
VALUES 
('三体', '刘慈欣', '重庆出版社', '重庆出版社', 45.00, '文化大革命如火如荼进行的同时，军方探寻外星文明的绝秘计划“红岸工程”取得了突破性进展。但在按下发射键的那一刻，历经劫难的叶文洁没有意识到，她彻底改变了人类的命运。地球文明向宇宙发出的第一声啼鸣，以太阳为中心，以光速向宇宙深处飞驰……', 1, 0, 0, 0),
('活着', '余华', '作家出版社', '作家出版社', 45.00, '地主少爷福贵嗜赌成性，终于赌光了家业，一贫如洗。穷困之中的福贵因为母亲生病前去求医，没想到半路上被国民党部队抓了壮丁，后被解放军所俘虏，回到家乡他才知道母亲已经去世，妻子家珍含辛茹苦带大了一双儿女，但女儿不幸变成了聋哑人……', 1, 0, 0, 0),
('白鹿原', '陈忠实', '人民文学出版社', '人民文学出版社', 42.00, '这是一部渭河平原50年变迁的雄奇史诗，一轴中国农村斑斓多彩、触目惊心的长幅画卷。主人公六娶六丧，神秘的序曲预示着不祥。一个家庭两代子孙，为争夺白鹿原的统治代代争斗不已，上演了一幕幕惊心动魄的话剧……', 1, 0, 0, 0),
('平凡的世界', '路遥', '中国文联出版公司', '中国文联出版公司', 49.00, '这是一部全景式地表现中国当代城乡社会生活的长篇小说。全书共三部。作者在近十年间广阔背景上，通过复杂的矛盾纠葛，刻画了社会各阶层众多普通人的形象。劳动与爱情，挫折与追求，痛苦与欢乐，日常生活与巨大社会冲突，纷繁地交织在一起……', 1, 0, 0, 0),
('围城', '钱钟书', '人民文学出版社', '人民文学出版社', 58.00, '人生是围城，婚姻是围城，冲进去了，就被生存的种种烦愁包围。作者以他洒脱幽默的文笔，述说着以方鸿渐为主的一群知识分子的喜怒哀乐、勾心斗角……', 1, 0, 0, 0),
('尘埃落定', '阿来', '人民文学出版社', '人民文学出版社', 39.00, '一个声势显赫的康巴藏族土司，在酒后和汉族太太生了一个傻瓜儿子。这个人人都认定的傻子与现实生活格格不入，却有着超时代的预感和举止，成为土司制度兴衰的见证人……', 1, 0, 0, 0),
('额尔古纳河右岸', '迟子建', '北京十月文艺出版社', '北京十月文艺出版社', 45.00, '在中俄边界的额尔古纳河右岸，居住着一支数百年前自贝加尔湖畔迁徙而至，与驯鹿相依为命的鄂温克人。他们信奉萨满，逐驯鹿喜食物而搬迁、游猎，在享受大自然恩赐的同时也艰辛备尝……', 1, 0, 0, 0),
('一句顶一万句', '刘震云', '长江文艺出版社', '长江文艺出版社', 52.00, '小说分两个部分：前半部写的是过去：孤独无助的农民吴摩西为了寻找与人私奔的老婆，在路上失去唯一能够“说得着”的养女，为了寻找她，他不得不走出延津；后半部写的是现在：吴摩西养女巧玲的儿子牛爱国，同样为了寻找与人私奔的老婆，走向延津……', 1, 0, 0, 0),
('暗算', '麦家', '人民文学出版社', '人民文学出版社', 48.00, '这是一部直接反映反间谍部门的核心机关——无线电侦听与密码破译的小说。小说以特别单位701为背景，讲述了一个个身怀绝技却鲜为人知的侦听员、破译员的故事……', 1, 0, 0, 0),
('房思琪的初恋乐园', '林奕含', '北京联合出版公司', '北京联合出版公司', 46.00, '小小的房思琪住在金碧辉煌的人生里，她的脸和她可以想象的将来一样漂亮。补习班国文名师李国华是同一栋高级住宅的邻居。崇拜文学的小房思琪同样崇拜饱读诗书的李老师。有一天李老师说，你的程度这么好，不如每个礼拜交一篇作文给我吧，不收你周点费。思琪听话地下楼了。老师在家里等她，桌上没有纸笔。这是老师爱你的方式……', 1, 0, 0, 0),
('白夜行', '东野圭吾', '南海出版公司', '南海出版公司', 43.00, '只希望能手牵手在太阳下散步”，这个象征故事内核的绝望念想，有如一个美丽的幌子，随着无数凌乱、压抑、悲凉的故事片段像纪录片一样一一还原：没有痴痴相思，没有海枯石烂，只剩下一个冰冷绝望的诡计，最后一丝温情也被完全抛弃……', 1, 0, 0, 0),
('剑来', '烽火戏诸侯', '纵横中文网', '纵横中文网', 55.00, '大千世界，无奇不有。我陈平安，唯有一剑，可搬山，倒海，降妖，镇魔，敕神，摘星，断江，摧城，开天！', 0, 0, 0, 0);

-- ============================================================
-- 3. 借阅记录表（borrow）
-- ============================================================
DROP TABLE IF EXISTS `borrow`;
CREATE TABLE `borrow` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `book_id` BIGINT NOT NULL COMMENT '图书ID',
  `borrow_date` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '借阅时间',
  `due_date` DATETIME NOT NULL COMMENT '应还时间（借阅日期+10天）',
  `return_date` DATETIME DEFAULT NULL COMMENT '实际归还时间',
  `renew_count` INT DEFAULT 0 COMMENT '续借次数',
  `status` TINYINT DEFAULT 0 COMMENT '状态：0借阅中/1已归还/2逾期',
  `return_type` TINYINT DEFAULT 0 COMMENT '归还方式：0用户自己归还/1管理员强制归还',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_book_id` (`book_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_borrow_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_borrow_book` FOREIGN KEY (`book_id`) REFERENCES `book` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='借阅记录表';

-- ============================================================
-- 4. 预约记录表（reservation）
-- ============================================================
DROP TABLE IF EXISTS `reservation`;
CREATE TABLE `reservation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `book_id` BIGINT NOT NULL COMMENT '图书ID',
  `reservation_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '预约时间',
  `available_time` DATETIME DEFAULT NULL COMMENT '变为可借阅的时间',
  `status` TINYINT DEFAULT 0 COMMENT '状态：0等待借阅/1可借阅/2已借阅/3已取消/4超时取消',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_book_id` (`book_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_reservation_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_reservation_book` FOREIGN KEY (`book_id`) REFERENCES `book` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预约记录表';

-- ============================================================
-- 5. 收藏表（favorite）
-- ============================================================
DROP TABLE IF EXISTS `favorite`;
CREATE TABLE `favorite` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `book_id` BIGINT NOT NULL COMMENT '图书ID',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_book` (`user_id`, `book_id`),
  CONSTRAINT `fk_favorite_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_favorite_book` FOREIGN KEY (`book_id`) REFERENCES `book` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

-- ============================================================
-- 6. 评论表（comment）
-- ============================================================
DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `book_id` BIGINT NOT NULL COMMENT '图书ID',
  `parent_id` BIGINT DEFAULT NULL COMMENT '父评论ID（楼中楼回复用，NULL表示顶级评论）',
  `content` TEXT NOT NULL COMMENT '评论内容',
  `like_count` INT DEFAULT 0 COMMENT '点赞数',
  `dislike_count` INT DEFAULT 0 COMMENT '踩数',
  `status` TINYINT DEFAULT 0 COMMENT '状态：0正常/1系统删除/2管理员删除/3管理员恢复',
  `is_reported` TINYINT DEFAULT 0 COMMENT '是否被@管理员标记',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
  PRIMARY KEY (`id`),
  KEY `idx_book_id` (`book_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parent_id` (`parent_id`),
  CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_comment_book` FOREIGN KEY (`book_id`) REFERENCES `book` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';
-- ============================================================
-- 7. 反馈表（feedback）
-- ============================================================
DROP TABLE IF EXISTS `feedback`;
CREATE TABLE `feedback` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '反馈ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `type` TINYINT NOT NULL COMMENT '类型：0功能建议/1举报用户/2内容错误/3系统BUG/4其他',
  `title` VARCHAR(200) NOT NULL COMMENT '标题',
  `content` TEXT NOT NULL COMMENT '内容',
  `status` TINYINT DEFAULT 0 COMMENT '状态：0待回复/1已回复',
  `reply` TEXT DEFAULT NULL COMMENT '管理员回复内容',
  `reply_time` DATETIME DEFAULT NULL COMMENT '回复时间',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `fk_feedback_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='反馈表';

-- ============================================================
-- 8. 通知表（notification）
-- ============================================================
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID（NULL表示系统公告，所有用户可见）',
  `type` TINYINT NOT NULL COMMENT '类型：0系统公告/1借阅成功/2预约轮到/3预约超时/4还书成功/5逾期提醒/6反馈回复/7催还提醒',
  `title` VARCHAR(200) NOT NULL COMMENT '标题',
  `content` TEXT NOT NULL COMMENT '内容',
  `is_read` TINYINT DEFAULT 0 COMMENT '是否已读',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_is_read` (`is_read`),
  CONSTRAINT `fk_notification_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

-- ============================================================
-- 9. 公告表（announcement）
-- ============================================================
DROP TABLE IF EXISTS `announcement`;
CREATE TABLE `announcement` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `title` VARCHAR(200) NOT NULL COMMENT '标题',
  `content` TEXT NOT NULL COMMENT '内容',
  `admin_id` BIGINT NOT NULL COMMENT '发布管理员ID',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0已删除/1正常',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_admin_id` (`admin_id`),
  CONSTRAINT `fk_announcement_admin` FOREIGN KEY (`admin_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告表';

-- ============================================================
-- 10. 验证码表（verification_code）
-- ============================================================
DROP TABLE IF EXISTS `verification_code`;
CREATE TABLE `verification_code` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `email` VARCHAR(100) NOT NULL COMMENT '邮箱',
  `code` VARCHAR(10) NOT NULL COMMENT '验证码',
  `type` TINYINT NOT NULL COMMENT '类型：0注册/1找回密码',
  `expired_at` DATETIME NOT NULL COMMENT '过期时间（有效期5分钟）',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_email` (`email`),
  KEY `idx_expired_at` (`expired_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='验证码表';

-- ============================================================
-- 11. 公告已读记录表（announcement_read）
-- ============================================================
DROP TABLE IF EXISTS `announcement_read`;
CREATE TABLE `announcement_read` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `announcement_id` BIGINT NOT NULL COMMENT '公告ID',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '已读时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_announcement_read_user` (`user_id`, `announcement_id`),
  KEY `idx_announcement_read_announcement` (`announcement_id`),
  CONSTRAINT `fk_announcement_read_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_announcement_read_announcement` FOREIGN KEY (`announcement_id`) REFERENCES `announcement` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='公告已读记录表';

-- ============================================================
-- 12. 搜索记录表（search_log）
-- ============================================================
DROP TABLE IF EXISTS `search_log`;
CREATE TABLE `search_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '搜索用户ID',
  `keyword` VARCHAR(100) NOT NULL COMMENT '搜索关键词',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '搜索时间',
  PRIMARY KEY (`id`),
  KEY `idx_search_user_keyword` (`user_id`, `keyword`),
  KEY `idx_search_keyword` (`keyword`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='搜索记录表';

-- ============================================================
-- 13.6.1 评论点赞记录表（comment_like）
-- ============================================================
CREATE TABLE IF NOT EXISTS `comment_like` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `comment_id` BIGINT NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_comment_like_user_comment` (`user_id`, `comment_id`),
  CONSTRAINT `fk_comment_like_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_comment_like_comment` FOREIGN KEY (`comment_id`) REFERENCES `comment` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论点赞记录表';

-- ============================================================
-- 14. 6.2 评论举报记录表（comment_report）
-- ============================================================
CREATE TABLE IF NOT EXISTS `comment_report` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `comment_id` BIGINT NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_comment_report_user_comment` (`user_id`, `comment_id`),
  CONSTRAINT `fk_comment_report_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_comment_report_comment` FOREIGN KEY (`comment_id`) REFERENCES `comment` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论举报记录表';

-- 15.2. 新建 comment_dislike 表
CREATE TABLE IF NOT EXISTS `comment_dislike` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `comment_id` BIGINT NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_comment_dislike_user_comment` (`user_id`, `comment_id`),
  CONSTRAINT `fk_comment_dislike_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `fk_comment_dislike_comment` FOREIGN KEY (`comment_id`) REFERENCES `comment` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论踩记录表';

-- ============================================================
-- 16.操作申请表（application）
-- ============================================================
DROP TABLE IF EXISTS `application`;
CREATE TABLE `application` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `applicant_id` BIGINT NOT NULL COMMENT '申请人ID',
  `type` VARCHAR(50) NOT NULL COMMENT '申请类型：delete_user / cancel_user / batch_delete_book / other',
  `target_id` BIGINT DEFAULT NULL COMMENT '操作目标ID',
  `content` TEXT COMMENT '申请理由',
  `status` TINYINT DEFAULT 0 COMMENT '状态：0待审批/1已同意/2已拒绝',
  `reply` TEXT DEFAULT NULL COMMENT '最高管理员的回复',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `reply_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_applicant_id` (`applicant_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_application_user` FOREIGN KEY (`applicant_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作申请表';

-- ============================================================
-- 17.管理员操作日志表（admin_operation_log）
-- ============================================================
DROP TABLE IF EXISTS `admin_operation_log`;
CREATE TABLE `admin_operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `admin_id` BIGINT NOT NULL COMMENT '管理员ID',
  `operation` VARCHAR(50) NOT NULL COMMENT '操作类型：delete_comment / restore_comment / edit_book 等',
  `target_id` BIGINT DEFAULT NULL COMMENT '操作目标ID',
  `detail` VARCHAR(500) DEFAULT NULL COMMENT '操作详情',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_admin_id` (`admin_id`),
  CONSTRAINT `fk_admin_log_user` FOREIGN KEY (`admin_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员操作日志表';

-- ============================================================
-- 18用户自定义主题表（user_theme）
-- ============================================================
DROP TABLE IF EXISTS `user_theme`;
CREATE TABLE `user_theme` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `name` VARCHAR(50) NOT NULL COMMENT '自定义主题名',
  `primary_color` VARCHAR(20) NOT NULL COMMENT '主色',
  `dark_color` VARCHAR(20) NOT NULL COMMENT '深色',
  `light_color` VARCHAR(20) NOT NULL COMMENT '浅色',
  `palette` VARCHAR(240) DEFAULT NULL COMMENT '自定义配色（逗号分隔的8个#RRGGBB）',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `fk_user_theme_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户自定义主题表';

-- ============================================================
-- 初始化数据：插入管理员账号
-- 账号：admin@library.com
-- 密码：admin123
-- ============================================================
-- INSERT INTO `user` (`email`, `password`, `nickname`, `role`, `status`, `created_at`, `updated_at`)
-- VALUES ('admin@library.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员', 1, 1, NOW(), NOW());
INSERT INTO `user` (`email`, `password`, `nickname`, `role`, `status`, `is_super_admin`, `pending_admin`, `created_at`, `updated_at`)
VALUES ('admin@library.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员', 1, 1, 1, 0, NOW(), NOW());

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

-- -----------------------------------------------------------------------------
-- 图书库存功能：book 表新增 stock 字段
-- 执行方式：USE 你的数据库; SOURCE sql/upgrade_book_stock.sql;
-- 说明：只在字段不存在时新增，可重复执行；已有图书统一初始化为默认库存 10。
-- -----------------------------------------------------------------------------
ALTER TABLE `book` ADD COLUMN `stock` INT DEFAULT 10 COMMENT '库存数量（可借余量）';

UPDATE `book` SET `stock` = 10 WHERE `stock` IS NULL OR `stock` < 0;

















