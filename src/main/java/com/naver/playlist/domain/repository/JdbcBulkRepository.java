package com.naver.playlist.domain.repository;

import com.naver.playlist.domain.dto.playlist.req.ReorderPlayListItemsRequest;
import com.naver.playlist.domain.dto.redis.PlayListCreateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class JdbcBulkRepository {

    private static final String INSERT_SQL = """
        INSERT INTO play_list
          (play_list_id, member_id, title, description, created_date, last_modified_date)
        VALUES
          (:id, :memberId, :title, :description, :createdDate, :lastModifiedDate)
        ON DUPLICATE KEY UPDATE play_list_id = play_list_id
    """;

    private static final String UPDATE_SQL = """
        UPDATE play_list_item
           SET position           = :position,
               last_modified_date = :lastModifiedDate
         WHERE play_list_item_id  = :id
    """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public void bulkInsert(List<PlayListCreateDto> requests) {

        SqlParameterSource[] batch = requests.stream()
                .map(req -> new MapSqlParameterSource()
                        .addValue("id",               req.getPlaylistId())
                        .addValue("memberId",         req.getMemberId())
                        .addValue("title",            req.getTitle())
                        .addValue("description",      req.getDescription())
                        .addValue("createdDate",      req.getCreatedDate())
                        .addValue("lastModifiedDate", req.getCreatedDate()))
                .toArray(SqlParameterSource[]::new);

        jdbcTemplate.batchUpdate(INSERT_SQL, batch);
    }

    public void bulkUpdatePosition(List<ReorderPlayListItemsRequest> requests) {
        LocalDateTime now = LocalDateTime.now();

        SqlParameterSource[] batch = requests.stream()
                .map(r -> new MapSqlParameterSource()
                        .addValue("id",               r.getPlayListItemId())
                        .addValue("position",         r.getPosition())
                        .addValue("lastModifiedDate", now))
                .toArray(SqlParameterSource[]::new);

        jdbcTemplate.batchUpdate(UPDATE_SQL, batch);
    }
}
