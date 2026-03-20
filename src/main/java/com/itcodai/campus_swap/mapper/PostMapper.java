package com.itcodai.campus_swap.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itcodai.campus_swap.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 动态Mapper接口
 */
@Mapper
public interface PostMapper extends BaseMapper<Post> {

    /**
     * 查询包含指定标签（任意一个）的 post_id 列表
     */
    @Select("SELECT DISTINCT post_id FROM t_post_tag WHERE tag_id IN (${tagIds})")
    List<Long> selectPostIdsByTagIds(@Param("tagIds") String tagIds);
}
