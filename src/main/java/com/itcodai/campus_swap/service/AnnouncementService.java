package com.itcodai.campus_swap.service;

import com.itcodai.campus_swap.dto.AnnouncementDTO;
import com.itcodai.campus_swap.entity.Announcement;

import java.util.List;

/**
 * 公告 Service
 */
public interface AnnouncementService {

    /** 获取所有上线中的公告（前台展示用，按 sort 降序） */
    List<Announcement> listActive();

    /** 获取所有公告（管理端分页） */
    List<Announcement> listAll();

    /** 新增公告 */
    void create(AnnouncementDTO dto, Long createdBy);

    /** 更新公告 */
    void update(Long id, AnnouncementDTO dto);

    /** 上下线切换 */
    void updateStatus(Long id, Integer status);

    /** 删除公告 */
    void delete(Long id);
}
