-- ============================================================
-- 校园换物平台 campus_swap 数据库建表脚本
-- 字符集：utf8mb4   排序规则：utf8mb4_0900_ai_ci
-- 使用方式：mysql -u root -p < database.sql
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 创建数据库（如不存在）
CREATE DATABASE IF NOT EXISTS `campus_swap`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE `campus_swap`;

-- ----------------------------
-- 1. 用户表
-- ----------------------------
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `email`       varchar(100) NOT NULL                COMMENT '邮箱（登录账号，唯一）',
  `password`    varchar(100) NOT NULL                COMMENT 'BCrypt 加密后的密码',
  `nickname`    varchar(50)  NOT NULL                COMMENT '昵称',
  `school`      varchar(100) DEFAULT NULL            COMMENT '学校',
  `avatar`      varchar(255) DEFAULT NULL            COMMENT '头像 URL',
  `phone`       varchar(20)  DEFAULT NULL            COMMENT '手机号',
  `role`        tinyint      NOT NULL DEFAULT '0'    COMMENT '角色: 0-普通用户 1-管理员 2-超级管理员',
  `status`      tinyint      NOT NULL DEFAULT '0'    COMMENT '账号状态: 0-正常 1-禁用',
  `deleted`     tinyint      NOT NULL DEFAULT '0'    COMMENT '逻辑删除: 0-正常 1-已删除',
  `is_verified` tinyint(1)   NOT NULL DEFAULT '0'    COMMENT '学生认证状态: 0-未认证 1-已认证',
  `created_at`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP                         COMMENT '创建时间',
  `updated_at`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';


-- ----------------------------
-- 2. 管理员权限表
-- ----------------------------
DROP TABLE IF EXISTS `t_admin_permission`;
CREATE TABLE `t_admin_permission` (
  `id`         bigint      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `admin_id`   bigint      NOT NULL               COMMENT '管理员用户ID（role=1的用户）',
  `perm_code`  varchar(50) NOT NULL               COMMENT '权限码：USER_MANAGE / ITEM_MANAGE',
  `created_at` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '授权时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_perm` (`admin_id`, `perm_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理员权限表';


-- ----------------------------
-- 3. 商品（闲置物）表
-- ----------------------------
DROP TABLE IF EXISTS `t_item`;
CREATE TABLE `t_item` (
  `id`           bigint        NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `title`        varchar(100)  NOT NULL               COMMENT '商品标题',
  `price`        decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '价格（元）',
  `category`     varchar(30)   NOT NULL               COMMENT '分类: 数码/书籍/服饰/生活用品/其他',
  `description`  text                  DEFAULT NULL   COMMENT '商品描述',
  `cover_image`  varchar(255)          DEFAULT NULL   COMMENT '封面图 URL',
  `seller_id`    bigint        NOT NULL               COMMENT '发布者用户 ID',
  `images`       text                  DEFAULT NULL   COMMENT '图片 URL 列表（JSON 数组）',
  `tags`         varchar(500)          DEFAULT NULL   COMMENT '标签列表（JSON 数组，如 ["九成新","包邮"]）',
  `status`       tinyint       NOT NULL DEFAULT '0'   COMMENT '状态: 0-在售 1-已下架 2-已售出',
  `audit_status` tinyint       NOT NULL DEFAULT '0'   COMMENT '审核状态: 0-待审核 1-已通过 2-已拒绝',
  `audit_remark` varchar(200)          DEFAULT NULL   COMMENT '审核备注（拒绝原因）',
  `deleted`      tinyint       NOT NULL DEFAULT '0'   COMMENT '逻辑删除: 0-正常 1-已删除',
  `created_at`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP                         COMMENT '发布时间',
  `updated_at`   datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_seller_id`  (`seller_id`),
  KEY `idx_category`   (`category`),
  KEY `idx_status`     (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品（闲置物）表';


-- ----------------------------
-- 4. 交易单表（以物换物）
-- ----------------------------
DROP TABLE IF EXISTS `t_trade`;
CREATE TABLE `t_trade` (
  `id`                          bigint       NOT NULL AUTO_INCREMENT          COMMENT '交易ID',
  `trade_no`                    varchar(32)  NOT NULL                         COMMENT '业务编号（唯一，格式：TRADE+yyyyMMddHHmmss+6位随机数）',
  `initiator_id`                bigint       NOT NULL                         COMMENT '甲方（发起方）用户ID',
  `receiver_id`                 bigint               DEFAULT NULL             COMMENT '乙方用户ID，PENDING_MATCH阶段可为空',
  `initiator_item_id`           bigint       NOT NULL                         COMMENT '甲方提供的物品ID',
  `receiver_item_id`            bigint               DEFAULT NULL             COMMENT '乙方提供的物品ID，MATCHED后填入',
  `status`                      varchar(32)  NOT NULL DEFAULT 'PENDING_MATCH' COMMENT '状态：PENDING_MATCH/MATCHED/AUDITING/WAITING_DELIVERY/WAITING_CONFIRM_RECEIPT/COMPLETED/TERMINATED',
  `audit_mode`                  tinyint      NOT NULL DEFAULT '1'             COMMENT '审核模式：0=无需审核 1=平台审核 2=双方互审',
  `audit_by`                    bigint               DEFAULT NULL             COMMENT '审核人ID',
  `audit_remark`                varchar(500)         DEFAULT NULL             COMMENT '审核备注/驳回原因',
  `initiator_delivered`         tinyint(1)   NOT NULL DEFAULT '0'             COMMENT '甲方是否已发货',
  `receiver_delivered`          tinyint(1)   NOT NULL DEFAULT '0'             COMMENT '乙方是否已发货',
  `initiator_logistics`         varchar(500)         DEFAULT NULL             COMMENT '甲方物流凭证（JSON）',
  `receiver_logistics`          varchar(500)         DEFAULT NULL             COMMENT '乙方物流凭证（JSON）',
  `initiator_confirmed_receipt` tinyint(1)   NOT NULL DEFAULT '0'             COMMENT '甲方是否已确认收货',
  `receiver_confirmed_receipt`  tinyint(1)   NOT NULL DEFAULT '0'             COMMENT '乙方是否已确认收货',
  `terminate_reason`            varchar(500)         DEFAULT NULL             COMMENT '终止原因',
  `initiator_want_terminate`    tinyint(1)   NOT NULL DEFAULT '0'             COMMENT '甲方是否已申请终止',
  `receiver_want_terminate`     tinyint(1)   NOT NULL DEFAULT '0'             COMMENT '乙方是否已申请终止',
  `delivery_timeout_hours`      int          NOT NULL DEFAULT '48'            COMMENT '发货超时小时数',
  `receipt_timeout_hours`       int          NOT NULL DEFAULT '72'            COMMENT '收货确认超时小时数',
  `delivery_deadline`           datetime             DEFAULT NULL             COMMENT '发货截止时间',
  `receipt_deadline`            datetime             DEFAULT NULL             COMMENT '收货确认截止时间',
  `matched_at`                  datetime             DEFAULT NULL             COMMENT '进入MATCHED状态的时间',
  `audit_passed_at`             datetime             DEFAULT NULL             COMMENT '审核通过时间',
  `waiting_delivery_at`         datetime             DEFAULT NULL             COMMENT '进入WAITING_DELIVERY状态的时间',
  `both_delivered_at`           datetime             DEFAULT NULL             COMMENT '双方均发货完成的时间',
  `completed_at`                datetime             DEFAULT NULL             COMMENT '交易完成时间',
  `terminated_at`               datetime             DEFAULT NULL             COMMENT '交易终止时间',
  `version`                     int          NOT NULL DEFAULT '0'             COMMENT '乐观锁版本号',
  `deleted`                     tinyint(1)   NOT NULL DEFAULT '0'             COMMENT '逻辑删除：0=正常 1=已删除',
  `created_at`                  datetime     NOT NULL                         COMMENT '创建时间',
  `updated_at`                  datetime     NOT NULL                         COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trade_no`        (`trade_no`),
  KEY `idx_initiator`             (`initiator_id`),
  KEY `idx_receiver`              (`receiver_id`),
  KEY `idx_status`                (`status`),
  KEY `idx_delivery_deadline`     (`status`, `delivery_deadline`),
  KEY `idx_receipt_deadline`      (`status`, `receipt_deadline`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='交易单表（以物换物）';


-- ----------------------------
-- 5. 交易状态变更日志表
-- ----------------------------
DROP TABLE IF EXISTS `t_trade_log`;
CREATE TABLE `t_trade_log` (
  `id`           bigint      NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `trade_id`     bigint      NOT NULL               COMMENT '关联交易ID',
  `from_status`  varchar(32)         DEFAULT NULL   COMMENT '变更前状态（首次创建时为空）',
  `to_status`    varchar(32) NOT NULL               COMMENT '变更后状态',
  `trigger_type` tinyint     NOT NULL DEFAULT '0'   COMMENT '触发方式：0=手动操作 1=系统自动',
  `operator_id`  bigint              DEFAULT NULL   COMMENT '操作人用户ID（系统触发时为空）',
  `remark`       varchar(500)        DEFAULT NULL   COMMENT '备注',
  `created_at`   datetime    NOT NULL               COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_trade`   (`trade_id`),
  KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='交易状态变更日志表';





-- ----------------------------
-- 7. 聊天会话表
-- ----------------------------
DROP TABLE IF EXISTS `t_conversation`;
CREATE TABLE `t_conversation` (
  `id`            bigint      NOT NULL AUTO_INCREMENT,
  `user1_id`      bigint      NOT NULL             COMMENT '较小 userId',
  `user2_id`      bigint      NOT NULL             COMMENT '较大 userId',
  `item_id`       bigint      NOT NULL DEFAULT '0' COMMENT '关联商品ID，0表示无',
  `last_msg`      varchar(500)        DEFAULT NULL COMMENT '最后一条消息内容',
  `last_msg_time` datetime            DEFAULT NULL COMMENT '最后一条消息时间',
  `user1_unread`  int         NOT NULL DEFAULT '0' COMMENT 'user1 未读消息数',
  `user2_unread`  int         NOT NULL DEFAULT '0' COMMENT 'user2 未读消息数',
  `created_at`    datetime    NOT NULL             COMMENT '创建时间',
  `updated_at`    datetime    NOT NULL             COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_conv`  (`user1_id`, `user2_id`, `item_id`),
  KEY `idx_user1`       (`user1_id`, `last_msg_time`),
  KEY `idx_user2`       (`user2_id`, `last_msg_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='聊天会话表';


-- ----------------------------
-- 8. 聊天消息表
-- ----------------------------
DROP TABLE IF EXISTS `t_message`;
CREATE TABLE `t_message` (
  `id`              bigint     NOT NULL AUTO_INCREMENT,
  `conversation_id` bigint     NOT NULL             COMMENT '所属会话ID',
  `sender_id`       bigint     NOT NULL             COMMENT '发送者用户ID',
  `receiver_id`     bigint     NOT NULL             COMMENT '接收者用户ID',
  `content`         text       NOT NULL             COMMENT '消息内容',
  `is_read`         tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已读: 0-未读 1-已读',
  `created_at`      datetime   NOT NULL             COMMENT '发送时间',
  `updated_at`      datetime   NOT NULL             COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_conv_time`       (`conversation_id`, `created_at`),
  KEY `idx_receiver_unread` (`receiver_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='聊天消息表';


-- ----------------------------
-- 9. 广场动态表
-- ----------------------------
DROP TABLE IF EXISTS `t_post`;
CREATE TABLE `t_post` (
  `id`             bigint        NOT NULL AUTO_INCREMENT COMMENT '动态ID',
  `user_id`        bigint        NOT NULL               COMMENT '发布用户ID',
  `item_id`        bigint                DEFAULT NULL   COMMENT '关联物品ID（类型1/3时填入）',
  `type`           tinyint       NOT NULL               COMMENT '动态类型: 1-发布物品 2-换物成功 3-分享动态 4-求换动态',
  `content`        text                  DEFAULT NULL   COMMENT '动态文字内容',
  `images`         varchar(2000)         DEFAULT NULL   COMMENT '图片URL列表（JSON数组，最多9张）',
  `like_count`     int           NOT NULL DEFAULT '0'   COMMENT '点赞数',
  `comment_count`  int           NOT NULL DEFAULT '0'   COMMENT '评论数',
  `favorite_count` int           NOT NULL DEFAULT '0'   COMMENT '收藏数',
  `view_count`     int           NOT NULL DEFAULT '0'   COMMENT '浏览数',
  `share_count`    int           NOT NULL DEFAULT '0'   COMMENT '分享数',
  `hot_score`      double        NOT NULL DEFAULT '0'   COMMENT '热度分 = like*3 + comment*2 + favorite*4 + share*1 + view*0.1',
  `created_at`     datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id`    (`user_id`),
  KEY `idx_item_id`    (`item_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='广场动态表';


-- ----------------------------
-- 10. 动态评论表
-- ----------------------------
DROP TABLE IF EXISTS `t_post_comment`;
CREATE TABLE `t_post_comment` (
  `id`         bigint   NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `post_id`    bigint   NOT NULL               COMMENT '动态ID',
  `user_id`    bigint   NOT NULL               COMMENT '评论用户ID',
  `content`    text     NOT NULL               COMMENT '评论内容',
  `parent_id`  bigint           DEFAULT NULL   COMMENT '父评论ID（NULL表示一级评论，有值表示回复）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='动态评论表';


-- ----------------------------
-- 11. 动态点赞表
-- ----------------------------
DROP TABLE IF EXISTS `t_post_like`;
CREATE TABLE `t_post_like` (
  `id`         bigint   NOT NULL AUTO_INCREMENT,
  `post_id`    bigint   NOT NULL               COMMENT '动态ID',
  `user_id`    bigint   NOT NULL               COMMENT '点赞用户ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_user` (`post_id`, `user_id`),
  KEY `idx_post_id`         (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='动态点赞表';


-- ----------------------------
-- 12. 动态收藏表
-- ----------------------------
DROP TABLE IF EXISTS `t_post_favorite`;
CREATE TABLE `t_post_favorite` (
  `id`         bigint   NOT NULL AUTO_INCREMENT,
  `post_id`    bigint   NOT NULL               COMMENT '动态ID',
  `user_id`    bigint   NOT NULL               COMMENT '收藏用户ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_user` (`post_id`, `user_id`),
  KEY `idx_post_id`         (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='动态收藏表';


-- ----------------------------
-- 13. 标签表
-- ----------------------------
DROP TABLE IF EXISTS `t_tag`;
CREATE TABLE `t_tag` (
  `id`   bigint      NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL COMMENT '标签名称',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='标签表';


-- ----------------------------
-- 14. 动态标签关联表
-- ----------------------------
DROP TABLE IF EXISTS `t_post_tag`;
CREATE TABLE `t_post_tag` (
  `id`      bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL COMMENT '动态ID',
  `tag_id`  bigint NOT NULL COMMENT '标签ID',
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_tag_id`  (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='动态标签关联表';


-- ----------------------------
-- 15. 学生档案表
-- ----------------------------
DROP TABLE IF EXISTS `t_student_record`;
CREATE TABLE `t_student_record` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `school`      varchar(100) NOT NULL                COMMENT '学校名称',
  `student_id`  varchar(50)  NOT NULL                COMMENT '学号',
  `real_name`   varchar(50)  NOT NULL                COMMENT '真实姓名',
  `extra_info`  varchar(200)         DEFAULT NULL    COMMENT '附加信息（专业/年级等）',
  `created_by`  bigint               DEFAULT NULL    COMMENT '录入管理员用户 ID',
  `created_at`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '录入时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_school_student` (`school`, `student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生档案表';


-- ----------------------------
-- 16. 学生认证申请表
-- ----------------------------
DROP TABLE IF EXISTS `t_student_verify`;
CREATE TABLE `t_student_verify` (
  `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `user_id`      bigint       NOT NULL                COMMENT '申请用户 ID',
  `school`       varchar(100) NOT NULL                COMMENT '填写的学校名称',
  `student_id`   varchar(50)  NOT NULL                COMMENT '填写的学号',
  `real_name`    varchar(50)  NOT NULL                COMMENT '填写的真实姓名',
  `extra_info`   varchar(200)         DEFAULT NULL    COMMENT '补充说明',
  `status`       tinyint      NOT NULL DEFAULT 0      COMMENT '状态: 0-待审核 1-已通过 2-已拒绝',
  `remark`       varchar(200)         DEFAULT NULL    COMMENT '审核备注（拒绝原因）',
  `reviewed_by`  bigint               DEFAULT NULL    COMMENT '审核管理员 ID',
  `reviewed_at`  datetime             DEFAULT NULL    COMMENT '审核时间',
  `created_at`   datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status`  (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生认证申请表';


SET FOREIGN_KEY_CHECKS = 1;





-- ============================================================
-- 通知表 t_notification
-- type: LIKE=点赞, FAVORITE=收藏, COMMENT=评论
-- ============================================================
CREATE TABLE IF NOT EXISTS t_notification (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  receiver_id   BIGINT       NOT NULL COMMENT '接收者用户ID（帖主）',
  sender_id     BIGINT       NOT NULL COMMENT '发送者用户ID（触发操作的人）',
  type          VARCHAR(20)  NOT NULL COMMENT '通知类型: LIKE/FAVORITE/COMMENT',
  post_id       BIGINT       NOT NULL COMMENT '关联的帖子ID',
  content       VARCHAR(255) DEFAULT NULL COMMENT '附加内容（评论时填评论内容预览）',
  is_read       TINYINT      NOT NULL DEFAULT 0 COMMENT '0-未读 1-已读',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_receiver_id (receiver_id),
  KEY idx_is_read (receiver_id, is_read),
  KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息通知表';

-- ----------------------------
-- AI 问一问机器人账号（固定 ID，勿删）
-- ----------------------------
INSERT IGNORE INTO t_user (id, email, password, nickname, avatar, role, status, is_verified, deleted, created_at, updated_at)
VALUES (999999999, 'ai_assistant@campus-swap.local', 'NOT_A_REAL_PASSWORD', '问一问', '', 0, 0, 0, 0, NOW(), NOW());



-- 创建动态举报表
CREATE TABLE IF NOT EXISTS `t_post_report` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '举报ID',
    `reporter_id` BIGINT       NOT NULL COMMENT '举报者用户ID',
    `post_id`     BIGINT       NOT NULL COMMENT '被举报动态ID',
    `reason`      TINYINT      NOT NULL COMMENT '举报原因：1-违法违规 2-色情低俗 3-虚假信息 4-侮辱谩骂 5-广告骚扰 6-其他',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '补充说明',
    `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '处理状态：0-待审核 1-已处理(内容下架) 2-已驳回(内容正常)',
    `remark`      VARCHAR(500) DEFAULT NULL COMMENT '管理员审核备注',
    `reviewed_by` BIGINT       DEFAULT NULL COMMENT '审核员ID',
    `reviewed_at` DATETIME     DEFAULT NULL COMMENT '审核时间',
    `created_at`  DATETIME     DEFAULT NULL COMMENT '举报时间',
    PRIMARY KEY (`id`),
    INDEX `idx_post_id` (`post_id`),
    INDEX `idx_reporter_id` (`reporter_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态举报表';




-- 交易申诉表
CREATE TABLE IF NOT EXISTS `t_trade_appeal` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '申诉ID',
    `trade_id`     BIGINT       NOT NULL COMMENT '关联交易ID',
    `appellant_id` BIGINT       NOT NULL COMMENT '申诉人用户ID',
    `content`      VARCHAR(500) NOT NULL COMMENT '申诉内容',
    `status`       TINYINT      NOT NULL DEFAULT 0 COMMENT '处理状态：0-待处理 1-已处理 2-已驳回',
    `remark`       VARCHAR(500) DEFAULT NULL COMMENT '管理员处理备注',
    `reviewed_by`  BIGINT       DEFAULT NULL COMMENT '处理管理员ID',
    `reviewed_at`  DATETIME     DEFAULT NULL COMMENT '处理时间',
    `created_at`   DATETIME     DEFAULT NULL COMMENT '申诉时间',
    PRIMARY KEY (`id`),
    INDEX `idx_trade_id` (`trade_id`),
    INDEX `idx_appellant_id` (`appellant_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易申诉表';



-- 公告表
CREATE TABLE IF NOT EXISTS t_announcement (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '公告ID',
    title       VARCHAR(100) NOT NULL COMMENT '公告标题',
    content     TEXT NOT NULL COMMENT '公告内容',
    type        TINYINT NOT NULL DEFAULT 1 COMMENT '公告类型: 1-普通 2-重要 3-紧急',
    status      TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-下线 1-上线',
    sort        INT NOT NULL DEFAULT 0 COMMENT '排序，越大越靠前',
    created_by  BIGINT COMMENT '创建人ID',
    start_time  DATETIME DEFAULT NULL COMMENT '生效开始时间，NULL表示立即生效',
    end_time    DATETIME DEFAULT NULL COMMENT '过期时间，NULL表示永久有效',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';

-- 初始公告数据
INSERT INTO t_announcement (title, content, type, status, sort, created_at)
VALUES
  ('欢迎使用换物广场', '欢迎来到校园换物广场！在这里，你可以发布闲置物品，和同学进行以物换物，让物品流转，让校园更环保。', 1, 1, 10, NOW()),
  ('文明换物倡议', '请遵守平台规范，发布真实信息，禁止发布违法违规内容。共建友好、诚信的校园换物社区。', 2, 1, 5, NOW());


-- ----------------------------
-- 增量补丁（兼容旧库，可重复执行）
-- 说明：
-- 1. 全量初始化时，上面的 CREATE TABLE 已是最新结构；
-- 2. 旧库升级时，执行以下补丁以补齐新增字段和索引。
-- ----------------------------

-- t_trade：补齐终止申请字段
SET @sql = IF(
  EXISTS(
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 't_trade'
      AND COLUMN_NAME = 'initiator_want_terminate'
  ),
  'SELECT 1',
  'ALTER TABLE `t_trade` ADD COLUMN `initiator_want_terminate` tinyint(1) NOT NULL DEFAULT 0 COMMENT ''甲方是否已申请终止'' AFTER `terminate_reason`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS(
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 't_trade'
      AND COLUMN_NAME = 'receiver_want_terminate'
  ),
  'SELECT 1',
  'ALTER TABLE `t_trade` ADD COLUMN `receiver_want_terminate` tinyint(1) NOT NULL DEFAULT 0 COMMENT ''乙方是否已申请终止'' AFTER `initiator_want_terminate`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- t_trade：补齐执行阶段时间字段
SET @sql = IF(
  EXISTS(
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 't_trade'
      AND COLUMN_NAME = 'waiting_delivery_at'
  ),
  'SELECT 1',
  'ALTER TABLE `t_trade` ADD COLUMN `waiting_delivery_at` DATETIME DEFAULT NULL COMMENT ''进入WAITING_DELIVERY状态的时间'' AFTER `audit_passed_at`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS(
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 't_trade'
      AND COLUMN_NAME = 'both_delivered_at'
  ),
  'SELECT 1',
  'ALTER TABLE `t_trade` ADD COLUMN `both_delivered_at` DATETIME DEFAULT NULL COMMENT ''双方均发货完成的时间'' AFTER `waiting_delivery_at`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- t_trade：补齐查询性能索引
SET @sql = IF(
  EXISTS(
    SELECT 1
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 't_trade'
      AND INDEX_NAME = 'idx_initiator'
  ),
  'SELECT 1',
  'ALTER TABLE `t_trade` ADD KEY `idx_initiator` (`initiator_id`)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS(
    SELECT 1
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 't_trade'
      AND INDEX_NAME = 'idx_receiver'
  ),
  'SELECT 1',
  'ALTER TABLE `t_trade` ADD KEY `idx_receiver` (`receiver_id`)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS(
    SELECT 1
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 't_trade'
      AND INDEX_NAME = 'idx_delivery_deadline'
  ),
  'SELECT 1',
  'ALTER TABLE `t_trade` ADD KEY `idx_delivery_deadline` (`status`, `delivery_deadline`)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS(
    SELECT 1
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 't_trade'
      AND INDEX_NAME = 'idx_receipt_deadline'
  ),
  'SELECT 1',
  'ALTER TABLE `t_trade` ADD KEY `idx_receipt_deadline` (`status`, `receipt_deadline`)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- t_trade_appeal：补齐处理结果字段
SET @sql = IF(
  EXISTS(
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 't_trade_appeal'
      AND COLUMN_NAME = 'remark'
  ),
  'SELECT 1',
  'ALTER TABLE `t_trade_appeal` ADD COLUMN `remark` VARCHAR(500) DEFAULT NULL COMMENT ''管理员处理备注'' AFTER `status`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS(
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 't_trade_appeal'
      AND COLUMN_NAME = 'reviewed_by'
  ),
  'SELECT 1',
  'ALTER TABLE `t_trade_appeal` ADD COLUMN `reviewed_by` BIGINT DEFAULT NULL COMMENT ''处理管理员ID'' AFTER `remark`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS(
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 't_trade_appeal'
      AND COLUMN_NAME = 'reviewed_at'
  ),
  'SELECT 1',
  'ALTER TABLE `t_trade_appeal` ADD COLUMN `reviewed_at` DATETIME DEFAULT NULL COMMENT ''处理时间'' AFTER `reviewed_by`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
-- 广场社交表补齐（兼容旧库，可重复执行）
CREATE TABLE IF NOT EXISTS `t_post_like` (
  `id`         bigint   NOT NULL AUTO_INCREMENT,
  `post_id`    bigint   NOT NULL COMMENT '动态ID',
  `user_id`    bigint   NOT NULL COMMENT '点赞用户ID',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_user` (`post_id`, `user_id`),
  KEY `idx_post_id` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='动态点赞表';

CREATE TABLE IF NOT EXISTS `t_post_favorite` (
  `id`         bigint   NOT NULL AUTO_INCREMENT,
  `post_id`    bigint   NOT NULL COMMENT '动态ID',
  `user_id`    bigint   NOT NULL COMMENT '收藏用户ID',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_user` (`post_id`, `user_id`),
  KEY `idx_post_id` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='动态收藏表';

CREATE TABLE IF NOT EXISTS `t_post_tag` (
  `id`      bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL COMMENT '动态ID',
  `tag_id`  bigint NOT NULL COMMENT '标签ID',
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='动态标签关联表';

-- 插入"闲置"标签（用于广场热门标签筛选，一键发布闲置动态时自动关联）
INSERT IGNORE INTO `t_tag` (`name`) VALUES ('闲置');

-- 商品表增加"同步广场"标记列
SET @sql = IF(
  EXISTS(
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 't_item'
      AND COLUMN_NAME = 'sync_to_square'
  ),
  'SELECT 1',
  'ALTER TABLE `t_item` ADD COLUMN `sync_to_square` tinyint(1) NOT NULL DEFAULT 0 COMMENT ''审核通过后是否同步发布广场动态: 0-否 1-是'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 公告表补齐创建人和生效时间字段
SET @sql = IF(
  EXISTS(
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 't_announcement'
      AND COLUMN_NAME = 'created_by'
  ),
  'SELECT 1',
  'ALTER TABLE `t_announcement` ADD COLUMN `created_by` BIGINT COMMENT ''创建人ID'' AFTER `sort`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS(
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 't_announcement'
      AND COLUMN_NAME = 'start_time'
  ),
  'SELECT 1',
  'ALTER TABLE `t_announcement` ADD COLUMN `start_time` DATETIME DEFAULT NULL COMMENT ''生效开始时间，NULL表示立即生效'' AFTER `created_by`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS(
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 't_announcement'
      AND COLUMN_NAME = 'end_time'
  ),
  'SELECT 1',
  'ALTER TABLE `t_announcement` ADD COLUMN `end_time` DATETIME DEFAULT NULL COMMENT ''过期时间，NULL表示永久有效'' AFTER `start_time`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 学生档案表：补齐旧库缺失字段
SET @sql = IF(
  EXISTS(
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 't_student_record'
      AND COLUMN_NAME = 'extra_info'
  ),
  'SELECT 1',
  'ALTER TABLE `t_student_record` ADD COLUMN `extra_info` varchar(200) DEFAULT NULL COMMENT ''附加信息（专业/年级等）'' AFTER `real_name`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS(
    SELECT 1
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 't_student_record'
      AND COLUMN_NAME = 'created_by'
  ),
  'SELECT 1',
  'ALTER TABLE `t_student_record` ADD COLUMN `created_by` BIGINT DEFAULT NULL COMMENT ''录入管理员用户 ID'' AFTER `extra_info`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 学生认证申请表：兼容旧库补表
CREATE TABLE IF NOT EXISTS `t_student_verify` (
  `id`          bigint NOT NULL AUTO_INCREMENT,
  `user_id`     bigint NOT NULL COMMENT '申请用户 ID',
  `school`      varchar(100) NOT NULL COMMENT '填写的学校名称',
  `student_id`  varchar(50) NOT NULL COMMENT '填写的学号',
  `real_name`   varchar(50) NOT NULL COMMENT '填写的真实姓名',
  `extra_info`  varchar(200) DEFAULT NULL COMMENT '补充说明',
  `status`      tinyint NOT NULL DEFAULT 0 COMMENT '状态: 0-待审核 1-已通过 2-已拒绝',
  `remark`      varchar(200) DEFAULT NULL COMMENT '审核备注（拒绝原因）',
  `reviewed_by` bigint DEFAULT NULL COMMENT '审核管理员 ID',
  `reviewed_at` datetime DEFAULT NULL COMMENT '审核时间',
  `created_at`  timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='学生认证申请表';
