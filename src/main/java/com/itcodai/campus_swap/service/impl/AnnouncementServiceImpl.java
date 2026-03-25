package com.itcodai.campus_swap.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itcodai.campus_swap.common.exception.BusinessException;
import com.itcodai.campus_swap.common.result.ResultCode;
import com.itcodai.campus_swap.dto.AnnouncementDTO;
import com.itcodai.campus_swap.entity.Announcement;
import com.itcodai.campus_swap.mapper.AnnouncementMapper;
import com.itcodai.campus_swap.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementMapper announcementMapper;

    @Override
    public List<Announcement> listActive() {
        LocalDateTime now = LocalDateTime.now();
        return announcementMapper.selectList(
                new LambdaQueryWrapper<Announcement>()
                        .eq(Announcement::getStatus, 1)
                        .and(w -> w.isNull(Announcement::getStartTime)
                                   .or().le(Announcement::getStartTime, now))
                        .and(w -> w.isNull(Announcement::getEndTime)
                                   .or().gt(Announcement::getEndTime, now))
                        .orderByDesc(Announcement::getSort)
                        .orderByDesc(Announcement::getCreatedAt)
        );
    }

    @Override
    public List<Announcement> listAll() {
        return announcementMapper.selectList(
                new LambdaQueryWrapper<Announcement>()
                        .orderByDesc(Announcement::getSort)
                        .orderByDesc(Announcement::getCreatedAt)
        );
    }

    @Override
    public void create(AnnouncementDTO dto, Long createdBy) {
        Announcement ann = new Announcement();
        ann.setTitle(dto.getTitle());
        ann.setContent(dto.getContent());
        ann.setType(dto.getType());
        ann.setSort(dto.getSort() != null ? dto.getSort() : 0);
        ann.setStatus(1);
        ann.setCreatedBy(createdBy);
        ann.setStartTime(dto.getStartTime());
        ann.setEndTime(dto.getEndTime());
        announcementMapper.insert(ann);
    }

    @Override
    public void update(Long id, AnnouncementDTO dto) {
        Announcement ann = announcementMapper.selectById(id);
        if (ann == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        ann.setTitle(dto.getTitle());
        ann.setContent(dto.getContent());
        ann.setType(dto.getType());
        if (dto.getSort() != null) {
            ann.setSort(dto.getSort());
        }
        ann.setStartTime(dto.getStartTime());
        ann.setEndTime(dto.getEndTime());
        announcementMapper.updateById(ann);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        Announcement ann = announcementMapper.selectById(id);
        if (ann == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        ann.setStatus(status);
        announcementMapper.updateById(ann);
    }

    @Override
    public void delete(Long id) {
        if (announcementMapper.selectById(id) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "公告不存在");
        }
        announcementMapper.deleteById(id);
    }
}
