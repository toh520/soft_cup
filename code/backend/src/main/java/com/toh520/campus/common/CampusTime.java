package com.toh520.campus.common;

import java.time.*;

/** 相对日期与业务时间统一使用上海时区，拒绝无具体时间的请假草稿。 */
public final class CampusTime {
  public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

  private CampusTime() {}

  public static OffsetDateTime now() {
    return OffsetDateTime.now(ZONE);
  }

  public static OffsetDateTime parse(String value) {
    try {
      try {
        return OffsetDateTime.parse(value).atZoneSameInstant(ZONE).toOffsetDateTime();
      } catch (java.time.format.DateTimeParseException ignored) {
        return LocalDateTime.parse(value).atZone(ZONE).toOffsetDateTime();
      }
    } catch (Exception e) {
      throw new BusinessException(400, "请填写明确的日期和时间");
    }
  }
}
