package com.itcodai.campus_swap.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itcodai.campus_swap.common.exception.BusinessException;
import com.itcodai.campus_swap.common.result.ResultCode;
import com.itcodai.campus_swap.entity.AdminPermission;
import com.itcodai.campus_swap.entity.Item;
import com.itcodai.campus_swap.entity.User;
import com.itcodai.campus_swap.mapper.AdminPermissionMapper;
import com.itcodai.campus_swap.mapper.ItemMapper;
import com.itcodai.campus_swap.mapper.UserMapper;
import com.itcodai.campus_swap.service.AdminService;
import com.itcodai.campus_swap.vo.AdminDetailVO;
import com.itcodai.campus_swap.vo.AdminUserVO;
import com.itcodai.campus_swap.vo.ItemVO;
import com.itcodai.campus_swap.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserMapper userMapper;
    private final ItemMapper itemMapper;
    private final AdminPermissionMapper adminPermissionMapper;

    /** 合法权限码集合 */
    private static final Set<String> VALID_PERM_CODES = Set.of("USER_MANAGE", "ITEM_MANAGE", "ITEM_AUDIT");

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
        // 超级管理员唯一，不允许通过此接口将任何用户提升为超级管理员
        if (role < 0 || role > 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只能设置为普通用户(0)或管理员(1)");
        }
        User target = getExistingUser(targetId);
        // 不能修改超级管理员的角色
        if (target.getRole() != null && target.getRole() >= 2) {
            throw new BusinessException(ResultCode.FORBIDDEN, "不能修改超级管理员的角色");
        }
        target.setRole(role);
        userMapper.updateById(target);
    }

    @Override
    public void deleteUser(Long targetId) {
        User target = getExistingUser(targetId);
        if (target.getRole() != null && target.getRole() >= 2) {
            throw new BusinessException(ResultCode.FORBIDDEN, "不能删除超级管理员账号");
        }
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

    // ===== 管理员权限管理 =====

    @Override
    public PageVO<AdminDetailVO> listAdmins(String keyword, int page, int size) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getRole, 1)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(User::getNickname, keyword)
                        .or()
                        .like(User::getEmail, keyword))
                .orderByDesc(User::getCreatedAt);
        Page<User> result = userMapper.selectPage(new Page<>(page, size), wrapper);
        List<AdminDetailVO> records = result.getRecords().stream()
                .map(user -> {
                    AdminDetailVO vo = new AdminDetailVO();
                    BeanUtils.copyProperties(user, vo);
                    vo.setPermissions(getAdminPermissions(user.getId()));
                    return vo;
                })
                .collect(Collectors.toList());
        return PageVO.of(records, result.getTotal(), page, size);
    }

    @Override
    public List<String> getAdminPermissions(Long adminId) {
        return adminPermissionMapper.selectList(
                new LambdaQueryWrapper<AdminPermission>()
                        .eq(AdminPermission::getAdminId, adminId)
        ).stream().map(AdminPermission::getPermCode).collect(Collectors.toList());
    }

    @Override
    public void setAdminPermissions(Long adminId, List<String> permissions) {
        // 校验目标用户必须是管理员（role=1）
        User target = getExistingUser(adminId);
        if (target.getRole() == null || target.getRole() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "目标用户不是管理员");
        }
        // 校验权限码合法性
        if (permissions != null) {
            for (String code : permissions) {
                if (!VALID_PERM_CODES.contains(code)) {
                    throw new BusinessException(ResultCode.BAD_REQUEST, "无效的权限码：" + code);
                }
            }
        }
        // 全量覆盖：先删后插
        adminPermissionMapper.delete(
                new LambdaQueryWrapper<AdminPermission>()
                        .eq(AdminPermission::getAdminId, adminId)
        );
        if (permissions != null && !permissions.isEmpty()) {
            permissions.stream().distinct().forEach(code -> {
                AdminPermission ap = new AdminPermission();
                ap.setAdminId(adminId);
                ap.setPermCode(code);
                adminPermissionMapper.insert(ap);
            });
        }
    }

    // ===== 商品审核 =====

    @Override
    public PageVO<ItemVO> listPendingItems(String keyword, String category, int page, int size) {
        LambdaQueryWrapper<Item> wrapper = new LambdaQueryWrapper<Item>()
                .eq(Item::getAuditStatus, 0)
                .like(StringUtils.hasText(keyword), Item::getTitle, keyword)
                .eq(StringUtils.hasText(category), Item::getCategory, category)
                .orderByAsc(Item::getCreatedAt);
        Page<Item> result = itemMapper.selectPage(new Page<>(page, size), wrapper);
        List<ItemVO> records = result.getRecords().stream()
                .map(this::toItemVO)
                .collect(Collectors.toList());
        return PageVO.of(records, result.getTotal(), page, size);
    }

    @Override
    public void auditItem(Long itemId, int action, String remark) {
        if (action != 1 && action != 2) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "无效的审核操作");
        }
        if (action == 2 && !StringUtils.hasText(remark)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "拒绝时必须填写原因");
        }
        Item item = itemMapper.selectById(itemId);
        if (item == null) throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在");
        item.setAuditStatus(action);
        item.setAuditRemark(action == 2 ? remark : null);
        itemMapper.updateById(item);
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
        vo.setAuditStatus(item.getAuditStatus());
        vo.setAuditRemark(item.getAuditRemark());
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
