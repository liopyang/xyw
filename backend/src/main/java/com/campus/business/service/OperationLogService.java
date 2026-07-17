package com.campus.business.service;

import com.campus.business.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OperationLogService {
  private final JdbcTemplate jdbc;
  private final HttpServletRequest request;

  public void record(String module, String type, Long target, String description) {
    var user = SecurityUtils.current();
    jdbc.update(
        "INSERT INTO"
            + " operation_log(operator_id,operator_name,module,operation_type,target_id,operation_description,ip_address)"
            + " VALUES(?,?,?,?,?,?,?)",
        user.id(),
        user.realName(),
        module,
        type,
        target,
        description,
        request.getRemoteAddr());
  }
}
