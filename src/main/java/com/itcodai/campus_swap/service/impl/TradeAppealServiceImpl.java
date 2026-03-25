package com.itcodai.campus_swap.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itcodai.campus_swap.common.exception.BusinessException;
import com.itcodai.campus_swap.common.result.ResultCode;
import com.itcodai.campus_swap.entity.Item;
import com.itcodai.campus_swap.entity.Trade;
import com.itcodai.campus_swap.entity.TradeAppeal;
import com.itcodai.campus_swap.entity.TradeLog;
import com.itcodai.campus_swap.entity.User;
import com.itcodai.campus_swap.enums.TradeStatus;
import com.itcodai.campus_swap.mapper.ItemMapper;
import com.itcodai.campus_swap.mapper.TradeAppealMapper;
import com.itcodai.campus_swap.mapper.TradeLogMapper;
import com.itcodai.campus_swap.mapper.TradeMapper;
import com.itcodai.campus_swap.mapper.UserMapper;
import com.itcodai.campus_swap.service.TradeAppealService;
import com.itcodai.campus_swap.vo.PageVO;
import com.itcodai.campus_swap.vo.TradeAppealVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TradeAppealServiceImpl implements TradeAppealService {

    private final TradeAppealMapper tradeAppealMapper;
    private final TradeMapper tradeMapper;
    private final ItemMapper itemMapper;
    private final TradeLogMapper tradeLogMapper;
    private final UserMapper userMapper;

    private static final String[] STATUS_DESCS = {"待处理", "已处理", "已驳回"};

    @Override
    public void submitAppeal(Long tradeId, Long userId, String content) {
        Trade trade = tradeMapper.selectById(tradeId);
        if (trade == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "交易不存在");
        }
        boolean isParty = userId.equals(trade.getInitiatorId()) || userId.equals(trade.getReceiverId());
        if (!isParty) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有交易参与方才能提交申诉");
        }
        TradeAppeal appeal = new TradeAppeal();
        appeal.setTradeId(tradeId);
        appeal.setAppellantId(userId);
        appeal.setContent(content);
        appeal.setStatus(0);
        tradeAppealMapper.insert(appeal);
    }

    @Override
    public List<TradeAppealVO> getMyAppealsByTrade(Long tradeId, Long userId) {
        Trade trade = tradeMapper.selectById(tradeId);
        if (trade == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "交易不存在");
        }
        boolean isParty = userId.equals(trade.getInitiatorId()) || userId.equals(trade.getReceiverId());
        if (!isParty) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权查看");
        }
        List<TradeAppeal> appeals = tradeAppealMapper.selectList(
                new LambdaQueryWrapper<TradeAppeal>()
                        .eq(TradeAppeal::getTradeId, tradeId)
                        .eq(TradeAppeal::getAppellantId, userId)
                        .orderByDesc(TradeAppeal::getCreatedAt)
        );
        return appeals.stream().map(a -> toVO(a, trade)).collect(Collectors.toList());
    }

    @Override
    public PageVO<TradeAppealVO> listAppeals(Integer status, int page, int size) {
        LambdaQueryWrapper<TradeAppeal> wrapper = new LambdaQueryWrapper<TradeAppeal>()
                .eq(status != null, TradeAppeal::getStatus, status)
                .orderByDesc(TradeAppeal::getCreatedAt);
        Page<TradeAppeal> result = tradeAppealMapper.selectPage(new Page<>(page, size), wrapper);
        List<TradeAppealVO> records = result.getRecords().stream()
                .map(a -> {
                    Trade trade = tradeMapper.selectById(a.getTradeId());
                    return toVO(a, trade);
                })
                .collect(Collectors.toList());
        return PageVO.of(records, result.getTotal(), page, size);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewAppeal(Long appealId, int action, String remark, Long reviewerId) {
        if (action != 1 && action != 2) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "无效的处理动作");
        }
        TradeAppeal appeal = tradeAppealMapper.selectById(appealId);
        if (appeal == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "申诉记录不存在");
        }
        if (appeal.getStatus() != 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "申诉已处理，无法重复操作");
        }

        // 更新申诉状态
        appeal.setStatus(action);
        appeal.setRemark(remark);
        appeal.setReviewedBy(reviewerId);
        appeal.setReviewedAt(LocalDateTime.now());
        tradeAppealMapper.updateById(appeal);

        // action=1（已处理）：终止交易，双方商品重新上架
        if (action == 1) {
            Trade trade = tradeMapper.selectById(appeal.getTradeId());
            if (trade == null) return;

            // 已是终态则跳过
            TradeStatus current = TradeStatus.fromCode(trade.getStatus());
            if (current != null && current.isTerminal()) return;

            String fromStatus = trade.getStatus();
            trade.setStatus(TradeStatus.TERMINATED.getCode());
            trade.setTerminateReason("管理员处理申诉后终止：" + (remark != null ? remark : ""));
            trade.setTerminatedAt(LocalDateTime.now());
            tradeMapper.updateById(trade);

            // 解锁双方物品，恢复在售
            unlockItem(trade.getInitiatorItemId());
            unlockItem(trade.getReceiverItemId());

            // 写操作日志
            TradeLog log = new TradeLog();
            log.setTradeId(trade.getId());
            log.setFromStatus(fromStatus);
            log.setToStatus(TradeStatus.TERMINATED.getCode());
            log.setTriggerType(0); // 手动
            log.setOperatorId(reviewerId);
            log.setRemark("管理员处理申诉，交易终止，商品已重新上架");
            tradeLogMapper.insert(log);
        }
        // action=2（已驳回）：交易不变，无需额外操作
    }

    private void unlockItem(Long itemId) {
        if (itemId == null) return;
        Item current = itemMapper.selectById(itemId);
        if (current != null && current.getStatus() == 3) {
            Item upd = new Item();
            upd.setId(itemId);
            upd.setStatus(0);
            itemMapper.updateById(upd);
        }
    }

    private TradeAppealVO toVO(TradeAppeal appeal, Trade trade) {
        TradeAppealVO vo = new TradeAppealVO();
        vo.setId(appeal.getId());
        vo.setTradeId(appeal.getTradeId());
        vo.setTradeNo(trade != null ? trade.getTradeNo() : null);
        vo.setAppellantId(appeal.getAppellantId());
        vo.setContent(appeal.getContent());
        vo.setStatus(appeal.getStatus());
        int s = appeal.getStatus() == null ? 0 : appeal.getStatus();
        vo.setStatusDesc(s < STATUS_DESCS.length ? STATUS_DESCS[s] : String.valueOf(s));
        vo.setRemark(appeal.getRemark());
        vo.setReviewedBy(appeal.getReviewedBy());
        vo.setReviewedAt(appeal.getReviewedAt());
        vo.setCreatedAt(appeal.getCreatedAt());

        User appellant = userMapper.selectById(appeal.getAppellantId());
        if (appellant != null) {
            vo.setAppellantNickname(appellant.getNickname());
            vo.setAppellantAvatar(appellant.getAvatar());
        }
        return vo;
    }
}

