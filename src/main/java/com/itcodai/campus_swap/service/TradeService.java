package com.itcodai.campus_swap.service;

import com.itcodai.campus_swap.common.result.Result;
import com.itcodai.campus_swap.dto.*;
import com.itcodai.campus_swap.vo.TradeLogVO;
import com.itcodai.campus_swap.vo.TradeVO;

import java.util.List;

/**
 * 交易服务接口（以物换物状态机）
 */
public interface TradeService {

    /**
     * 甲方发起交易申请
     * <p>前置条件：甲方物品在售且审核通过，且未参与其他活跃交易
     *
     * @param userId 当前登录用户（甲方）
     * @param dto    发起请求
     * @return 新建的交易单 VO
     */
    Result<TradeVO> initiateTrade(Long userId, InitiateTradeDTO dto);

    /**
     * 乙方确认匹配，PENDING_MATCH → MATCHED（→ AUDIT_PENDING 或 WAITING_DELIVERY）
     *
     * @param userId  当前登录用户（乙方）
     * @param tradeId 交易ID
     * @param dto     乙方选择提供的物品
     */
    Result<TradeVO> matchTrade(Long userId, Long tradeId, MatchTradeDTO dto);

    /**
     * 审核员处理交易审核（仅平台审核模式），AUDIT_PENDING → AUDIT_PASSED / AUDIT_REJECTED
     * <p>需要管理员权限（role >= 1）
     *
     * @param userId  审核操作员ID
     * @param tradeId 交易ID
     * @param dto     审核结果
     */
    Result<TradeVO> auditTrade(Long userId, Long tradeId, AuditTradeDTO dto);

    /**
     * 一方确认已发货，记录物流凭证，双方均发货后自动推进状态
     * <p>WAITING_DELIVERY 阶段：当前登录方标记"已发货"，双方均标记后 → BOTH_DELIVERED
     *
     * @param userId  操作方（甲方或乙方）
     * @param tradeId 交易ID
     * @param dto     物流凭证信息
     */
    Result<TradeVO> deliver(Long userId, Long tradeId, DeliverDTO dto);

    /**
     * 一方确认已收货，双方均确认后 → COMPLETED
     * <p>BOTH_DELIVERED 或 WAITING_CONFIRM_RECEIPT 阶段可操作
     *
     * @param userId  操作方（甲方或乙方）
     * @param tradeId 交易ID
     */
    Result<TradeVO> confirmReceipt(Long userId, Long tradeId);

    /**
     * 终止交易（任意非终态）
     * <p>交易双方或管理员均可发起终止
     *
     * @param userId  操作人ID
     * @param tradeId 交易ID
     * @param dto     终止原因
     */
    Result<Void> terminateTrade(Long userId, Long tradeId, TerminateTradeDTO dto);

    /**
     * 回滚交易状态（仅管理员可操作）
     * <p>
     * 规则：审核阶段 → MATCHED；执行阶段前三步 → AUDIT_PASSED（或 MATCHED 若无审核）
     *
     * @param userId  管理员ID
     * @param tradeId 交易ID
     * @param dto     回滚原因
     */
    Result<TradeVO> rollbackTrade(Long userId, Long tradeId, RollbackTradeDTO dto);

    /**
     * 获取交易详情
     *
     * @param userId  当前登录用户（需是交易双方之一或管理员）
     * @param tradeId 交易ID
     */
    Result<TradeVO> getTradeDetail(Long userId, Long tradeId);

    /**
     * 获取我的交易列表
     *
     * @param userId 当前登录用户
     * @param status 状态过滤（null 表示全部）
     */
    Result<List<TradeVO>> getMyTrades(Long userId, String status);

    /**
     * 获取交易状态变更日志
     *
     * @param userId  当前登录用户（需是交易双方之一或管理员）
     * @param tradeId 交易ID
     */
    Result<List<TradeLogVO>> getTradeLogs(Long userId, Long tradeId);

    /**
     * 定时任务：扫描并处理超时交易单（系统内部调用，无需权限校验）
     */
    void processTimeoutTrades();

    /**
     * 查询物品当前的活跃交易（非 COMPLETED/TERMINATED/AUDIT_REJECTED）
     *
     * @param userId 当前用户（用于构建 VO 的 myRole 字段，可传 null）
     * @param itemId 物品ID
     */
    Result<TradeVO> getItemActiveTrade(Long userId, Long itemId);
}
