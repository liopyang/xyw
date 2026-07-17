package com.campus.business.service;

import com.campus.business.common.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContentBlockValidator {
  private static final Set<String> TYPES =
      Set.of(
          "heading",
          "subheading",
          "paragraph",
          "notice",
          "image",
          "gallery",
          "parameters",
          "materials",
          "steps",
          "precautions",
          "faq",
          "phone",
          "copy_link",
          "copy_text",
          "divider");
  private final ObjectMapper objectMapper;

  public String validate(String json) {
    try {
      JsonNode blocks = objectMapper.readTree(json == null ? "[]" : json);
      if (!blocks.isArray() || blocks.size() > 100) throw new BusinessException("内容块格式不正确");
      for (JsonNode block : blocks) {
        String type = block.path("type").asText();
        if (!TYPES.contains(type)) throw new BusinessException("包含不支持的内容块类型");
        rejectMarkup(block.path("text").asText(""));
        rejectMarkup(block.path("value").asText(""));
        if ("copy_link".equals(type)) validateLink(block.path("value").asText());
      }
      return objectMapper.writeValueAsString(blocks);
    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      throw new BusinessException("内容块 JSON 格式不正确");
    }
  }

  private void validateLink(String value) {
    try {
      if (value == null || value.isBlank() || value.length() > 1000)
        throw new IllegalArgumentException();
      URI uri = URI.create(value.trim());
      if (!("http".equalsIgnoreCase(uri.getScheme())
          || "https".equalsIgnoreCase(uri.getScheme()))) {
        throw new IllegalArgumentException();
      }
    } catch (Exception e) {
      throw new BusinessException("链接只允许有效的 http 或 https 地址");
    }
  }

  private void rejectMarkup(String value) {
    String normalized = value.toLowerCase();
    if (normalized.contains("<script") || normalized.contains("javascript:")) {
      throw new BusinessException("内容中包含不安全代码");
    }
  }
}
