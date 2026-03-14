package com.itcodai.campus_swap.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 管理员批量导入学生档案
 */
@Data
public class BatchStudentRecordDTO {

    @NotEmpty(message = "导入列表不能为空")
    @Size(max = 500, message = "单次最多导入500条")
    @Valid
    private List<StudentRecordDTO> records;
}
