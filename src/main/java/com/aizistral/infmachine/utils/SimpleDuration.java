package com.aizistral.infmachine.utils;

import java.util.concurrent.TimeUnit;

import com.aizistral.infmachine.config.Localization;

import lombok.Value;
import lombok.With;

@Value
public class SimpleDuration {
    @With
    private final long duration;
    private final TimeUnit timeUnit;

    public boolean greaterThan(SimpleDuration other) {
        return this.toMillis() > other.toMillis();
    }

    public long toMillis() {
        return TimeUnit.MILLISECONDS.convert(this.duration, this.timeUnit);
    }

    public String getLocalized() {
        if (this.duration <= 0)
            return Localization.get("time.eternal");

        String key = "time." + this.timeUnit.name().toLowerCase() + (this.duration == 1 ? ".singular" : ".plural");
        return Localization.get(key, this.duration);
    }

    public static SimpleDuration fromString(String str) throws IllegalArgumentException {
        try {
            long time = Long.parseLong(str.substring(0, str.length() - 1));
            String unit = str.substring(str.length() - 1, str.length()).toLowerCase();

            TimeUnit timeUnit;

            switch (unit) {
            case "s":
                timeUnit = TimeUnit.SECONDS;
                break;
            case "m":
                timeUnit = TimeUnit.MINUTES;
                break;
            case "h":
                timeUnit = TimeUnit.HOURS;
                break;
            case "d":
                timeUnit = TimeUnit.DAYS;
                break;
            default:
                throw new IllegalArgumentException("No such unit: " + unit);
            }

            return new SimpleDuration(time, timeUnit);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Could not parse duration string: " + str, ex);
        }
    }

}