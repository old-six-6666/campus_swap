package com.itcodai.campus_swap.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端用户信息 VO
 */
@Data
public class AdminUserVO {
    private Long id;
    private String email;
    private String nickname;
    private String school;
    private String phone;
    private String avatar;
    /** 角色：0-普通用户  1-管理员  2-超级管理员 */
    private Integer role;
    /** 状态：0-正常  1-禁用 */
    private Integer status;
    private LocalDateTime createdAt;
    /** 权限码列表（仅 role=1 时有值） */
    private List<String> permissions;
}
