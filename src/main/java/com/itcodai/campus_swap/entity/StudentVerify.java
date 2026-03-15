package com.itcodai.campus_swap.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户学生认证申请
 * status: 0-待审核  1-已通过  2-已拒绝
 */
@Data
@TableName("t_student_verify")
public class StudentVerify {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申请认证的用户 ID */
    private Long userId;

    /** 填写的学校名称 */
    private String school;

    /** 填写的学号 */
    private String studentId;

    /** 填写的真实姓名 */
    private String realName;

    /** 补充说明 */
    private String extraInfo;

    /**
     * 认证状态：0-待审核  1-已通过  2-已拒绝
     */
    private Integer status;

    /** 审核备注（拒绝原因等） */
    private String remark;

    /** 审核管理员 ID */
    private Long reviewedBy;

    /** 审核时间 */
    private LocalDateTime reviewedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
