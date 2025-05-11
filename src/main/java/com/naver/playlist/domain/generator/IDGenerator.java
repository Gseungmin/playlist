package com.naver.playlist.domain.generator;

import com.naver.playlist.web.exception.CommonException;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.springframework.stereotype.Component;

import java.io.Serializable;

import static com.naver.playlist.web.exception.ExceptionType.SYSTEM_TIME_EXCEPTION;

@Component
public class IDGenerator implements IdentifierGenerator {

    private static final long EPOCH           = 1700000000000L;
    private static final long SERVER_ID       = 1L;
    private static final long SERVER_ID_BITS  = 5L;
    private static final long SEQUENCE_BITS   = 12L;
    private static final long MAX_SERVER_ID   = ~(-1L << SERVER_ID_BITS);
    private static final long MAX_SEQUENCE    = ~(-1L << SEQUENCE_BITS);

    private long sequence      = 0L;
    private long lastTimestamp = -1L;

    private long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    private long waitForNextMillis(long lastTimestamp) {
        long timestamp = currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTimeMillis();
        }
        return timestamp;
    }

    public synchronized long generateId() {
        long timestamp = currentTimeMillis();
        if (timestamp < lastTimestamp) {
            throw new CommonException(
                    SYSTEM_TIME_EXCEPTION.getCode(),
                    SYSTEM_TIME_EXCEPTION.getErrorMessage()
            );
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                timestamp = waitForNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0;
        }

        lastTimestamp = timestamp;

        return ((timestamp - EPOCH) << (SERVER_ID_BITS + SEQUENCE_BITS))
                | (SERVER_ID << SEQUENCE_BITS)
                | sequence;
    }

    @Override
    public Serializable generate(
            SharedSessionContractImplementor session,
            Object entity
    ) throws HibernateException {
        return generateId();
    }
}