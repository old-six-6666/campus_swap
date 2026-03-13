package com.itcodai.campus_swap.controller;

import com.itcodai.campus_swap.common.exception.BusinessException;
import com.itcodai.campus_swap.common.result.Result;
import com.itcodai.campus_swap.common.result.ResultCode;
import com.itcodai.campus_swap.service.AdminService;
import com.itcodai.campus_swap.vo.AdminUserVO;
import com.itcodai.campus_swap.vo.ItemVO;
import com.itcodai.campus_swap.vo.PageVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端接口（需 role >= 1）
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // ================================================================
    //  用户管理
    // ================================================================

    /** 分页查询用户列表 */
    @GetMapping("/users")
    public Result<PageVO<AdminUserVO>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int size) {
        return Result.success(adminService.listUsers(keyword, page, size));
    }

    /** 禁用 / 启用用户（管理员只能操作普通用户，超管可操作管理员） */
    @PutMapping("/users/{id:\\d+}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id,
                                         @RequestParam int status,
                                         HttpServletRequest request) {
        Long operatorId = (Long) request.getAttribute("userId");
        int operatorRole = (int) request.getAttribute("userRole");
        adminService.updateUserStatus(operatorId, operatorRole, id, status);
        return Result.success();
    }

    /** 修改用户角色（仅超级管理员） */
    @PutMapping("/users/{id:\\d+}/role")
    public Result<Void> updateUserRole(@PathVariable Long id,
                                       @RequestParam int role,
                                       HttpServletRequest request) {
        requireSuperAdmin(request);
        adminService.updateUserRole(id, role);
        return Result.success();
    }

    /** 删除用户（仅超级管理员） */
    @DeleteMapping("/users/{id:\\d+}")
    public Result<Void> deleteUser(@PathVariable Long id,
                                   HttpServletRequest request) {
        requireSuperAdmin(request);
        adminService.deleteUser(id);
        return Result.success();
    }

    // ================================================================
    //  商品管理
    // ================================================================

    /** 分页查询所有商品 */
    @GetMapping("/items")
    public Result<PageVO<ItemVO>> listAllItems(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int size) {
        return Result.success(adminService.listAllItems(keyword, category, status, page, size));
    }

    /** 修改商品状态（0-在售 1-已下架 2-已售出） */
    @PutMapping("/items/{id:\\d+}/status")
    public Result<Void> updateItemStatus(@PathVariable Long id,
                                         @RequestParam int status) {
        adminService.updateItemStatus(id, status);
        return Result.success();
    }

    /** 强制删除商品 */
    @DeleteMapping("/items/{id:\\d+}")
    public Result<Void> deleteItem(@PathVariable Long id) {
        adminService.forceDeleteItem(id);
        return Result.success();
    }

    // ================================================================
    //  私有工具
    // ================================================================

    private void requireSuperAdmin(HttpServletRequest request) {
        int role = (int) request.getAttribute("userRole");
        if (role < 2) {
            throw new BusinessException(ResultCode.FORBIDDEN, "该操作仅超级管理员可用");
        }
    }
}
