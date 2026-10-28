package com.sam.membermgmt.utils;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class OrderNoGenerator {

    private final AtomicInteger sequence = new AtomicInteger(1);
    private volatile String currentMinute = "";

    public synchronized String generate() {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        if (!now.equals(currentMinute)) {
            currentMinute = now;
            sequence.set(1);
        }
        return "SAM" + now + String.format("%04d", sequence.getAndIncrement());
    }
}
