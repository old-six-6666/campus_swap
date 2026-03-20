package com.itcodai.campus_swap.service;

import com.itcodai.campus_swap.vo.PageVO;
import com.itcodai.campus_swap.vo.PostReportVO;

/**
 * 动态举报服务接口
 */
public interface PostReportService {

    /**
     * 用户举报动态
     * @param postId     被举报动态ID
     * @param reporterId 举报者ID
     * @param reason     举报原因（1-6）
     * @param description 补充说明
     */
    void reportPost(Long postId, Long reporterId, Integer reason, String description);

    /**
     * 管理员分页查询举报列表
     * @param status  状态过滤（null=全部，0=待审核，1=已处理，2=已驳回）
     * @param page    页码
     * @param size    每页数量
     */
    PageVO<PostReportVO> listReports(Integer status, int page, int size);

    /**
     * 管理员审核举报
     * @param reportId   举报ID
     * @param action     1=处理(内容下架)  2=驳回(内容正常)
     * @param remark     审核备注
     * @param reviewerId 审核员ID
     */
    void reviewReport(Long reportId, Integer action, String remark, Long reviewerId);
}
