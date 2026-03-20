-- 举报功能 SQL 迁移脚本
-- 执行时间：2026-03-19

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
