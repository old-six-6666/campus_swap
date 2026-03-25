package com.itcodai.campus_swap.controller;

import com.itcodai.campus_swap.common.result.Result;
import com.itcodai.campus_swap.dto.*;
import com.itcodai.campus_swap.service.TradeAppealService;
import com.itcodai.campus_swap.service.TradeService;
import com.itcodai.campus_swap.vo.TradeAppealVO;
import com.itcodai.campus_swap.vo.TradeLogVO;
import com.itcodai.campus_swap.vo.TradeVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 以物换物交易控制器
 *
 * <pre>
 * POST   /api/trade/initiate                  甲方发起交易申请
 * POST   /api/trade/{tradeId}/match           乙方确认匹配
 * POST   /api/trade/{tradeId}/audit           管理员审核（需管理员权限）
 * POST   /api/trade/{tradeId}/deliver         一方确认发货
 * POST   /api/trade/{tradeId}/confirm-receipt 一方确认收货
 * POST   /api/trade/{tradeId}/terminate       终止交易
 * POST   /api/trade/{tradeId}/rollback        管理员回滚状态
 * POST   /api/trade/{tradeId}/appeal          提交申诉
 * GET    /api/trade/{tradeId}                 获取交易详情
 * GET    /api/trade/my                        获取我的交易列表
 * GET    /api/trade/{tradeId}/logs            获取状态变更日志
 * GET    /api/trade/{tradeId}/appeals         查询我在该交易的申诉列表
 * </pre>
 *
 * 所有接口均需 JWT 认证（由 JwtInterceptor 拦截）。
 * userId 从 request attribute "userId" 中取出（JwtInterceptor 写入）。
 */
@RestController
@RequestMapping("/api/trade")
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;
    private final TradeAppealService tradeAppealService;

    /**
     * 获取当前登录用户ID（由 JwtInterceptor 注入到 request attribute）
     */
    private Long currentUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    // ============================================================
    // 交易生命周期操作
    // ============================================================

    /**
     * 甲方发起交易申请 → PENDING_MATCH
     */
    @PostMapping("/initiate")
    public Result<TradeVO> initiate(@RequestBody @Valid InitiateTradeDTO dto,
                                    HttpServletRequest request) {
        return tradeService.initiateTrade(currentUserId(request), dto);
    }

    /**
     * 乙方确认匹配 → MATCHED（→ AUDIT_PENDING 或 WAITING_DELIVERY）
     */
    @PostMapping("/{tradeId}/match")
    public Result<TradeVO> match(@PathVariable Long tradeId,
                                 @RequestBody @Valid MatchTradeDTO dto,
                                 HttpServletRequest request) {
        return tradeService.matchTrade(currentUserId(request), tradeId, dto);
    }

    /**
     * 管理员审核 → AUDIT_PASSED 或 AUDIT_REJECTED
     */
    @PostMapping("/{tradeId}/audit")
    public Result<TradeVO> audit(@PathVariable Long tradeId,
                                 @RequestBody @Valid AuditTradeDTO dto,
                                 HttpServletRequest request) {
        return tradeService.auditTrade(currentUserId(request), tradeId, dto);
    }

    /**
     * 一方确认发货；双方均发货后自动推进至 BOTH_DELIVERED → WAITING_CONFIRM_RECEIPT
     */
    @PostMapping("/{tradeId}/deliver")
    public Result<TradeVO> deliver(@PathVariable Long tradeId,
                                   @RequestBody(required = false) DeliverDTO dto,
                                   HttpServletRequest request) {
        return tradeService.deliver(currentUserId(request), tradeId,
                dto != null ? dto : new DeliverDTO());
    }

    /**
     * 一方确认收货；双方均确认后 → COMPLETED
     */
    @PostMapping("/{tradeId}/confirm-receipt")
    public Result<TradeVO> confirmReceipt(@PathVariable Long tradeId,
                                          HttpServletRequest request) {
        return tradeService.confirmReceipt(currentUserId(request), tradeId);
    }

    /**
     * 终止交易（任意非终态，双方或管理员均可操作）
     */
    @PostMapping("/{tradeId}/terminate")
    public Result<Void> terminate(@PathVariable Long tradeId,
                                  @RequestBody(required = false) TerminateTradeDTO dto,
                                  HttpServletRequest request) {
        return tradeService.terminateTrade(currentUserId(request), tradeId, dto);
    }

    /**
     * 管理员回滚交易状态（仅限审核阶段及执行阶段前三步）
     */
    @PostMapping("/{tradeId}/rollback")
    public Result<TradeVO> rollback(@PathVariable Long tradeId,
                                    @RequestBody(required = false) RollbackTradeDTO dto,
                                    HttpServletRequest request) {
        return tradeService.rollbackTrade(currentUserId(request), tradeId, dto);
    }

    // ============================================================
    // 查询接口
    // ============================================================

    /**
     * 查询物品当前活跃的交易（用于在商品详情页展示"交换中"提示）
     * 未登录也可调用（userId 为 null 时 myRole=admin 兜底）
     */
    @GetMapping("/item/{itemId}")
    public Result<TradeVO> getItemActiveTrade(@PathVariable Long itemId,
                                              HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return tradeService.getItemActiveTrade(userId, itemId);
    }

    /**
     * 获取需要当前用户操作的交易数量（"交易"菜单红点用）
     */
    @GetMapping("/pending-count")
    public Result<Map<String, Object>> pendingCount(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.success(Map.of("count", 0));
        return Result.success(Map.of("count", tradeService.countPendingAction(userId)));
    }

    /**
     * 获取交易详情
     */
    @GetMapping("/{tradeId}")
    public Result<TradeVO> getDetail(@PathVariable Long tradeId,
                                     HttpServletRequest request) {
        return tradeService.getTradeDetail(currentUserId(request), tradeId);
    }

    /**
     * 获取我的交易列表
     *
     * @param status 状态过滤（可选，不传则返回全部）
     */
    @GetMapping("/my")
    public Result<List<TradeVO>> myTrades(@RequestParam(required = false) String status,
                                          HttpServletRequest request) {
        return tradeService.getMyTrades(currentUserId(request), status);
    }

    /**
     * 获取交易状态变更日志
     */
    @GetMapping("/{tradeId}/logs")
    public Result<List<TradeLogVO>> getLogs(@PathVariable Long tradeId,
                                            HttpServletRequest request) {
        return tradeService.getTradeLogs(currentUserId(request), tradeId);
    }

    // ============================================================
    // 申诉接口
    // ============================================================

    /**
     * 提交申诉（甲方或乙方均可在交易进行中提交）
     */
    @PostMapping("/{tradeId}/appeal")
    public Result<Void> submitAppeal(@PathVariable Long tradeId,
                                     @RequestBody @Valid SubmitAppealDTO dto,
                                     HttpServletRequest request) {
        tradeAppealService.submitAppeal(tradeId, currentUserId(request), dto.getContent());
        return Result.success();
    }

    /**
     * 查询当前用户在该交易中提交的申诉列表
     */
    @GetMapping("/{tradeId}/appeals")
    public Result<List<TradeAppealVO>> getMyAppeals(@PathVariable Long tradeId,
                                                    HttpServletRequest request) {
        return Result.success(tradeAppealService.getMyAppealsByTrade(tradeId, currentUserId(request)));
    }
}
