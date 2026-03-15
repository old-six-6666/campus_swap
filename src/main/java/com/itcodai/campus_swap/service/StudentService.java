package com.itcodai.campus_swap.service;

import com.itcodai.campus_swap.dto.BatchStudentRecordDTO;
import com.itcodai.campus_swap.dto.StudentRecordDTO;
import com.itcodai.campus_swap.dto.StudentVerifyApplyDTO;
import com.itcodai.campus_swap.vo.PageVO;
import com.itcodai.campus_swap.vo.StudentRecordVO;
import com.itcodai.campus_swap.vo.StudentVerifyVO;

/**
 * 学生认证服务
 */
public interface StudentService {

    // ===== 学生档案管理（管理员） =====

    /** 分页查询学生档案 */
    PageVO<StudentRecordVO> listRecords(String school, String keyword, int page, int size);

    /** 添加单条学生档案 */
    void addRecord(StudentRecordDTO dto, Long operatorId);

    /** 批量导入学生档案，返回成功导入条数 */
    int batchImportRecords(BatchStudentRecordDTO dto, Long operatorId);

    /** 删除学生档案 */
    void deleteRecord(Long id);

    // ===== 认证申请审核（管理员） =====

    /** 分页查询认证申请列表 */
    PageVO<StudentVerifyVO> listVerifications(Integer status, String keyword, int page, int size);

    /** 审核认证申请：action=1 通过，action=2 拒绝 */
    void reviewVerification(Long id, int action, String remark, Long reviewerId);

    // ===== 用户端 =====

    /** 提交认证申请（已有待审核/通过的申请不可重复提交） */
    void applyVerify(Long userId, StudentVerifyApplyDTO dto);

    /** 获取当前用户的认证申请详情（无申请时返回 null） */
    StudentVerifyVO getMyVerification(Long userId);
}
