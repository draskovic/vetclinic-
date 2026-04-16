package com.softart.vetclinic.config;

import com.softart.vetclinic.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class BookingRateLimiter {

    private static final int MAX_PER_PHONE = 5;   // max 5 po telefonu na sat
    private static final int MAX_PER_IP = 20;      // max 20 po IP na sat

    private final ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    public void checkLimit(String key) {
        int limit = key.startsWith("phone:") ? MAX_PER_PHONE : MAX_PER_IP;

        AtomicInteger counter = counters.computeIfAbsent(key, k -> new AtomicInteger(0));
        int current = counter.incrementAndGet();

        if (current > limit) {
            log.warn("Rate limit prekoračen za ključ: {}, count: {}", key, current);
            throw new BadRequestException("Previše zahteva. Pokušajte ponovo za sat vremena.");
        }
    }

    @Scheduled(fixedRate = 3600000) // Čišćenje svakih sat vremena
    public void resetCounters() {
        int size = counters.size();
        counters.clear();
        if (size > 0) {
            log.debug("BookingRateLimiter: obrisano {} brojača", size);
        }
    }
}
