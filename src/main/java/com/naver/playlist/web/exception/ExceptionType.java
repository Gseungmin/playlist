package com.naver.playlist.web.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExceptionType {

    /* Common Exception */
    SYSTEM_TIME_EXCEPTION( 90000, "올바르지 않은 시간정보입니다."),

    /* MEMBER Exception */
    MEMBER_AUTH_ID_INVALID( 10000, "회원 인증정보가 적절하지 않습니다."),

    /* DTO Exception */
    PLAY_LIST_TITLE_INVALID( 80000, "플레이리스트 제목을 40자 이내로 작성해주세요."),
    PLAY_LIST_DESCRIPTION_INVALID( 80000, "플레이리스트 내용은 1000자 이내로 작성해주세요.");

    private final int code;
    private final String errorMessage;
}
