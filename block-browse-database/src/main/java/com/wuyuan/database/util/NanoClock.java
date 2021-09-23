package com.wuyuan.database.util;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

public class NanoClock extends Clock {
    private final Clock clock;

    private final long initialNanos;

    private final Instant initialInstant;

    public NanoClock() {
        this(Clock.systemUTC());

    }

    public NanoClock(final Clock clock) {
    this.clock = clock;

    initialInstant = clock.instant();

    initialNanos = getSystemNanos();

    }

@Override

    public ZoneId getZone() {
        return clock.getZone();

    }

    @Override

    public Clock withZone(ZoneId zone) {
        return new NanoClock(clock.withZone(zone));

    }

    @Override

    public Instant instant() {
        return initialInstant.plusNanos(getSystemNanos() - initialNanos);

    }

    private long getSystemNanos() {
        return System.nanoTime();

    }

    public static void main(String[] args) throws InterruptedException {
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()

                .appendInstant(9).toFormatter();

        for (int i = 0; i < 10; i++) {
            final Clock clock = new NanoClock();

            System.out.println(formatter.format(clock.instant()));

            Thread.sleep(200);

        }
    }
}
