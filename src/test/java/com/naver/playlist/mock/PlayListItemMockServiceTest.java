package com.naver.playlist.mock;

import com.naver.playlist.ServiceTest;
import com.naver.playlist.domain.dto.playlist.req.ReorderPlayListItemsRequest;
import com.naver.playlist.domain.dto.playlist.res.PlayListReOrderResponse;
import com.naver.playlist.domain.entity.member.Member;
import com.naver.playlist.domain.entity.music.Music;
import com.naver.playlist.domain.entity.playlist.PlayList;
import com.naver.playlist.domain.entity.playlist.PlayListItem;
import com.naver.playlist.domain.repository.*;
import com.naver.playlist.domain.service.PlayListItemService;
import com.naver.playlist.web.exception.entity.PlayListException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.naver.playlist.web.exception.ExceptionType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Transactional
@ActiveProfiles("test")
@SpringBootTest()
public class PlayListItemMockServiceTest extends ServiceTest {

    @Autowired
    private PlayListItemRepository playListItemRepository;

    @Autowired
    private PlayListRepository playListRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RedissonClient redisson;

    @Autowired
    private MusicRepository musicRepository;

    private PlayListItemService playListItemService;

    @Mock
    JdbcBulkRepository bulkRepository;

    private Member 회원1;
    private PlayList 플레이리스트;
    List<PlayListItem> 플레이리스트아이템;

    @BeforeEach
    public void setUp() throws InterruptedException {
        playListItemService = new PlayListItemService(
                bulkRepository,
                playListItemRepository,
                playListRepository,
                redisson
        );

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
    @DisplayName("1. 데이터베이스 접근 예외 - 순서 중복 시 리오더링이 발생한다.")
    void 플레이리스트에_존재하는_노래만_순서를_바꿀수있다() {
        //given
        List<ReorderPlayListItemsRequest> request = List.of(
                new ReorderPlayListItemsRequest(플레이리스트아이템.get(0).getId(), 1000L),
                new ReorderPlayListItemsRequest(플레이리스트아이템.get(1).getId(), 2000L)
        );

        doThrow(new DuplicateKeyException("키 중복 에러"))
                .doNothing()
                .when(bulkRepository).bulkUpdatePosition(any());

        //when
        PlayListReOrderResponse result = playListItemService.reorder(
                플레이리스트.getId(),
                회원1.getId(),
                request
        );

        verify(bulkRepository, times(2)).bulkUpdatePosition(any());
        assertThat(result.isSuccess()).isEqualTo(false);
        assertThat(result.getPlayListItems().size()).isEqualTo(10);
    }
}