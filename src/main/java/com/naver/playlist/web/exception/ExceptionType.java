package com.naver.playlist.web.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExceptionType {

    /* Common Exception */
    SYSTEM_TIME_EXCEPTION( 90000, "올바르지 않은 시간정보입니다.");

    private final int code;
    private final String errorMessage;
}
