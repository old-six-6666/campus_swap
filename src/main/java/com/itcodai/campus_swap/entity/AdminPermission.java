package com.itcodai.campus_swap.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员权限实体（对应 t_admin_permission 表）
 */
@Data
@TableName("t_admin_permission")
public class AdminPermission {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 管理员用户 ID（role=1 的用户） */
    private Long adminId;

    /** 权限码：USER_MANAGE / ITEM_MANAGE */
    private String permCode;

    private LocalDateTime createdAt;
}
