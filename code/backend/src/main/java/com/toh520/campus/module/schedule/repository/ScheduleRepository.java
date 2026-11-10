package com.toh520.campus.module.schedule.repository;

import com.toh520.campus.common.*;
import com.toh520.campus.module.schedule.dto.ScheduleRequest;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ScheduleRepository {
  private final JdbcTemplate jdbc;

  public ScheduleRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Map<String, Object>> list(long classId, Integer weekday) {
    return jdbc.query(
        "SELECT * FROM class_schedules WHERE class_id=? AND (? IS NULL OR weekday=?) ORDER BY weekday,start_time",
        Rows.INSTANCE,
        classId,
        weekday,
        weekday);
  }

  public int conflicts(Long id, ScheduleRequest request) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM class_schedules WHERE class_id=? AND weekday=? AND start_time<? AND end_time>? AND (? IS NULL OR id<>?)",
        Integer.class,
        request.classId(),
        request.weekday(),
        request.endTime(),
        request.startTime(),
        id,
        id);
  }

  public void save(Long id, ScheduleRequest request) {
    if (id == null)
      jdbc.update(
          "INSERT INTO class_schedules(class_id,course_name,teacher,weekday,start_time,end_time,classroom) VALUES (?,?,?,?,?,?,?)",
          request.classId(),
          request.courseName(),
          request.teacher(),
          request.weekday(),
          request.startTime(),
          request.endTime(),
          request.classroom());
    else
      BusinessException.require(
          jdbc.update(
                  "UPDATE class_schedules SET class_id=?,course_name=?,teacher=?,weekday=?,start_time=?,end_time=?,classroom=? WHERE id=?",
                  request.classId(),
                  request.courseName(),
                  request.teacher(),
                  request.weekday(),
                  request.startTime(),
                  request.endTime(),
                  request.classroom(),
                  id)
              == 1,
          "课程不存在");
  }

  public void delete(long id) {
    BusinessException.require(
        jdbc.update("DELETE FROM class_schedules WHERE id=?", id) == 1, "课程不存在");
  }
}
