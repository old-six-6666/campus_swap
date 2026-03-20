package com.itcodai.campus_swap.controller;

import com.itcodai.campus_swap.common.result.Result;
import com.itcodai.campus_swap.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 广场通用接口控制器
 */
@RestController
@RequestMapping({"/square", "/api/square"})
@RequiredArgsConstructor
public class SquareController {

    private final TagMapper tagMapper;

    /**
     * 获取热门标签（按使用次数降序）
     */
    @GetMapping("/tags/hot")
    public Result<List<Map<String, Object>>> getHotTags() {
        List<Map<String, Object>> tags = tagMapper.selectHotTags();
        return Result.success(tags);
    }
}
