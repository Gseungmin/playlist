package com.naver.playlist.web.controller;

import com.naver.playlist.domain.dto.playlist.req.CreatePlayListItemRequest;
import com.naver.playlist.domain.dto.playlist.req.DeletePlayListItemRequest;
import com.naver.playlist.domain.dto.playlist.req.PlayListCreateRequest;
import com.naver.playlist.domain.dto.redis.PlayListCreateDto;
import com.naver.playlist.domain.service.PlayListService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.naver.playlist.domain.validator.MemberValidator.validateMemberId;
import static com.naver.playlist.domain.validator.PlayListValidator.validatePlayListCreate;

@RestController
@RequestMapping("/playlist")
@RequiredArgsConstructor
public class PlayListController {

    private final PlayListService playListService;

    /*
     * 플레이리스트 생성
     * 1. DTO 유효성 검사
     * 2. 사용자 인증 정보 조회
     * 3. 플레이리스트 생성 정보를 인증정보와 함께 레디스에 스냅샷(백업) 생성
     * 4. 사용자에게 미리 응답
     * 5. 500ms 주기에 맞춰서 배치 삽입
     * 6. 만약 배치 삽입할 개수가 50개를 넘는다면 미리 벌크 인서트 및 성공 시 삭제
     * */
    @PostMapping
    public PlayListCreateDto create(
            HttpServletRequest request,
            @RequestBody PlayListCreateRequest dto
    ) {
        validatePlayListCreate(dto);
        Long memberId = validateMemberId(request);
        return playListService.create(dto, memberId);
    }

    @PatchMapping("/{playlistId}")
    public void update(
            HttpServletRequest request,
            @PathVariable Long playlistId
    ) {
        return;
    }

    @DeleteMapping("/{playlistId}")
    public void delete(
            HttpServletRequest request,
            @PathVariable Long playlistId
    ) {
        return;
    }

    @GetMapping("/{playlistId}/items")
    public void getItemList(
            HttpServletRequest request,
            @PathVariable Long playlistId,
            @RequestParam(required = false) Long cursor
    ) {
        return;
    }

    @PostMapping("/{playlistId}/items")
    public void addItemToPlaylist(
            HttpServletRequest request,
            @PathVariable Long playlistId,
            @RequestBody CreatePlayListItemRequest dto
    ) {
        return;
    }

    @DeleteMapping("/{playlistId}/items")
    public void deleteItemFromPlaylist(
            HttpServletRequest request,
            @PathVariable Long playlistId,
            @RequestBody DeletePlayListItemRequest dto
    ) {
        return;
    }
}

