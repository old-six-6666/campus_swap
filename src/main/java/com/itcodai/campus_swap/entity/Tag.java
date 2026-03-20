package com.itcodai.campus_swap.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 标签实体
 */
@Data
@TableName("t_tag")
public class Tag {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
}
