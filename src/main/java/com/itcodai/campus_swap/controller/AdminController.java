package com.itcodai.campus_swap.controller;

import com.itcodai.campus_swap.common.exception.BusinessException;
import com.itcodai.campus_swap.common.result.Result;
import com.itcodai.campus_swap.common.result.ResultCode;
import com.itcodai.campus_swap.dto.AdminPermissionsDTO;
import com.itcodai.campus_swap.dto.BatchStudentRecordDTO;
import com.itcodai.campus_swap.dto.ItemAuditDTO;
import com.itcodai.campus_swap.dto.PostReportReviewDTO;
import com.itcodai.campus_swap.dto.StudentRecordDTO;
import com.itcodai.campus_swap.dto.StudentVerifyReviewDTO;
import com.itcodai.campus_swap.service.AdminService;
import com.itcodai.campus_swap.service.PostReportService;
import com.itcodai.campus_swap.service.StudentService;
import com.itcodai.campus_swap.vo.AdminDetailVO;
import com.itcodai.campus_swap.vo.AdminUserVO;
import com.itcodai.campus_swap.vo.ItemVO;
import com.itcodai.campus_swap.vo.PageVO;
import com.itcodai.campus_swap.vo.PostReportVO;
import com.itcodai.campus_swap.vo.StudentRecordVO;
import com.itcodai.campus_swap.vo.StudentVerifyVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端接口（需 role >= 1）
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final StudentService studentService;
    private final PostReportService postReportService;

    // ================================================================
    //  用户管理
    // ================================================================

    /** 分页查询用户列表（需 USER_MANAGE 权限或超级管理员） */
    @GetMapping("/users")
    public Result<PageVO<AdminUserVO>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int size,
            HttpServletRequest request) {
        requirePermission(request, "USER_MANAGE");
        return Result.success(adminService.listUsers(keyword, page, size));
    }

    /** 禁用 / 启用用户（管理员只能操作普通用户，超管可操作管理员） */
    @PutMapping("/users/{id:\\d+}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id,
                                         @RequestParam int status,
                                         HttpServletRequest request) {
        requirePermission(request, "USER_MANAGE");
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

    /** 分页查询所有商品（需 ITEM_MANAGE 权限或超级管理员） */
    @GetMapping("/items")
    public Result<PageVO<ItemVO>> listAllItems(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int size,
            HttpServletRequest request) {
        requirePermission(request, "ITEM_MANAGE");
        return Result.success(adminService.listAllItems(keyword, category, status, page, size));
    }

    /** 修改商品状态（0-在售 1-已下架 2-已售出） */
    @PutMapping("/items/{id:\\d+}/status")
    public Result<Void> updateItemStatus(@PathVariable Long id,
                                         @RequestParam int status,
                                         HttpServletRequest request) {
        requirePermission(request, "ITEM_MANAGE");
        adminService.updateItemStatus(id, status);
        return Result.success();
    }

    /** 强制删除商品 */
    @DeleteMapping("/items/{id:\\d+}")
    public Result<Void> deleteItem(@PathVariable Long id,
                                   HttpServletRequest request) {
        requirePermission(request, "ITEM_MANAGE");
        adminService.forceDeleteItem(id);
        return Result.success();
    }

    // ================================================================
    //  商品审核（需 ITEM_AUDIT 权限）
    // ================================================================

    /** 分页查询待审核商品 */
    @GetMapping("/items/pending")
    public Result<PageVO<ItemVO>> listPendingItems(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int size,
            HttpServletRequest request) {
        requirePermission(request, "ITEM_AUDIT");
        return Result.success(adminService.listPendingItems(keyword, category, page, size));
    }

    /** 审核商品（通过/拒绝） */
    @PutMapping("/items/{id:\\d+}/audit")
    public Result<Void> auditItem(@PathVariable Long id,
                                  @RequestBody ItemAuditDTO dto,
                                  HttpServletRequest request) {
        requirePermission(request, "ITEM_AUDIT");
        adminService.auditItem(id, dto.getAction(), dto.getRemark());
        return Result.success();
    }

    // ================================================================
    //  管理员管理（仅超级管理员）
    // ================================================================

    /** 分页查询所有管理员（role=1），含权限列表 */
    @GetMapping("/admins")
    public Result<PageVO<AdminDetailVO>> listAdmins(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int size,
            HttpServletRequest request) {
        requireSuperAdmin(request);
        return Result.success(adminService.listAdmins(keyword, page, size));
    }

    /** 获取某管理员的权限列表（超级管理员专用） */
    @GetMapping("/admins/{id:\\d+}/permissions")
    public Result<List<String>> getAdminPermissions(@PathVariable Long id,
                                                     HttpServletRequest request) {
        requireSuperAdmin(request);
        return Result.success(adminService.getAdminPermissions(id));
    }

    /** 全量覆盖某管理员的权限（超级管理员专用） */
    @PutMapping("/admins/{id:\\d+}/permissions")
    public Result<Void> setAdminPermissions(@PathVariable Long id,
                                             @RequestBody AdminPermissionsDTO dto,
                                             HttpServletRequest request) {
        requireSuperAdmin(request);
        adminService.setAdminPermissions(id, dto.getPermissions());
        return Result.success();
    }

    /** 当前管理员查询自己的权限（普通管理员可用，超级管理员返回全部权限） */
    @GetMapping("/me/permissions")
    public Result<List<String>> getMyPermissions(HttpServletRequest request) {
        int role = (int) request.getAttribute("userRole");
        if (role >= 2) {
            // 超级管理员拥有所有权限
            return Result.success(List.of("USER_MANAGE", "ITEM_MANAGE", "ITEM_AUDIT", "STUDENT_MANAGE", "CONTENT_AUDIT"));
        }
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(adminService.getAdminPermissions(userId));
    }

    // ================================================================
    //  学生档案管理（需 STUDENT_MANAGE 权限）
    // ================================================================

    /** 分页查询学生档案 */
    @GetMapping("/students")
    public Result<PageVO<StudentRecordVO>> listStudentRecords(
            @RequestParam(required = false) String school,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int size,
            HttpServletRequest request) {
        requirePermission(request, "STUDENT_MANAGE");
        return Result.success(studentService.listRecords(school, keyword, page, size));
    }

    /** 添加单条学生档案 */
    @PostMapping("/students")
    public Result<Void> addStudentRecord(@Valid @RequestBody StudentRecordDTO dto,
                                         HttpServletRequest request) {
        requirePermission(request, "STUDENT_MANAGE");
        Long operatorId = (Long) request.getAttribute("userId");
        studentService.addRecord(dto, operatorId);
        return Result.success();
    }

    /** 批量导入学生档案 */
    @PostMapping("/students/batch")
    public Result<Integer> batchImportStudentRecords(@Valid @RequestBody BatchStudentRecordDTO dto,
                                                      HttpServletRequest request) {
        requirePermission(request, "STUDENT_MANAGE");
        Long operatorId = (Long) request.getAttribute("userId");
        int count = studentService.batchImportRecords(dto, operatorId);
        return Result.success(count);
    }

    /** 删除学生档案 */
    @DeleteMapping("/students/{id:\\d+}")
    public Result<Void> deleteStudentRecord(@PathVariable Long id,
                                             HttpServletRequest request) {
        requirePermission(request, "STUDENT_MANAGE");
        studentService.deleteRecord(id);
        return Result.success();
    }

    // ================================================================
    //  学生认证申请审核（需 STUDENT_MANAGE 权限）
    // ================================================================

    /** 分页查询认证申请列表 */
    @GetMapping("/verifications")
    public Result<PageVO<StudentVerifyVO>> listVerifications(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int size,
            HttpServletRequest request) {
        requirePermission(request, "STUDENT_MANAGE");
        return Result.success(studentService.listVerifications(status, keyword, page, size));
    }

    /** 审核认证申请 */
    @PutMapping("/verifications/{id:\\d+}/review")
    public Result<Void> reviewVerification(@PathVariable Long id,
                                            @Valid @RequestBody StudentVerifyReviewDTO dto,
                                            HttpServletRequest request) {
        requirePermission(request, "STUDENT_MANAGE");
        Long reviewerId = (Long) request.getAttribute("userId");
        studentService.reviewVerification(id, dto.getAction(), dto.getRemark(), reviewerId);
        return Result.success();
    }

    // ================================================================
    //  举报内容审核（需 CONTENT_AUDIT 权限）
    // ================================================================

    /** 分页查询举报列表 */
    @GetMapping("/reports")
    public Result<PageVO<PostReportVO>> listReports(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int size,
            HttpServletRequest request) {
        requirePermission(request, "CONTENT_AUDIT");
        return Result.success(postReportService.listReports(status, page, size));
    }

    /** 审核举报（处理/驳回） */
    @PutMapping("/reports/{id:\\d+}/review")
    public Result<Void> reviewReport(@PathVariable Long id,
                                     @Valid @RequestBody PostReportReviewDTO dto,
                                     HttpServletRequest request) {
        requirePermission(request, "CONTENT_AUDIT");
        Long reviewerId = (Long) request.getAttribute("userId");
        postReportService.reviewReport(id, dto.getAction(), dto.getRemark(), reviewerId);
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

    /**
     * 权限检查：超级管理员直接放行；普通管理员需具备指定权限码
     */
    private void requirePermission(HttpServletRequest request, String permCode) {
        int role = (int) request.getAttribute("userRole");
        if (role >= 2) return; // 超管放行
        Long userId = (Long) request.getAttribute("userId");
        List<String> perms = adminService.getAdminPermissions(userId);
        if (!perms.contains(permCode)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无操作权限，需要 " + permCode);
        }
    }
}
