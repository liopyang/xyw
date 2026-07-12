package com.campus.business.controller;

import com.campus.business.common.ApiResponse;
import com.campus.business.common.BusinessException;
import com.campus.business.common.PageResult;
import com.campus.business.security.SecurityUtils;
import com.campus.business.service.BusinessConfigService;
import com.campus.business.service.JdbcInsertService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/mini")
@RequiredArgsConstructor
public class MiniappController {
    private static final Set<String> ISSUE_TYPES = Set.of(
            "CAMPUS_CARD", "CAMPUS_NETWORK", "DRIVING_SCHOOL", "RENEWAL", "ACCOUNT", "OTHER"
    );
    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcInsertService inserts;
    private final OrderRuleService rules;
    private final ReferenceNumberService numbers;
    private final BusinessConfigService configs;

    public record MiniOrder(
            @NotBlank(message = "请选择业务类型") String businessType,
            @NotBlank(message = "请填写姓名") @Size(max = 50) String name,
            @NotBlank(message = "请填写手机号") @Pattern(regexp = "^1\\d{10}$", message = "手机号格式错误") String phone,
            String businessNumber,
            @NotBlank(message = "请选择来源渠道") String sourceChannel,
            @Size(max = 500) String remark,
            String studentNo,
            String idCardLastSix,
            String licenseType,
            String classType,
            @PositiveOrZero BigDecimal paymentAmount,
            @PositiveOrZero BigDecimal renewalAmount
    ) {
    }

    public record MiniIssue(
            @NotBlank String issueType,
            @NotBlank @Size(max = 5000) String description,
            @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "手机号格式错误") String contactPhone,
            String businessNumber
    ) {
    }

    @GetMapping("/home")
    public ApiResponse<Map<String, Object>> home() {
        var user = SecurityUtils.current();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("role", user.role());
        result.put("drivingPrices", configs.drivingPrices());
        if (!"AGENT".equals(user.role())) {
            return ApiResponse.ok(result);
        }
        Long agentId = agentId(user.id());
        result.put("todayOrders", count(
                "SELECT COUNT(*) FROM biz_order WHERE deleted=0 AND agent_id=:agentId AND DATE(created_at)=CURDATE()",
                agentId));
        result.put("monthOrders", count(
                "SELECT COUNT(*) FROM biz_order WHERE deleted=0 AND agent_id=:agentId AND created_at>=DATE_FORMAT(CURDATE(),'%Y-%m-01')",
                agentId));
        result.put("pendingOrders", count(
                "SELECT COUNT(*) FROM biz_order WHERE deleted=0 AND agent_id=:agentId AND audit_status='PENDING'",
                agentId));
        result.put("confirmedOrders", count(
                "SELECT COUNT(*) FROM biz_order WHERE deleted=0 AND agent_id=:agentId AND audit_status='CONFIRMED'",
                agentId));
        return ApiResponse.ok(result);
    }

    @GetMapping("/configs")
    public ApiResponse<Map<String, Object>> configs() {
        return ApiResponse.ok(Map.of("drivingPrices", configs.drivingPrices()));
    }

    @GetMapping("/orders")
    @PreAuthorize("hasRole('AGENT')")
    public ApiResponse<PageResult<Map<String, Object>>> orders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        page = Math.max(1, page);
        pageSize = Math.min(100, Math.max(1, pageSize));
        Long agentId = agentId(SecurityUtils.current().id());
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("agentId", agentId)
                .addValue("offset", (page - 1) * pageSize)
                .addValue("size", pageSize);
        long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM biz_order WHERE deleted=0 AND agent_id=:agentId",
                parameters,
                Long.class
        );
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id,order_no orderNo,business_type businessType,customer_name name,contact_phone phone," +
                        "business_number businessNumber,audit_status auditStatus,created_at createdAt,remark " +
                        "FROM biz_order WHERE deleted=0 AND agent_id=:agentId ORDER BY created_at DESC,id DESC " +
                        "LIMIT :offset,:size",
                parameters
        );
        return ApiResponse.ok(new PageResult<>(rows, total, page, pageSize));
    }

    @PostMapping("/orders")
    @PreAuthorize("hasRole('AGENT')")
    @Transactional
    public ApiResponse<Map<String, Long>> order(@Valid @RequestBody MiniOrder request) {
        if (!Set.of("ONLINE", "AGENT").contains(request.sourceChannel())) {
            throw new BusinessException("代理订单来源只能选择线上或代理");
        }
        BigDecimal paymentAmount = request.paymentAmount();
        if ("DRIVING_SCHOOL".equals(request.businessType()) && paymentAmount == null
                && request.licenseType() != null && request.classType() != null) {
            paymentAmount = configs.drivingPrices().get(request.licenseType() + "_" + request.classType());
        }
        rules.validate(new OrderRuleService.OrderData(
                request.businessType(), request.businessNumber(), request.sourceChannel(), "PENDING",
                request.studentNo(), request.idCardLastSix(), request.licenseType(), request.classType(),
                paymentAmount, request.renewalAmount()
        ));
        rules.checkDuplicate(request.businessType(), request.businessNumber(), request.phone(), null);

        Long agentId = agentId(SecurityUtils.current().id());
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("orderNo", numbers.orderNumber(request.businessType()))
                .addValue("businessType", request.businessType())
                .addValue("name", request.name().trim())
                .addValue("phone", request.phone())
                .addValue("businessNumber", blankToNull(request.businessNumber()))
                .addValue("sourceChannel", request.sourceChannel())
                .addValue("agentId", agentId)
                .addValue("userId", SecurityUtils.current().id())
                .addValue("remark", blankToNull(request.remark()));
        long id = inserts.insert(
                "INSERT INTO biz_order(order_no,business_type,customer_name,contact_phone,business_number," +
                        "source_channel,agent_id,created_by,audit_status,remark) " +
                        "VALUES(:orderNo,:businessType,:name,:phone,:businessNumber,:sourceChannel,:agentId,:userId,'PENDING',:remark)",
                parameters
        );
        saveExtension(id, request, paymentAmount);
        return ApiResponse.ok(Map.of("id", id));
    }

    @GetMapping("/issues")
    public ApiResponse<PageResult<Map<String, Object>>> issues(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        page = Math.max(1, page);
        pageSize = Math.min(100, Math.max(1, pageSize));
        Long userId = SecurityUtils.current().id();
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("offset", (page - 1) * pageSize)
                .addValue("size", pageSize);
        long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM support_issue WHERE deleted=0 AND submitter_id=:userId",
                parameters,
                Long.class
        );
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id,issue_no issueNo,issue_type issueType,description,business_number businessNumber," +
                        "status,process_remark processRemark,submitted_at submittedAt,processed_at processedAt " +
                        "FROM support_issue WHERE deleted=0 AND submitter_id=:userId " +
                        "ORDER BY submitted_at DESC,id DESC LIMIT :offset,:size",
                parameters
        );
        return ApiResponse.ok(new PageResult<>(rows, total, page, pageSize));
    }

    @GetMapping("/issues/{id}")
    public ApiResponse<Map<String, Object>> issueDetail(@PathVariable Long id) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id,issue_no issueNo,issue_type issueType,description,business_number businessNumber," +
                        "contact_phone contactPhone,status,process_remark processRemark,submitted_at submittedAt," +
                        "processed_at processedAt FROM support_issue " +
                        "WHERE id=:id AND deleted=0 AND submitter_id=:userId",
                Map.of("id", id, "userId", SecurityUtils.current().id())
        );
        if (rows.isEmpty()) {
            throw new BusinessException("问题不存在或无权访问");
        }
        Map<String, Object> result = new LinkedHashMap<>(rows.get(0));
        result.put("images", jdbc.queryForList(
                "SELECT id,image_url imageUrl FROM support_issue_image WHERE issue_id=:id ORDER BY id",
                Map.of("id", id)
        ));
        return ApiResponse.ok(result);
    }

    @PostMapping("/issues")
    @Transactional
    public ApiResponse<Map<String, Long>> issue(@Valid @RequestBody MiniIssue request) {
        if (!ISSUE_TYPES.contains(request.issueType())) {
            throw new BusinessException("问题类型不正确");
        }
        var user = SecurityUtils.current();
        String submitterType = "AGENT".equals(user.role()) ? "AGENT" : "USER";
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("issueNo", numbers.issueNumber())
                .addValue("userId", user.id())
                .addValue("submitterType", submitterType)
                .addValue("name", user.realName())
                .addValue("phone", request.contactPhone())
                .addValue("issueType", request.issueType())
                .addValue("description", request.description().trim())
                .addValue("businessNumber", blankToNull(request.businessNumber()));
        long id = inserts.insert(
                "INSERT INTO support_issue(issue_no,submitter_id,submitter_type,submitter_name,contact_phone," +
                        "issue_type,description,business_number,status) " +
                        "VALUES(:issueNo,:userId,:submitterType,:name,:phone,:issueType,:description,:businessNumber,'PENDING')",
                parameters
        );
        return ApiResponse.ok(Map.of("id", id));
    }

    private void saveExtension(Long id, MiniOrder request, BigDecimal paymentAmount) {
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
                            .addValue("classType", request.classType()).addValue("paymentAmount", paymentAmount)
            );
        } else if ("RENEWAL".equals(request.businessType())) {
            jdbc.update(
                    "INSERT INTO order_renewal(order_id,renewal_amount) VALUES(:id,:renewalAmount)",
                    new MapSqlParameterSource().addValue("id", id).addValue("renewalAmount", request.renewalAmount())
            );
        }
    }

    private Long agentId(Long userId) {
        List<Long> ids = jdbc.query(
                "SELECT id FROM agent WHERE user_id=:userId AND deleted=0 AND status=1",
                Map.of("userId", userId),
                (rs, rowNum) -> rs.getLong(1)
        );
        if (ids.isEmpty()) {
            throw new BusinessException("代理账号不可用");
        }
        return ids.get(0);
    }

    private long count(String sql, Long agentId) {
        Long value = jdbc.queryForObject(sql, Map.of("agentId", agentId), Long.class);
        return value == null ? 0 : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
