-- ============================================================
--  换了吗 — 数据库初始化脚本（完整建表，全新环境直接执行）
--  数据库: campus_swap  字符集: utf8mb4
-- ============================================================

CREATE DATABASE IF NOT EXISTS campus_swap
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE campus_swap;

-- ============================================================
-- 1. 用户表 t_user
--    role:   0-普通用户  1-管理员  2-超级管理员（唯一）
--    status: 0-正常      1-禁用
-- ============================================================
CREATE TABLE IF NOT EXISTS t_user (
  id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  email      VARCHAR(100) NOT NULL                COMMENT '邮箱（登录账号，唯一）',
  password   VARCHAR(100) NOT NULL                COMMENT 'BCrypt 加密后的密码',
  nickname   VARCHAR(50)  NOT NULL                COMMENT '昵称',
  school     VARCHAR(100) DEFAULT NULL            COMMENT '学校',
  avatar     VARCHAR(255) DEFAULT NULL            COMMENT '头像 URL',
  phone      VARCHAR(20)  DEFAULT NULL            COMMENT '手机号',
  role       TINYINT      NOT NULL DEFAULT 0      COMMENT '角色: 0-普通用户 1-管理员 2-超级管理员',
  status     TINYINT      NOT NULL DEFAULT 0      COMMENT '账号状态: 0-正常 1-禁用',
  deleted    TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0-正常 1-已删除',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                   COMMENT '创建时间',
  updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================================
-- 2. 商品表 t_item
--    status:       0-在售      1-已下架  2-已售出
--    audit_status: 0-待审核    1-已通过  2-已拒绝
-- ============================================================
CREATE TABLE IF NOT EXISTS t_item (
  id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  title        VARCHAR(100)   NOT NULL                COMMENT '商品标题',
  price        DECIMAL(10, 2) NOT NULL DEFAULT 0.00   COMMENT '价格（元）',
  category     VARCHAR(30)    NOT NULL                COMMENT '分类: 数码/书籍/服饰/生活用品/其他',
  description  TEXT           DEFAULT NULL            COMMENT '商品描述',
  cover_image  VARCHAR(255)   DEFAULT NULL            COMMENT '封面图 URL',
  images       TEXT           DEFAULT NULL            COMMENT '图片 URL 列表（JSON 数组）',
  seller_id    BIGINT         NOT NULL                COMMENT '发布者用户 ID',
  status       TINYINT        NOT NULL DEFAULT 0      COMMENT '状态: 0-在售 1-已下架 2-已售出',
  audit_status TINYINT        NOT NULL DEFAULT 0      COMMENT '审核状态: 0-待审核 1-已通过 2-已拒绝',
  audit_remark VARCHAR(200)   DEFAULT NULL            COMMENT '审核备注（拒绝原因）',
  deleted      TINYINT        NOT NULL DEFAULT 0      COMMENT '逻辑删除: 0-正常 1-已删除',
  created_at   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP                   COMMENT '发布时间',
  updated_at   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_seller_id  (seller_id),
  KEY idx_category   (category),
  KEY idx_status     (status),
  KEY idx_audit      (audit_status),
  KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品（闲置物）表';

-- ============================================================
-- 3. 管理员权限表 t_admin_permission
--    perm_code: USER_MANAGE / ITEM_MANAGE / ITEM_AUDIT
-- ============================================================
CREATE TABLE IF NOT EXISTS t_admin_permission (
  id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  admin_id   BIGINT      NOT NULL                COMMENT '管理员用户 ID（role=1 的用户）',
  perm_code  VARCHAR(50) NOT NULL                COMMENT '权限码: USER_MANAGE / ITEM_MANAGE / ITEM_AUDIT',
  created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '授权时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_admin_perm (admin_id, perm_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员权限表';

-- ============================================================
-- 4. 学生档案表 t_student_record（管理员维护）
--    同一学校内学号唯一
-- ============================================================
CREATE TABLE IF NOT EXISTS t_student_record (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  school      VARCHAR(100) NOT NULL                COMMENT '学校名称',
  student_id  VARCHAR(50)  NOT NULL                COMMENT '学号',
  real_name   VARCHAR(50)  NOT NULL                COMMENT '真实姓名',
  extra_info  VARCHAR(200) DEFAULT NULL            COMMENT '附加信息（专业/年级等）',
  created_by  BIGINT       DEFAULT NULL            COMMENT '录入管理员用户 ID',
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '录入时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_school_student (school, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生档案表';

-- ============================================================
-- 5. 学生认证申请表 t_student_verify
--    每个用户只允许有一条待审核或已通过的申请
--    status: 0-待审核  1-已通过  2-已拒绝
-- ============================================================
CREATE TABLE IF NOT EXISTS t_student_verify (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  user_id      BIGINT       NOT NULL                COMMENT '申请用户 ID',
  school       VARCHAR(100) NOT NULL                COMMENT '填写的学校名称',
  student_id   VARCHAR(50)  NOT NULL                COMMENT '填写的学号',
  real_name    VARCHAR(50)  NOT NULL                COMMENT '填写的真实姓名',
  extra_info   VARCHAR(200) DEFAULT NULL            COMMENT '补充说明',
  status       TINYINT      NOT NULL DEFAULT 0      COMMENT '状态: 0-待审核 1-已通过 2-已拒绝',
  remark       VARCHAR(200) DEFAULT NULL            COMMENT '审核备注（拒绝原因）',
  reviewed_by  BIGINT       DEFAULT NULL            COMMENT '审核管理员 ID',
  reviewed_at  DATETIME     DEFAULT NULL            COMMENT '审核时间',
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  PRIMARY KEY (id),
  KEY idx_user_id (user_id),
  KEY idx_status  (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生认证申请表';

-- ============================================================
-- 6. t_user 新增认证状态字段（已有库执行此 ALTER）
-- ============================================================
ALTER TABLE t_user
  ADD COLUMN IF NOT EXISTS is_verified TINYINT NOT NULL DEFAULT 0 COMMENT '学生认证状态: 0-未认证 1-已认证'
  AFTER phone;

-- ============================================================
-- 7. 创建超级管理员
--    推荐做法：通过前台 /register 注册后执行下方 UPDATE
-- ============================================================
-- UPDATE t_user SET role = 2 WHERE email = 'your_email@example.com';
