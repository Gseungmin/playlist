package com.naver.playlist.domain.repository;

import com.naver.playlist.domain.dto.projection.PlayListStatProjection;
import com.naver.playlist.domain.entity.playlist.PlayList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlayListRepository extends JpaRepository<PlayList, Long> {

    @Query("""
            select  pl                            as playList,
                    count(pi)                     as count,
                    coalesce(max(pi.position), 0) as lastOrder
            from    PlayList pl
            left join PlayListItem pi on pi.playList = pl
            where   pl.id         = :playListId
              and   pl.member.id  = :memberId
            group by pl
    """)
    Optional<PlayListStatProjection> findPlayListWithStat(
            Long playListId,
            Long memberId
    );
}
