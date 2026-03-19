package com.itcodai.campus_swap.controller;

import com.itcodai.campus_swap.common.result.Result;
import com.itcodai.campus_swap.dto.AnnouncementDTO;
import com.itcodai.campus_swap.entity.Announcement;
import com.itcodai.campus_swap.service.AnnouncementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公告接口
 * - 前台公开：GET /api/announcements/active
 * - 管理端：/api/admin/announcements/**（需要 role >= 1）
 */
@RestController
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    // ================================================================
    //  前台公开接口（JWT 拦截器会验证 token，但无权限要求）
    // ================================================================

    /** 获取所有上线公告（广场横幅展示用） */
    @GetMapping("/api/announcements/active")
    public Result<List<Announcement>> listActive() {
        return Result.success(announcementService.listActive());
    }

    // ================================================================
    //  管理端接口
    // ================================================================

    /** 获取全部公告列表（含下线） */
    @GetMapping("/api/admin/announcements")
    public Result<List<Announcement>> listAll(HttpServletRequest request) {
        requireAdmin(request);
        return Result.success(announcementService.listAll());
    }

    /** 新增公告 */
    @PostMapping("/api/admin/announcements")
    public Result<Void> create(@Valid @RequestBody AnnouncementDTO dto,
                               HttpServletRequest request) {
        requireAdmin(request);
        Long createdBy = (Long) request.getAttribute("userId");
        announcementService.create(dto, createdBy);
        return Result.success();
    }

    /** 更新公告 */
    @PutMapping("/api/admin/announcements/{id:\\d+}")
    public Result<Void> update(@PathVariable Long id,
                               @Valid @RequestBody AnnouncementDTO dto,
                               HttpServletRequest request) {
        requireAdmin(request);
        announcementService.update(id, dto);
        return Result.success();
    }

    /** 上线 / 下线公告 */
    @PutMapping("/api/admin/announcements/{id:\\d+}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @RequestParam int status,
                                     HttpServletRequest request) {
        requireAdmin(request);
        announcementService.updateStatus(id, status);
        return Result.success();
    }

    /** 删除公告 */
    @DeleteMapping("/api/admin/announcements/{id:\\d+}")
    public Result<Void> delete(@PathVariable Long id,
                               HttpServletRequest request) {
        requireAdmin(request);
        announcementService.delete(id);
        return Result.success();
    }

    // ================================================================
    //  私有工具
    // ================================================================

    private void requireAdmin(HttpServletRequest request) {
        int role = (int) request.getAttribute("userRole");
        if (role < 1) {
            throw new com.itcodai.campus_swap.common.exception.BusinessException(
                    com.itcodai.campus_swap.common.result.ResultCode.FORBIDDEN, "无操作权限");
        }
    }
}
