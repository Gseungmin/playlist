package com.naver.playlist;

import com.naver.playlist.domain.dto.playlist.req.PlayListCreateRequest;
import com.naver.playlist.domain.dto.redis.PlayListCreateDto;
import com.naver.playlist.domain.entity.member.Member;
import com.naver.playlist.domain.entity.playlist.PlayList;
import com.naver.playlist.domain.redis.RedisHashService;
import com.naver.playlist.domain.repository.MemberRepository;
import com.naver.playlist.domain.repository.PlayListRepository;
import com.naver.playlist.domain.service.PlayListService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static com.naver.playlist.domain.constant.RedisConstants.PLAY_LIST_HASH_NAME;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@ActiveProfiles("test")
@SpringBootTest()
public class PlayListTest extends ServiceTest {

    @Autowired
    private RedisHashService redisHashService;

    @Autowired
    private PlayListService playListService;

    @Autowired
    private PlayListRepository playListRepository;

    @Autowired
    private MemberRepository memberRepository;

    @TestConfiguration
    static class SyncAsyncConfig {

        @Primary
        @Bean(name = { "taskExecutor", "applicationTaskExecutor" })
        public Executor syncExecutor() {
            return new SyncTaskExecutor();
        }
    }

    @Test
    @DisplayName("1. 플레이리스트 생성 – 플레이리스트를 생성하면 레디스에 캐시된다.")
    void 플레이리스트_1부터_10까지_레디스삽입() {
        //given
        Member 회원 = memberRepository.save(new Member("이름"));
        flushAndClear();

        //when
        for (int i = 1; i <= 10; i++) {
            PlayListCreateRequest 요청 = new PlayListCreateRequest("제목" + i, "설명" + i);
            playListService.create(요청, 회원.getId());
        }

        //then
        Map<String, PlayListCreateDto> 해시 = redisHashService.findAllPlayList(PLAY_LIST_HASH_NAME);
        Map<String, PlayListCreateDto> 해시_UNKNOWN = redisHashService.findAllPlayList("UNKNOWN");
        assertThat(해시.size()).isEqualTo(10);
        assertThat(해시_UNKNOWN.size()).isEqualTo(0);
    }

    @Test
    @DisplayName("1. 플레이리스트 생성 – 플레이리스트 캐시가 50개 도달 시 벌크 인서트가 실행되고 해시는 초기화 된다")
    void 플레이리스트_50개_도달시_벌크인서트_실행() {
        //given
        Member 회원 = memberRepository.save(new Member("이름"));
        flushAndClear();

        //when
        for (int i = 1; i <= 50; i++) {
            PlayListCreateRequest 요청 = new PlayListCreateRequest("제목" + i, "설명" + i);
            playListService.create(요청, 회원.getId());
        }

        flushAndClear();

        //then
        Map<String, PlayListCreateDto> 해시 = redisHashService.findAllPlayList(PLAY_LIST_HASH_NAME);
        List<PlayList> 응답 = playListRepository.findAll();
        assertThat(해시.size()).isEqualTo(0);
        assertThat(응답.size()).isEqualTo(50);
    }
}