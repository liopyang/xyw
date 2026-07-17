package com.campus.business.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.campus.business.common.BusinessException;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class OrderRuleServiceTest {
  private OrderRuleService rules;

  @BeforeEach
  void setUp() {
    rules =
        new OrderRuleService(
            mock(NamedParameterJdbcTemplate.class), mock(BusinessConfigService.class));
  }

  @Test
  void validatesAllBusinessSpecificRequiredFields() {
    assertThatCode(
            () -> rules.validate(data("CAMPUS_CARD", "18600000001", null, null, null, null, null)))
        .doesNotThrowAnyException();
    assertThatCode(
            () ->
                rules.validate(
                    data("CAMPUS_NETWORK", "18600000002", "20260001", "123456", null, null, null)))
        .doesNotThrowAnyException();
    assertThatCode(
            () ->
                rules.validate(
                    data(
                        "DRIVING_SCHOOL",
                        null,
                        null,
                        null,
                        "C1",
                        "NORMAL",
                        new BigDecimal("2800"))))
        .doesNotThrowAnyException();
    assertThatCode(
            () ->
                rules.validate(
                    new OrderRuleService.OrderData(
                        "RENEWAL",
                        "18600000003",
                        "STORE",
                        "CONFIRMED",
                        null,
                        null,
                        null,
                        null,
                        null,
                        BigDecimal.TEN)))
        .doesNotThrowAnyException();
  }

  @Test
  void rejectsMissingNetworkIdentityAndInvalidEnums() {
    assertThatThrownBy(
            () ->
                rules.validate(
                    data("CAMPUS_NETWORK", "18600000002", "", "12AB56", null, null, null)))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(
            () ->
                rules.validate(
                    data("DRIVING_SCHOOL", null, null, null, "C3", "VIP", BigDecimal.ONE)))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(
            () ->
                rules.validate(
                    new OrderRuleService.OrderData(
                        "RENEWAL",
                        "18600000003",
                        "UNKNOWN",
                        "CONFIRMED",
                        null,
                        null,
                        null,
                        null,
                        null,
                        BigDecimal.TEN)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("来源渠道");
  }

  private OrderRuleService.OrderData data(
      String type,
      String number,
      String studentNo,
      String suffix,
      String license,
      String classType,
      BigDecimal payment) {
    return new OrderRuleService.OrderData(
        type, number, "AGENT", "PENDING", studentNo, suffix, license, classType, payment, null);
  }
}
