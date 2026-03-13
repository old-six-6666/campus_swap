package com.itcodai.campus_swap.dto;

import lombok.Data;

import java.util.List;

/**
 * 设置管理员权限请求体
 */
@Data
public class AdminPermissionsDTO {

    /** 权限码列表（全量覆盖），允许为空（清空所有权限） */
    private List<String> permissions;
}
