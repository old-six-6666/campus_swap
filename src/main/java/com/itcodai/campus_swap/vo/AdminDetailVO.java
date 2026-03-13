package com.itcodai.campus_swap.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 管理员详情 VO（含权限列表），供超级管理员查看
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AdminDetailVO extends AdminUserVO {

    /** 已授予的权限码列表，如 ["USER_MANAGE", "ITEM_MANAGE"] */
    private List<String> permissions;
}
