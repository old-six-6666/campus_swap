package com.itcodai.campus_swap.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itcodai.campus_swap.common.exception.BusinessException;
import com.itcodai.campus_swap.common.result.ResultCode;
import com.itcodai.campus_swap.dto.BatchStudentRecordDTO;
import com.itcodai.campus_swap.dto.StudentRecordDTO;
import com.itcodai.campus_swap.dto.StudentVerifyApplyDTO;
import com.itcodai.campus_swap.entity.StudentRecord;
import com.itcodai.campus_swap.entity.StudentVerify;
import com.itcodai.campus_swap.entity.User;
import com.itcodai.campus_swap.mapper.StudentRecordMapper;
import com.itcodai.campus_swap.mapper.StudentVerifyMapper;
import com.itcodai.campus_swap.mapper.UserMapper;
import com.itcodai.campus_swap.service.StudentService;
import com.itcodai.campus_swap.vo.PageVO;
import com.itcodai.campus_swap.vo.StudentRecordVO;
import com.itcodai.campus_swap.vo.StudentVerifyVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentRecordMapper recordMapper;
    private final StudentVerifyMapper verifyMapper;
    private final UserMapper userMapper;

    // ===== 学生档案管理 =====

    @Override
    public PageVO<StudentRecordVO> listRecords(String school, String keyword, int page, int size) {
        LambdaQueryWrapper<StudentRecord> wrapper = new LambdaQueryWrapper<StudentRecord>()
                .eq(StringUtils.hasText(school), StudentRecord::getSchool, school)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(StudentRecord::getStudentId, keyword)
                        .or()
                        .like(StudentRecord::getRealName, keyword))
                .orderByDesc(StudentRecord::getCreatedAt);
        Page<StudentRecord> result = recordMapper.selectPage(new Page<>(page, size), wrapper);
        List<StudentRecordVO> records = result.getRecords().stream()
                .map(this::toRecordVO)
                .collect(Collectors.toList());
        return PageVO.of(records, result.getTotal(), page, size);
    }

    @Override
    public void addRecord(StudentRecordDTO dto, Long operatorId) {
        checkDuplicateRecord(dto.getSchool(), dto.getStudentId(), null);
        StudentRecord record = new StudentRecord();
        BeanUtils.copyProperties(dto, record);
        record.setCreatedBy(operatorId);
        recordMapper.insert(record);
    }

    @Override
    @Transactional
    public int batchImportRecords(BatchStudentRecordDTO dto, Long operatorId) {
        int successCount = 0;
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < dto.getRecords().size(); i++) {
            StudentRecordDTO item = dto.getRecords().get(i);
            try {
                // 跳过重复记录（同校同学号），不抛错
                boolean exists = recordMapper.selectCount(
                        new LambdaQueryWrapper<StudentRecord>()
                                .eq(StudentRecord::getSchool, item.getSchool())
                                .eq(StudentRecord::getStudentId, item.getStudentId())
                ) > 0;
                if (exists) {
                    errors.add("第" + (i + 1) + "行：" + item.getSchool() + " 学号 " + item.getStudentId() + " 已存在，已跳过");
                    continue;
                }
                StudentRecord record = new StudentRecord();
                BeanUtils.copyProperties(item, record);
                record.setCreatedBy(operatorId);
                recordMapper.insert(record);
                successCount++;
            } catch (Exception e) {
                errors.add("第" + (i + 1) + "行导入失败：" + e.getMessage());
            }
        }
        if (successCount == 0 && !errors.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "全部导入失败：" + String.join("；", errors));
        }
        return successCount;
    }

    @Override
    public void deleteRecord(Long id) {
        StudentRecord record = recordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "学生档案不存在");
        }
        recordMapper.deleteById(id);
    }

    // ===== 认证申请审核 =====

    @Override
    public PageVO<StudentVerifyVO> listVerifications(Integer status, String keyword, int page, int size) {
        LambdaQueryWrapper<StudentVerify> wrapper = new LambdaQueryWrapper<StudentVerify>()
                .eq(status != null, StudentVerify::getStatus, status)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(StudentVerify::getRealName, keyword)
                        .or()
                        .like(StudentVerify::getStudentId, keyword)
                        .or()
                        .like(StudentVerify::getSchool, keyword))
                .orderByDesc(StudentVerify::getCreatedAt);
        Page<StudentVerify> result = verifyMapper.selectPage(new Page<>(page, size), wrapper);
        List<StudentVerifyVO> records = result.getRecords().stream()
                .map(this::toVerifyVO)
                .collect(Collectors.toList());
        return PageVO.of(records, result.getTotal(), page, size);
    }

    @Override
    @Transactional
    public void reviewVerification(Long id, int action, String remark, Long reviewerId) {
        if (action != 1 && action != 2) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "无效的审核操作");
        }
        if (action == 2 && !StringUtils.hasText(remark)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "拒绝时必须填写原因");
        }
        StudentVerify verify = verifyMapper.selectById(id);
        if (verify == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "认证申请不存在");
        }
        if (verify.getStatus() != 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该申请已审核，无法重复操作");
        }

        verify.setStatus(action);
        verify.setRemark(action == 2 ? remark : null);
        verify.setReviewedBy(reviewerId);
        verify.setReviewedAt(LocalDateTime.now());
        verifyMapper.updateById(verify);

        // 通过时：在学生档案中验证匹配，并标记用户为已认证
        if (action == 1) {
            boolean matched = recordMapper.selectCount(
                    new LambdaQueryWrapper<StudentRecord>()
                            .eq(StudentRecord::getSchool, verify.getSchool())
                            .eq(StudentRecord::getStudentId, verify.getStudentId())
                            .eq(StudentRecord::getRealName, verify.getRealName())
            ) > 0;
            if (!matched) {
                throw new BusinessException(ResultCode.BAD_REQUEST,
                        "学生档案中未找到匹配记录（学校+学号+姓名），请先确认档案信息后再通过");
            }
            User user = userMapper.selectById(verify.getUserId());
            if (user != null) {
                user.setIsVerified(1);
                userMapper.updateById(user);
            }
        }
    }

    // ===== 用户端 =====

    @Override
    public void applyVerify(Long userId, StudentVerifyApplyDTO dto) {
        // 检查是否已有待审核或已通过的申请
        StudentVerify existing = verifyMapper.selectOne(
                new LambdaQueryWrapper<StudentVerify>()
                        .eq(StudentVerify::getUserId, userId)
                        .in(StudentVerify::getStatus, 0, 1)
        );
        if (existing != null) {
            String msg = existing.getStatus() == 0 ? "已有待审核的认证申请，请耐心等待" : "已完成学生认证，无需重复申请";
            throw new BusinessException(ResultCode.BAD_REQUEST, msg);
        }
        StudentVerify verify = new StudentVerify();
        verify.setUserId(userId);
        verify.setSchool(dto.getSchool());
        verify.setStudentId(dto.getStudentId());
        verify.setRealName(dto.getRealName());
        verify.setExtraInfo(dto.getExtraInfo());
        verify.setStatus(0);
        verifyMapper.insert(verify);
    }

    @Override
    public StudentVerifyVO getMyVerification(Long userId) {
        // 优先返回最新一条（包含已拒绝的）
        StudentVerify verify = verifyMapper.selectOne(
                new LambdaQueryWrapper<StudentVerify>()
                        .eq(StudentVerify::getUserId, userId)
                        .orderByDesc(StudentVerify::getCreatedAt)
                        .last("LIMIT 1")
        );
        return verify == null ? null : toVerifyVO(verify);
    }

    // ===== 私有辅助 =====

    private void checkDuplicateRecord(String school, String studentId, Long excludeId) {
        LambdaQueryWrapper<StudentRecord> wrapper = new LambdaQueryWrapper<StudentRecord>()
                .eq(StudentRecord::getSchool, school)
                .eq(StudentRecord::getStudentId, studentId)
                .ne(excludeId != null, StudentRecord::getId, excludeId);
        if (recordMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "该学校（" + school + "）的学号 " + studentId + " 已存在");
        }
    }

    private StudentRecordVO toRecordVO(StudentRecord record) {
        StudentRecordVO vo = new StudentRecordVO();
        BeanUtils.copyProperties(record, vo);
        if (record.getCreatedBy() != null) {
            User admin = userMapper.selectById(record.getCreatedBy());
            if (admin != null) vo.setCreatedByNickname(admin.getNickname());
        }
        return vo;
    }

    private StudentVerifyVO toVerifyVO(StudentVerify verify) {
        StudentVerifyVO vo = new StudentVerifyVO();
        BeanUtils.copyProperties(verify, vo);
        User user = userMapper.selectById(verify.getUserId());
        if (user != null) {
            vo.setUserNickname(user.getNickname());
            vo.setUserEmail(user.getEmail());
        }
        if (verify.getReviewedBy() != null) {
            User reviewer = userMapper.selectById(verify.getReviewedBy());
            if (reviewer != null) vo.setReviewedByNickname(reviewer.getNickname());
        }
        return vo;
    }
}
