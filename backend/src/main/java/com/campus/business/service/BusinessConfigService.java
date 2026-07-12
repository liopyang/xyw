package com.campus.business.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BusinessConfigService {
    private static final int DEFAULT_DUPLICATE_WINDOW_DAYS = 30;
    private final NamedParameterJdbcTemplate jdbc;

    public int duplicateWindowDays() {
        String value = value("duplicateWindowDays", String.valueOf(DEFAULT_DUPLICATE_WINDOW_DAYS));
        try {
            return Math.max(0, Math.min(3650, Integer.parseInt(value)));
        } catch (NumberFormatException ignored) {
            return DEFAULT_DUPLICATE_WINDOW_DAYS;
        }
    }

    public Map<String, BigDecimal> drivingPrices() {
        Map<String, BigDecimal> prices = new LinkedHashMap<>();
        prices.put("C1_NORMAL", decimalValue("drivingC1NormalPrice", "2800"));
        prices.put("C1_FULL", decimalValue("drivingC1FullPrice", "3600"));
        prices.put("C2_NORMAL", decimalValue("drivingC2NormalPrice", "3000"));
        prices.put("C2_FULL", decimalValue("drivingC2FullPrice", "3900"));
        return prices;
    }

    private BigDecimal decimalValue(String key, String fallback) {
        try {
            return new BigDecimal(value(key, fallback));
        } catch (NumberFormatException ignored) {
            return new BigDecimal(fallback);
        }
    }

    private String value(String key, String fallback) {
        List<String> values = jdbc.query(
                "SELECT config_value FROM business_config WHERE config_key=:key LIMIT 1",
                Map.of("key", key),
                (rs, rowNum) -> rs.getString(1)
        );
        return values.isEmpty() ? fallback : values.get(0);
    }
}
