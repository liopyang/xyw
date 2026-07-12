package com.campus.business.controller;

import com.campus.business.common.ApiResponse;
import com.campus.business.common.BusinessException;
import com.campus.business.common.PageResult;
import com.campus.business.entity.BizOrder;
import com.campus.business.mapper.BizOrderMapper;
import com.campus.business.security.SecurityUtils;
import com.campus.business.service.OperationLogService;
import com.campus.business.service.OrderRuleService;
import com.campus.business.service.ReferenceNumberService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final NamedParameterJdbcTemplate jdbc;
    private final BizOrderMapper mapper;
    private final OperationLogService logs;
    private final OrderRuleService rules;
    private final ReferenceNumberService numbers;

    public record OrderView(
            Long id,
            String orderNo,
            String businessType,
            String name,
            String phone,
            String businessNumber,
            String sourceChannel,
            String agentName,
            String auditStatus,
            String exportStatus,
            LocalDateTime createdAt,
            String remark,
            boolean deleted
    ) {
    }

    public record OrderRequest(
            @NotBlank(message = "请选择业务类型") String businessType,
            @NotBlank(message = "请填写姓名") @Size(max = 50, message = "姓名不能超过50个字符") String name,
            @NotBlank(message = "请填写联系电话") @Pattern(regexp = "^1\\d{10}$", message = "手机号格式错误") String phone,
            String businessNumber,
            @NotBlank(message = "请选择来源渠道") String sourceChannel,
            Long agentId,
            String auditStatus,
            @Size(max = 500, message = "备注不能超过500个字符") String remark,
            String studentNo,
            String idCardLastSix,
            String licenseType,
            String classType,
            @PositiveOrZero(message = "缴费数额不能小于0") BigDecimal paymentAmount,
            @PositiveOrZero(message = "续费金额不能小于0") BigDecimal renewalAmount
    ) {
    }

    @GetMapping
    public ApiResponse<PageResult<OrderView>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String sourceChannel,
            @RequestParam(required = false) String auditStatus,
            @RequestParam(required = false) String exportStatus,
            @RequestParam(required = false) Long agentId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") boolean includeDeleted
    ) {
        if (includeDeleted && !"OWNER".equals(SecurityUtils.current().role())) {
            throw new BusinessException("只有老板可以查看已作废订单");
        }
        page = Math.max(1, page);
        pageSize = Math.min(100, Math.max(1, pageSize));
        StringBuilder where = new StringBuilder(includeDeleted ? " WHERE 1=1" : " WHERE o.deleted=0");
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        add(where, parameters, "businessType", businessType, "o.business_type");
        add(where, parameters, "sourceChannel", sourceChannel, "o.source_channel");
        add(where, parameters, "auditStatus", auditStatus, "o.audit_status");
        add(where, parameters, "exportStatus", exportStatus, "n.export_status");
        if (agentId != null) {
            where.append(" AND o.agent_id=:agentId");
            parameters.addValue("agentId", agentId);
        }
        addDateRange(where, parameters, startTime, endTime);
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (o.order_no LIKE :keyword OR o.customer_name LIKE :keyword OR o.contact_phone LIKE :keyword " +
                    "OR o.business_number LIKE :keyword OR a.name LIKE :keyword OR o.remark LIKE :keyword)");
            parameters.addValue("keyword", "%" + keyword.trim() + "%");
        }

        String from = " FROM biz_order o LEFT JOIN agent a ON a.id=o.agent_id " +
                "LEFT JOIN order_campus_network n ON n.order_id=o.id";
        long total = Optional.ofNullable(jdbc.queryForObject("SELECT COUNT(*)" + from + where, parameters, Long.class)).orElse(0L);
        parameters.addValue("offset", (page - 1) * pageSize).addValue("size", pageSize);
        String sql = "SELECT o.id,o.order_no,o.business_type,o.customer_name,o.contact_phone,o.business_number," +
                "o.source_channel,a.name agent_name,o.audit_status,n.export_status,o.created_at,o.remark,o.deleted" +
                from + where + " ORDER BY o.created_at DESC,o.id DESC LIMIT :offset,:size";
        List<OrderView> rows = jdbc.query(sql, parameters, (rs, rowNum) -> new OrderView(
                rs.getLong("id"),
                rs.getString("order_no"),
                rs.getString("business_type"),
                rs.getString("customer_name"),
                rs.getString("contact_phone"),
                rs.getString("business_number"),
                rs.getString("source_channel"),
                rs.getString("agent_name"),
                rs.getString("audit_status"),
                rs.getString("export_status"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getString("remark"),
                rs.getBoolean("deleted")
        ));
        return ApiResponse.ok(new PageResult<>(rows, total, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.ok(find(id));
    }

    @PostMapping
    @Transactional
    public ApiResponse<Map<String, Long>> create(@Valid @RequestBody OrderRequest request) {
        validate(request);
        rules.checkDuplicate(request.businessType(), request.businessNumber(), request.phone(), null);
        validateAgent(request.agentId());

        BizOrder order = new BizOrder();
        order.setOrderNo(numbers.orderNumber(request.businessType()));
        apply(order, request);
        order.setCreatedBy(SecurityUtils.current().id());
        order.setAuditStatus(blankToDefault(request.auditStatus(), "CONFIRMED"));
        order.setDeleted(0);
        mapper.insert(order);
        saveExtension(order.getId(), request);
        logs.record("ORDER", "CREATE", order.getId(), "新增订单 " + order.getOrderNo());
        return ApiResponse.ok(Map.of("id", order.getId()));
    }

    @PutMapping("/{id}")
    @Transactional
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody OrderRequest request) {
        BizOrder order = required(id);
        if (!order.getBusinessType().equals(request.businessType())) {
            throw new BusinessException("不能修改订单业务类型");
        }
        validate(request);
        rules.checkDuplicate(request.businessType(), request.businessNumber(), request.phone(), id);
        validateAgent(request.agentId());
        apply(order, request);
        if (request.auditStatus() != null && !request.auditStatus().isBlank()) {
            order.setAuditStatus(request.auditStatus());
        }
        mapper.updateById(order);
        updateExtension(id, request);
        logs.record("ORDER", "UPDATE", id, "修改订单 " + order.getOrderNo());
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<Void> confirm(@PathVariable Long id) {
        BizOrder order = required(id);
        order.setAuditStatus("CONFIRMED");
        mapper.updateById(order);
        logs.record("ORDER", "CONFIRM", id, "确认订单 " + order.getOrderNo());
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/audit-status/toggle")
    @Transactional
    public ApiResponse<Void> toggleAuditStatus(@PathVariable Long id) {
        BizOrder order = required(id);
        String nextStatus = "CONFIRMED".equals(order.getAuditStatus()) ? "PENDING" : "CONFIRMED";
        order.setAuditStatus(nextStatus);
        mapper.updateById(order);
        logs.record("ORDER", "AUDIT_STATUS", id,
                ("CONFIRMED".equals(nextStatus) ? "确认订单 " : "订单改为待确认 ") + order.getOrderNo());
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> voidOrder(@PathVariable Long id) {
        int updated = jdbc.update(
                "UPDATE biz_order SET deleted=1,deleted_by=:userId,deleted_at=NOW() WHERE id=:id AND deleted=0",
                Map.of("userId", SecurityUtils.current().id(), "id", id)
        );
        if (updated == 0) {
            throw new BusinessException("订单不存在或已作废");
        }
        logs.record("ORDER", "VOID", id, "作废订单");
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<Void> restore(@PathVariable Long id) {
        int updated = jdbc.update(
                "UPDATE biz_order SET deleted=0,deleted_by=NULL,deleted_at=NULL WHERE id=:id AND deleted=1",
                Map.of("id", id)
        );
        if (updated == 0) {
            throw new BusinessException("订单不存在或未作废");
        }
        logs.record("ORDER", "RESTORE", id, "恢复订单");
        return ApiResponse.ok();
    }

    private void validate(OrderRequest request) {
        rules.validate(new OrderRuleService.OrderData(
                request.businessType(), request.businessNumber(), request.sourceChannel(), request.auditStatus(),
                request.studentNo(), request.idCardLastSix(), request.licenseType(), request.classType(),
                request.paymentAmount(), request.renewalAmount()
        ));
    }

    private void add(StringBuilder where, MapSqlParameterSource parameters, String key, String value, String column) {
        if (value != null && !value.isBlank()) {
            where.append(" AND ").append(column).append("=:").append(key);
            parameters.addValue(key, value);
        }
    }

    private void addDateRange(StringBuilder where, MapSqlParameterSource parameters, String startTime, String endTime) {
        try {
            if (startTime != null && !startTime.isBlank()) {
                where.append(" AND o.created_at>=:startTime");
                parameters.addValue("startTime", LocalDate.parse(startTime).atStartOfDay());
            }
            if (endTime != null && !endTime.isBlank()) {
                where.append(" AND o.created_at<:endTime");
                parameters.addValue("endTime", LocalDate.parse(endTime).plusDays(1).atStartOfDay());
            }
        } catch (DateTimeParseException e) {
            throw new BusinessException("日期格式应为yyyy-MM-dd");
        }
    }

    private BizOrder required(Long id) {
        BizOrder order = mapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    private Map<String, Object> find(Long id) {
        String sql = "SELECT o.id,o.order_no orderNo,o.business_type businessType,o.customer_name name," +
                "o.contact_phone phone,o.business_number businessNumber,o.source_channel sourceChannel,o.agent_id agentId," +
                "a.name agentName,o.audit_status auditStatus,n.export_status exportStatus,n.student_no studentNo," +
                "n.id_card_last_six idCardLastSix,d.license_type licenseType,d.class_type classType," +
                "d.payment_amount paymentAmount,r.renewal_amount renewalAmount,o.created_at createdAt,o.remark,o.deleted " +
                "FROM biz_order o LEFT JOIN agent a ON a.id=o.agent_id " +
                "LEFT JOIN order_campus_network n ON n.order_id=o.id " +
                "LEFT JOIN order_driving_school d ON d.order_id=o.id " +
                "LEFT JOIN order_renewal r ON r.order_id=o.id WHERE o.id=:id";
        List<Map<String, Object>> rows = jdbc.queryForList(sql, Map.of("id", id));
        if (rows.isEmpty()) {
            throw new BusinessException("订单不存在");
        }
        Map<String, Object> result = new LinkedHashMap<>(rows.get(0));
        Object deleted = result.get("deleted");
        if (deleted instanceof Number value) {
            result.put("deleted", value.intValue() != 0);
        }
        return result;
    }

    private void apply(BizOrder order, OrderRequest request) {
        order.setBusinessType(request.businessType());
        order.setCustomerName(request.name().trim());
        order.setContactPhone(request.phone());
        order.setBusinessNumber(trimToNull(request.businessNumber()));
        order.setSourceChannel(request.sourceChannel());
        order.setAgentId(request.agentId());
        order.setRemark(trimToNull(request.remark()));
    }

    private void validateAgent(Long agentId) {
        if (agentId == null) {
            return;
        }
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM agent WHERE id=:id AND deleted=0",
                Map.of("id", agentId),
                Long.class
        );
        if (count == null || count == 0) {
            throw new BusinessException("归属代理不存在");
        }
    }

    private void saveExtension(Long id, OrderRequest request) {
        if ("CAMPUS_NETWORK".equals(request.businessType())) {
            jdbc.update(
                    "INSERT INTO order_campus_network(order_id,student_no,id_card_last_six,export_status) " +
                            "VALUES(:id,:studentNo,:idCardLastSix,'NOT_EXPORTED')",
                    new MapSqlParameterSource().addValue("id", id).addValue("studentNo", request.studentNo().trim())
                            .addValue("idCardLastSix", request.idCardLastSix())
            );
        } else if ("DRIVING_SCHOOL".equals(request.businessType())) {
            jdbc.update(
                    "INSERT INTO order_driving_school(order_id,license_type,class_type,payment_amount) " +
                            "VALUES(:id,:licenseType,:classType,:paymentAmount)",
                    new MapSqlParameterSource().addValue("id", id).addValue("licenseType", request.licenseType())
                            .addValue("classType", request.classType()).addValue("paymentAmount", request.paymentAmount())
            );
        } else if ("RENEWAL".equals(request.businessType())) {
            jdbc.update(
                    "INSERT INTO order_renewal(order_id,renewal_amount) VALUES(:id,:renewalAmount)",
                    new MapSqlParameterSource().addValue("id", id).addValue("renewalAmount", request.renewalAmount())
            );
        }
    }

    private void updateExtension(Long id, OrderRequest request) {
        if ("CAMPUS_NETWORK".equals(request.businessType())) {
            jdbc.update(
                    "UPDATE order_campus_network SET student_no=:studentNo,id_card_last_six=:idCardLastSix," +
                            "export_status='NOT_EXPORTED' WHERE order_id=:id",
                    new MapSqlParameterSource().addValue("id", id).addValue("studentNo", request.studentNo().trim())
                            .addValue("idCardLastSix", request.idCardLastSix())
            );
        } else if ("DRIVING_SCHOOL".equals(request.businessType())) {
            jdbc.update(
                    "UPDATE order_driving_school SET license_type=:licenseType,class_type=:classType," +
                            "payment_amount=:paymentAmount WHERE order_id=:id",
                    new MapSqlParameterSource().addValue("id", id).addValue("licenseType", request.licenseType())
                            .addValue("classType", request.classType()).addValue("paymentAmount", request.paymentAmount())
            );
        } else if ("RENEWAL".equals(request.businessType())) {
            jdbc.update(
                    "UPDATE order_renewal SET renewal_amount=:renewalAmount WHERE order_id=:id",
                    new MapSqlParameterSource().addValue("id", id).addValue("renewalAmount", request.renewalAmount())
            );
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
