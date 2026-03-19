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
