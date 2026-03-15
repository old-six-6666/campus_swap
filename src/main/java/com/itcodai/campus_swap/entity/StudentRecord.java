package com.itcodai.campus_swap.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生信息库（由管理员维护）
 */
@Data
@TableName("t_student_record")
public class StudentRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学校名称 */
    private String school;

    /** 学号（同一学校内唯一） */
    private String studentId;

    /** 真实姓名 */
    private String realName;

    /** 附加信息（如专业、年级等） */
    private String extraInfo;

    /** 录入管理员 ID */
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
