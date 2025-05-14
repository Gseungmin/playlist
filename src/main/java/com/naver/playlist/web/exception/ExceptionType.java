package com.naver.playlist.web.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExceptionType {

    /* Common Exception */
    SYSTEM_TIME_EXCEPTION( 90000, "올바르지 않은 시간정보입니다."),
    SERIALIZABLE_EXCEPTION( 90001, "직렬화에 실패하였습니다."),
    UN_SERIALIZABLE_EXCEPTION( 90002, "역직렬화에 실패하였습니다."),

    /* INFRA Exception */
    LUA_SCRIPT_RETURN_INVALID( 91000, "루아 스크립트 반환 정보가 잘못되었습니다."),
    REDIS_LOCK_INTERRUPT( 91001, "인터럽트가 발생했습니다."),

    /* MEMBER Exception */
    MEMBER_AUTH_ID_INVALID( 10000, "회원 인증정보가 적절하지 않습니다."),

    /* PLAYLIST Exception */
    PLAY_LIST_NOT_EXIST( 20000, "플레이리스트가 존재하지 않습니다."),
    PLAY_LIST_AUTH_INVALID( 20001, "플레이리스트 접근 권한이 없습니다."),
    PLAY_LIST_ITEM_NOT_EXIST( 20002, "플레이리스트 노래가 존재하지 않습니다."),
    PLAY_LIST_NOT_MATCH_ITEM( 20003, "같은 플레이리스트의 노래만 업데이트 해주세요."),
    PLAY_LIST_EXCEED_LIMIT( 20004, "플레이리스트는 최대 1000개까지 생성할 수 있습니다."),
    PLAY_LIST_NOT_CONCURRENCY( 20005, "플레이리스트는 동시에 수정할 수 없어요."),

    MUSIC_NOT_EXIST( 21000, "음악이 존재하지 않습니다."),


    /* DTO Exception */
    PLAY_LIST_TITLE_INVALID( 80000, "플레이리스트 제목을 40자 이내로 작성해주세요."),
    PLAY_LIST_DESCRIPTION_INVALID( 80000, "플레이리스트 내용은 1000자 이내로 작성해주세요.");

    private final int code;
    private final String errorMessage;
}
