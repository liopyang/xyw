package com.campus.business.controller;

import com.campus.business.common.ApiResponse;
import com.campus.business.common.BusinessException;
import com.campus.business.service.OrderRuleService;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
  private static final List<String> TYPES =
      List.of("CAMPUS_CARD", "CAMPUS_NETWORK", "DRIVING_SCHOOL", "RENEWAL");
  private final NamedParameterJdbcTemplate jdbc;

  @GetMapping("/cards")
  public ApiResponse<List<Map<String, Object>>> cards() {
    List<Map<String, Object>> result = new ArrayList<>();
    for (String type : TYPES) {
      MapSqlParameterSource parameters = new MapSqlParameterSource("type", type);
      Map<String, Object> count =
          jdbc.queryForMap(
              "SELECT SUM(DATE(created_at)=CURDATE()) today,COUNT(*) month FROM biz_order "
                  + "WHERE deleted=0 AND audit_status='CONFIRMED' AND business_type=:type "
                  + "AND created_at>=DATE_FORMAT(CURDATE(),'%Y-%m-01')",
              parameters);
      result.add(
          Map.of(
              "businessType", type,
              "today", number(count.get("today")),
              "month", number(count.get("month"))));
    }
    return ApiResponse.ok(result);
  }

  @GetMapping("/trend")
  public ApiResponse<List<Map<String, Object>>> trend(
      @RequestParam(defaultValue = "CAMPUS_CARD") String businessType,
      @RequestParam(defaultValue = "7d") String range,
      @RequestParam(required = false) String start,
      @RequestParam(required = false) String end,
      @RequestParam(required = false) String startTime,
      @RequestParam(required = false) String endTime) {
    if (!OrderRuleService.BUSINESS_TYPES.contains(businessType)) {
      throw new BusinessException("业务类型不正确");
    }
    LocalDate resolvedStart = parseDate(firstNonBlank(start, startTime));
    LocalDate resolvedEnd = parseDate(firstNonBlank(end, endTime));
    if (resolvedStart == null && resolvedEnd == null) {
      int days =
          "30d".equals(range) ? 30 : "month".equals(range) ? LocalDate.now().getDayOfMonth() : 7;
      resolvedEnd = LocalDate.now();
      resolvedStart = resolvedEnd.minusDays(days - 1L);
    } else if (resolvedStart == null) {
      resolvedStart = resolvedEnd.minusDays(6);
    } else if (resolvedEnd == null) {
      resolvedEnd = LocalDate.now();
    }
    if (resolvedStart.isAfter(resolvedEnd)) {
      throw new BusinessException("开始时间不能晚于结束时间");
    }
    long days = ChronoUnit.DAYS.between(resolvedStart, resolvedEnd) + 1;
    if (days > 366) {
      throw new BusinessException("趋势查询范围不能超过366天");
    }

    MapSqlParameterSource parameters =
        new MapSqlParameterSource()
            .addValue("type", businessType)
            .addValue("start", resolvedStart.atStartOfDay())
            .addValue("end", resolvedEnd.plusDays(1).atStartOfDay());
    Map<LocalDate, Long> counts = new HashMap<>();
    jdbc.query(
        "SELECT DATE(created_at) day,COUNT(*) count FROM biz_order "
            + "WHERE deleted=0 AND audit_status='CONFIRMED' AND business_type=:type "
            + "AND created_at>=:start AND created_at<:end GROUP BY DATE(created_at)",
        parameters,
        (rs, rowNum) -> {
          counts.put(rs.getDate("day").toLocalDate(), rs.getLong("count"));
          return null;
        });
    List<Map<String, Object>> result = new ArrayList<>();
    for (int i = 0; i < days; i++) {
      LocalDate day = resolvedStart.plusDays(i);
      result.add(Map.of("date", day.toString(), "count", counts.getOrDefault(day, 0L)));
    }
    return ApiResponse.ok(result);
  }

  @GetMapping("/agent-ranking")
  public ApiResponse<List<Map<String, Object>>> ranking(
      @RequestParam(defaultValue = "CAMPUS_CARD") String type,
      @RequestParam(defaultValue = "today") String range) {
    if (!"TOTAL".equals(type) && !OrderRuleService.BUSINESS_TYPES.contains(type)) {
      throw new BusinessException("业务类型不正确");
    }
    String typeSql = "TOTAL".equals(type) ? "" : " AND o.business_type=:type";
    MapSqlParameterSource parameters = new MapSqlParameterSource();
    if (!"TOTAL".equals(type)) {
      parameters.addValue("type", type);
    }
    String timeSql =
        "month".equals(range)
            ? "o.created_at>=DATE_FORMAT(CURDATE(),'%Y-%m-01')"
            : "DATE(o.created_at)=CURDATE()";
    String sql =
        "SELECT a.id agentId,a.name agentName,COUNT(o.id) count FROM agent a LEFT JOIN biz_order o"
            + " ON o.agent_id=a.id AND o.deleted=0 AND o.audit_status='CONFIRMED' AND "
            + timeSql
            + typeSql
            + " WHERE a.deleted=0 AND a.status=1 GROUP BY a.id,a.name ORDER BY count DESC LIMIT 10";
    return ApiResponse.ok(jdbc.queryForList(sql, parameters));
  }

  @GetMapping("/todos")
  public ApiResponse<Map<String, Object>> todos() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put(
        "pendingOrders",
        scalar("SELECT COUNT(*) FROM biz_order WHERE deleted=0 AND audit_status='PENDING'"));
    result.put(
        "unexportedNetworks",
        scalar(
            "SELECT COUNT(*) FROM biz_order o JOIN order_campus_network n ON n.order_id=o.id WHERE"
                + " o.deleted=0 AND o.audit_status='CONFIRMED' AND"
                + " n.export_status='NOT_EXPORTED'"));
    result.put(
        "pendingIssues",
        scalar("SELECT COUNT(*) FROM support_issue WHERE deleted=0 AND status='PENDING'"));
    result.put(
        "processingIssues",
        scalar("SELECT COUNT(*) FROM support_issue WHERE deleted=0 AND status='PROCESSING'"));
    result.put(
        "monthlyAgentOrders",
        scalar(
            "SELECT COUNT(*) FROM biz_order WHERE deleted=0 AND audit_status='CONFIRMED' "
                + "AND agent_id IS NOT NULL AND created_at>=DATE_FORMAT(CURDATE(),'%Y-%m-01')"));
    return ApiResponse.ok(result);
  }

  private long scalar(String sql) {
    return Optional.ofNullable(jdbc.getJdbcTemplate().queryForObject(sql, Long.class)).orElse(0L);
  }

  private long number(Object value) {
    return value == null ? 0 : ((Number) value).longValue();
  }

  private LocalDate parseDate(String value) {
    if (value == null) {
      return null;
    }
    try {
      return LocalDate.parse(value);
    } catch (DateTimeParseException e) {
      throw new BusinessException("日期格式应为yyyy-MM-dd");
    }
  }

  private String firstNonBlank(String first, String second) {
    if (first != null && !first.isBlank()) {
      return first;
    }
    return second == null || second.isBlank() ? null : second;
  }
}
