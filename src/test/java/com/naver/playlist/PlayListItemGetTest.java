package com.naver.playlist;

import com.naver.playlist.domain.dto.playlist.req.ReorderPlayListItemsRequest;
import com.naver.playlist.domain.dto.playlist.res.PlayListItemResponse;
import com.naver.playlist.domain.dto.playlist.res.PlayListResponse;
import com.naver.playlist.domain.entity.member.Member;
import com.naver.playlist.domain.entity.music.Music;
import com.naver.playlist.domain.entity.playlist.PlayList;
import com.naver.playlist.domain.entity.playlist.PlayListItem;
import com.naver.playlist.domain.redis.RedisPlayListService;
import com.naver.playlist.domain.repository.MemberRepository;
import com.naver.playlist.domain.repository.MusicRepository;
import com.naver.playlist.domain.repository.PlayListRepository;
import com.naver.playlist.domain.service.PlayListItemService;
import com.naver.playlist.domain.service.PlayListService;
import com.naver.playlist.web.exception.entity.PlayListException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.naver.playlist.domain.constant.EntityConstants.MAX_PLAY_PAGE_SIZE;
import static com.naver.playlist.web.exception.ExceptionType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Transactional
@ActiveProfiles("test")
@SpringBootTest()
public class PlayListItemGetTest extends ServiceTest {

    @Autowired
    private PlayListRepository playListRepository;

    @Autowired
    private PlayListItemService playListItemService;

    @Autowired
    private PlayListService playListService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MusicRepository musicRepository;

    @Autowired
    private RedisPlayListService redisPlayListService;

    private Member 회원1;
    private PlayList 플레이리스트1;
    private PlayList 플레이리스트2;
    List<PlayListItem> 플레이리스트1아이템;

    @BeforeEach
    public void setUp() throws InterruptedException {
        회원1 = memberRepository.save(new Member("이름"));
        플레이리스트1 = playListRepository.save(new PlayList("제목1", "설명1", 회원1));
        플레이리스트2 = playListRepository.save(new PlayList("제목2", "설명2", 회원1));
        for (int i = 0; i < 100; i++) {
            Music 음악 = musicRepository.save(new Music("음악" + i));
            playListItemService.create(플레이리스트1.getId(), 회원1.getId(), 음악);
        }
        flushAndClear();
        플레이리스트1 = playListRepository.findById(플레이리스트1.getId()).get();
        플레이리스트1아이템 = 플레이리스트1.getPlayListItemList();
    }

    @Test
    @DisplayName("1. 플레이리스트에 조회 - 플레이리스트는 페이지 단위로 조회된다.")
    void 플레이리스트는_페이지_단위로_조회된다() throws InterruptedException {
        //given when
        PlayListResponse 플레이리스트1_페이지1 = playListService.get(this.플레이리스트1.getId(), null);
        PlayListResponse 플레이리스트2_페이지1 = playListService.get(this.플레이리스트2.getId(), null);

        // then
        assertThat(플레이리스트1_페이지1.isHasNext()).isEqualTo(true);
        assertThat(플레이리스트1_페이지1.getPlayListItems().size()).isEqualTo(MAX_PLAY_PAGE_SIZE);

        assertThat(플레이리스트2_페이지1.isHasNext()).isEqualTo(false);
        assertThat(플레이리스트2_페이지1.getPlayListItems().size()).isEqualTo(0);
    }

    @Test
    @DisplayName("2. 플레이리스트에 조회 - 플레이리스트는 끝 페이지까지 조회할수있다.")
    void 플레이리스트_마지막_페이지까지_조회() throws InterruptedException {
        //given when
        Long 커서 = null;
        for (int i = 0; i < 5; i++) {
            PlayListResponse 페이지 = playListService.get(this.플레이리스트1.getId(), 커서);
            List<PlayListItemResponse> 페이지아이템 = 페이지.getPlayListItems();
            커서 = 페이지아이템.get(페이지아이템.size()-1).getOrder();

            // then
            assertThat(페이지.isHasNext()).isEqualTo(i == 4 ? false : true);
            assertThat(페이지.getPlayListItems().size()).isEqualTo(MAX_PLAY_PAGE_SIZE);
        }
    }

    @Test
    @DisplayName("3. 플레이리스트에 조회 - 플레이리스트 조회시 캐시에 저장된다.")
    void 플레이리스트_조회시_캐시에_저장된다() throws InterruptedException {
        //given when
        PlayListResponse 플레이리스트1_페이지1 = playListService.get(this.플레이리스트1.getId(), null);

        //when
        PlayListResponse 캐시_존재 = redisPlayListService.getPlayList(플레이리스트1.getId(), null);
        PlayListResponse 캐시_미존재 = redisPlayListService.getPlayList(1L, null);

        //then
        assertThat(캐시_미존재).isNull();
        assertThat(캐시_존재).isNotNull();
        List<PlayListItemResponse> 플레이리스트아이템 = 플레이리스트1_페이지1.getPlayListItems();
        for (int i = 0; i < 플레이리스트아이템.size(); i++) {
            PlayListItemResponse DB_아이템 = 플레이리스트아이템.get(i);
            PlayListItemResponse 캐시_아이템 = 플레이리스트아이템.get(i);
            assertThat(DB_아이템.getId()).isEqualTo(캐시_아이템.getId());
        }
    }
}