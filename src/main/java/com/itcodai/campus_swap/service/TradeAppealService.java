package com.itcodai.campus_swap.service;

import com.itcodai.campus_swap.vo.PageVO;
import com.itcodai.campus_swap.vo.TradeAppealVO;

import java.util.List;

/**
 * 交易申诉服务接口
 */
public interface TradeAppealService {

    /** 提交申诉（申诉人必须是交易的甲方或乙方） */
    void submitAppeal(Long tradeId, Long userId, String content);

    /** 查询某交易中当前用户自己的申诉列表 */
    List<TradeAppealVO> getMyAppealsByTrade(Long tradeId, Long userId);

    /** 管理员分页查询所有申诉 */
    PageVO<TradeAppealVO> listAppeals(Integer status, int page, int size);

    /** 管理员处理申诉（action=1 已处理，action=2 已驳回） */
    void reviewAppeal(Long appealId, int action, String remark, Long reviewerId);
}
