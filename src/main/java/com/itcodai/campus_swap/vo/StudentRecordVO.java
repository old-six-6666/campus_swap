package com.itcodai.campus_swap.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学生档案 VO
 */
@Data
public class StudentRecordVO {
    private Long id;
    private String school;
    private String studentId;
    private String realName;
    private String extraInfo;
    private Long createdBy;
    /** 录入管理员昵称 */
    private String createdByNickname;
    private LocalDateTime createdAt;
}
