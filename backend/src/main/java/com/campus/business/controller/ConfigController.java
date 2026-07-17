package com.campus.business.controller;

import com.campus.business.common.*;
import com.campus.business.security.SecurityUtils;
import com.campus.business.service.OperationLogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/configs")
@RequiredArgsConstructor
public class ConfigController {
  private final JdbcTemplate jdbc;
  private final OperationLogService logs;

  public record ValueRequest(@NotBlank String value) {}

  @GetMapping
  public ApiResponse<List<Map<String, Object>>> list() {
    return ApiResponse.ok(
        jdbc.queryForList(
            "SELECT id,config_key configKey,config_value configValue,description,updated_at"
                + " updatedAt FROM business_config ORDER BY id"));
  }

  @PutMapping("/{key}")
  @PreAuthorize("hasRole('OWNER')")
  public ApiResponse<Void> update(@PathVariable String key, @Valid @RequestBody ValueRequest r) {
    int n =
        jdbc.update(
            "UPDATE business_config SET config_value=?,updated_by=? WHERE config_key=?",
            r.value(),
            SecurityUtils.current().id(),
            key);
    if (n == 0) throw new BusinessException("配置项不存在");
    logs.record("CONFIG", "UPDATE", null, "修改配置 " + key);
    return ApiResponse.ok();
  }
}
