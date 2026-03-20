package com.itcodai.campus_swap.controller;

import com.itcodai.campus_swap.common.result.Result;
import com.itcodai.campus_swap.common.result.ResultCode;
import com.itcodai.campus_swap.dto.ExchangeMatchDTO;
import com.itcodai.campus_swap.search.LocalSearchService;
import com.itcodai.campus_swap.vo.ItemVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 以物换物匹配接口
 */
@Slf4j
@RestController
@RequestMapping("/api/exchange")
@RequiredArgsConstructor
public class ExchangeMatchController {

    private final LocalSearchService localSearchService;

    /**
     * 核心匹配接口
     * POST /api/exchange/match
     * 需要登录
     */
    @PostMapping("/match")
    public Result<List<ItemVO>> match(
            @RequestBody ExchangeMatchDTO dto,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.fail(ResultCode.UNAUTHORIZED, "请先登录");
        }
        List<ItemVO> result = localSearchService.matchForExchange(
                userId,
                dto.getCategory(),
                dto.getExpectCategory(),
                dto.getDescriptionKeyword(),
                dto.getMinNewDegree(),
                dto.getMaxNewDegree(),
                dto.getPageNum() == null ? 1 : dto.getPageNum(),
                dto.getPageSize() == null ? 20 : dto.getPageSize()
        );
        return Result.success(result);
    }
}
