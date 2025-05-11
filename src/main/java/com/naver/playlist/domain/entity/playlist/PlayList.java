package com.naver.playlist.domain.entity.playlist;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.naver.playlist.domain.entity.base.BaseEntity;
import com.naver.playlist.domain.entity.member.Member;
import com.naver.playlist.domain.generator.SnowflakeId;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import static com.naver.playlist.domain.constant.EntityConstants.MAX_PLAY_LIST_DESCRIPTION;
import static com.naver.playlist.domain.constant.EntityConstants.MAX_PLAY_LIST_TITLE;

@Entity
@Getter
@Setter(AccessLevel.NONE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayList extends BaseEntity {

    @Id @SnowflakeId
    @GeneratedValue
    @Column(name = "playListId")
    private Long id;

    @Column(nullable = false, length = MAX_PLAY_LIST_TITLE)
    private String title;

    @Column(nullable = false, length = MAX_PLAY_LIST_DESCRIPTION)
    private String description;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "memberId")
    private Member member;

    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "playList")
    private List<PlayListItem> playListItemList = new ArrayList<>();
}
