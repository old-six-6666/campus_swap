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
}
