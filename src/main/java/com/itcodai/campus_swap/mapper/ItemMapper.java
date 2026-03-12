package com.itcodai.campus_swap.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itcodai.campus_swap.entity.Item;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品 Mapper
 */
@Mapper
public interface ItemMapper extends BaseMapper<Item> {
}
