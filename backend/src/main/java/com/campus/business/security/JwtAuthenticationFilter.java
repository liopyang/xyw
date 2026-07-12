package com.campus.business.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.business.entity.SysUser;
import com.campus.business.mapper.SysUserMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final SysUserMapper users;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String value = request.getHeader("Authorization");
        if (value != null && value.startsWith("Bearer ") && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(value.substring(7));
        }
        chain.doFilter(request, response);
    }

    private void authenticate(String token) {
        try {
            LoginUser claims = jwtService.parse(token);
            SysUser user = users.selectOne(Wrappers.<SysUser>lambdaQuery()
                    .eq(SysUser::getId, claims.id())
                    .eq(SysUser::getStatus, 1)
                    .eq(SysUser::getDeleted, 0));
            if (user == null) {
                return;
            }
            LoginUser principal = new LoginUser(user.getId(), user.getUsername(), user.getRealName(), user.getRoleCode());
            var authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + principal.role()))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception ignored) {
            SecurityContextHolder.clearContext();
        }
    }
}
