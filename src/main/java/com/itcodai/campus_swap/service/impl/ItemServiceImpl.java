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
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
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
                .eq(Item::getAuditStatus, 1)   // 只展示已通过审核的商品
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
        item.setTitle(dto.getTitle());
        item.setPrice(dto.getPrice());
        item.setCategory(dto.getCategory());
        item.setDescription(dto.getDescription());
        item.setSellerId(sellerId);
        item.setStatus(0);
        item.setAuditStatus(0);   // 新发布商品默认待审核
        List<String> imgs = dto.getImages();
        if (imgs != null && !imgs.isEmpty()) {
            item.setCoverImage(imgs.get(0));
            item.setImages(JSONUtil.toJsonStr(imgs));
        }
        itemMapper.insert(item);
        return item.getId();
    }

    @Override
    public void updateItem(Long sellerId, Long itemId, ItemPublishDTO dto) {
        Item item = itemMapper.selectById(itemId);
        checkOwnership(item, sellerId, itemId);
        item.setTitle(dto.getTitle());
        item.setPrice(dto.getPrice());
        item.setCategory(dto.getCategory());
        item.setDescription(dto.getDescription());
        // 编辑后重置为待审核
        item.setAuditStatus(0);
        item.setAuditRemark(null);
        List<String> imgs = dto.getImages();
        if (imgs != null && !imgs.isEmpty()) {
            item.setCoverImage(imgs.get(0));
            item.setImages(JSONUtil.toJsonStr(imgs));
        } else {
            item.setCoverImage(null);
            item.setImages(null);
        }
        itemMapper.updateById(item);
    }

    @Override
    public void deleteItem(Long sellerId, Long itemId) {
        Item item = itemMapper.selectById(itemId);
        checkOwnership(item, sellerId, itemId);
        itemMapper.deleteById(itemId);
    }

    @Override
    public PageVO<ItemVO> getMyItems(Long sellerId, int page, int size) {
        LambdaQueryWrapper<Item> wrapper = new LambdaQueryWrapper<Item>()
                .eq(Item::getSellerId, sellerId)
                .orderByDesc(Item::getCreatedAt);
        Page<Item> pageResult = itemMapper.selectPage(new Page<>(page, size), wrapper);
        List<ItemVO> records = pageResult.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return PageVO.of(records, pageResult.getTotal(), page, size);
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
        vo.setId(item.getId());
        vo.setTitle(item.getTitle());
        vo.setPrice(item.getPrice());
        vo.setCategory(item.getCategory());
        vo.setDescription(item.getDescription());
        vo.setCoverImage(item.getCoverImage());
        vo.setStatus(item.getStatus());
        vo.setAuditStatus(item.getAuditStatus());
        vo.setAuditRemark(item.getAuditRemark());
        vo.setCreatedAt(item.getCreatedAt());
        vo.setSellerId(item.getSellerId());
        if (StringUtils.hasText(item.getImages())) {
            vo.setImages(JSONUtil.toList(item.getImages(), String.class));
        } else {
            vo.setImages(Collections.emptyList());
        }
        User seller = userMapper.selectById(item.getSellerId());
        if (seller != null) {
            vo.setSellerNickname(seller.getNickname());
            vo.setSellerAvatar(seller.getAvatar());
        }
        return vo;
    }
}
