package com.itcodai.campus_swap.vo;

import lombok.Data;

/**
 * 用户信息 VO
 */
@Data
public class UserVO {
    private Long id;
    private String email;
    private String nickname;
    private String school;
    private String avatar;
    private String phone;
    /** 角色：0-普通用户  1-管理员  2-超级管理员 */
    private Integer role;
    /** 账号状态：0-正常  1-禁用 */
    private Integer status;
    /** 学生认证状态：0-未认证  1-已认证 */
    private Integer isVerified;
}
