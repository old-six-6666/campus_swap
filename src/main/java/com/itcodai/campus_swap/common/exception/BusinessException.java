package com.itcodai.campus_swap.common.exception;

import com.itcodai.campus_swap.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常（用于主动抛出可预期的业务错误）
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }
}
