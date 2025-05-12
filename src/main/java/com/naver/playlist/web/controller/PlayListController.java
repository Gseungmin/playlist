package com.naver.playlist.web.controller;

import com.naver.playlist.domain.dto.playlist.req.CreatePlayListItemRequest;
import com.naver.playlist.domain.dto.playlist.req.DeletePlayListItemRequest;
import com.naver.playlist.domain.dto.playlist.req.PlayListCreateRequest;
import com.naver.playlist.domain.dto.playlist.req.ReorderPlayListItemsRequest;
import com.naver.playlist.domain.dto.redis.PlayListCreateDto;
import com.naver.playlist.domain.service.PlayListItemService;
import com.naver.playlist.domain.service.PlayListService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.naver.playlist.domain.validator.MemberValidator.validateMemberId;
import static com.naver.playlist.domain.validator.PlayListValidator.validatePlayListCreate;

@RestController
@RequestMapping("/playlist")
@RequiredArgsConstructor
public class PlayListController {

    private final PlayListService playListService;
    private final PlayListItemService playListItemService;

    /*
     * 플레이리스트 생성
     * 플레이리스트 생성은 대규모로 이어지고 있음을 가정한다.
     * 레디스와 벌크 인서트를 통해 플레이리스트 생성 성능을 최적화한다.
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

    /*
     * 플레이리스트 순서 변경
     * 플레이리스트 순서 변경은 오직 플레이리스트 작성자만 가능하다.
     * Gap-based numbering 방식을 통해 순서를 구성한다.
     * 순서 변경의 경우 클라이언트에서 체크 후 전송되어짐을 가정한다.
     * 즉, 노래의 순서가 변경되었을 경우, 이 변환된 순서 값을 클라이언트에서 감지하여 서버로 전송한다.
     * 전체 리오더링도 여기에 포함된다.
     * 서버는 배치 업데이트를 진행한다.
     * 1. DTO 유효성 검사
     * 2. 사용자 인증 정보 조회
     * 3. 업데이트 할 데이터 조회 - 이떄 쿼리 최적화를 통해 IN 쿼리로 진행한다.
     *     - playListItem과 playList가 올바르게 매핑되어 있는지 확인
     *     - playList와 회원이 올바르게 매핑되어 있는지 확인
     * 4. 데이터 배치 처리
     * 5. 만약 장애 발생시 리오더링을 진행 후 클라이언트에 알림
     * 6. 리오더링 과정에서 DTO 프로젝션을 통해서 성능 최적화
     * 7. 클라이언트는 재시도 요청 보냄
     * */
    @PatchMapping("/{playlistId}/items/order")
    public void reorderItems(
            HttpServletRequest request,
            @PathVariable Long playlistId,
            @RequestBody List<ReorderPlayListItemsRequest> dto
    ) {
        Long memberId = validateMemberId(request);
        playListItemService.reorder(playlistId, memberId, dto);
    }
}

