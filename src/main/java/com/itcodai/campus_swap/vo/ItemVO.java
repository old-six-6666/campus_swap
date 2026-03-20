package com.itcodai.campus_swap.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品信息 VO
 */
@Data
public class ItemVO {
    private Long id;
    private String title;
    private BigDecimal price;
    private String category;
    private String description;
    private String coverImage;
    private List<String> images;
    /** 标签列表 */
    private List<String> tags;
    private Long sellerId;
    private String sellerNickname;
    private String sellerAvatar;
    private Integer status;
    /** 审核状态：0-待审核 1-已通过 2-已拒绝 */
    private Integer auditStatus;
    /** 拒绝原因 */
    private String auditRemark;
    private LocalDateTime createdAt;
}
