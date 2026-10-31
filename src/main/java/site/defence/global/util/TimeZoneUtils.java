package site.defence.global.util;

import site.defence.global.exception.CommonErrorCode;
import site.defence.global.exception.GlobalException;

import java.time.DateTimeException;
import java.time.ZoneId;

public final class TimeZoneUtils {

    private static final ZoneId DEFAULT_ZONE_ID = ZoneId.of("Asia/Seoul");

    private TimeZoneUtils() {
    }

    public static ZoneId parseZoneId(String timeZone) {
        if (timeZone == null || timeZone.isBlank()) {
            return DEFAULT_ZONE_ID;
        }
        try {
            return ZoneId.of(timeZone);
        } catch (DateTimeException e) {
            throw new GlobalException(CommonErrorCode.INVALID_TIME_ZONE);
        }
    }
}
