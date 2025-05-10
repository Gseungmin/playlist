package com.naver.playlist.web.exception.entity;

import lombok.Getter;


@Getter
public class PlayListException extends RuntimeException {
    private final int code;
    private final String errorMessage;

    public PlayListException(int code, String errorMessage){
        this.code = code;
        this.errorMessage = errorMessage;
    }
}
