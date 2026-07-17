package com.campus.business.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.campus.business.common.ApiResponse;
import com.campus.business.common.BusinessException;
import com.campus.business.service.OperationLogService;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders/campus-network")
@RequiredArgsConstructor
public class ExportController {
  private static final Set<String> EXPORT_STATUSES = Set.of("NOT_EXPORTED", "EXPORTED");
  private static final Set<String> SOURCE_CHANNELS = Set.of("ONLINE", "AGENT", "STORE");
  private final NamedParameterJdbcTemplate jdbc;
  private final OperationLogService logs;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class NetworkExcelRow {
    @ExcelProperty("姓名")
    private String name;

    @ExcelProperty("联系电话")
    private String phone;

    @ExcelProperty("新办号码")
    private String number;

    @ExcelProperty("学号")
    private String studentNo;

    @ExcelProperty("身份证后六位")
    private String idLastSix;
  }

  @RequestMapping(
      value = "/export",
      method = {RequestMethod.GET, RequestMethod.POST})
  @Transactional
  public void export(
      @RequestParam(required = false) Long orderId,
      @RequestParam(required = false) String sourceChannel,
      @RequestParam(required = false) Long agentId,
      @RequestParam(required = false) String startTime,
      @RequestParam(required = false) String endTime,
      @RequestParam(required = false) String start,
      @RequestParam(required = false) String end,
      @RequestParam(required = false) String exportStatus,
      @RequestParam(required = false) String keyword,
      HttpServletResponse response)
      throws Exception {
    if (orderId != null) {
      int confirmed =
          jdbc.update(
              "UPDATE biz_order SET audit_status='CONFIRMED',updated_at=NOW() "
                  + "WHERE id=:id AND business_type='CAMPUS_NETWORK' AND deleted=0",
              Map.of("id", orderId));
      if (confirmed == 0) {
        throw new BusinessException("该订单不是有效的校园网订单");
      }
    }
    ExportQuery query =
        query(
            orderId, sourceChannel, agentId, startTime, endTime, start, end, exportStatus, keyword);
    QueryParts parts = parts(query);
    String from =
        " FROM biz_order o JOIN order_campus_network n ON n.order_id=o.id "
            + "LEFT JOIN agent a ON a.id=o.agent_id";
    List<Long> ids =
        jdbc.query(
            "SELECT o.id" + from + parts.where() + " ORDER BY o.created_at DESC,o.id DESC",
            parts.parameters(),
            (rs, rowNum) -> rs.getLong(1));
    if (ids.isEmpty()) {
      throw new BusinessException(orderId != null ? "该订单当前状态无法导出" : "没有符合导出条件的订单");
    }
    List<NetworkExcelRow> rows =
        jdbc.query(
            "SELECT"
                + " o.customer_name,o.contact_phone,o.business_number,n.student_no,n.id_card_last_six"
                + from
                + parts.where()
                + " ORDER BY o.created_at DESC,o.id DESC",
            parts.parameters(),
            (rs, rowNum) ->
                new NetworkExcelRow(
                    rs.getString(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getString(4),
                    rs.getString(5)));

    String filename = orderId == null ? "校园网订单.xlsx" : "校园网订单_" + orderId + ".xlsx";
    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setCharacterEncoding("utf-8");
    response.setHeader(
        "Content-Disposition",
        "attachment;filename*=UTF-8''"
            + URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20"));
    EasyExcel.write(response.getOutputStream(), NetworkExcelRow.class).sheet("校园网订单").doWrite(rows);

    jdbc.update(
        "UPDATE order_campus_network SET export_status='EXPORTED' WHERE order_id IN (:ids)",
        Map.of("ids", ids));
    logs.record("ORDER", "EXPORT", orderId, "导出校园网订单 " + ids.size() + " 条");
  }

  @GetMapping("/export/count")
  public ApiResponse<Map<String, Long>> count(
      @RequestParam(required = false) String sourceChannel,
      @RequestParam(required = false) Long agentId,
      @RequestParam(required = false) String startTime,
      @RequestParam(required = false) String endTime,
      @RequestParam(required = false) String start,
      @RequestParam(required = false) String end,
      @RequestParam(required = false) String exportStatus,
      @RequestParam(required = false) String keyword) {
    QueryParts parts =
        parts(
            query(
                null,
                sourceChannel,
                agentId,
                startTime,
                endTime,
                start,
                end,
                exportStatus,
                keyword));
    Long count =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM biz_order o JOIN order_campus_network n ON n.order_id=o.id "
                + "LEFT JOIN agent a ON a.id=o.agent_id"
                + parts.where(),
            parts.parameters(),
            Long.class);
    return ApiResponse.ok(Map.of("count", count == null ? 0L : count));
  }

  private ExportQuery query(
      Long orderId,
      String sourceChannel,
      Long agentId,
      String startTime,
      String endTime,
      String start,
      String end,
      String exportStatus,
      String keyword) {
    String resolvedStatus =
        exportStatus == null || exportStatus.isBlank() ? "NOT_EXPORTED" : exportStatus;
    if (!EXPORT_STATUSES.contains(resolvedStatus)) {
      throw new BusinessException("导出状态不正确");
    }
    if (sourceChannel != null
        && !sourceChannel.isBlank()
        && !SOURCE_CHANNELS.contains(sourceChannel)) {
      throw new BusinessException("来源渠道不正确");
    }
    return new ExportQuery(
        orderId,
        blankToNull(sourceChannel),
        agentId,
        firstNonBlank(startTime, start),
        firstNonBlank(endTime, end),
        resolvedStatus,
        blankToNull(keyword));
  }

  private QueryParts parts(ExportQuery query) {
    StringBuilder where =
        new StringBuilder(
            " WHERE o.business_type='CAMPUS_NETWORK' AND o.audit_status='CONFIRMED' AND o.deleted=0"
                + " AND n.export_status=:exportStatus");
    MapSqlParameterSource parameters =
        new MapSqlParameterSource("exportStatus", query.exportStatus());
    if (query.orderId() != null) {
      where.append(" AND o.id=:orderId");
      parameters.addValue("orderId", query.orderId());
    }
    if (query.sourceChannel() != null) {
      where.append(" AND o.source_channel=:sourceChannel");
      parameters.addValue("sourceChannel", query.sourceChannel());
    }
    if (query.agentId() != null) {
      where.append(" AND o.agent_id=:agentId");
      parameters.addValue("agentId", query.agentId());
    }
    LocalDate start = parseDate(query.startTime());
    LocalDate end = parseDate(query.endTime());
    if (start != null && end != null && start.isAfter(end)) {
      throw new BusinessException("开始时间不能晚于结束时间");
    }
    if (start != null) {
      where.append(" AND o.created_at>=:startTime");
      parameters.addValue("startTime", start.atStartOfDay());
    }
    if (end != null) {
      where.append(" AND o.created_at<:endTime");
      parameters.addValue("endTime", end.plusDays(1).atStartOfDay());
    }
    if (query.keyword() != null) {
      where.append(
          " AND (o.order_no LIKE :keyword OR o.customer_name LIKE :keyword OR o.contact_phone LIKE"
              + " :keyword OR o.business_number LIKE :keyword OR a.name LIKE :keyword OR o.remark"
              + " LIKE :keyword)");
      parameters.addValue("keyword", "%" + query.keyword().trim() + "%");
    }
    return new QueryParts(where.toString(), parameters);
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
    return first != null && !first.isBlank() ? first : blankToNull(second);
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private record ExportQuery(
      Long orderId,
      String sourceChannel,
      Long agentId,
      String startTime,
      String endTime,
      String exportStatus,
      String keyword) {}

  private record QueryParts(String where, MapSqlParameterSource parameters) {}
}
