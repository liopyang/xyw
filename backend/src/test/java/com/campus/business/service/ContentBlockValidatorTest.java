package com.campus.business.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campus.business.common.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ContentBlockValidatorTest {
  private final ContentBlockValidator validator = new ContentBlockValidator(new ObjectMapper());

  @Test
  void acceptsStructuredHttpLink() {
    assertThat(validator.validate("[{\"type\":\"copy_link\",\"value\":\"https://example.com/a\"}]"))
        .contains("https://example.com/a");
  }

  @Test
  void rejectsJavascriptLink() {
    assertThatThrownBy(
            () ->
                validator.validate("[{\"type\":\"copy_link\",\"value\":\"javascript:alert(1)\"}]"))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void rejectsUnknownBlock() {
    assertThatThrownBy(() -> validator.validate("[{\"type\":\"html\",\"text\":\"<b>x</b>\"}]"))
        .isInstanceOf(BusinessException.class);
  }
}
