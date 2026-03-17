package com.itcodai.campus_swap.service;

import com.itcodai.campus_swap.dto.ItemPublishDTO;
import com.itcodai.campus_swap.vo.ItemVO;
import com.itcodai.campus_swap.vo.PageVO;

/**
 * 商品服务接口
 */
public interface ItemService {

    /** 分页查询商品列表（支持关键词搜索、分类过滤） */
    PageVO<ItemVO> listItems(String keyword, String category, int page, int size);

    /** 根据 ID 查询商品详情 */
    ItemVO getItemById(Long id);

    /** 发布商品 */
    Long publishItem(Long sellerId, ItemPublishDTO dto);

    /** 更新商品信息（仅本人） */
    void updateItem(Long sellerId, Long itemId, ItemPublishDTO dto);

    /** 删除商品（仅本人，逻辑删除） */
    void deleteItem(Long sellerId, Long itemId);

    /** 查询当前用户发布的商品列表 */
    PageVO<ItemVO> getMyItems(Long sellerId, int page, int size);

    /** 查询指定用户已审核通过且在售的商品列表（公开展示） */
    PageVO<ItemVO> getUserItems(Long sellerId, int page, int size);
}
