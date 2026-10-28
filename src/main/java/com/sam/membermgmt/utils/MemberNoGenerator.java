package com.sam.membermgmt.utils;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class MemberNoGenerator {

    private final AtomicInteger sequence = new AtomicInteger(1);
    private volatile String currentDate = "";

    public synchronized String generate() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        if (!today.equals(currentDate)) {
            currentDate = today;
            sequence.set(1);
        }
        return "M" + today + String.format("%04d", sequence.getAndIncrement());
    }
}
