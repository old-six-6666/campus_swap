package com.itcodai.campus_swap.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itcodai.campus_swap.common.exception.BusinessException;
import com.itcodai.campus_swap.common.result.ResultCode;
import com.itcodai.campus_swap.dto.ItemPublishDTO;
import com.itcodai.campus_swap.entity.Item;
import com.itcodai.campus_swap.entity.User;
import com.itcodai.campus_swap.mapper.ItemMapper;
import com.itcodai.campus_swap.mapper.UserMapper;
import com.itcodai.campus_swap.service.ItemService;
import com.itcodai.campus_swap.vo.ItemVO;
import com.itcodai.campus_swap.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品服务实现
 */
@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemMapper itemMapper;
    private final UserMapper userMapper;

    @Override
    public PageVO<ItemVO> listItems(String keyword, String category, int page, int size) {
        LambdaQueryWrapper<Item> wrapper = new LambdaQueryWrapper<Item>()
                .eq(Item::getStatus, 0)
                .eq(StringUtils.hasText(category), Item::getCategory, category)
                .like(StringUtils.hasText(keyword), Item::getTitle, keyword)
                .orderByDesc(Item::getCreatedAt);

        Page<Item> pageResult = itemMapper.selectPage(new Page<>(page, size), wrapper);

        List<ItemVO> records = pageResult.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        return PageVO.of(records, pageResult.getTotal(), page, size);
    }

    @Override
    public ItemVO getItemById(Long id) {
        Item item = itemMapper.selectById(id);
        if (item == null || item.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在");
        }
        return toVO(item);
    }

    @Override
    public Long publishItem(Long sellerId, ItemPublishDTO dto) {
        Item item = new Item();
        BeanUtils.copyProperties(dto, item);
        item.setSellerId(sellerId);
        item.setStatus(0);
        itemMapper.insert(item);
        return item.getId();
    }

    @Override
    public void updateItem(Long sellerId, Long itemId, ItemPublishDTO dto) {
        Item item = itemMapper.selectById(itemId);
        checkOwnership(item, sellerId, itemId);
        BeanUtils.copyProperties(dto, item);
        itemMapper.updateById(item);
    }

    @Override
    public void deleteItem(Long sellerId, Long itemId) {
        Item item = itemMapper.selectById(itemId);
        checkOwnership(item, sellerId, itemId);
        itemMapper.deleteById(itemId);
    }

    // ---- 私有辅助方法 ----

    private void checkOwnership(Item item, Long sellerId, Long itemId) {
        if (item == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在");
        }
        if (!item.getSellerId().equals(sellerId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作他人的商品");
        }
    }

    private ItemVO toVO(Item item) {
        ItemVO vo = new ItemVO();
        BeanUtils.copyProperties(item, vo);
        User seller = userMapper.selectById(item.getSellerId());
        if (seller != null) {
            vo.setSellerNickname(seller.getNickname());
            vo.setSellerAvatar(seller.getAvatar());
        }
        return vo;
    }
}
