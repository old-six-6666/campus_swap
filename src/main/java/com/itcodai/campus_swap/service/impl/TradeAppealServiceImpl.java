package com.itcodai.campus_swap.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itcodai.campus_swap.common.exception.BusinessException;
import com.itcodai.campus_swap.common.result.ResultCode;
import com.itcodai.campus_swap.entity.Trade;
import com.itcodai.campus_swap.entity.TradeAppeal;
import com.itcodai.campus_swap.entity.User;
import com.itcodai.campus_swap.mapper.TradeAppealMapper;
import com.itcodai.campus_swap.mapper.TradeMapper;
import com.itcodai.campus_swap.mapper.UserMapper;
import com.itcodai.campus_swap.service.TradeAppealService;
import com.itcodai.campus_swap.vo.PageVO;
import com.itcodai.campus_swap.vo.TradeAppealVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TradeAppealServiceImpl implements TradeAppealService {

    private final TradeAppealMapper tradeAppealMapper;
    private final TradeMapper tradeMapper;
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
        appeal.setStatus(action);
        appeal.setRemark(remark);
        appeal.setReviewedBy(reviewerId);
        appeal.setReviewedAt(LocalDateTime.now());
        tradeAppealMapper.updateById(appeal);
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
