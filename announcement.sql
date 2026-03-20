-- 公告表
CREATE TABLE IF NOT EXISTS t_announcement (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '公告ID',
    title       VARCHAR(100) NOT NULL COMMENT '公告标题',
    content     TEXT NOT NULL COMMENT '公告内容',
    type        TINYINT NOT NULL DEFAULT 1 COMMENT '公告类型: 1-普通 2-重要 3-紧急',
    status      TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-下线 1-上线',
    sort        INT NOT NULL DEFAULT 0 COMMENT '排序，越大越靠前',
    created_by  BIGINT COMMENT '创建人ID',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';

-- 初始公告数据
INSERT INTO t_announcement (title, content, type, status, sort, created_at)
VALUES
  ('欢迎使用换物广场', '欢迎来到校园换物广场！在这里，你可以发布闲置物品，和同学进行以物换物，让物品流转，让校园更环保。', 1, 1, 10, NOW()),
  ('文明换物倡议', '请遵守平台规范，发布真实信息，禁止发布违法违规内容。共建友好、诚信的校园换物社区。', 2, 1, 5, NOW());
