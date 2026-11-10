package com.toh520.campus.common;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.RowMapper;

/** 数据库列转为明确的 JSON lowerCamelCase；敏感列由各 Repository 的投影排除。 */
public final class Rows implements RowMapper<Map<String, Object>> {
  public static final Rows INSTANCE = new Rows();

  private Rows() {}

  @Override
  public Map<String, Object> mapRow(ResultSet rs, int rowNum) throws SQLException {
    var result = new LinkedHashMap<String, Object>();
    var meta = rs.getMetaData();
    for (int index = 1; index <= meta.getColumnCount(); index++) {
      StringBuilder name = new StringBuilder();
      boolean upper = false;
      for (char c : meta.getColumnLabel(index).toCharArray()) {
        if (c == '_') upper = true;
        else {
          name.append(upper ? Character.toUpperCase(c) : c);
          upper = false;
        }
      }
      result.put(name.toString(), rs.getObject(index));
    }
    return result;
  }
}
