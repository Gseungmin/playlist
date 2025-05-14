package com.naver.playlist.domain.service;

import com.naver.playlist.domain.entity.music.Music;
import com.naver.playlist.domain.repository.MusicRepository;
import com.naver.playlist.web.exception.entity.PlayListException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.naver.playlist.web.exception.ExceptionType.MUSIC_NOT_EXIST;

@Service
@RequiredArgsConstructor
@Transactional
public class MusicService {

    private final MusicRepository musicRepository;

    public Music getMusic(
            Long musicId
    ) {
        Optional<Music> optional =
                musicRepository.findById(musicId);

        if (optional.isEmpty()) {
            throw new PlayListException(
                    MUSIC_NOT_EXIST.getCode(),
                    MUSIC_NOT_EXIST.getErrorMessage()
            );
        }

        return optional.get();
    }
}
