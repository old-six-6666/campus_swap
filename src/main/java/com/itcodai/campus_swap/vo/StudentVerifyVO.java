package com.itcodai.campus_swap.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生认证申请 VO
 */
@Data
public class StudentVerifyVO {
    private Long id;
    private Long userId;
    /** 申请用户昵称 */
    private String userNickname;
    /** 申请用户邮箱 */
    private String userEmail;
    private String school;
    private String studentId;
    private String realName;
    private String extraInfo;
    /** 状态：0-待审核  1-已通过  2-已拒绝 */
    private Integer status;
    private String remark;
    private Long reviewedBy;
    /** 审核管理员昵称 */
    private String reviewedByNickname;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}
