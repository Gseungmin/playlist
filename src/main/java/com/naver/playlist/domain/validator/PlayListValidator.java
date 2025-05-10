package com.naver.playlist.domain.validator;
import com.naver.playlist.domain.dto.playlist.req.PlayListCreateRequest;
import com.naver.playlist.web.exception.ExceptionType;
import com.naver.playlist.web.exception.entity.PlayListException;

import static com.naver.playlist.domain.constant.EntityConstants.MAX_PLAY_LIST_DESCRIPTION;
import static com.naver.playlist.domain.constant.EntityConstants.MAX_PLAY_LIST_TITLE;
import static com.naver.playlist.web.exception.ExceptionType.PLAY_LIST_DESCRIPTION_INVALID;
import static com.naver.playlist.web.exception.ExceptionType.PLAY_LIST_TITLE_INVALID;

public class PlayListValidator {

    public static void validatePlayListCreate(PlayListCreateRequest dto) {
        validateText(dto.getTitle(), MAX_PLAY_LIST_TITLE, PLAY_LIST_TITLE_INVALID);
        validateText(dto.getDescription(), MAX_PLAY_LIST_DESCRIPTION, PLAY_LIST_DESCRIPTION_INVALID);
    }

    private static void validateText(String text, int maxLen, ExceptionType error) {
        if (text == null || text.isBlank() || text.trim().length() > maxLen) {
            throw new PlayListException(error.getCode(), error.getErrorMessage());
        }
    }
}
