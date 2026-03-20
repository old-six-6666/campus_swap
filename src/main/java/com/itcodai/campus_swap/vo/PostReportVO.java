package com.itcodai.campus_swap.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 举报记录 VO（管理端使用）
 */
@Data
public class PostReportVO {

    private Long id;

    /** 举报者 */
    private Long reporterId;
    private String reporterNickname;
    private String reporterAvatar;

    /** 被举报动态 */
    private Long postId;
    private String postContent;
    private List<String> postImages;
    private Long postUserId;
    private String postUserNickname;

    /**
     * 举报原因：
     * 1-违法违规 2-色情低俗 3-虚假信息 4-侮辱谩骂 5-广告骚扰 6-其他
     */
    private Integer reason;
    private String reasonLabel;

    /** 补充说明 */
    private String description;

    /** 0-待审核 1-已处理(内容下架) 2-已驳回(内容正常) */
    private Integer status;
    private String statusLabel;

    /** 审核信息 */
    private String remark;
    private Long reviewedBy;
    private String reviewerNickname;
    private LocalDateTime reviewedAt;

    private LocalDateTime createdAt;
}
