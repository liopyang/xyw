package com.campus.business.controller;

import com.campus.business.common.ApiResponse;
import com.campus.business.common.BusinessException;
import com.campus.business.service.JdbcInsertService;
import com.campus.business.service.OperationLogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/mini/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class MiniUserAdminController {
  private final NamedParameterJdbcTemplate jdbc;
  private final JdbcInsertService inserts;
  private final OperationLogService logs;

  public record AgentRoleRequest(
      @NotBlank String roleCode,
      String name,
      @Pattern(regexp = "^1\\d{10}$", message = "手机号格式错误") String phone,
      String level) {}

  @GetMapping
  public ApiResponse<List<Map<String, Object>>> list(
      @RequestParam(required = false) String keyword) {
    String filter =
        keyword == null || keyword.isBlank()
            ? ""
            : " AND (m.nickname LIKE :keyword OR m.phone LIKE :keyword OR u.real_name LIKE :keyword"
                + " OR a.agent_no LIKE :keyword)";
    MapSqlParameterSource p = new MapSqlParameterSource();
    if (!filter.isEmpty()) p.addValue("keyword", "%" + keyword.trim() + "%");
    return ApiResponse.ok(
        jdbc.queryForList(
            "SELECT m.id,m.sys_user_id sysUserId,m.nickname,m.avatar_url"
                + " avatarUrl,m.phone,m.role_code roleCode,m.status,m.last_login_at"
                + " lastLoginAt,a.agent_no agentNo,a.level agentLevel FROM mini_user m JOIN"
                + " sys_user u ON u.id=m.sys_user_id LEFT JOIN agent a ON a.user_id=m.sys_user_id"
                + " AND a.deleted=0 WHERE m.deleted=0"
                + filter
                + " ORDER BY m.id DESC",
            p));
  }

  @PutMapping("/{id}/role")
  @Transactional
  public ApiResponse<Void> role(@PathVariable Long id, @Valid @RequestBody AgentRoleRequest r) {
    if (!Set.of("USER", "AGENT").contains(r.roleCode())) throw new BusinessException("角色不正确");
    Map<String, Object> user = one(id);
    long sysId = ((Number) user.get("sysUserId")).longValue();
    if ("AGENT".equals(r.roleCode())) {
      if (r.name() == null
          || r.name().isBlank()
          || r.phone() == null
          || r.level() == null
          || !Set.of("NORMAL", "ADVANCED", "CAMPUS_LEADER").contains(r.level()))
        throw new BusinessException("请完整填写代理姓名、手机号和等级");
      Long count =
          jdbc.queryForObject(
              "SELECT COUNT(*) FROM agent WHERE user_id=:id AND deleted=0",
              Map.of("id", sysId),
              Long.class);
      if (count == null || count == 0) {
        String no =
            "AG" + LocalDate.now().toString().replace("-", "") + String.format("%06d", sysId);
        inserts.insert(
            "INSERT INTO agent(agent_no,user_id,name,phone,level,status)"
                + " VALUES(:no,:userId,:name,:phone,:level,1)",
            new MapSqlParameterSource()
                .addValue("no", no)
                .addValue("userId", sysId)
                .addValue("name", r.name().trim())
                .addValue("phone", r.phone())
                .addValue("level", r.level()));
      } else
        jdbc.update(
            "UPDATE agent SET name=:name,phone=:phone,level=:level,status=1 WHERE user_id=:userId"
                + " AND deleted=0",
            new MapSqlParameterSource()
                .addValue("userId", sysId)
                .addValue("name", r.name().trim())
                .addValue("phone", r.phone())
                .addValue("level", r.level()));
      jdbc.update(
          "UPDATE mini_user SET phone=:phone,role_code='AGENT' WHERE id=:id",
          Map.of("phone", r.phone(), "id", id));
      jdbc.update(
          "UPDATE sys_user SET phone=:phone,real_name=:name,role_code='AGENT' WHERE id=:id",
          Map.of("phone", r.phone(), "name", r.name().trim(), "id", sysId));
    } else {
      jdbc.update(
          "UPDATE agent SET status=0,deleted=1 WHERE user_id=:id AND deleted=0",
          Map.of("id", sysId));
      jdbc.update("UPDATE mini_user SET role_code='USER' WHERE id=:id", Map.of("id", id));
      jdbc.update("UPDATE sys_user SET role_code='USER' WHERE id=:id", Map.of("id", sysId));
    }
    logs.record("MINI_USER", "ROLE", id, "设置小程序用户角色为 " + r.roleCode());
    return ApiResponse.ok();
  }

  private Map<String, Object> one(Long id) {
    List<Map<String, Object>> rows =
        jdbc.queryForList(
            "SELECT id,sys_user_id sysUserId FROM mini_user WHERE id=:id AND deleted=0",
            Map.of("id", id));
    if (rows.isEmpty()) throw new BusinessException("用户不存在");
    return rows.get(0);
  }
}
