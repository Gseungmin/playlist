package com.naver.playlist.domain.validator;
import com.naver.playlist.web.exception.entity.MemberException;
import jakarta.servlet.http.HttpServletRequest;
import static com.naver.playlist.domain.constant.EntityConstants.*;
import static com.naver.playlist.web.exception.ExceptionType.MEMBER_AUTH_ID_INVALID;

public class MemberValidator {

    public static Long validateMemberId(HttpServletRequest request) {
        Object memberId = request.getAttribute(AUTH_ATTRIBUTE);

        if (memberId instanceof Long id) {
            return id;
        }

        throw new MemberException(MEMBER_AUTH_ID_INVALID.getCode(), MEMBER_AUTH_ID_INVALID.getErrorMessage());
    }
}
