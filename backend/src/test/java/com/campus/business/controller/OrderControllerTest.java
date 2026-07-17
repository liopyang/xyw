package com.campus.business.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.campus.business.mapper.BizOrderMapper;
import com.campus.business.service.OperationLogService;
import com.campus.business.service.OrderRuleService;
import com.campus.business.service.ReferenceNumberService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class OrderControllerTest {

  @ParameterizedTest
  @CsvSource({"0,false", "1,true"})
  void detailNormalizesNumericDeletedFlagToBoolean(int databaseValue, boolean expected) {
    NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
    when(jdbc.queryForList(anyString(), anyMap()))
        .thenReturn(List.of(Map.of("id", 1L, "deleted", databaseValue)));

    OrderController controller =
        new OrderController(
            jdbc,
            mock(BizOrderMapper.class),
            mock(OperationLogService.class),
            mock(OrderRuleService.class),
            mock(ReferenceNumberService.class));

    Object deleted = controller.detail(1L).data().get("deleted");

    assertThat(deleted).isEqualTo(expected).isInstanceOf(Boolean.class);
  }

  @Test
  void detailKeepsBooleanDeletedFlagUnchanged() {
    NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
    when(jdbc.queryForList(anyString(), anyMap()))
        .thenReturn(List.of(Map.of("id", 1L, "deleted", true)));

    OrderController controller =
        new OrderController(
            jdbc,
            mock(BizOrderMapper.class),
            mock(OperationLogService.class),
            mock(OrderRuleService.class),
            mock(ReferenceNumberService.class));

    assertThat(controller.detail(1L).data().get("deleted"))
        .isEqualTo(true)
        .isInstanceOf(Boolean.class);
  }
}
