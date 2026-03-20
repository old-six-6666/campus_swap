package com.itcodai.campus_swap.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 创建/更新公告 DTO
 */
@Data
public class AnnouncementDTO {

    @NotBlank(message = "标题不能为空")
    @Length(max = 100, message = "标题最多100字")
    private String title;

    @NotBlank(message = "内容不能为空")
    @Length(max = 2000, message = "内容最多2000字")
    private String content;

    @NotNull(message = "类型不能为空")
    @Min(value = 1, message = "类型无效")
    @Max(value = 3, message = "类型无效")
    private Integer type;

    /** 排序权重，越大越靠前 */
    private Integer sort;
}
