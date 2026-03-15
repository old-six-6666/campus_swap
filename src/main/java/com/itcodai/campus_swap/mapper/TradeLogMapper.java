package com.itcodai.campus_swap.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itcodai.campus_swap.entity.TradeLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 交易状态变更日志 Mapper
 */
@Mapper
public interface TradeLogMapper extends BaseMapper<TradeLog> {
}
