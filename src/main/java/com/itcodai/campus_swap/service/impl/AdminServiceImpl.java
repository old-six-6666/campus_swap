package com.itcodai.campus_swap.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itcodai.campus_swap.common.exception.BusinessException;
import com.itcodai.campus_swap.common.result.ResultCode;
import com.itcodai.campus_swap.entity.Item;
import com.itcodai.campus_swap.entity.User;
import com.itcodai.campus_swap.mapper.ItemMapper;
import com.itcodai.campus_swap.mapper.UserMapper;
import com.itcodai.campus_swap.service.AdminService;
import com.itcodai.campus_swap.vo.AdminUserVO;
import com.itcodai.campus_swap.vo.ItemVO;
import com.itcodai.campus_swap.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserMapper userMapper;
    private final ItemMapper itemMapper;

    // ===== 用户管理 =====

    @Override
    public PageVO<AdminUserVO> listUsers(String keyword, int page, int size) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .like(StringUtils.hasText(keyword), User::getNickname, keyword)
                .or(StringUtils.hasText(keyword), w -> w.like(User::getEmail, keyword))
                .orderByDesc(User::getCreatedAt);
        Page<User> result = userMapper.selectPage(new Page<>(page, size), wrapper);
        List<AdminUserVO> records = result.getRecords().stream()
                .map(this::toUserVO)
                .collect(Collectors.toList());
        return PageVO.of(records, result.getTotal(), page, size);
    }

    @Override
    public void updateUserStatus(Long operatorId, int operatorRole, Long targetId, int status) {
        User target = getExistingUser(targetId);
        // 管理员（role=1）只能操作普通用户（role=0）
        if (operatorRole < 2 && target.getRole() != null && target.getRole() >= 1) {
            throw new BusinessException(ResultCode.FORBIDDEN, "管理员不能操作其他管理员账号");
        }
        // 不能操作自己
        if (targetId.equals(operatorId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能修改自己的状态");
        }
        target.setStatus(status);
        userMapper.updateById(target);
    }

    @Override
    public void updateUserRole(Long targetId, int role) {
        if (role < 0 || role > 2) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "无效的角色值");
        }
        User target = getExistingUser(targetId);
        target.setRole(role);
        userMapper.updateById(target);
    }

    @Override
    public void deleteUser(Long targetId) {
        getExistingUser(targetId);
        userMapper.deleteById(targetId);
    }

    // ===== 商品管理 =====

    @Override
    public PageVO<ItemVO> listAllItems(String keyword, String category, Integer status, int page, int size) {
        LambdaQueryWrapper<Item> wrapper = new LambdaQueryWrapper<Item>()
                .like(StringUtils.hasText(keyword), Item::getTitle, keyword)
                .eq(StringUtils.hasText(category), Item::getCategory, category)
                .eq(status != null, Item::getStatus, status)
                .orderByDesc(Item::getCreatedAt);
        Page<Item> result = itemMapper.selectPage(new Page<>(page, size), wrapper);
        List<ItemVO> records = result.getRecords().stream()
                .map(this::toItemVO)
                .collect(Collectors.toList());
        return PageVO.of(records, result.getTotal(), page, size);
    }

    @Override
    public void updateItemStatus(Long itemId, int status) {
        Item item = itemMapper.selectById(itemId);
        if (item == null) throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在");
        item.setStatus(status);
        itemMapper.updateById(item);
    }

    @Override
    public void forceDeleteItem(Long itemId) {
        Item item = itemMapper.selectById(itemId);
        if (item == null) throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在");
        itemMapper.deleteById(itemId);
    }

    // ===== 私有辅助 =====

    private User getExistingUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        return user;
    }

    private AdminUserVO toUserVO(User user) {
        AdminUserVO vo = new AdminUserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }

    private ItemVO toItemVO(Item item) {
        ItemVO vo = new ItemVO();
        vo.setId(item.getId());
        vo.setTitle(item.getTitle());
        vo.setPrice(item.getPrice());
        vo.setCategory(item.getCategory());
        vo.setDescription(item.getDescription());
        vo.setCoverImage(item.getCoverImage());
        vo.setStatus(item.getStatus());
        vo.setCreatedAt(item.getCreatedAt());
        vo.setSellerId(item.getSellerId());
        if (StringUtils.hasText(item.getImages())) {
            vo.setImages(JSONUtil.toList(item.getImages(), String.class));
        } else {
            vo.setImages(Collections.emptyList());
        }
        // 查询卖家信息
        User seller = userMapper.selectById(item.getSellerId());
        if (seller != null) {
            vo.setSellerNickname(seller.getNickname());
            vo.setSellerAvatar(seller.getAvatar());
        }
        return vo;
    }
}
