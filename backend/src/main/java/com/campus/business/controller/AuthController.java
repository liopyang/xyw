package com.campus.business.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.business.common.ApiResponse;
import com.campus.business.common.BusinessException;
import com.campus.business.entity.SysUser;
import com.campus.business.mapper.SysUserMapper;
import com.campus.business.security.JwtService;
import com.campus.business.security.LoginUser;
import com.campus.business.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
  private final SysUserMapper users;
  private final PasswordEncoder encoder;
  private final JwtService jwt;

  public record LoginRequest(
      @NotBlank(message = "请输入账号") String username, @NotBlank(message = "请输入密码") String password) {}

  public record ChangePasswordRequest(
      @NotBlank(message = "请输入当前密码") String currentPassword,
      @NotBlank(message = "请输入新密码") @Size(min = 8, max = 72, message = "新密码长度应为8至72位")
          String newPassword) {}

  public record UserView(Long id, String username, String realName, String role) {}

  @PostMapping("/login")
  public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
    SysUser user =
        users.selectOne(
            Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, request.username())
                .eq(SysUser::getStatus, 1)
                .eq(SysUser::getDeleted, 0));
    if (user == null || !encoder.matches(request.password(), user.getPassword())) {
      throw new BusinessException("账号或密码错误");
    }
    LoginUser principal =
        new LoginUser(user.getId(), user.getUsername(), user.getRealName(), user.getRoleCode());
    return ApiResponse.ok(Map.of("token", jwt.create(principal), "user", view(principal)));
  }

  @GetMapping("/me")
  public ApiResponse<UserView> me() {
    return ApiResponse.ok(view(SecurityUtils.current()));
  }

  @PostMapping("/change-password")
  @Transactional
  public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
    LoginUser principal = SecurityUtils.current();
    SysUser user = users.selectById(principal.id());
    if (user == null
        || user.getStatus() != 1
        || !encoder.matches(request.currentPassword(), user.getPassword())) {
      throw new BusinessException("当前密码不正确");
    }
    if (encoder.matches(request.newPassword(), user.getPassword())) {
      throw new BusinessException("新密码不能与当前密码相同");
    }
    user.setPassword(encoder.encode(request.newPassword()));
    users.updateById(user);
    return ApiResponse.ok();
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout() {
    return ApiResponse.ok();
  }

  private UserView view(LoginUser user) {
    return new UserView(user.id(), user.username(), user.realName(), user.role());
  }
}
