package com.naver.playlist.web.controller;

import com.naver.playlist.domain.dto.playlist.req.CreatePlayListItemRequest;
import com.naver.playlist.domain.dto.playlist.req.DeletePlayListItemRequest;
import com.naver.playlist.domain.dto.playlist.req.PlayListCreateRequest;
import com.naver.playlist.domain.dto.playlist.req.ReorderPlayListItemsRequest;
import com.naver.playlist.domain.dto.redis.PlayListCreateDto;
import com.naver.playlist.domain.entity.music.Music;
import com.naver.playlist.domain.service.MusicService;
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

    private final MusicService musicService;
    private final PlayListService playListService;
    private final PlayListItemService playListItemService;

    /*
     * 플레이리스트 생성 API
     *
     * 요구사항:
     * - 플레이리스트 생성 요청이 대규모로 이루어지는 상황을 가정한다.
     * - Redis 캐싱 및 벌크 인서트를 통해 플레이리스트 생성 성능을 최적화한다.
     *
     * 프로세스:
     * 1. 요청 DTO의 유효성 검사
     * 2. 사용자 인증 정보 조회
     * 3. 플레이리스트 생성 데이터를 사용자 인증 정보와 함께 Redis에 스냅샷 형태로 임시 저장(백업)
     * 4. 즉시 사용자에게 성공 응답
     * 5. 500ms 간격으로 주기적 배치(batch) 삽입 수행
     * 6. 배치 삽입 대기 건수가 50개 이상이면 즉시 벌크 인서트를 수행하고, 성공적으로 처리된 항목은 Redis에서 삭제
     */
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

    /*
     * 플레이리스트 노래 추가 API
     *
     * 요구사항:
     * - 플레이리스트에 노래를 추가하는 기능은 해당 플레이리스트의 작성자만 가능하다.
     * - 플레이리스트의 최대 노래 수는 1000개로 제한된다.
     * - 애플리케이션 레벨에서 1차로 개수 제한을 하고, DB 트리거를 통해 2차적인 추가 제한을 실시한다.
     *
     * 프로세스:
     * 1. 요청 DTO의 유효성 검사
     * 2. 사용자 인증 정보 조회
     * 3. 사용자 권한(작성자 여부) 검증
     * 4. 음악 존재 여부 체크
     * 4. 단일 쿼리 최적화
     *    - 플레이리스트 자체 조회
     *    - 플레이리스트 내 노래 개수 제한 확인
     *    - 플레이리스트 내 가장 큰 Order 조회
     * 5. 분산락을 활용해 동시 접근 시 데이터 일관성 유지
     */
    @PostMapping("/{playlistId}/items")
    public void addItemToPlaylist(
            HttpServletRequest request,
            @PathVariable Long playlistId,
            @RequestBody CreatePlayListItemRequest dto
    ) throws InterruptedException {
        Long memberId = validateMemberId(request);

        Music music = musicService.getMusic(
                dto.getMusicId()
        );

        playListItemService.create(
                playlistId,
                memberId,
                music
        );
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
     * 플레이리스트 노래 순서 변경 API
     *
     * 요구사항:
     * - 플레이리스트의 순서 변경은 해당 플레이리스트의 작성자만 가능하다.
     * - 노래 순서는 Gap-based numbering 방식을 사용하여 관리한다.
     * - 클라이언트에서 노래 순서 변경을 감지하여, 변경된 순서 데이터를 서버로 전송한다.
     * - 전체 플레이리스트 리오더링 역시 이 방식으로 처리된다.
     *
     * 프로세스:
     * 1. DTO 유효성 검사
     * 2. 사용자 인증 정보 조회
     * 3. 업데이트 대상 데이터 조회 (IN 쿼리로 최적화 진행)
     *    - playListItem과 playList의 매핑 유효성 검사
     *    - playList와 회원(member)의 매핑 유효성 검사
     * 4. 변경된 순서 데이터를 배치(batch) 처리로 업데이트
     * 5. 장애 발생 시 재정렬(리오더링)을 서버에서 수행하고 클라이언트에 알림
     * 6. 재정렬(리오더링)을 위한 데이터 조회 시 DTO 프로젝션을 활용해 성능 최적화
     * 7. 클라이언트는 실패 응답을 받으면 재시도 요청 수행
     */
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

