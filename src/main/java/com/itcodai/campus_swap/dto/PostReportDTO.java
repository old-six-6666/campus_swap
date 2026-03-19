package com.itcodai.campus_swap.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 举报动态 DTO
 */
@Data
public class PostReportDTO {

    @NotNull(message = "举报原因不能为空")
    @Min(value = 1, message = "举报原因无效")
    @Max(value = 6, message = "举报原因无效")
    private Integer reason;

    /** 补充说明（选填，最多200字） */
    private String description;
}
