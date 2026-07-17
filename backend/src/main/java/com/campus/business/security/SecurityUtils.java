package com.campus.business.security;

import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
  private SecurityUtils() {}

  public static LoginUser current() {
    return (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
  }
}
