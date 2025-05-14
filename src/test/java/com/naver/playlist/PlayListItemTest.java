package com.naver.playlist;

import com.naver.playlist.domain.entity.member.Member;
import com.naver.playlist.domain.entity.music.Music;
import com.naver.playlist.domain.entity.playlist.PlayList;
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
import static org.junit.jupiter.api.Assertions.assertEquals;

@Transactional
@ActiveProfiles("test")
@SpringBootTest()
public class PlayListItemTest extends ServiceTest {

    @Autowired
    private PlayListRepository playListRepository;

    @Autowired
    private PlayListItemService playListItemService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MusicRepository musicRepository;

    private Member 회원1;
    private Member 회원2;
    private PlayList 플레이리스트;
    private List<Music> 음악리스트 = new ArrayList<>();

    @BeforeEach
    public void setUp() {
        회원1 = memberRepository.save(new Member("이름"));
        회원2 = memberRepository.save(new Member("이름"));
        플레이리스트 = playListRepository.save(new PlayList("제목", "설명", 회원1));
        for (int i = 0; i < 1001; i++) {
            음악리스트.add(musicRepository.save(new Music("음악" + i)));
        }
        flushAndClear();
    }

    @Test
    @DisplayName("1. 플레이리스트에 노래 삽입 - 플레이리스트에 노래를 1000개까지 삽입할 수 있다.")
    void 플레이리스트에_노래_삽입() {
        //given when then
        for (int i = 0; i < 1000; i++) {
            Music 음악 = 음악리스트.get(i);
            assertDoesNotThrow(() -> playListItemService.create(플레이리스트.getId(), 회원1.getId(), 음악));
        }
    }

    @Test
    @DisplayName("1. 플레이리스트에 노래 삽입 - 엣지 케이스 - 플레이리스트가 없으면 노래를 삽입할 수 없다.")
    void 플레이리스트가_없으면_노래를_만들수없다() {
        //given when then
        PlayListException 예외 = assertThrows(PlayListException.class, () -> {
            Music 음악 = 음악리스트.get(0);
            playListItemService.create(-1L, 회원1.getId(), 음악);
        });

        assertEquals(PLAY_LIST_NOT_EXIST.getCode(), 예외.getCode());
        assertEquals(PLAY_LIST_NOT_EXIST.getErrorMessage(), 예외.getErrorMessage());
    }

    @Test
    @DisplayName("1. 플레이리스트에 노래 삽입 - 엣지 케이스 - 플레이리스트에는 최대 1000개까지 노래를 삽입할 수 있다.")
    void 플레이리스트에는_최대_1000개까지_노래를_만들수없다() throws InterruptedException {
        //given
        for (int i = 0; i < 1000; i++) {
            Music 음악 = 음악리스트.get(i);
            playListItemService.create(플레이리스트.getId(), 회원1.getId(), 음악);
        }

        flushAndClear();

        //when then
        PlayListException 예외 = assertThrows(PlayListException.class, () -> {
            Music 음악 = 음악리스트.get(1000);
            playListItemService.create(플레이리스트.getId(), 회원1.getId(), 음악);
        });

        assertEquals(PLAY_LIST_EXCEED_LIMIT.getCode(), 예외.getCode());
        assertEquals(PLAY_LIST_EXCEED_LIMIT.getErrorMessage(), 예외.getErrorMessage());
    }

    @Test
    @DisplayName("2. 플레이리스트에 노래 동시 삽입 - 분산락을 통해 동시성 문제를 제어한다.")
    @Sql(
            value = "/sql/cleanup_test_data.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    void 분산락으로_플레이리스트_동시성_문제를_해결한다() throws InterruptedException {
        //given
        Music 음악1 = 음악리스트.get(0);
        Music 음악2 = 음악리스트.get(1);

        TestTransaction.flagForCommit();
        TestTransaction.end();

        //when
        TestTransaction.start();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go    = new CountDownLatch(1);

        List<Throwable> thrown = Collections.synchronizedList(new ArrayList<>());

        Runnable 스레드1 = () -> {
            ready.countDown();
            try { go.await(); playListItemService.create(플레이리스트.getId(), 회원1.getId(), 음악1); }
            catch (Throwable e) { thrown.add(e); }
        };
        Runnable 스레드2 = () -> {
            ready.countDown();
            try { go.await(); playListItemService.create(플레이리스트.getId(), 회원1.getId(), 음악2); }
            catch (Throwable e) { thrown.add(e); }
        };

        pool.execute(스레드1);
        pool.execute(스레드2);
        ready.await();
        go.countDown();
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        flushAndClear();

        //then
        assertEquals(thrown.size(), 1);
        PlayListException error = (PlayListException) thrown.get(0);
        assertEquals(PLAY_LIST_NOT_CONCURRENCY.getCode(), error.getCode());
        assertEquals(PLAY_LIST_NOT_CONCURRENCY.getErrorMessage(), error.getErrorMessage());
    }
}