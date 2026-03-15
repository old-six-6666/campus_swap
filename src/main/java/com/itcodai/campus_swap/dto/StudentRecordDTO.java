package com.itcodai.campus_swap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员添加单条学生档案
 */
@Data
public class StudentRecordDTO {

    @NotBlank(message = "学校名称不能为空")
    @Size(max = 100, message = "学校名称最长100字符")
    private String school;

    @NotBlank(message = "学号不能为空")
    @Size(max = 50, message = "学号最长50字符")
    private String studentId;

    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 50, message = "姓名最长50字符")
    private String realName;

    @Size(max = 200, message = "附加信息最长200字符")
    private String extraInfo;
}
