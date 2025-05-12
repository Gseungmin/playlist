package com.naver.playlist.domain.repository;

import com.naver.playlist.domain.entity.playlist.PlayList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayListRepository extends JpaRepository<PlayList, Long> {
}
