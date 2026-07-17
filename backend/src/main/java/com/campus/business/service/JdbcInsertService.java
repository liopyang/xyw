package com.campus.business.service;

import com.campus.business.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JdbcInsertService {
  private final NamedParameterJdbcTemplate jdbc;

  public long insert(String sql, SqlParameterSource parameters) {
    KeyHolder keys = new GeneratedKeyHolder();
    jdbc.update(sql, parameters, keys, new String[] {"id"});
    Number key = keys.getKey();
    if (key == null) {
      throw new BusinessException("数据保存失败");
    }
    return key.longValue();
  }
}
