package com.itcodai.campus_swap.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Applies a small set of idempotent schema patches for older local databases.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TradeSchemaMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    private static final List<ColumnPatch> COLUMN_PATCHES = List.of(
            new ColumnPatch("t_trade", "initiator_want_terminate",
                    "ALTER TABLE `t_trade` ADD COLUMN `initiator_want_terminate` tinyint(1) NOT NULL DEFAULT 0 COMMENT '甲方是否已申请终止' AFTER `terminate_reason`"),
            new ColumnPatch("t_trade", "receiver_want_terminate",
                    "ALTER TABLE `t_trade` ADD COLUMN `receiver_want_terminate` tinyint(1) NOT NULL DEFAULT 0 COMMENT '乙方是否已申请终止' AFTER `initiator_want_terminate`"),
            new ColumnPatch("t_trade", "waiting_delivery_at",
                    "ALTER TABLE `t_trade` ADD COLUMN `waiting_delivery_at` DATETIME DEFAULT NULL COMMENT '进入WAITING_DELIVERY状态的时间' AFTER `audit_passed_at`"),
            new ColumnPatch("t_trade", "both_delivered_at",
                    "ALTER TABLE `t_trade` ADD COLUMN `both_delivered_at` DATETIME DEFAULT NULL COMMENT '双方均发货完成的时间' AFTER `waiting_delivery_at`"),
            new ColumnPatch("t_trade_appeal", "remark",
                    "ALTER TABLE `t_trade_appeal` ADD COLUMN `remark` VARCHAR(500) DEFAULT NULL COMMENT '管理员处理备注' AFTER `status`"),
            new ColumnPatch("t_trade_appeal", "reviewed_by",
                    "ALTER TABLE `t_trade_appeal` ADD COLUMN `reviewed_by` BIGINT DEFAULT NULL COMMENT '处理管理员ID' AFTER `remark`"),
            new ColumnPatch("t_trade_appeal", "reviewed_at",
                    "ALTER TABLE `t_trade_appeal` ADD COLUMN `reviewed_at` DATETIME DEFAULT NULL COMMENT '处理时间' AFTER `reviewed_by`")
    );

    private static final List<IndexPatch> INDEX_PATCHES = List.of(
            new IndexPatch("t_trade", "idx_initiator",
                    "ALTER TABLE `t_trade` ADD KEY `idx_initiator` (`initiator_id`)"),
            new IndexPatch("t_trade", "idx_receiver",
                    "ALTER TABLE `t_trade` ADD KEY `idx_receiver` (`receiver_id`)"),
            new IndexPatch("t_trade", "idx_delivery_deadline",
                    "ALTER TABLE `t_trade` ADD KEY `idx_delivery_deadline` (`status`, `delivery_deadline`)"),
            new IndexPatch("t_trade", "idx_receipt_deadline",
                    "ALTER TABLE `t_trade` ADD KEY `idx_receipt_deadline` (`status`, `receipt_deadline`)")
    );

    @Override
    public void run(ApplicationArguments args) {
        COLUMN_PATCHES.forEach(this::ensureColumn);
        INDEX_PATCHES.forEach(this::ensureIndex);
    }

    private void ensureColumn(ColumnPatch patch) {
        if (hasColumn(patch.tableName(), patch.columnName())) {
            return;
        }
        log.warn("[schema] applying missing column patch: {}.{}", patch.tableName(), patch.columnName());
        jdbcTemplate.execute(patch.ddl());
    }

    private void ensureIndex(IndexPatch patch) {
        if (hasIndex(patch.tableName(), patch.indexName())) {
            return;
        }
        log.warn("[schema] applying missing index patch: {}.{}", patch.tableName(), patch.indexName());
        jdbcTemplate.execute(patch.ddl());
    }

    private boolean hasColumn(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                tableName,
                columnName
        );
        return count != null && count > 0;
    }

    private boolean hasIndex(String tableName, String indexName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?",
                Integer.class,
                tableName,
                indexName
        );
        return count != null && count > 0;
    }

    private record ColumnPatch(String tableName, String columnName, String ddl) {
    }

    private record IndexPatch(String tableName, String indexName, String ddl) {
    }
}