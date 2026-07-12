package com.campus.business.controller;

import com.campus.business.common.ApiResponse;
import com.campus.business.common.BusinessException;
import com.campus.business.common.PageResult;
import com.campus.business.service.JdbcInsertService;
import com.campus.business.service.OperationLogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {
    private static final Set<String> LEVELS = Set.of("NORMAL", "ADVANCED", "CAMPUS_LEADER");
    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcInsertService inserts;
    private final PasswordEncoder encoder;
    private final OperationLogService logs;

    public record AgentRequest(
            @NotBlank(message = "请填写代理姓名") String name,
            @NotBlank(message = "请填写手机号") @Pattern(regexp = "^1\\d{10}$", message = "手机号格式错误") String phone,
            @NotBlank(message = "请选择代理等级") String level,
            String password
    ) {
    }

    public record StatusRequest(
            @NotNull @Min(0) @Max(1) Integer status
    ) {
    }

    @GetMapping
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status
    ) {
        page = Math.max(1, page);
        pageSize = Math.min(100, Math.max(1, pageSize));
        StringBuilder where = new StringBuilder(" WHERE a.deleted=0");
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        if (status != null) {
            where.append(" AND a.status=:status");
            parameters.addValue("status", status);
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (a.agent_no LIKE :keyword OR a.name LIKE :keyword OR a.phone LIKE :keyword)");
            parameters.addValue("keyword", "%" + keyword.trim() + "%");
        }
        String from = " FROM agent a";
        long total = jdbc.queryForObject("SELECT COUNT(*)" + from + where, parameters, Long.class);
        parameters.addValue("offset", (page - 1) * pageSize).addValue("size", pageSize);
        String sql = "SELECT a.id,a.agent_no agentNo,a.name,a.phone,a.level,a.status,a.created_at createdAt," +
                "SUM(CASE WHEN o.audit_status='CONFIRMED' AND o.deleted=0 AND DATE(o.created_at)=CURDATE() THEN 1 ELSE 0 END) todayOrders," +
                "SUM(CASE WHEN o.audit_status='CONFIRMED' AND o.deleted=0 AND o.created_at>=DATE_FORMAT(CURDATE(),'%Y-%m-01') THEN 1 ELSE 0 END) monthOrders " +
                "FROM agent a LEFT JOIN biz_order o ON o.agent_id=a.id" + where +
                " GROUP BY a.id ORDER BY a.created_at DESC LIMIT :offset,:size";
        return ApiResponse.ok(new PageResult<>(jdbc.queryForList(sql, parameters), total, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.ok(one(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    @Transactional
    public ApiResponse<Map<String, Long>> create(@Valid @RequestBody AgentRequest request) {
        validateLevel(request.level());
        if (request.password() == null || request.password().length() < 8 || request.password().length() > 72) {
            throw new BusinessException("初始密码长度应为8至72位");
        }
        ensurePhoneAvailable(request.phone(), null);
        MapSqlParameterSource userParameters = new MapSqlParameterSource()
                .addValue("phone", request.phone())
                .addValue("password", encoder.encode(request.password()))
                .addValue("name", request.name().trim());
        try {
            long userId = inserts.insert(
                    "INSERT INTO sys_user(username,phone,password,real_name,role_code,status) " +
                            "VALUES(:phone,:phone,:password,:name,'AGENT',1)",
                    userParameters
            );
            String agentNo = "AG" + LocalDate.now().toString().replace("-", "") + String.format("%06d", userId);
            MapSqlParameterSource agentParameters = new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("agentNo", agentNo)
                    .addValue("name", request.name().trim())
                    .addValue("phone", request.phone())
                    .addValue("level", request.level());
            long id = inserts.insert(
                    "INSERT INTO agent(agent_no,user_id,name,phone,level,status) " +
                            "VALUES(:agentNo,:userId,:name,:phone,:level,1)",
                    agentParameters
            );
            logs.record("AGENT", "CREATE", id, "新增代理 " + request.name());
            return ApiResponse.ok(Map.of("id", id));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("手机号已被使用");
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    @Transactional
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody AgentRequest request) {
        validateLevel(request.level());
        Map<String, Object> agent = one(id);
        Long userId = ((Number) agent.get("userId")).longValue();
        ensurePhoneAvailable(request.phone(), userId);
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("userId", userId)
                .addValue("name", request.name().trim())
                .addValue("phone", request.phone())
                .addValue("level", request.level());
        try {
            jdbc.update("UPDATE agent SET name=:name,phone=:phone,level=:level WHERE id=:id", parameters);
            jdbc.update(
                    "UPDATE sys_user SET username=:phone,real_name=:name,phone=:phone WHERE id=:userId",
                    parameters
            );
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("手机号已被使用");
        }
        logs.record("AGENT", "UPDATE", id, "编辑代理资料");
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('OWNER')")
    @Transactional
    public ApiResponse<Void> status(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        Map<String, Object> agent = one(id);
        Long userId = ((Number) agent.get("userId")).longValue();
        jdbc.update("UPDATE agent SET status=:status WHERE id=:id", Map.of("id", id, "status", request.status()));
        jdbc.update("UPDATE sys_user SET status=:status WHERE id=:userId", Map.of("userId", userId, "status", request.status()));
        logs.record(
                "AGENT",
                request.status() == 1 ? "ENABLE" : "DISABLE",
                id,
                request.status() == 1 ? "启用代理" : "停用代理"
        );
        return ApiResponse.ok();
    }

    private Map<String, Object> one(Long id) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id,agent_no agentNo,user_id userId,name,phone,level,status,created_at createdAt " +
                        "FROM agent WHERE id=:id AND deleted=0",
                Map.of("id", id)
        );
        if (rows.isEmpty()) {
            throw new BusinessException("代理不存在");
        }
        return rows.get(0);
    }

    private void ensurePhoneAvailable(String phone, Long ownUserId) {
        String sql = "SELECT COUNT(*) FROM sys_user WHERE (username=:phone OR phone=:phone)";
        MapSqlParameterSource parameters = new MapSqlParameterSource("phone", phone);
        if (ownUserId != null) {
            sql += " AND id<>:ownUserId";
            parameters.addValue("ownUserId", ownUserId);
        }
        Long count = jdbc.queryForObject(sql, parameters, Long.class);
        if (count != null && count > 0) {
            throw new BusinessException("手机号已被使用");
        }
    }

    private void validateLevel(String level) {
        if (!LEVELS.contains(level)) {
            throw new BusinessException("代理等级不正确");
        }
    }
}
