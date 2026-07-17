package com.campus.business.service;

import com.campus.business.common.BusinessException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ReferenceNumberService {
  private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;
  private static final char[] ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
  private static final Map<String, String> ORDER_PREFIXES =
      Map.of(
          "CAMPUS_CARD", "CARD",
          "CAMPUS_NETWORK", "NET",
          "DRIVING_SCHOOL", "DRIVE",
          "RENEWAL", "RENEW");
  private final SecureRandom random = new SecureRandom();

  public String orderNumber(String businessType) {
    String prefix = ORDER_PREFIXES.get(businessType);
    if (prefix == null) {
      throw new BusinessException("业务类型不正确");
    }
    return prefix + LocalDate.now().format(DAY) + randomPart(12);
  }

  public String issueNumber() {
    return "ISSUE" + LocalDate.now().format(DAY) + randomPart(12);
  }

  private String randomPart(int length) {
    StringBuilder value = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      value.append(ALPHABET[random.nextInt(ALPHABET.length)]);
    }
    return value.toString();
  }
}
