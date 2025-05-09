package com.naver.playlist.domain.entity.member;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.naver.playlist.domain.entity.base.BaseEntity;
import com.naver.playlist.domain.entity.playlist.PlayList;
import com.naver.playlist.generator.SnowflakeId;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter(AccessLevel.NONE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Id @SnowflakeId
    @GeneratedValue
    @Column(name = "memberId")
    private Long id;

    @JsonIgnore
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "member")
    private List<PlayList> playListList = new ArrayList<>();
}
