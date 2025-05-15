package com.naver.playlist.domain.repository;

import com.naver.playlist.domain.entity.playlist.PlayListItem;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.naver.playlist.domain.constant.EntityConstants.MAX_PLAY_PAGE_SIZE;
import static com.naver.playlist.domain.entity.playlist.QPlayListItem.playListItem;

@Repository
@RequiredArgsConstructor
public class PaginationRepository {

	private final JPAQueryFactory query;

	public List<PlayListItem> getPlayListItemByCursor(
			Long cursor,
			Long playListId
	) {
		return query.select(playListItem)
				.from(playListItem)
				.join(playListItem.music).fetchJoin()
				.where(
						getId(playListId),
						getAfter(cursor)
				)
				.orderBy(playListItem.position.asc())
				.limit(MAX_PLAY_PAGE_SIZE + 1)
				.fetch();
	}

	private BooleanExpression getAfter(Long cursor) {
		return cursor == null ? null : playListItem.position.gt(cursor);
	}

	private BooleanExpression getId(Long playListId) {
		return playListId == null ? null : playListItem.playList.id.eq(playListId);
	}
}
