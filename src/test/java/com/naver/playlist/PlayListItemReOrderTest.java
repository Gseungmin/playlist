package com.naver.playlist;

import com.naver.playlist.domain.dto.playlist.req.ReorderPlayListItemsRequest;
import com.naver.playlist.domain.entity.member.Member;
import com.naver.playlist.domain.entity.music.Music;
import com.naver.playlist.domain.entity.playlist.PlayList;
import com.naver.playlist.domain.entity.playlist.PlayListItem;
import com.naver.playlist.domain.repository.MemberRepository;
import com.naver.playlist.domain.repository.MusicRepository;
import com.naver.playlist.domain.repository.PlayListRepository;
import com.naver.playlist.domain.service.PlayListItemService;
import com.naver.playlist.web.exception.entity.PlayListException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.naver.playlist.web.exception.ExceptionType.*;
import static org.junit.jupiter.api.Assertions.*;

@Transactional
@ActiveProfiles("test")
@SpringBootTest()
public class PlayListItemReOrderTest extends ServiceTest {

    @Autowired
    private PlayListRepository playListRepository;

    @Autowired
    private PlayListItemService playListItemService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MusicRepository musicRepository;

    private Member 회원1;
    private PlayList 플레이리스트;
    List<PlayListItem> 플레이리스트아이템;

    @BeforeEach
    public void setUp() throws InterruptedException {
        회원1 = memberRepository.save(new Member("이름"));
        플레이리스트 = playListRepository.save(new PlayList("제목", "설명", 회원1));
        for (int i = 0; i < 10; i++) {
            Music 음악 = musicRepository.save(new Music("음악" + i));
            playListItemService.create(플레이리스트.getId(), 회원1.getId(), 음악);
        }
        flushAndClear();
        플레이리스트 = playListRepository.findById(플레이리스트.getId()).get();
        플레이리스트아이템 = 플레이리스트.getPlayListItemList();
    }

    @Test
    @DisplayName("1. 플레이리스트에 순서 변경 - 엣지 케이스 - 플레이리스트에 없는 노래는 순서를 바꿀 수 없다.")
    void 플레이리스트에_존재하는_노래만_순서를_바꿀수있다() {
        //given when then
        PlayListException 예외 = assertThrows(PlayListException.class, () -> {
            playListItemService.reorder(
                    플레이리스트.getId(),
                    회원1.getId(),
                    List.of(
                            new ReorderPlayListItemsRequest(1L, 1000L)
                    )
            );
        });

        assertEquals(PLAY_LIST_ITEM_NOT_EXIST.getCode(), 예외.getCode());
        assertEquals(PLAY_LIST_ITEM_NOT_EXIST.getErrorMessage(), 예외.getErrorMessage());
    }

    @Test
    @DisplayName("2. 플레이리스트에 순서 변경 - 엣지 케이스 - 플레이리스트 ID와 아이템이 가지는 플레이리스트 아이템의 ID가 다르면 안된다.")
    void 수정할_플레이리스트_ID와_아이템이_가진_플레이리스트_ID가_다르면_안된다() {
        //given when then
        PlayListException 예외 = assertThrows(PlayListException.class, () -> {
            playListItemService.reorder(
                    1L,
                    회원1.getId(),
                    List.of(
                            new ReorderPlayListItemsRequest(플레이리스트아이템.get(0).getId(), 1000L)
                    )
            );
        });

        assertEquals(PLAY_LIST_NOT_MATCH_ITEM.getCode(), 예외.getCode());
        assertEquals(PLAY_LIST_NOT_MATCH_ITEM.getErrorMessage(), 예외.getErrorMessage());
    }

    @Test
    @DisplayName("3. 플레이리스트에 순서 변경 - 엣지 케이스 - 플레이리스트 생성 회원만 플레이리스트를 수정할 수 있다.")
    void 플레이리스트_생성_회원만_플레이리스트를_수정할수있다() {
        //given when then
        PlayListException 예외 = assertThrows(PlayListException.class, () -> {
            playListItemService.reorder(
                    플레이리스트.getId(),
                    1L,
                    List.of(
                            new ReorderPlayListItemsRequest(플레이리스트아이템.get(0).getId(), 1000L)
                    )
            );
        });

        assertEquals(PLAY_LIST_AUTH_INVALID.getCode(), 예외.getCode());
        assertEquals(PLAY_LIST_AUTH_INVALID.getErrorMessage(), 예외.getErrorMessage());
    }

    @Test
    @DisplayName("4. 플레이리스트에 순서 변경 -  플레이리스트 순서를 변경할수있다")
    void 플레이리스트_순서변경() {
        //given when then
        assertDoesNotThrow(() -> {
                playListItemService.reorder(
                        플레이리스트.getId(),
                        회원1.getId(),
                        List.of(
                                new ReorderPlayListItemsRequest(플레이리스트아이템.get(0).getId(), 1000L),
                                new ReorderPlayListItemsRequest(플레이리스트아이템.get(1).getId(), 2000L)
                        )
                );
        });
    }
}