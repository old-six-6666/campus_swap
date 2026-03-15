package com.itcodai.campus_swap.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itcodai.campus_swap.entity.Trade;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 交易单 Mapper
 */
@Mapper
public interface TradeMapper extends BaseMapper<Trade> {

    /**
     * 查询物品是否已被「锁定」（MATCHED 及之后阶段，双方均已承诺，不可再参与新交易）
     * <p>
     * 注意：PENDING_MATCH 阶段乙方尚未响应，不算锁定，不在此查询范围内。
     * 排除的终态：COMPLETED、TERMINATED、AUDIT_REJECTED。
     *
     * @param itemId 物品ID
     * @return 锁定中的交易单，null 表示物品可自由参与新交易
     */
    @Select("SELECT * FROM t_trade " +
            "WHERE deleted = 0 " +
            "  AND status NOT IN ('PENDING_MATCH', 'COMPLETED', 'TERMINATED', 'AUDIT_REJECTED') " +
            "  AND (initiator_item_id = #{itemId} OR receiver_item_id = #{itemId}) " +
            "LIMIT 1")
    Trade selectActiveTradeByItemId(@Param("itemId") Long itemId);

    /**
     * 同 {@link #selectActiveTradeByItemId}，但排除指定交易（用于 matchTrade 时避免查到当前正在处理的交易本身）
     *
     * @param itemId         物品ID
     * @param excludeTradeId 要排除的交易ID（当前正在处理的交易）
     */
    @Select("SELECT * FROM t_trade " +
            "WHERE deleted = 0 " +
            "  AND status NOT IN ('PENDING_MATCH', 'COMPLETED', 'TERMINATED', 'AUDIT_REJECTED') " +
            "  AND (initiator_item_id = #{itemId} OR receiver_item_id = #{itemId}) " +
            "  AND id != #{excludeTradeId} " +
            "LIMIT 1")
    Trade selectActiveTradeByItemIdExcluding(@Param("itemId") Long itemId,
                                             @Param("excludeTradeId") Long excludeTradeId);

    /**
     * 查询指定用户对指定物品是否存在尚未匹配的挂起申请（PENDING_MATCH）
     * 防止同一用户对同一物品重复发起申请
     *
     * @param initiatorId    发起方用户ID
     * @param initiatorItemId 发起方物品ID
     */
    @Select("SELECT * FROM t_trade " +
            "WHERE deleted = 0 " +
            "  AND initiator_id = #{initiatorId} " +
            "  AND initiator_item_id = #{initiatorItemId} " +
            "  AND status = 'PENDING_MATCH' " +
            "LIMIT 1")
    Trade selectPendingMatchByInitiatorAndItem(@Param("initiatorId") Long initiatorId,
                                               @Param("initiatorItemId") Long initiatorItemId);

    /**
     * 查询发货超时的交易单（WAITING_DELIVERY 且当前时间已超过 delivery_deadline）
     */
    @Select("SELECT * FROM t_trade " +
            "WHERE deleted = 0 " +
            "  AND status = 'WAITING_DELIVERY' " +
            "  AND delivery_deadline IS NOT NULL " +
            "  AND delivery_deadline < NOW()")
    List<Trade> selectTimeoutDeliveryTrades();

    /**
     * 查询收货确认超时的交易单（WAITING_CONFIRM_RECEIPT 且当前时间已超过 receipt_deadline）
     */
    @Select("SELECT * FROM t_trade " +
            "WHERE deleted = 0 " +
            "  AND status = 'WAITING_CONFIRM_RECEIPT' " +
            "  AND receipt_deadline IS NOT NULL " +
            "  AND receipt_deadline < NOW()")
    List<Trade> selectTimeoutReceiptTrades();
}
