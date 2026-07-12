package com.campus.business.controller;

import com.campus.business.common.ApiResponse;
import com.campus.business.common.BusinessException;
import com.campus.business.security.SecurityUtils;
import com.campus.business.service.BusinessConfigService;
import com.campus.business.service.OrderRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mini/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('AGENT')")
public class MiniOrderManageController {
    private final NamedParameterJdbcTemplate jdbc;
    private final OrderRuleService rules;
    private final BusinessConfigService configs;

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.ok(one(id));
    }

    @PutMapping("/{id}")
    @Transactional
    public ApiResponse<Void> update(
            @PathVariable Long id,
            @Valid @RequestBody MiniappController.MiniOrder request
    ) {
        Map<String, Object> old = one(id);
        if ("EXPORTED".equals(old.get("exportStatus"))) {
            throw new BusinessException("已导出的校园网订单不能修改");
        }
        String businessType = (String) old.get("businessType");
        if (!businessType.equals(request.businessType())) {
            throw new BusinessException("不能修改订单业务类型");
        }
        BigDecimal paymentAmount = request.paymentAmount();
        if ("DRIVING_SCHOOL".equals(businessType) && paymentAmount == null
                && request.licenseType() != null && request.classType() != null) {
            paymentAmount = configs.drivingPrices().get(request.licenseType() + "_" + request.classType());
        }
        rules.validate(new OrderRuleService.OrderData(
                businessType,
                request.businessNumber(),
                (String) old.get("sourceChannel"),
                "PENDING",
                request.studentNo(),
                request.idCardLastSix(),
                request.licenseType(),
                request.classType(),
                paymentAmount,
                request.renewalAmount()
        ));
        rules.checkDuplicate(businessType, request.businessNumber(), request.phone(), id);

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("name", request.name().trim())
                .addValue("phone", request.phone())
                .addValue("businessNumber", blankToNull(request.businessNumber()))
                .addValue("remark", blankToNull(request.remark()));
        jdbc.update(
                "UPDATE biz_order SET customer_name=:name,contact_phone=:phone,business_number=:businessNumber," +
                        "remark=:remark,audit_status='PENDING' WHERE id=:id",
                parameters
        );
        if ("CAMPUS_NETWORK".equals(businessType)) {
            jdbc.update(
                    "UPDATE order_campus_network SET student_no=:studentNo,id_card_last_six=:idCardLastSix " +
                            "WHERE order_id=:id",
                    parameters.addValue("studentNo", request.studentNo().trim())
                            .addValue("idCardLastSix", request.idCardLastSix())
            );
        } else if ("DRIVING_SCHOOL".equals(businessType)) {
            jdbc.update(
                    "UPDATE order_driving_school SET license_type=:licenseType,class_type=:classType," +
                            "payment_amount=:paymentAmount WHERE order_id=:id",
                    parameters.addValue("licenseType", request.licenseType())
                            .addValue("classType", request.classType())
                            .addValue("paymentAmount", paymentAmount)
            );
        } else if ("RENEWAL".equals(businessType)) {
            jdbc.update(
                    "UPDATE order_renewal SET renewal_amount=:renewalAmount WHERE order_id=:id",
                    parameters.addValue("renewalAmount", request.renewalAmount())
            );
        }
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        Map<String, Object> row = one(id);
        if (!"PENDING".equals(row.get("auditStatus"))) {
            throw new BusinessException("只能作废待确认订单");
        }
        if ("EXPORTED".equals(row.get("exportStatus"))) {
            throw new BusinessException("已导出订单不能作废");
        }
        jdbc.update(
                "UPDATE biz_order SET deleted=1,deleted_by=:userId,deleted_at=NOW() WHERE id=:id AND deleted=0",
                Map.of("userId", SecurityUtils.current().id(), "id", id)
        );
        return ApiResponse.ok();
    }

    private Map<String, Object> one(Long id) {
        String sql = "SELECT o.id,o.order_no orderNo,o.business_type businessType,o.customer_name name," +
                "o.contact_phone phone,o.business_number businessNumber,o.source_channel sourceChannel," +
                "o.audit_status auditStatus,o.remark,o.created_at createdAt,n.student_no studentNo," +
                "n.id_card_last_six idCardLastSix,n.export_status exportStatus,d.license_type licenseType," +
                "d.class_type classType,d.payment_amount paymentAmount,r.renewal_amount renewalAmount " +
                "FROM biz_order o JOIN agent a ON a.id=o.agent_id " +
                "LEFT JOIN order_campus_network n ON n.order_id=o.id " +
                "LEFT JOIN order_driving_school d ON d.order_id=o.id " +
                "LEFT JOIN order_renewal r ON r.order_id=o.id " +
                "WHERE o.id=:id AND o.deleted=0 AND a.user_id=:userId";
        List<Map<String, Object>> rows = jdbc.queryForList(
                sql,
                Map.of("id", id, "userId", SecurityUtils.current().id())
        );
        if (rows.isEmpty()) {
            throw new BusinessException("订单不存在或无权访问");
        }
        return rows.get(0);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
