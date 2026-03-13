-- ============================================================
--  校园闲置物置换平台 — 数据库操作手册
--  数据库: campus_swap
--  字符集: utf8mb4
--  执行顺序: 依次运行本文件所有 SQL 即可完成初始化
-- ============================================================

-- 1. 创建数据库
CREATE DATABASE IF NOT EXISTS campus_swap
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE campus_swap;

-- ============================================================
-- 2. 用户表 t_user
-- ============================================================
DROP TABLE IF EXISTS t_user;
CREATE TABLE t_user (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  email       VARCHAR(100) NOT NULL                COMMENT '邮箱（登录账号，唯一）',
  password    VARCHAR(100) NOT NULL                COMMENT 'BCrypt 加密后的密码',
  nickname    VARCHAR(50)  NOT NULL                COMMENT '昵称',
  school      VARCHAR(100) DEFAULT NULL            COMMENT '学校',
  avatar      VARCHAR(255) DEFAULT NULL            COMMENT '头像 URL',
  phone       VARCHAR(20)  DEFAULT NULL            COMMENT '手机号',
  role        TINYINT      NOT NULL DEFAULT 0      COMMENT '角色: 0-普通用户 1-管理员 2-超级管理员',
  status      TINYINT      NOT NULL DEFAULT 0      COMMENT '账号状态: 0-正常 1-禁用',
  deleted     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0-正常 1-已删除',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================================
-- 如果已有旧表，执行此迁移语句替代重建（二选一）
-- ============================================================
-- ALTER TABLE t_user
--   ADD COLUMN email VARCHAR(100) UNIQUE COMMENT '邮箱（登录账号，唯一）' AFTER id,
--   DROP INDEX uk_username,
--   DROP COLUMN username;

-- ============================================================
-- 3. 商品表 t_item
-- ============================================================
DROP TABLE IF EXISTS t_item;
CREATE TABLE t_item (
  id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  title        VARCHAR(100)   NOT NULL                COMMENT '商品标题',
  price        DECIMAL(10, 2) NOT NULL DEFAULT 0.00   COMMENT '价格（元）',
  category     VARCHAR(30)    NOT NULL                COMMENT '分类: 数码/书籍/服饰/生活用品/其他',
  description  TEXT           DEFAULT NULL            COMMENT '商品描述',
  cover_image  VARCHAR(255)   DEFAULT NULL            COMMENT '封面图 URL',
  seller_id    BIGINT         NOT NULL                COMMENT '发布者用户 ID',
  images       TEXT           DEFAULT NULL            COMMENT '图片 URL 列表（JSON 数组）',
  status       TINYINT        NOT NULL DEFAULT 0      COMMENT '状态: 0-在售 1-已下架 2-已售出',
  deleted      TINYINT        NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0-正常 1-已删除',
  created_at   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  updated_at   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_seller_id (seller_id),
  KEY idx_category  (category),
  KEY idx_status    (status),
  KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品（闲置物）表';

-- ============================================================
-- 4. 初始化测试数据
-- ============================================================

-- 测试用户（密码均为 123456，BCrypt 加密）
INSERT INTO t_user (email, password, nickname, school) VALUES
('test1@campus.com', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW', '张三', '北京大学'),
('test2@campus.com', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW', '李四', '清华大学'),
('test3@campus.com', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW', '王五', '复旦大学');

-- 测试商品
INSERT INTO t_item (title, price, category, description, cover_image, seller_id, status) VALUES
('九成新 MacBook Air M1',        4500.00, '数码',     'M1 芯片，8G 内存，256G 存储，电池健康 92%，配原装充电器', NULL, 1, 0),
('高等数学（同济第七版）上下册', 20.00,   '书籍',     '少量笔记，无缺页，适合大一新生', NULL, 1, 0),
('Nike Air Force 1 白鞋 42码',   280.00,  '服饰',     '穿了三次，鞋盒在，95新', NULL, 2, 0),
('宿舍台灯（带 USB 充电口）',    35.00,   '生活用品', '换宿舍用不上，正常使用无损坏', NULL, 2, 0),
('iPad 2021 10.2寸 64G WiFi',    1600.00, '数码',     '含钢化膜和保护套，性能完好', NULL, 3, 0),
('有机化学（第四版）',            15.00,  '书籍',     '期末复习完就出，几乎全新', NULL, 3, 0);

-- ============================================================
-- 5. 迁移：为已有库执行以下 ALTER（已重建的库跳过）
-- ============================================================
-- ALTER TABLE t_item ADD COLUMN images TEXT DEFAULT NULL COMMENT '图片 URL 列表（JSON 数组）' AFTER cover_image;
-- ALTER TABLE t_user ADD COLUMN role   TINYINT NOT NULL DEFAULT 0 COMMENT '角色: 0-普通用户 1-管理员 2-超级管理员' AFTER phone;
-- ALTER TABLE t_user ADD COLUMN status TINYINT NOT NULL DEFAULT 0 COMMENT '账号状态: 0-正常 1-禁用' AFTER role;

-- 初始超级管理员（密码 Admin@2025，BCrypt 加密）
-- INSERT INTO t_user (email, password, nickname, role)
-- VALUES ('admin@campus.com', '$2a$10$7EqJtq98hPqEX7fNZaFWoOe3d9DVHM3eAt5IG5lMRMqY8x7tqq7Oa', '超级管理员', 2);

-- ============================================================
-- 6. 常用查询示例
-- ============================================================

-- 查询所有在售商品（按发布时间倒序）
-- SELECT * FROM t_item WHERE status = 0 AND deleted = 0 ORDER BY created_at DESC;

-- 关键词搜索
-- SELECT * FROM t_item WHERE status = 0 AND deleted = 0 AND title LIKE '%MacBook%';

-- 按分类筛选
-- SELECT * FROM t_item WHERE status = 0 AND deleted = 0 AND category = '数码';

-- 查询某用户发布的所有商品
-- SELECT * FROM t_item WHERE seller_id = 1 AND deleted = 0;

-- 连表查询商品 + 卖家昵称
-- SELECT i.*, u.nickname AS seller_nickname, u.avatar AS seller_avatar
-- FROM t_item i
-- LEFT JOIN t_user u ON i.seller_id = u.id
-- WHERE i.status = 0 AND i.deleted = 0
-- ORDER BY i.created_at DESC;

-- ============================================================
-- 6. 常用维护操作
-- ============================================================

-- 手动下架某商品
-- UPDATE t_item SET status = 1 WHERE id = ?;

-- 标记商品已售出
-- UPDATE t_item SET status = 2 WHERE id = ?;

-- 逻辑删除用户
-- UPDATE t_user SET deleted = 1 WHERE id = ?;

-- 重置用户密码（新密码需先在 Java 中 BCrypt 加密后替换 <hash>）
-- UPDATE t_user SET password = '<BCrypt_hash>' WHERE username = 'test1';

-- 查看各分类商品数量
-- SELECT category, COUNT(*) AS cnt FROM t_item WHERE deleted = 0 GROUP BY category;

ALTER TABLE t_item
    ADD COLUMN images TEXT DEFAULT NULL COMMENT '图片 URL 列表（JSON 数组）' AFTER cover_image;


ALTER TABLE t_user ADD COLUMN role   TINYINT NOT NULL DEFAULT 0 AFTER phone;
ALTER TABLE t_user ADD COLUMN status TINYINT NOT NULL DEFAULT 0 AFTER role;

-- 创建超级管理员（推荐做法）：
--   1. 通过前台 /register 页面正常注册账号（密码由应用 BCrypt 加密，hash 一定正确）
--   2. 执行下方 UPDATE 将该账号提升为超级管理员
-- UPDATE t_user SET role = 2 WHERE email = 'your_admin@example.com';

DELETE FROM t_user WHERE email = 'admin@campus.com';

UPDATE t_user SET role = 2 WHERE email = '19971516560@163.com';


