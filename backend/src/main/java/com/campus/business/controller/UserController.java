package com.campus.business.controller;

import com.campus.business.common.ApiResponse;
import com.campus.business.common.BusinessException;
import com.campus.business.common.PageResult;
import com.campus.business.security.SecurityUtils;
import com.campus.business.service.JdbcInsertService;
import com.campus.business.service.OperationLogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class UserController {
  private final NamedParameterJdbcTemplate jdbc;
  private final JdbcInsertService inserts;
  private final PasswordEncoder encoder;
  private final OperationLogService logs;

  public record UserRequest(
      @NotBlank String username,
      @NotBlank String realName,
      @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "手机号格式错误") String phone,
      String password) {}

  public record StatusRequest(@NotNull @Min(0) @Max(1) Integer status) {}

  @GetMapping
  public ApiResponse<PageResult<Map<String, Object>>> list(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(required = false) String keyword) {
    page = Math.max(1, page);
    pageSize = Math.min(100, Math.max(1, pageSize));
    String where = " WHERE deleted=0 AND role_code IN ('OWNER','ADMIN')";
    MapSqlParameterSource parameters = new MapSqlParameterSource();
    if (keyword != null && !keyword.isBlank()) {
      where += " AND (username LIKE :keyword OR real_name LIKE :keyword OR phone LIKE :keyword)";
      parameters.addValue("keyword", "%" + keyword.trim() + "%");
    }
    long total =
        jdbc.queryForObject("SELECT COUNT(*) FROM sys_user" + where, parameters, Long.class);
    parameters.addValue("offset", (page - 1) * pageSize).addValue("size", pageSize);
    return ApiResponse.ok(
        new PageResult<>(
            jdbc.queryForList(
                "SELECT id,username,real_name realName,phone,role_code role,status,created_at"
                    + " createdAt FROM sys_user"
                    + where
                    + " ORDER BY created_at DESC,id DESC LIMIT :offset,:size",
                parameters),
            total,
            page,
            pageSize));
  }

  @PostMapping
  @Transactional
  public ApiResponse<Map<String, Long>> create(@Valid @RequestBody UserRequest request) {
    validatePassword(request.password(), true);
    MapSqlParameterSource parameters =
        parameters(request).addValue("password", encoder.encode(request.password()));
    try {
      long id =
          inserts.insert(
              "INSERT INTO sys_user(username,phone,password,real_name,role_code,status) "
                  + "VALUES(:username,:phone,:password,:name,'ADMIN',1)",
              parameters);
      logs.record("USER", "CREATE", id, "新增管理员 " + request.username());
      return ApiResponse.ok(Map.of("id", id));
    } catch (DataIntegrityViolationException e) {
      throw new BusinessException("账号或手机号已存在");
    }
  }

  @PutMapping("/{id}")
  @Transactional
  public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
    protect(id);
    validatePassword(request.password(), false);
    MapSqlParameterSource parameters = parameters(request).addValue("id", id);
    String passwordSql = "";
    if (request.password() != null && !request.password().isBlank()) {
      parameters.addValue("password", encoder.encode(request.password()));
      passwordSql = ",password=:password";
    }
    try {
      jdbc.update(
          "UPDATE sys_user SET username=:username,phone=:phone,real_name=:name"
              + passwordSql
              + " WHERE id=:id AND role_code='ADMIN'",
          parameters);
    } catch (DataIntegrityViolationException e) {
      throw new BusinessException("账号或手机号已存在");
    }
    logs.record("USER", "UPDATE", id, "编辑管理员");
    return ApiResponse.ok();
  }

  @PutMapping("/{id}/status")
  public ApiResponse<Void> status(
      @PathVariable Long id, @Valid @RequestBody StatusRequest request) {
    protect(id);
    jdbc.update(
        "UPDATE sys_user SET status=:status WHERE id=:id AND role_code='ADMIN'",
        Map.of("id", id, "status", request.status()));
    logs.record(
        "USER",
        request.status() == 1 ? "ENABLE" : "DISABLE",
        id,
        request.status() == 1 ? "启用管理员" : "停用管理员");
    return ApiResponse.ok();
  }

  private MapSqlParameterSource parameters(UserRequest request) {
    return new MapSqlParameterSource()
        .addValue("username", request.username().trim())
        .addValue("phone", request.phone())
        .addValue("name", request.realName().trim());
  }

  private void protect(Long id) {
    if (id.equals(SecurityUtils.current().id())) {
      throw new BusinessException("不能通过此页面修改当前账号");
    }
    Long count =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM sys_user WHERE id=:id AND deleted=0 AND role_code='ADMIN'",
            Map.of("id", id),
            Long.class);
    if (count == null || count == 0) {
      throw new BusinessException("管理员不存在");
    }
  }

  private void validatePassword(String password, boolean required) {
    if (!required && (password == null || password.isBlank())) {
      return;
    }
    if (password == null || password.length() < 8 || password.length() > 72) {
      throw new BusinessException("密码长度应为8至72位");
    }
  }
}
