
SET FOREIGN_KEY_CHECKS = 0;   -- FK 잠시 해제

TRUNCATE TABLE play_list_item;  -- 가장 먼저 (자식 테이블)
TRUNCATE TABLE play_list;       -- 부모
TRUNCATE TABLE music;           -- FK 대상(자식 없음)
TRUNCATE TABLE member;          -- 최상위

SET FOREIGN_KEY_CHECKS = 1;   -- FK 재활성화