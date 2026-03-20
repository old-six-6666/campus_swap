package com.itcodai.campus_swap.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itcodai.campus_swap.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 标签Mapper接口
 */
@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    /**
     * 查询热门标签（按关联动态数量降序）
     * 连接 t_post_tag 统计每个标签被用了多少次
     */
    @Select("SELECT t.id, t.name, COUNT(pt.tag_id) AS count " +
            "FROM t_tag t " +
            "LEFT JOIN t_post_tag pt ON t.id = pt.tag_id " +
            "GROUP BY t.id, t.name " +
            "ORDER BY count DESC " +
            "LIMIT 20")
    List<Map<String, Object>> selectHotTags();
}
