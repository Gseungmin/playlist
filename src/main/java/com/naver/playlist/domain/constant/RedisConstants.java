package com.naver.playlist.domain.constant;

public class RedisConstants {

    /*KEY*/
    public static final String PLAY_LIST_HASH_NAME = "playlist:create";
    public static final String LOCK_KEY = "playlist:lock:";

    /*TIME OUT*/
    public static final int DUPLICATION_TIME_OUT_DAY = 1;
    public static final int LOCK_NO_WAIT = 0;
    public static final int LOCK_MAX_TIME = 2;
}