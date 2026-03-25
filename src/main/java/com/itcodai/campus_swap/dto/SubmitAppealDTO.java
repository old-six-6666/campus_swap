package com.itcodai.campus_swap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SubmitAppealDTO {

    @NotBlank(message = "申诉内容不能为空")
    @Size(max = 500, message = "申诉内容不超过500字")
    private String content;
}
