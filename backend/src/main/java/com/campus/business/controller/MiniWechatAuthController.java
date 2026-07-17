package com.campus.business.controller;

import com.campus.business.common.ApiResponse;
import com.campus.business.common.BusinessException;
import com.campus.business.security.JwtService;
import com.campus.business.security.LoginUser;
import com.campus.business.security.SecurityUtils;
import com.campus.business.service.JdbcInsertService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api/mini/auth")
@RequiredArgsConstructor
public class MiniWechatAuthController {
  private final NamedParameterJdbcTemplate jdbc;
  private final JdbcInsertService inserts;
  private final JwtService jwt;
  private final PasswordEncoder passwords;

  @Value("${campus.wechat.app-id:}")
  private String appId;

  @Value("${campus.wechat.app-secret:}")
  private String appSecret;

  public record LoginRequest(@NotBlank String code) {}

  @PostMapping("/wechat-login")
  @Transactional
  public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
    if (appId.isBlank() || appSecret.isBlank()) throw new BusinessException("微信登录尚未配置");
    @SuppressWarnings("unchecked")
    Map<String, Object> wx =
        RestClient.create()
            .get()
            .uri(
                builder ->
                    builder
                        .scheme("https")
                        .host("api.weixin.qq.com")
                        .path("/sns/jscode2session")
                        .queryParam("appid", appId)
                        .queryParam("secret", appSecret)
                        .queryParam("js_code", request.code())
                        .queryParam("grant_type", "authorization_code")
                        .build())
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(Map.class);
    Object openidValue = wx == null ? null : wx.get("openid");
    String openid = openidValue instanceof String value ? value : "";
    if (openid.isBlank()) throw new BusinessException("微信登录失败，请重新尝试");
    String unionid = wx.get("unionid") == null ? null : String.valueOf(wx.get("unionid"));

    List<Map<String, Object>> rows =
        jdbc.queryForList(
            "SELECT m.id,m.sys_user_id sysUserId,m.role_code"
                + " roleCode,m.status,u.username,u.real_name realName,u.status userStatus FROM"
                + " mini_user m LEFT JOIN sys_user u ON u.id=m.sys_user_id WHERE m.openid=:openid"
                + " AND m.deleted=0",
            Map.of("openid", openid));
    Map<String, Object> row;
    if (rows.isEmpty()) row = createUser(openid, unionid);
    else row = rows.get(0);
    if (((Number) row.get("status")).intValue() != 1
        || ((Number) row.get("userStatus")).intValue() != 1) throw new BusinessException("账号已停用");
    long sysUserId = ((Number) row.get("sysUserId")).longValue();
    String role = agentRole(sysUserId, String.valueOf(row.get("roleCode")));
    jdbc.update(
        "UPDATE mini_user SET"
            + " role_code=:role,last_login_at=NOW(),unionid=COALESCE(:unionid,unionid) WHERE"
            + " id=:id",
        new MapSqlParameterSource()
            .addValue("role", role)
            .addValue("unionid", unionid)
            .addValue("id", row.get("id")));
    jdbc.update(
        "UPDATE sys_user SET role_code=:role WHERE id=:id", Map.of("role", role, "id", sysUserId));
    LoginUser principal =
        new LoginUser(
            sysUserId,
            String.valueOf(row.get("username")),
            String.valueOf(row.get("realName")),
            role);
    return ApiResponse.ok(Map.of("token", jwt.create(principal), "user", profile(sysUserId)));
  }

  @GetMapping("/me")
  public ApiResponse<Map<String, Object>> me() {
    return ApiResponse.ok(profile(SecurityUtils.current().id()));
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout() {
    return ApiResponse.ok();
  }

  @PostMapping("/cancel-account")
  @Transactional
  public ApiResponse<Void> cancel() {
    long id = SecurityUtils.current().id();
    jdbc.update("UPDATE mini_user SET status=0,deleted=1 WHERE sys_user_id=:id", Map.of("id", id));
    jdbc.update("UPDATE sys_user SET status=0,deleted=1 WHERE id=:id", Map.of("id", id));
    return ApiResponse.ok();
  }

  private Map<String, Object> createUser(String openid, String unionid) {
    String hash = sha256(openid).substring(0, 20);
    String username = "wx_" + hash;
    String realName = "微信用户" + hash.substring(0, 6);
    long sysId =
        inserts.insert(
            "INSERT INTO sys_user(username,password,real_name,role_code,status)"
                + " VALUES(:username,:password,:realName,'USER',1)",
            new MapSqlParameterSource()
                .addValue("username", username)
                .addValue("password", passwords.encode(UUID.randomUUID().toString()))
                .addValue("realName", realName));
    long miniId =
        inserts.insert(
            "INSERT INTO mini_user(sys_user_id,openid,unionid,role_code,status,last_login_at)"
                + " VALUES(:sysId,:openid,:unionid,'USER',1,NOW())",
            new MapSqlParameterSource()
                .addValue("sysId", sysId)
                .addValue("openid", openid)
                .addValue("unionid", unionid));
    return Map.of(
        "id",
        miniId,
        "sysUserId",
        sysId,
        "roleCode",
        "USER",
        "status",
        1,
        "userStatus",
        1,
        "username",
        username,
        "realName",
        realName);
  }

  private String agentRole(long sysId, String current) {
    Long count =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM agent WHERE user_id=:id AND status=1 AND deleted=0",
            Map.of("id", sysId),
            Long.class);
    return count != null && count > 0 ? "AGENT" : "USER";
  }

  private Map<String, Object> profile(long sysId) {
    List<Map<String, Object>> rows =
        jdbc.queryForList(
            "SELECT m.sys_user_id id,u.username,u.real_name realName,m.nickname,m.avatar_url"
                + " avatarUrl,m.phone,m.role_code role,m.status,a.agent_no agentNo,a.level"
                + " agentLevel FROM mini_user m JOIN sys_user u ON u.id=m.sys_user_id LEFT JOIN"
                + " agent a ON a.user_id=m.sys_user_id AND a.deleted=0 WHERE m.sys_user_id=:id AND"
                + " m.deleted=0",
            Map.of("id", sysId));
    if (rows.isEmpty()) throw new BusinessException("小程序账号不存在");
    return rows.get(0);
  }

  private String sha256(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
