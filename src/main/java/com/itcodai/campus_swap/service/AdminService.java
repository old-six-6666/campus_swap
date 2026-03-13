package com.itcodai.campus_swap.service;

import com.itcodai.campus_swap.vo.AdminDetailVO;
import com.itcodai.campus_swap.vo.AdminUserVO;
import com.itcodai.campus_swap.vo.ItemVO;
import com.itcodai.campus_swap.vo.PageVO;

import java.util.List;

/**
 * 管理端服务接口
 */
public interface AdminService {

    // ===== 用户管理 =====

    /** 分页查询用户列表（支持关键词搜索） */
    PageVO<AdminUserVO> listUsers(String keyword, int page, int size);

    /**
     * 更新用户账号状态（禁用/启用）
     * 管理员只能操作普通用户；超级管理员可操作任意用户
     */
    void updateUserStatus(Long operatorId, int operatorRole, Long targetId, int status);

    /**
     * 更新用户角色（仅超级管理员）
     */
    void updateUserRole(Long targetId, int role);

    /**
     * 删除用户（仅超级管理员，逻辑删除）
     */
    void deleteUser(Long targetId);

    // ===== 商品管理 =====

    /** 分页查询所有商品（支持关键词、分类、状态过滤） */
    PageVO<ItemVO> listAllItems(String keyword, String category, Integer status, int page, int size);

    /**
     * 更新商品状态（0-在售 1-已下架 2-已售出）
     */
    void updateItemStatus(Long itemId, int status);

    /**
     * 强制删除商品（逻辑删除）
     */
    void forceDeleteItem(Long itemId);

    // ===== 商品审核（需 ITEM_AUDIT 权限） =====

    /** 分页查询待审核商品列表 */
    PageVO<ItemVO> listPendingItems(String keyword, String category, int page, int size);

    /**
     * 审核商品：action=1 通过，action=2 拒绝（拒绝时需填 remark）
     */
    void auditItem(Long itemId, int action, String remark);

    // ===== 管理员权限管理（仅超级管理员） =====

    /** 分页查询所有管理员（role=1），含各自权限列表 */
    PageVO<AdminDetailVO> listAdmins(String keyword, int page, int size);

    /** 获取某管理员的权限码列表 */
    List<String> getAdminPermissions(Long adminId);

    /**
     * 全量覆盖某管理员的权限（传空列表=清空）
     * 同时校验 adminId 必须是 role=1 的用户
     */
    void setAdminPermissions(Long adminId, List<String> permissions);
}
