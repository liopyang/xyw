package com.campus.business.service;

import com.campus.business.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderRuleService {
    public static final Set<String> BUSINESS_TYPES = Set.of("CAMPUS_CARD", "CAMPUS_NETWORK", "DRIVING_SCHOOL", "RENEWAL");
    public static final Set<String> SOURCE_CHANNELS = Set.of("ONLINE", "AGENT", "STORE");
    public static final Set<String> AUDIT_STATUSES = Set.of("PENDING", "CONFIRMED");
    private static final Set<String> LICENSE_TYPES = Set.of("C1", "C2");
    private static final Set<String> CLASS_TYPES = Set.of("NORMAL", "FULL");

    private final NamedParameterJdbcTemplate jdbc;
    private final BusinessConfigService configs;

    public record OrderData(
            String businessType,
            String businessNumber,
            String sourceChannel,
            String auditStatus,
            String studentNo,
            String idCardLastSix,
            String licenseType,
            String classType,
            BigDecimal paymentAmount,
            BigDecimal renewalAmount
    ) {
    }

    public void validate(OrderData data) {
        requireEnum(data.businessType(), BUSINESS_TYPES, "业务类型不正确");
        requireEnum(data.sourceChannel(), SOURCE_CHANNELS, "来源渠道不正确");
        if (data.auditStatus() != null && !data.auditStatus().isBlank()) {
            requireEnum(data.auditStatus(), AUDIT_STATUSES, "审核状态不正确");
        }

        switch (data.businessType()) {
            case "CAMPUS_CARD" -> requireText(data.businessNumber(), "请填写业务号码");
            case "CAMPUS_NETWORK" -> {
                requireText(data.businessNumber(), "请填写新办号码");
                requireText(data.studentNo(), "请填写学号");
                if (data.idCardLastSix() == null || !data.idCardLastSix().matches("^\\d{6}$")) {
                    throw new BusinessException("身份证后六位必须是6位数字");
                }
            }
            case "DRIVING_SCHOOL" -> {
                requireEnum(data.licenseType(), LICENSE_TYPES, "车型只能选择C1或C2");
                requireEnum(data.classType(), CLASS_TYPES, "班型只能选择普通班或全包班");
                requireNonNegative(data.paymentAmount(), "请填写有效的缴费数额");
            }
            case "RENEWAL" -> {
                requireText(data.businessNumber(), "请填写业务号码");
                requireNonNegative(data.renewalAmount(), "请填写有效的续费金额");
            }
            default -> throw new BusinessException("业务类型不正确");
        }
    }

    public void checkDuplicate(String businessType, String businessNumber, String phone, Long excludeId) {
        String field = businessNumber != null && !businessNumber.isBlank() ? "business_number" : "contact_phone";
        String value = "business_number".equals(field) ? businessNumber.trim() : phone;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("type", businessType)
                .addValue("value", value)
                .addValue("cutoff", LocalDateTime.now().minusDays(configs.duplicateWindowDays()));
        StringBuilder sql = new StringBuilder(
                "SELECT order_no,created_at FROM biz_order " +
                        "WHERE deleted=0 AND business_type=:type AND " + field + "=:value AND created_at>=:cutoff"
        );
        if (excludeId != null) {
            sql.append(" AND id<>:excludeId");
            params.addValue("excludeId", excludeId);
        }
        sql.append(" ORDER BY created_at DESC LIMIT 1");
        List<String> duplicate = jdbc.query(sql.toString(), params,
                (rs, rowNum) -> rs.getString("order_no") + "（" + rs.getTimestamp("created_at").toLocalDateTime() + "）");
        if (!duplicate.isEmpty()) {
            throw new BusinessException("发现重复订单：" + duplicate.get(0));
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
    }

    private void requireEnum(String value, Set<String> values, String message) {
        if (value == null || !values.contains(value)) {
            throw new BusinessException(message);
        }
    }

    private void requireNonNegative(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(message);
        }
    }
}
