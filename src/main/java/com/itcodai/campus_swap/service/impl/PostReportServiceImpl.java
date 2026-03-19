package com.itcodai.campus_swap.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itcodai.campus_swap.common.exception.BusinessException;
import com.itcodai.campus_swap.common.result.ResultCode;
import com.itcodai.campus_swap.entity.Post;
import com.itcodai.campus_swap.entity.PostReport;
import com.itcodai.campus_swap.entity.User;
import com.itcodai.campus_swap.mapper.PostMapper;
import com.itcodai.campus_swap.mapper.PostReportMapper;
import com.itcodai.campus_swap.mapper.UserMapper;
import com.itcodai.campus_swap.service.PostReportService;
import com.itcodai.campus_swap.vo.PageVO;
import com.itcodai.campus_swap.vo.PostReportVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostReportServiceImpl implements PostReportService {

    private final PostReportMapper postReportMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;

    private static final String[] REASON_LABELS = {
            "", "违法违规", "色情低俗", "虚假信息", "侮辱谩骂", "广告骚扰", "其他"
    };

    @Override
    public void reportPost(Long postId, Long reporterId, Integer reason, String description) {
        // 检查动态是否存在
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "动态不存在");
        }
        // 不能举报自己的动态
        if (post.getUserId().equals(reporterId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能举报自己的动态");
        }
        // 同一用户对同一动态只能举报一次（状态为待审核时）
        long existing = postReportMapper.selectCount(
                new LambdaQueryWrapper<PostReport>()
                        .eq(PostReport::getPostId, postId)
                        .eq(PostReport::getReporterId, reporterId)
                        .eq(PostReport::getStatus, 0)
        );
        if (existing > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "您已举报过该动态，请等待审核");
        }

        PostReport report = new PostReport();
        report.setPostId(postId);
        report.setReporterId(reporterId);
        report.setReason(reason);
        if (StringUtils.hasText(description)) {
            // 截断超长说明
            report.setDescription(description.length() > 200
                    ? description.substring(0, 200) : description);
        }
        report.setStatus(0);
        postReportMapper.insert(report);
    }

    @Override
    public PageVO<PostReportVO> listReports(Integer status, int page, int size) {
        LambdaQueryWrapper<PostReport> wrapper = new LambdaQueryWrapper<PostReport>()
                .eq(status != null, PostReport::getStatus, status)
                .orderByAsc(PostReport::getStatus)        // 待审核优先
                .orderByDesc(PostReport::getCreatedAt);
        Page<PostReport> result = postReportMapper.selectPage(new Page<>(page, size), wrapper);
        List<PostReportVO> records = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return PageVO.of(records, result.getTotal(), page, size);
    }

    @Override
    @Transactional
    public void reviewReport(Long reportId, Integer action, String remark, Long reviewerId) {
        if (action != 1 && action != 2) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "无效的处理操作");
        }
        PostReport report = postReportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "举报记录不存在");
        }
        if (report.getStatus() != 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该举报已处理");
        }

        report.setStatus(action);
        report.setRemark(remark);
        report.setReviewedBy(reviewerId);
        report.setReviewedAt(LocalDateTime.now());
        postReportMapper.updateById(report);

        // action=1：内容下架（逻辑删除动态）
        if (action == 1) {
            Post post = postMapper.selectById(report.getPostId());
            if (post != null) {
                postMapper.deleteById(report.getPostId());
            }
        }
    }

    // ===== 私有辅助 =====

    private PostReportVO toVO(PostReport report) {
        PostReportVO vo = new PostReportVO();
        vo.setId(report.getId());
        vo.setReporterId(report.getReporterId());
        vo.setPostId(report.getPostId());
        vo.setReason(report.getReason());
        vo.setReasonLabel(report.getReason() != null && report.getReason() >= 1
                && report.getReason() <= 6 ? REASON_LABELS[report.getReason()] : "未知");
        vo.setDescription(report.getDescription());
        vo.setStatus(report.getStatus());
        vo.setStatusLabel(statusLabel(report.getStatus()));
        vo.setRemark(report.getRemark());
        vo.setReviewedBy(report.getReviewedBy());
        vo.setReviewedAt(report.getReviewedAt());
        vo.setCreatedAt(report.getCreatedAt());

        // 举报者信息
        User reporter = userMapper.selectById(report.getReporterId());
        if (reporter != null) {
            vo.setReporterNickname(reporter.getNickname());
            vo.setReporterAvatar(reporter.getAvatar());
        }

        // 被举报动态信息（可能已被删除）
        Post post = postMapper.selectById(report.getPostId());
        if (post != null) {
            vo.setPostContent(post.getContent());
            vo.setPostUserId(post.getUserId());
            if (StringUtils.hasText(post.getImages())) {
                vo.setPostImages(JSONUtil.toList(post.getImages(), String.class));
            } else {
                vo.setPostImages(Collections.emptyList());
            }
            User postUser = userMapper.selectById(post.getUserId());
            if (postUser != null) {
                vo.setPostUserNickname(postUser.getNickname());
            }
        } else {
            vo.setPostContent("[动态已被删除]");
            vo.setPostImages(Collections.emptyList());
        }

        // 审核员信息
        if (report.getReviewedBy() != null) {
            User reviewer = userMapper.selectById(report.getReviewedBy());
            if (reviewer != null) {
                vo.setReviewerNickname(reviewer.getNickname());
            }
        }

        return vo;
    }

    private String statusLabel(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "待审核";
            case 1 -> "已处理(内容下架)";
            case 2 -> "已驳回(内容正常)";
            default -> "未知";
        };
    }
}
