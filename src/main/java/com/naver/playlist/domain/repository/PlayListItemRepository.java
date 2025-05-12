package com.naver.playlist.domain.repository;

import com.naver.playlist.domain.entity.playlist.PlayListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayListItemRepository extends JpaRepository<PlayListItem, Long> {

    @Query("select pli from PlayListItem pli " +
            "join fetch pli.playList pl " +
            "join fetch pl.member m " +
            "where pli.id in :ids")
    List<PlayListItem> findPlayListItemForUpdate(List<Long> ids);
}
