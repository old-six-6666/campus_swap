package com.itcodai.campus_swap.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 广场动态实体类
 * 对应表 t_post
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_post")
public class Post {
    
    /**
     * 动态ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 发布用户ID
     */
    private Long userId;
    
    /**
     * 关联商品ID
     */
    private Long itemId;
    
    /**
     * 动态类型: 1-发布商品 2-换物成功 3-分享动态
     */
    private Integer type;
    
    /**
     * 动态内容
     */
    private String content;

    /**
     * 图片URL列表，JSON数组存储，最多9张
     */
    private String images;

    /**
     * 前端传入的图片URL列表（不映射数据库列），由 controller 序列化后写入 images
     */
    @TableField(exist = false)
    private List<String> imageList;

    /**
     * 前端传入的标签ID列表（不映射数据库列），由 service 写入 t_post_tag
     */
    @TableField(exist = false)
    private List<Long> tagIds;
    
    /**
     * 点赞数
     */
    private Integer likeCount;
    
    /**
     * 评论数
     */
    private Integer commentCount;
    
    /**
     * 收藏数
     */
    private Integer favoriteCount;

    /**
     * 浏览数
     */
    private Integer viewCount;

    /**
     * 分享数
     */
    private Integer shareCount;

    /**
     * 热度分（综合计算）
     */
    private Double hotScore;

    /**
     * 发布时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    /**
     * 动态类型枚举
     */
    public enum PostType {
        PUBLISH_ITEM(1, "发布商品"),
        SWAP_SUCCESS(2, "换物成功"),
        SHARE_EXPERIENCE(3, "分享动态");
        
        private final Integer code;
        private final String desc;
        
        PostType(Integer code, String desc) {
            this.code = code;
            this.desc = desc;
        }
        
        public Integer getCode() {
            return code;
        }
        
        public String getDesc() {
            return desc;
        }
        
        public static PostType fromCode(Integer code) {
            for (PostType type : PostType.values()) {
                if (type.getCode().equals(code)) {
                    return type;
                }
            }
            return null;
        }
    }
}