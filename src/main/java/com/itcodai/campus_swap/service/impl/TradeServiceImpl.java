package com.itcodai.campus_swap.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itcodai.campus_swap.common.exception.BusinessException;
import com.itcodai.campus_swap.common.result.Result;
import com.itcodai.campus_swap.common.result.ResultCode;
import com.itcodai.campus_swap.config.TradeConfig;
import com.itcodai.campus_swap.dto.*;
import com.itcodai.campus_swap.entity.Item;
import com.itcodai.campus_swap.entity.Trade;
import com.itcodai.campus_swap.entity.TradeLog;
import com.itcodai.campus_swap.entity.User;
import com.itcodai.campus_swap.enums.TradeStatus;
import com.itcodai.campus_swap.mapper.ItemMapper;
import com.itcodai.campus_swap.mapper.TradeLogMapper;
import com.itcodai.campus_swap.mapper.TradeMapper;
import com.itcodai.campus_swap.mapper.UserMapper;
import com.itcodai.campus_swap.service.TradeService;
import com.itcodai.campus_swap.vo.TradeLogVO;
import com.itcodai.campus_swap.vo.TradeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 以物换物交易服务实现
 *
 * <h3>状态机说明</h3>
 * <pre>
 * PENDING_MATCH
 *   ↓ 乙方确认匹配
 * MATCHED
 *   ↓ 审核开启时           ↓ 审核关闭时
 * AUDIT_PENDING         WAITING_DELIVERY
 *   ↓ 审核通过
 * AUDIT_PASSED
 *   ↓
 * WAITING_DELIVERY  ←─ 回滚起点（执行阶段前三步可回滚至此）
 *   ↓ 双方均标记已发货
 * BOTH_DELIVERED
 *   ↓ 一方确认收货
 * WAITING_CONFIRM_RECEIPT
 *   ↓ 另一方确认收货
 * COMPLETED
 *
 * 任意非终态 → TERMINATED（取消/超时/争议）
 * </pre>
 *
 * <h3>乐观锁</h3>
 * 状态变更使用 {@code updateById()} 携带 {@code @Version} 字段；
 * 若并发写入导致更新行数为 0，抛出业务异常提示用户重试。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradeServiceImpl implements TradeService {

    // ============================================================
    // 状态机：允许的正向流转（fromStatus → 可达的 toStatus 集合）
    // ============================================================
    private static final Map<String, Set<String>> FORWARD_TRANSITIONS;

    static {
        FORWARD_TRANSITIONS = new HashMap<>();
        FORWARD_TRANSITIONS.put("PENDING_MATCH",
                Set.of("MATCHED", "TERMINATED"));
        FORWARD_TRANSITIONS.put("MATCHED",
                Set.of("AUDIT_PENDING", "WAITING_DELIVERY", "TERMINATED"));
        FORWARD_TRANSITIONS.put("AUDIT_PENDING",
                Set.of("AUDIT_PASSED", "AUDIT_REJECTED", "TERMINATED"));
        FORWARD_TRANSITIONS.put("AUDIT_PASSED",
                Set.of("WAITING_DELIVERY", "TERMINATED"));
        FORWARD_TRANSITIONS.put("AUDIT_REJECTED",
                Set.of("TERMINATED")); // 实际已是终态，此处为保险
        FORWARD_TRANSITIONS.put("WAITING_DELIVERY",
                Set.of("BOTH_DELIVERED", "TERMINATED"));
        FORWARD_TRANSITIONS.put("BOTH_DELIVERED",
                Set.of("WAITING_CONFIRM_RECEIPT", "TERMINATED"));
        FORWARD_TRANSITIONS.put("WAITING_CONFIRM_RECEIPT",
                Set.of("COMPLETED", "TERMINATED"));
        // COMPLETED / TERMINATED 无后继状态
    }

    private static final int TRIGGER_MANUAL = 0;
    private static final int TRIGGER_SYSTEM = 1;

    private final TradeMapper tradeMapper;
    private final TradeLogMapper tradeLogMapper;
    private final ItemMapper itemMapper;
    private final UserMapper userMapper;
    private final TradeConfig tradeConfig;

    // ============================================================
    // 核心业务方法
    // ============================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TradeVO> initiateTrade(Long userId, InitiateTradeDTO dto) {
        // 1. 检查是否已对该物品发起过尚未匹配的申请（避免重复挂起）
        Trade existing = tradeMapper.selectPendingMatchByInitiatorAndItem(userId, dto.getInitiatorItemId());
        if (existing != null) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "您已对此物品发起过交换申请（交易号：" + existing.getTradeNo() + "），请等待对方响应或先取消该申请");
        }

        // 2. 校验甲方物品
        Item initiatorItem = validateItemForTrade(dto.getInitiatorItemId(), userId, null);

        // 2. 若指定了乙方物品，校验其可用性（不强制乙方持有，由乙方在 match 阶段自行确认）
        if (dto.getReceiverItemId() != null) {
            Item receiverItem = itemMapper.selectById(dto.getReceiverItemId());
            if (receiverItem == null || receiverItem.getDeleted() == 1) {
                throw new BusinessException(ResultCode.NOT_FOUND, "目标物品不存在");
            }
            if (receiverItem.getStatus() != 0 || receiverItem.getAuditStatus() != 1) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "目标物品当前不可交换");
            }
            // 检查目标物品是否已在活跃交易中
            Trade receiverActiveTrade = tradeMapper.selectActiveTradeByItemId(dto.getReceiverItemId());
            if (receiverActiveTrade != null) {
                throw new BusinessException(ResultCode.BAD_REQUEST,
                        "目标物品已参与其他交易（交易号：" + receiverActiveTrade.getTradeNo() + "），请等待该交易完成后再试");
            }
            // 指定乙方时，自动设定 receiverId
            if (dto.getReceiverId() == null) {
                dto.setReceiverId(receiverItem.getSellerId());
            }
        }

        // 3. 不允许与自己交易
        if (dto.getReceiverId() != null && dto.getReceiverId().equals(userId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能与自己发起交易");
        }

        // 4. 创建交易单
        Trade trade = new Trade();
        trade.setTradeNo(generateTradeNo());
        trade.setInitiatorId(userId);
        trade.setReceiverId(dto.getReceiverId());
        trade.setInitiatorItemId(dto.getInitiatorItemId());
        trade.setReceiverItemId(dto.getReceiverItemId());
        trade.setStatus(TradeStatus.PENDING_MATCH.getCode());
        trade.setAuditMode(tradeConfig.getAudit().isEnabled() ? tradeConfig.getAudit().getMode() : 0);
        trade.setInitiatorDelivered(false);
        trade.setReceiverDelivered(false);
        trade.setInitiatorConfirmedReceipt(false);
        trade.setReceiverConfirmedReceipt(false);
        trade.setDeliveryTimeoutHours(tradeConfig.getTimeout().getDeliveryHours());
        trade.setReceiptTimeoutHours(tradeConfig.getTimeout().getReceiptHours());
        trade.setVersion(0);

        tradeMapper.insert(trade);

        // 5. 写初始日志
        writeLog(trade.getId(), null, TradeStatus.PENDING_MATCH.getCode(),
                TRIGGER_MANUAL, userId, dto.getRemark() != null ? dto.getRemark() : "甲方发起交易申请");

        log.info("[交易] 新建交易单 tradeNo={} initiatorId={}", trade.getTradeNo(), userId);
        return Result.success(buildTradeVO(trade, userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TradeVO> matchTrade(Long userId, Long tradeId, MatchTradeDTO dto) {
        Trade trade = getTradeOrThrow(tradeId);

        // 1. 状态校验
        assertStatus(trade, TradeStatus.PENDING_MATCH);

        // 2. 不能自己与自己匹配
        if (trade.getInitiatorId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "甲方不能确认自己发起的交易匹配");
        }

        // 3. 若指定了乙方，验证身份
        if (trade.getReceiverId() != null && !trade.getReceiverId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "此交易只能由指定用户确认匹配");
        }

        // 4. 若甲方已指定了乙方物品，校验一致性
        if (trade.getReceiverItemId() != null && !trade.getReceiverItemId().equals(dto.getReceiverItemId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "甲方指定了交换物品 ID=" + trade.getReceiverItemId() + "，请使用该物品确认匹配");
        }

        // 5. 校验乙方物品（排除当前交易本身，防止数据不一致时出现自引用误报）
        validateItemForTrade(dto.getReceiverItemId(), userId, tradeId);

        // 6. 更新交易单
        trade.setReceiverId(userId);
        trade.setReceiverItemId(dto.getReceiverItemId());
        trade.setMatchedAt(LocalDateTime.now());

        doTransition(trade, TradeStatus.MATCHED.getCode(), userId, "乙方确认匹配", TRIGGER_MANUAL);

        // 7. 双方物品进入"交换中"锁定状态（防止重复参与其他交易）
        lockItemsForTrade(trade);

        // 8. 根据审核配置自动进入下一阶段
        if (tradeConfig.getAudit().isEnabled()) {
            doTransition(trade, TradeStatus.AUDIT_PENDING.getCode(), null, "系统自动进入审核队列", TRIGGER_SYSTEM);
        } else {
            enterWaitingDelivery(trade, null);
        }

        return Result.success(buildTradeVO(trade, userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TradeVO> auditTrade(Long userId, Long tradeId, AuditTradeDTO dto) {
        // 1. 权限：需要管理员
        checkAdminPermission(userId);

        Trade trade = getTradeOrThrow(tradeId);

        // 2. 状态校验
        assertStatus(trade, TradeStatus.AUDIT_PENDING);

        // 3. 驳回时必须填写原因
        if (!dto.getPassed() && (dto.getRemark() == null || dto.getRemark().isBlank())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "审核驳回时必须填写驳回原因");
        }

        trade.setAuditBy(userId);
        trade.setAuditRemark(dto.getRemark());

        if (dto.getPassed()) {
            trade.setAuditPassedAt(LocalDateTime.now());
            doTransition(trade, TradeStatus.AUDIT_PASSED.getCode(), userId, "审核通过：" + dto.getRemark(), TRIGGER_MANUAL);
            // 审核通过后自动进入发货准备阶段
            enterWaitingDelivery(trade, userId);
        } else {
            doTransition(trade, TradeStatus.AUDIT_REJECTED.getCode(), userId, "审核驳回：" + dto.getRemark(), TRIGGER_MANUAL);
            // 审核驳回：解锁双方物品，恢复为在售状态
            unlockItemsFromTrade(trade);
        }

        return Result.success(buildTradeVO(trade, userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TradeVO> deliver(Long userId, Long tradeId, DeliverDTO dto) {
        Trade trade = getTradeOrThrow(tradeId);

        // 1. 状态校验
        assertStatus(trade, TradeStatus.WAITING_DELIVERY);

        // 2. 权限：只有双方可操作
        boolean isInitiator = trade.getInitiatorId().equals(userId);
        boolean isReceiver = userId.equals(trade.getReceiverId());
        if (!isInitiator && !isReceiver) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权限操作此交易");
        }

        // 3. 防止重复标记
        if (isInitiator && Boolean.TRUE.equals(trade.getInitiatorDelivered())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "您已标记发货，无需重复操作");
        }
        if (isReceiver && Boolean.TRUE.equals(trade.getReceiverDelivered())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "您已标记发货，无需重复操作");
        }

        // 4. 更新发货标记
        if (isInitiator) {
            trade.setInitiatorDelivered(true);
            trade.setInitiatorLogistics(dto.getLogistics());
        } else {
            trade.setReceiverDelivered(true);
            trade.setReceiverLogistics(dto.getLogistics());
        }

        String role = isInitiator ? "甲方" : "乙方";
        String remark = role + "已发货" + (dto.getLogistics() != null ? "，物流：" + dto.getLogistics() : "");

        // 5. 判断是否双方均已发货
        if (Boolean.TRUE.equals(trade.getInitiatorDelivered()) &&
                Boolean.TRUE.equals(trade.getReceiverDelivered())) {
            trade.setBothDeliveredAt(LocalDateTime.now());
            doTransition(trade, TradeStatus.BOTH_DELIVERED.getCode(), userId, remark, TRIGGER_MANUAL);
            // 立即进入收货确认阶段并设置超时
            enterWaitingConfirmReceipt(trade, userId);
        } else {
            // 仅更新，不变更状态
            int updated = tradeMapper.updateById(trade);
            if (updated == 0) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "操作冲突，请刷新后重试");
            }
            writeLog(trade.getId(), trade.getStatus(), trade.getStatus(),
                    TRIGGER_MANUAL, userId, remark + "（等待对方发货）");
        }

        return Result.success(buildTradeVO(trade, userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TradeVO> confirmReceipt(Long userId, Long tradeId) {
        Trade trade = getTradeOrThrow(tradeId);

        // 1. 状态校验：BOTH_DELIVERED 或 WAITING_CONFIRM_RECEIPT 均可操作
        String status = trade.getStatus();
        if (!TradeStatus.BOTH_DELIVERED.getCode().equals(status) &&
                !TradeStatus.WAITING_CONFIRM_RECEIPT.getCode().equals(status)) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "当前状态（" + status + "）无法确认收货");
        }

        // 2. 权限
        boolean isInitiator = trade.getInitiatorId().equals(userId);
        boolean isReceiver = userId.equals(trade.getReceiverId());
        if (!isInitiator && !isReceiver) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权限操作此交易");
        }

        // 3. 防止重复确认
        if (isInitiator && Boolean.TRUE.equals(trade.getInitiatorConfirmedReceipt())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "您已确认收货，无需重复操作");
        }
        if (isReceiver && Boolean.TRUE.equals(trade.getReceiverConfirmedReceipt())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "您已确认收货，无需重复操作");
        }

        // 4. 更新确认标记
        if (isInitiator) {
            trade.setInitiatorConfirmedReceipt(true);
        } else {
            trade.setReceiverConfirmedReceipt(true);
        }

        String role = isInitiator ? "甲方" : "乙方";
        boolean bothConfirmed = Boolean.TRUE.equals(trade.getInitiatorConfirmedReceipt())
                && Boolean.TRUE.equals(trade.getReceiverConfirmedReceipt());

        if (bothConfirmed) {
            // 双方均确认 → COMPLETED
            trade.setCompletedAt(LocalDateTime.now());
            doTransition(trade, TradeStatus.COMPLETED.getCode(), userId,
                    role + "确认收货，交易完成", TRIGGER_MANUAL);
            // 将双方物品标记为已售出
            markItemsSold(trade);
        } else {
            // 仅第一方确认
            if (TradeStatus.BOTH_DELIVERED.getCode().equals(status)) {
                doTransition(trade, TradeStatus.WAITING_CONFIRM_RECEIPT.getCode(), userId,
                        role + "已确认收货，等待对方确认", TRIGGER_MANUAL);
            } else {
                // 已在 WAITING_CONFIRM_RECEIPT，仅更新字段
                int updated = tradeMapper.updateById(trade);
                if (updated == 0) {
                    throw new BusinessException(ResultCode.BAD_REQUEST, "操作冲突，请刷新后重试");
                }
                writeLog(trade.getId(), status, status,
                        TRIGGER_MANUAL, userId, role + "已确认收货，等待对方确认");
            }
        }

        return Result.success(buildTradeVO(trade, userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> terminateTrade(Long userId, Long tradeId, TerminateTradeDTO dto) {
        Trade trade = getTradeOrThrow(tradeId);

        // 1. 终态不能再操作
        TradeStatus current = TradeStatus.fromCode(trade.getStatus());
        if (current != null && current.isTerminal()) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "交易已处于终态（" + current.getDescription() + "），无法终止");
        }

        // 2. 权限：交易双方或管理员
        boolean isAdmin = isAdmin(userId);
        boolean isParty = trade.getInitiatorId().equals(userId) ||
                (trade.getReceiverId() != null && trade.getReceiverId().equals(userId));
        if (!isAdmin && !isParty) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权限终止此交易");
        }

        String reason = dto != null && dto.getReason() != null ? dto.getReason() : "用户主动取消";
        trade.setTerminateReason(reason);
        trade.setTerminatedAt(LocalDateTime.now());

        doTransition(trade, TradeStatus.TERMINATED.getCode(), userId, reason, TRIGGER_MANUAL);

        // 解锁双方物品，恢复为在售状态（仅 MATCHED 及之后才锁定了物品）
        unlockItemsFromTrade(trade);

        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TradeVO> rollbackTrade(Long userId, Long tradeId, RollbackTradeDTO dto) {
        // 1. 管理员权限
        checkAdminPermission(userId);

        Trade trade = getTradeOrThrow(tradeId);
        String currentStatus = trade.getStatus();

        // 2. 计算回滚目标状态
        String rollbackTarget = computeRollbackTarget(currentStatus, trade.getAuditMode());
        if (rollbackTarget == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "当前状态（" + currentStatus + "）不支持回滚");
        }

        String reason = dto != null && dto.getReason() != null ? dto.getReason() : "管理员回滚";

        // 3. 若从执行阶段回滚，需清除执行阶段数据
        if (isExecutionStage(currentStatus)) {
            trade.setInitiatorDelivered(false);
            trade.setReceiverDelivered(false);
            trade.setInitiatorLogistics(null);
            trade.setReceiverLogistics(null);
            trade.setInitiatorConfirmedReceipt(false);
            trade.setReceiverConfirmedReceipt(false);
            trade.setDeliveryDeadline(null);
            trade.setReceiptDeadline(null);
            trade.setBothDeliveredAt(null);
            trade.setWaitingDeliveryAt(null);
        }

        // 4. 若从审核阶段回滚到 MATCHED，清除审核数据
        if (isAuditStage(currentStatus)) {
            trade.setAuditBy(null);
            trade.setAuditRemark(null);
            trade.setAuditPassedAt(null);
        }

        doTransition(trade, rollbackTarget, userId, "管理员回滚：" + reason, TRIGGER_MANUAL);

        log.info("[交易] 管理员回滚 tradeId={} {} → {} 原因={}", tradeId, currentStatus, rollbackTarget, reason);
        return Result.success(buildTradeVO(trade, userId));
    }

    @Override
    public Result<TradeVO> getTradeDetail(Long userId, Long tradeId) {
        Trade trade = getTradeOrThrow(tradeId);

        // 只有交易双方或管理员可查看
        boolean isParty = trade.getInitiatorId().equals(userId) ||
                (trade.getReceiverId() != null && trade.getReceiverId().equals(userId));
        if (!isParty && !isAdmin(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权限查看此交易");
        }

        return Result.success(buildTradeVO(trade, userId));
    }

    @Override
    public Result<List<TradeVO>> getMyTrades(Long userId, String status) {
        LambdaQueryWrapper<Trade> wrapper = new LambdaQueryWrapper<Trade>()
                .and(w -> w.eq(Trade::getInitiatorId, userId)
                        .or().eq(Trade::getReceiverId, userId))
                .orderByDesc(Trade::getCreatedAt);

        if (status != null && !status.isBlank()) {
            wrapper.eq(Trade::getStatus, status);
        }

        List<Trade> trades = tradeMapper.selectList(wrapper);
        List<TradeVO> vos = trades.stream()
                .map(t -> buildTradeVO(t, userId))
                .collect(Collectors.toList());

        return Result.success(vos);
    }

    @Override
    public Result<List<TradeLogVO>> getTradeLogs(Long userId, Long tradeId) {
        Trade trade = getTradeOrThrow(tradeId);

        boolean isParty = trade.getInitiatorId().equals(userId) ||
                (trade.getReceiverId() != null && trade.getReceiverId().equals(userId));
        if (!isParty && !isAdmin(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权限查看此交易日志");
        }

        List<TradeLog> logs = tradeLogMapper.selectList(
                new LambdaQueryWrapper<TradeLog>()
                        .eq(TradeLog::getTradeId, tradeId)
                        .orderByAsc(TradeLog::getCreatedAt));

        // 批量加载操作人信息
        Set<Long> operatorIds = logs.stream()
                .filter(l -> l.getOperatorId() != null)
                .map(TradeLog::getOperatorId)
                .collect(Collectors.toSet());
        Map<Long, String> nicknameMap = new HashMap<>();
        if (!operatorIds.isEmpty()) {
            userMapper.selectBatchIds(operatorIds)
                    .forEach(u -> nicknameMap.put(u.getId(), u.getNickname()));
        }

        List<TradeLogVO> vos = logs.stream().map(l -> {
            TradeLogVO vo = new TradeLogVO();
            vo.setId(l.getId());
            vo.setTradeId(l.getTradeId());
            vo.setFromStatus(l.getFromStatus());
            vo.setFromStatusDesc(describeStatus(l.getFromStatus()));
            vo.setToStatus(l.getToStatus());
            vo.setToStatusDesc(describeStatus(l.getToStatus()));
            vo.setTriggerType(l.getTriggerType());
            vo.setTriggerTypeDesc(l.getTriggerType() == TRIGGER_SYSTEM ? "系统自动" : "手动操作");
            vo.setOperatorId(l.getOperatorId());
            vo.setOperatorNickname(l.getOperatorId() != null ? nicknameMap.get(l.getOperatorId()) : "系统");
            vo.setRemark(l.getRemark());
            vo.setCreatedAt(l.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());

        return Result.success(vos);
    }

    /**
     * 定时任务入口：扫描超时交易，自动终止
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processTimeoutTrades() {
        // 发货超时
        List<Trade> deliveryTimeout = tradeMapper.selectTimeoutDeliveryTrades();
        for (Trade trade : deliveryTimeout) {
            try {
                trade.setTerminateReason("发货超时（超过 " + trade.getDeliveryTimeoutHours() + " 小时未完成发货）");
                trade.setTerminatedAt(LocalDateTime.now());
                doTransition(trade, TradeStatus.TERMINATED.getCode(), null,
                        trade.getTerminateReason(), TRIGGER_SYSTEM);
                unlockItemsFromTrade(trade);
                log.info("[超时] 发货超时终止 tradeId={}", trade.getId());
            } catch (Exception e) {
                log.error("[超时] 发货超时处理失败 tradeId={}", trade.getId(), e);
            }
        }

        // 收货确认超时
        List<Trade> receiptTimeout = tradeMapper.selectTimeoutReceiptTrades();
        for (Trade trade : receiptTimeout) {
            try {
                trade.setTerminateReason("收货确认超时（超过 " + trade.getReceiptTimeoutHours() + " 小时未完成收货确认）");
                trade.setTerminatedAt(LocalDateTime.now());
                doTransition(trade, TradeStatus.TERMINATED.getCode(), null,
                        trade.getTerminateReason(), TRIGGER_SYSTEM);
                unlockItemsFromTrade(trade);
                log.info("[超时] 收货确认超时终止 tradeId={}", trade.getId());
            } catch (Exception e) {
                log.error("[超时] 收货确认超时处理失败 tradeId={}", trade.getId(), e);
            }
        }
    }

    // ============================================================
    // 私有辅助方法
    // ============================================================

    /**
     * 校验状态机允许的流转，更新实体状态，执行 updateById（含乐观锁），写日志
     *
     * @param trade       待更新的交易实体（已从 DB 加载，携带 version）
     * @param toStatus    目标状态 code
     * @param operatorId  操作人（系统触发时传 null）
     * @param remark      备注
     * @param triggerType TRIGGER_MANUAL / TRIGGER_SYSTEM
     */
    private void doTransition(Trade trade, String toStatus, Long operatorId,
                              String remark, int triggerType) {
        String fromStatus = trade.getStatus();

        // 校验流转合法性（回滚不经此方法，直接传目标状态也会在此被拦截）
        Set<String> allowed = FORWARD_TRANSITIONS.get(fromStatus);
        if (allowed == null || !allowed.contains(toStatus)) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "非法状态流转：" + fromStatus + " → " + toStatus);
        }

        trade.setStatus(toStatus);

        // 写入各阶段时间戳
        fillTimestamp(trade, toStatus);

        int updated = tradeMapper.updateById(trade);
        if (updated == 0) {
            // 乐观锁冲突：version 已被其他事务更新
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "操作冲突（并发更新），请刷新后重试");
        }

        writeLog(trade.getId(), fromStatus, toStatus, triggerType, operatorId, remark);
    }

    /**
     * 进入 WAITING_DELIVERY 阶段（设置发货截止时间）
     */
    private void enterWaitingDelivery(Trade trade, Long operatorId) {
        trade.setWaitingDeliveryAt(LocalDateTime.now());
        trade.setDeliveryDeadline(LocalDateTime.now().plusHours(trade.getDeliveryTimeoutHours()));
        doTransition(trade, TradeStatus.WAITING_DELIVERY.getCode(), operatorId,
                "系统进入等待发货阶段，截止时间：" + trade.getDeliveryDeadline(), TRIGGER_SYSTEM);
    }

    /**
     * 进入 WAITING_CONFIRM_RECEIPT 阶段（设置收货确认截止时间）
     */
    private void enterWaitingConfirmReceipt(Trade trade, Long operatorId) {
        trade.setReceiptDeadline(LocalDateTime.now().plusHours(trade.getReceiptTimeoutHours()));
        doTransition(trade, TradeStatus.WAITING_CONFIRM_RECEIPT.getCode(), operatorId,
                "双方均已发货，进入收货确认阶段，截止时间：" + trade.getReceiptDeadline(), TRIGGER_SYSTEM);
    }

    /**
     * 校验物品是否可参与交易
     *
     * @param itemId         物品ID
     * @param ownerId        期望的所有者（传 null 则不校验归属）
     * @param excludeTradeId 排除的交易ID（传 null 表示不排除；matchTrade 时传当前交易ID，避免查到自身造成误报）
     * @return 校验通过的 Item
     */
    private Item validateItemForTrade(Long itemId, Long ownerId, Long excludeTradeId) {
        Item item = itemMapper.selectById(itemId);
        if (item == null || item.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "物品不存在");
        }
        if (ownerId != null && !item.getSellerId().equals(ownerId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "该物品不属于当前用户");
        }
        if (item.getStatus() == 3) {
            // 物品被锁定在活跃交易中（status=3 交换中），给出更精确的提示
            Trade lockedTrade = excludeTradeId != null
                    ? tradeMapper.selectActiveTradeByItemIdExcluding(itemId, excludeTradeId)
                    : tradeMapper.selectActiveTradeByItemId(itemId);
            String tradeNoHint = lockedTrade != null ? "（交易号：" + lockedTrade.getTradeNo() + "）" : "";
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "物品【" + item.getTitle() + "】正在其他交易中" + tradeNoHint + "，请先完成或取消该交易");
        }
        if (item.getStatus() != 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "物品【" + item.getTitle() + "】已下架或已售出，无法参与交易");
        }
        if (item.getAuditStatus() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "物品【" + item.getTitle() + "】尚未通过审核，无法参与交易");
        }
        // 检查是否已在活跃交易中（排除当前正在处理的交易，防止自引用误报）
        Trade activeTrade = excludeTradeId != null
                ? tradeMapper.selectActiveTradeByItemIdExcluding(itemId, excludeTradeId)
                : tradeMapper.selectActiveTradeByItemId(itemId);
        if (activeTrade != null) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "物品【" + item.getTitle() + "】已参与其他交易（交易号：" + activeTrade.getTradeNo() + "），请先完成或取消该交易");
        }
        return item;
    }

    /**
     * 交易完成后将双方物品标记为已售出（status=2）
     */
    private void markItemsSold(Trade trade) {
        if (trade.getInitiatorItemId() != null) {
            Item item = new Item();
            item.setId(trade.getInitiatorItemId());
            item.setStatus(2);
            itemMapper.updateById(item);
        }
        if (trade.getReceiverItemId() != null) {
            Item item = new Item();
            item.setId(trade.getReceiverItemId());
            item.setStatus(2);
            itemMapper.updateById(item);
        }
    }

    /**
     * 计算回滚目标状态
     *
     * @param currentStatus 当前状态 code
     * @param auditMode     0=无审核，其他=有审核
     * @return 回滚目标状态 code，null 表示不支持回滚
     */
    private String computeRollbackTarget(String currentStatus, Integer auditMode) {
        boolean hasAudit = auditMode != null && auditMode != 0;
        return switch (currentStatus) {
            // 审核阶段 → 匹配阶段
            case "AUDIT_PENDING", "AUDIT_PASSED" -> TradeStatus.MATCHED.getCode();
            // 执行阶段前三步 → 审核阶段（有审核）或匹配阶段（无审核）
            case "WAITING_DELIVERY", "BOTH_DELIVERED", "WAITING_CONFIRM_RECEIPT" ->
                    hasAudit ? TradeStatus.AUDIT_PASSED.getCode() : TradeStatus.MATCHED.getCode();
            default -> null; // 终态或 PENDING_MATCH/MATCHED 不支持回滚
        };
    }

    private boolean isExecutionStage(String status) {
        return "WAITING_DELIVERY".equals(status) ||
                "BOTH_DELIVERED".equals(status) ||
                "WAITING_CONFIRM_RECEIPT".equals(status);
    }

    private boolean isAuditStage(String status) {
        return "AUDIT_PENDING".equals(status) || "AUDIT_PASSED".equals(status);
    }

    private void assertStatus(Trade trade, TradeStatus expected) {
        if (!expected.getCode().equals(trade.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "当前交易状态为【" + describeStatus(trade.getStatus()) + "】，不支持此操作");
        }
    }

    private Trade getTradeOrThrow(Long tradeId) {
        Trade trade = tradeMapper.selectById(tradeId);
        if (trade == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "交易单不存在");
        }
        return trade;
    }

    private void checkAdminPermission(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getRole() < 1) {
            throw new BusinessException(ResultCode.FORBIDDEN, "需要管理员权限");
        }
    }

    private boolean isAdmin(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null && user.getRole() >= 1;
    }

    /**
     * 填充状态对应的时间戳字段
     */
    private void fillTimestamp(Trade trade, String toStatus) {
        LocalDateTime now = LocalDateTime.now();
        switch (toStatus) {
            case "MATCHED" -> trade.setMatchedAt(now);
            case "AUDIT_PASSED" -> trade.setAuditPassedAt(now);
            case "WAITING_DELIVERY" -> trade.setWaitingDeliveryAt(now);
            case "BOTH_DELIVERED" -> trade.setBothDeliveredAt(now);
            case "COMPLETED" -> trade.setCompletedAt(now);
            case "TERMINATED" -> trade.setTerminatedAt(now);
        }
    }

    /**
     * 写状态变更日志
     */
    private void writeLog(Long tradeId, String fromStatus, String toStatus,
                          int triggerType, Long operatorId, String remark) {
        TradeLog log = new TradeLog();
        log.setTradeId(tradeId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setTriggerType(triggerType);
        log.setOperatorId(operatorId);
        log.setRemark(remark);
        tradeLogMapper.insert(log);
    }

    /**
     * 构建 TradeVO（含关联的用户和物品信息）
     */
    private TradeVO buildTradeVO(Trade trade, Long currentUserId) {
        TradeVO vo = new TradeVO();
        vo.setId(trade.getId());
        vo.setTradeNo(trade.getTradeNo());
        vo.setInitiatorId(trade.getInitiatorId());
        vo.setReceiverId(trade.getReceiverId());
        vo.setInitiatorItemId(trade.getInitiatorItemId());
        vo.setReceiverItemId(trade.getReceiverItemId());
        vo.setStatus(trade.getStatus());
        vo.setStatusDesc(describeStatus(trade.getStatus()));
        vo.setAuditMode(trade.getAuditMode());
        vo.setAuditRemark(trade.getAuditRemark());
        vo.setInitiatorDelivered(trade.getInitiatorDelivered());
        vo.setReceiverDelivered(trade.getReceiverDelivered());
        vo.setInitiatorLogistics(trade.getInitiatorLogistics());
        vo.setReceiverLogistics(trade.getReceiverLogistics());
        vo.setInitiatorConfirmedReceipt(trade.getInitiatorConfirmedReceipt());
        vo.setReceiverConfirmedReceipt(trade.getReceiverConfirmedReceipt());
        vo.setTerminateReason(trade.getTerminateReason());
        vo.setDeliveryDeadline(trade.getDeliveryDeadline());
        vo.setReceiptDeadline(trade.getReceiptDeadline());
        vo.setMatchedAt(trade.getMatchedAt());
        vo.setAuditPassedAt(trade.getAuditPassedAt());
        vo.setWaitingDeliveryAt(trade.getWaitingDeliveryAt());
        vo.setBothDeliveredAt(trade.getBothDeliveredAt());
        vo.setCompletedAt(trade.getCompletedAt());
        vo.setTerminatedAt(trade.getTerminatedAt());
        vo.setCreatedAt(trade.getCreatedAt());
        vo.setUpdatedAt(trade.getUpdatedAt());

        // 角色标注
        if (trade.getInitiatorId().equals(currentUserId)) {
            vo.setMyRole("initiator");
        } else if (trade.getReceiverId() != null && trade.getReceiverId().equals(currentUserId)) {
            vo.setMyRole("receiver");
        } else {
            vo.setMyRole("admin");
        }

        // 填充甲方信息
        fillUserInfo(vo, trade.getInitiatorId(), true);

        // 填充乙方信息
        if (trade.getReceiverId() != null) {
            fillUserInfo(vo, trade.getReceiverId(), false);
        }

        // 填充甲方物品信息
        fillItemInfo(vo, trade.getInitiatorItemId(), true);

        // 填充乙方物品信息
        if (trade.getReceiverItemId() != null) {
            fillItemInfo(vo, trade.getReceiverItemId(), false);
        }

        return vo;
    }

    private void fillUserInfo(TradeVO vo, Long userId, boolean isInitiator) {
        User user = userMapper.selectById(userId);
        if (user == null) return;
        if (isInitiator) {
            vo.setInitiatorNickname(user.getNickname());
            vo.setInitiatorAvatar(user.getAvatar());
        } else {
            vo.setReceiverNickname(user.getNickname());
            vo.setReceiverAvatar(user.getAvatar());
        }
    }

    private void fillItemInfo(TradeVO vo, Long itemId, boolean isInitiator) {
        Item item = itemMapper.selectById(itemId);
        if (item == null) return;
        if (isInitiator) {
            vo.setInitiatorItemTitle(item.getTitle());
            vo.setInitiatorItemCoverImage(item.getCoverImage());
        } else {
            vo.setReceiverItemTitle(item.getTitle());
            vo.setReceiverItemCoverImage(item.getCoverImage());
        }
    }

    private String describeStatus(String statusCode) {
        if (statusCode == null) return null;
        TradeStatus s = TradeStatus.fromCode(statusCode);
        return s != null ? s.getDescription() : statusCode;
    }

    /**
     * 生成交易编号：TRADE + yyyyMMddHHmmss + 6位随机数
     */
    private String generateTradeNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int rand = (int) (Math.random() * 900000) + 100000;
        return "TRADE" + timestamp + rand;
    }

    /**
     * 将双方物品锁定（status = 3 "交换中"），防止重复参与其他交易
     */
    private void lockItemsForTrade(Trade trade) {
        setItemTradingStatus(trade.getInitiatorItemId(), 3);
        if (trade.getReceiverItemId() != null) {
            setItemTradingStatus(trade.getReceiverItemId(), 3);
        }
    }

    /**
     * 解锁双方物品，恢复在售状态（status 3→0），用于交易终止/驳回场景
     */
    private void unlockItemsFromTrade(Trade trade) {
        resetIfLocked(trade.getInitiatorItemId());
        if (trade.getReceiverItemId() != null) {
            resetIfLocked(trade.getReceiverItemId());
        }
    }

    private void setItemTradingStatus(Long itemId, int status) {
        if (itemId == null) return;
        Item upd = new Item();
        upd.setId(itemId);
        upd.setStatus(status);
        itemMapper.updateById(upd);
    }

    private void resetIfLocked(Long itemId) {
        if (itemId == null) return;
        Item current = itemMapper.selectById(itemId);
        if (current != null && current.getStatus() == 3) {
            setItemTradingStatus(itemId, 0);
        }
    }

    // ============================================================
    // 按物品查询活跃交易
    // ============================================================

    @Override
    public Result<TradeVO> getItemActiveTrade(Long userId, Long itemId) {
        Trade trade = tradeMapper.selectActiveTradeByItemId(itemId);
        if (trade == null) return Result.success(null);
        return Result.success(buildTradeVO(trade, userId != null ? userId : 0L));
    }
}
