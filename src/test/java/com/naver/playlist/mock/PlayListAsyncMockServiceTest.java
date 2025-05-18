package com.naver.playlist.mock;

import com.naver.playlist.domain.dto.playlist.res.PlayListCreateResponse;
import com.naver.playlist.domain.dto.redis.PlayListCreateDto;
import com.naver.playlist.domain.redis.RedisHashService;
import com.naver.playlist.domain.repository.JdbcBulkRepository;
import com.naver.playlist.domain.service.PlayListAsyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PlayListAsyncMockServiceTest {

    @Mock JdbcBulkRepository bulkRepository;
    @Mock RedisHashService redisHashService;
    private PlayListAsyncService asyncService;

    @BeforeEach
    void setUp() {
        asyncService = new PlayListAsyncService(bulkRepository, redisHashService);
    }

    @Test
    @DisplayName("1. 데이터베이스 접근 예외 – 데이터베이스 접근 예외시 실패큐에 저장한다.")
    void 데이터베이스_접근_예외시_실패큐에_저장한다() {
        // given
        List<PlayListCreateDto> list = List.of(
                new PlayListCreateDto(1L, 1L, "", "", LocalDateTime.now().toString())
        );

        PlayListCreateResponse dto = new PlayListCreateResponse("스냅샷 키", list);

        doThrow(new DataAccessResourceFailureException("데이터베이스 접근 에러"))
                .when(bulkRepository).bulkInsert(list);

        // when
        asyncService.bulkInsertAsync(dto);

        // then
        Map<String, List<PlayListCreateDto>> 실패큐 = asyncService.getFallbackCache();
        assertThat(실패큐.size()).isEqualTo(1);
        assertThat(실패큐.get("스냅샷 키").size()).isEqualTo(1);
        assertThat(실패큐.get("스냅샷 키 NULL")).isNull();
    }

    @Test
    @DisplayName("2. 데이터베이스 재접근 예외 – 데이터베이스 재접근 예외시 여전히 실패큐에 저장한다.")
    void 재시도_실패_시_큐_유지() {
        // given
        List<PlayListCreateDto> list = List.of(
                new PlayListCreateDto(1L, 1L, "", "", LocalDateTime.now().toString())
        );
        PlayListCreateResponse dto = new PlayListCreateResponse("스냅샷 키", list);

        doThrow(new DataAccessResourceFailureException("데이터베이스 접근 에러"))
                .when(bulkRepository).bulkInsert(list);

        asyncService.bulkInsertAsync(dto);

        // when
        asyncService.retryFallbackCache();

        // then
        Map<String, List<PlayListCreateDto>> 실패큐 = asyncService.getFallbackCache();
        assertThat(실패큐).containsKey("스냅샷 키");
        assertThat(실패큐.get("스냅샷 키")).hasSize(1);
        verify(bulkRepository, times(2)).bulkInsert(list);
        verify(redisHashService, never()).deleteSnapshot("스냅샷 키");
    }

    @Test
    @DisplayName("3. 데이터베이스 재접근 성공 – 데이터베이스 재접근 성공시 실패큐에서 제거된다.")
    void 재시도_성공_시_큐_삭제() {
        // given
        List<PlayListCreateDto> list = List.of(
                new PlayListCreateDto(1L, 1L, "", "", LocalDateTime.now().toString())
        );
        PlayListCreateResponse dto = new PlayListCreateResponse("스냅샷 키", list);

        doThrow(new DataAccessResourceFailureException("데이터베이스 접근 에러"))
                .doNothing()
                .when(bulkRepository).bulkInsert(list);

        asyncService.bulkInsertAsync(dto);

        // when
        asyncService.retryFallbackCache();

        // then
        assertThat(asyncService.getFallbackCache()).isEmpty();
        verify(bulkRepository, times(2)).bulkInsert(list);
        verify(redisHashService).deleteSnapshot("스냅샷 키");
    }
}