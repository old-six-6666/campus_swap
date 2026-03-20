USE campus_swap;
UPDATE t_user SET password = '$2a$10$DsKE..H8uBgLY3GEKkEibOAij.aSsBEAdVZpXFBaHXxCaoZp44etq' WHERE email = '1502893851@qq.com';
UPDATE t_user SET role = 2 WHERE email = '1502893851@qq.com';





CREATE TABLE IF NOT EXISTS `t_appeal` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `trade_id`    BIGINT        NOT NULL COMMENT '关联交易ID',
    `trade_no`    VARCHAR(32)   NOT NULL COMMENT '交易编号',
    `user_id`     BIGINT        NOT NULL COMMENT '申诉人用户ID',
    `reason`      VARCHAR(1000) NOT NULL COMMENT '申诉理由',
    `images`      VARCHAR(2000) DEFAULT NULL COMMENT '凭证图片URL列表',
    `status`      TINYINT       NOT NULL DEFAULT 0 COMMENT '处理状态',
    `handle_note` VARCHAR(500)  DEFAULT NULL COMMENT '管理员处理备注',
    `handled_by`  BIGINT        DEFAULT NULL COMMENT '处理管理员ID',
    `handled_at`  DATETIME      DEFAULT NULL COMMENT '处理时间',
    -- 方案：created_at 改为 DATETIME，默认值设为 '1970-01-01 00:00:00' 或由代码插入
    `created_at`  DATETIME      NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '申诉提交时间',
    -- 仅保留 updated_at 这一列拥有自动更新功能
    `updated_at`  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_trade_id` (`trade_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易申诉表';
CREATE TABLE IF NOT EXISTS `t_trade_admin_log` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `trade_id`     BIGINT       NOT NULL COMMENT '关联交易ID',
    `trade_no`     VARCHAR(32)  NOT NULL COMMENT '交易编号',
    `admin_id`     BIGINT       NOT NULL COMMENT '操作管理员ID',
    `action`       VARCHAR(32)  NOT NULL COMMENT '操作类型',
    `action_desc`  VARCHAR(100) NOT NULL COMMENT '操作描述',
    `reason`       VARCHAR(500) DEFAULT NULL COMMENT '操作原因',
    -- 只有一列 TIMESTAMP 时，DEFAULT CURRENT_TIMESTAMP 是没问题的
    `created_at`   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_trade_id` (`trade_id`),
    KEY `idx_admin_id` (`admin_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员交易操作日志';

ALTER TABLE t_item
ADD COLUMN `tags` varchar(500) DEFAULT NULL COMMENT '标签列表（JSON 数组）';