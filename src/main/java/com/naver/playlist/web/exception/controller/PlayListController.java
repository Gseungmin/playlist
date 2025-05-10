package com.naver.playlist.web.exception.controller;

import com.naver.playlist.domain.dto.playlist.req.CreatePlayListItemRequest;
import com.naver.playlist.domain.dto.playlist.req.DeletePlayListItemRequest;
import com.naver.playlist.domain.dto.playlist.req.PlayListCreateRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/playlist")
@RequiredArgsConstructor
public class PlayListController {

    @PostMapping
    public void create(
            HttpServletRequest request,
            @RequestBody PlayListCreateRequest dto
    ) {
        return;
    }

    @PatchMapping("/{playlistId}")
    public void update(
            HttpServletRequest request,
            @PathVariable Long playlistId
    ) {
        return;
    }

    @DeleteMapping("/{playlistId}")
    public void delete(
            HttpServletRequest request,
            @PathVariable Long playlistId
    ) {
        return;
    }

    @GetMapping("/{playlistId}/items")
    public void getItemList(
            HttpServletRequest request,
            @PathVariable Long playlistId,
            @RequestParam(required = false) Long cursor
    ) {
        return;
    }

    @PostMapping("/{playlistId}/items")
    public void addItemToPlaylist(
            HttpServletRequest request,
            @PathVariable Long playlistId,
            @RequestBody CreatePlayListItemRequest dto
    ) {
        return;
    }

    @DeleteMapping("/{playlistId}/items")
    public void deleteItemFromPlaylist(
            HttpServletRequest request,
            @PathVariable Long playlistId,
            @RequestBody DeletePlayListItemRequest dto
    ) {
        return;
    }
}

