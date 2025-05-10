package com.naver.playlist.web.exception.handler;

import com.naver.playlist.web.exception.CommonException;
import com.naver.playlist.web.exception.ErrorResult;
import com.naver.playlist.web.exception.entity.MemberException;
import com.naver.playlist.web.exception.entity.PlayListException;
import com.naver.playlist.web.exception.infra.InfraException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class CommonExceptionHandler {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(CommonException.class)
    public ErrorResult commonExceptionHandle(CommonException e, HttpServletRequest request) {
        log.error("[CommonException] url: {} | errorMessage: {} | cause Exception: ",
                request.getRequestURL(), e.getMessage(), e.getCause());
        return new ErrorResult(e.getCode(), e.getErrorMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(PlayListException.class)
    public ErrorResult playListExceptionHandle(PlayListException e, HttpServletRequest request) {
        log.error("[PlayListException] url: {} | errorMessage: {} | cause Exception: ",
                request.getRequestURL(), e.getMessage(), e.getCause());
        return new ErrorResult(e.getCode(), e.getErrorMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MemberException.class)
    public ErrorResult memberExceptionHandle(MemberException e, HttpServletRequest request) {
        log.error("[MemberException] url: {} | errorMessage: {} | cause Exception: ",
                request.getRequestURL(), e.getMessage(), e.getCause());
        return new ErrorResult(e.getCode(), e.getErrorMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(InfraException.class)
    public ErrorResult infraExceptionHandle(InfraException e, HttpServletRequest request) {
        log.error("[InfraException] url: {} | errorMessage: {} | cause Exception: ",
                request.getRequestURL(), e.getMessage(), e.getCause());
        return new ErrorResult(e.getCode(), e.getErrorMessage());
    }
}
