package com.itcodai.campus_swap.controller;

import com.itcodai.campus_swap.common.result.Result;
import com.itcodai.campus_swap.dto.ItemPublishDTO;
import com.itcodai.campus_swap.service.ItemService;
import com.itcodai.campus_swap.vo.ItemVO;
import com.itcodai.campus_swap.vo.PageVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 商品相关接口
 */
@RestController
@RequestMapping("/api/item")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    /** GET /api/item/list — 分页查询商品列表（公开） */
    @GetMapping("/list")
    public Result<PageVO<ItemVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return Result.success(itemService.listItems(keyword, category, page, size));
    }

    /** GET /api/item/{id} — 获取商品详情（公开），仅匹配纯数字 ID */
    @GetMapping("/{id:\\d+}")
    public Result<ItemVO> getDetail(@PathVariable Long id) {
        return Result.success(itemService.getItemById(id));
    }

    /** GET /api/item/my — 查询当前用户的商品列表（需登录） */
    @GetMapping("/my")
    public Result<PageVO<ItemVO>> myItems(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(itemService.getMyItems(userId, page, size));
    }

    /** POST /api/item/publish — 发布商品（需登录） */
    @PostMapping("/publish")
    public Result<Long> publish(@Valid @RequestBody ItemPublishDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(itemService.publishItem(userId, dto));
    }

    /** PUT /api/item/{id} — 更新商品（需登录，仅本人） */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody ItemPublishDTO dto,
                               HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        itemService.updateItem(userId, id, dto);
        return Result.success();
    }

    /** DELETE /api/item/{id} — 删除商品（需登录，仅本人） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        itemService.deleteItem(userId, id);
        return Result.success();
    }

    /** GET /api/item/user/{userId} — 查询指定用户已审核通过且在售的商品列表（公开） */
    @GetMapping("/user/{userId:\\d+}")
    public Result<PageVO<ItemVO>> userItems(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        return Result.success(itemService.getUserItems(userId, page, size));
    }
}
