-- ============================================================
-- 广场动态表 t_post
-- ============================================================
DROP TABLE IF EXISTS t_post;
CREATE TABLE t_post (
  id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '动态ID',
  user_id      BIGINT      NOT NULL COMMENT '发布用户ID',
  item_id      BIGINT      DEFAULT NULL COMMENT '关联商品ID',
  type         TINYINT     NOT NULL COMMENT '动态类型: 1-发布商品 2-换物成功 3-分享动态',
  content      TEXT        DEFAULT NULL COMMENT '动态内容',
  like_count   INT         NOT NULL DEFAULT 0 COMMENT '点赞数',
  comment_count INT        NOT NULL DEFAULT 0 COMMENT '评论数',
  favorite_count INT       NOT NULL DEFAULT 0 COMMENT '收藏数',
  created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  PRIMARY KEY (id),
  KEY idx_user_id (user_id),
  KEY idx_item_id (item_id),
  KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='广场动态表';



-- ============================================================
-- 动态点赞表
-- ============================================================
DROP TABLE IF EXISTS t_post_like;
CREATE TABLE t_post_like (
  id         BIGINT   NOT NULL AUTO_INCREMENT,
  post_id    BIGINT   NOT NULL COMMENT '动态ID',
  user_id    BIGINT   NOT NULL COMMENT '点赞用户ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_post_user (post_id, user_id),
  KEY idx_post_id (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态点赞表';





-- ============================================================
-- 动态收藏表
-- ============================================================
DROP TABLE IF EXISTS t_post_favorite;
CREATE TABLE t_post_favorite (
  id         BIGINT   NOT NULL AUTO_INCREMENT,
  post_id    BIGINT   NOT NULL COMMENT '动态ID',
  user_id    BIGINT   NOT NULL COMMENT '收藏用户ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_post_user (post_id, user_id),
  KEY idx_post_id (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态收藏表';




-- ============================================================
-- 动态评论表
-- ============================================================
DROP TABLE IF EXISTS t_post_comment;
CREATE TABLE t_post_comment (
  id         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  post_id    BIGINT   NOT NULL COMMENT '动态ID',
  user_id    BIGINT   NOT NULL COMMENT '评论用户ID',
  content    TEXT     NOT NULL COMMENT '评论内容',
  parent_id  BIGINT   DEFAULT NULL COMMENT '父评论ID（用于回复）',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_post_id (post_id),
  KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态评论表';



-- ============================================================
-- 标签表
-- ============================================================
DROP TABLE IF EXISTS t_tag;
CREATE TABLE t_tag (
  id    BIGINT      NOT NULL AUTO_INCREMENT,
  name  VARCHAR(50) NOT NULL COMMENT '标签名称',
  PRIMARY KEY (id),
  UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签表';


-- ============================================================
-- 动态标签关系表
-- ============================================================
DROP TABLE IF EXISTS t_post_tag;
CREATE TABLE t_post_tag (
  id      BIGINT NOT NULL AUTO_INCREMENT,
  post_id BIGINT NOT NULL,
  tag_id  BIGINT NOT NULL,
  PRIMARY KEY (id),
  KEY idx_post_id (post_id),
  KEY idx_tag_id (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态标签关系表';


-- ============================================================
-- 换物记录表
-- ============================================================
DROP TABLE IF EXISTS t_swap_record;
CREATE TABLE t_swap_record (
  id           BIGINT   NOT NULL AUTO_INCREMENT,
  item_a_id    BIGINT   NOT NULL COMMENT '物品A',
  item_b_id    BIGINT   NOT NULL COMMENT '物品B',
  user_a_id    BIGINT   NOT NULL,
  user_b_id    BIGINT   NOT NULL,
  status       TINYINT  NOT NULL DEFAULT 0 COMMENT '0-协商中 1-已完成',
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='换物记录表';




