package com.itcodai.campus_swap.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 动态-标签关联实体，对应 t_post_tag 表
 */
@Data
@TableName("t_post_tag")
public class PostTag {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;

    private Long tagId;
}
