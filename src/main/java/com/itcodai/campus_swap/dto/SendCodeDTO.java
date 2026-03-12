package com.itcodai.campus_swap.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 发送验证码请求 DTO
 */
@Data
public class SendCodeDTO {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 场景：REGISTER / FORGOT_PASSWORD / CHANGE_PASSWORD */
    @NotBlank(message = "场景不能为空")
    @Pattern(regexp = "REGISTER|FORGOT_PASSWORD|CHANGE_PASSWORD", message = "不支持的场景类型")
    private String scene;
}
