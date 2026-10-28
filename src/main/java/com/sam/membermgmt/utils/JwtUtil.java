package com.sam.membermgmt.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT Token，包含用户ID、用户名和角色列表
     */
    public String generateToken(Long userId, String username, List<String> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("roles", roles != null ? String.join(",", roles) : "");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 生成会员端 JWT Token。与后台用户 Token 通过 userType 字段区分，
     * 避免 sys_user.id 与 member.id 的 ID 空间冲突。
     */
    public String generateMemberToken(Long memberId, String username, List<String> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userType", "MEMBER");
        claims.put("memberId", memberId);
        claims.put("username", username);
        claims.put("roles", roles != null ? String.join(",", roles) : "");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 获取 Token 的用户类型：MEMBER 表示会员端，其余（null）视为后台用户。
     */
    public String getUserTypeFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("userType", String.class);
    }

    /**
     * 从会员 Token 中获取会员 ID（仅会员 Token 携带该字段）。
     */
    public Long getMemberIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("memberId", Long.class);
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("userId", Long.class);
    }

    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    /**
     * 从 Token 中获取角色列表
     */
    public List<String> getRolesFromToken(String token) {
        Claims claims = parseToken(token);
        String rolesStr = claims.get("roles", String.class);
        if (rolesStr == null || rolesStr.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(rolesStr.split(","));
    }

    /**
     * 生成 Refresh Token（仅含 userId，长有效期）
     */
    public String generateRefreshToken(Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "refresh");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(String.valueOf(userId))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 从 Refresh Token 中获取 userId
     */
    public Long getUserIdFromRefreshToken(String token) {
        Claims claims = parseToken(token);
        String type = claims.get("type", String.class);
        if (!"refresh".equals(type)) {
            throw new RuntimeException("非法的 Refresh Token");
        }
        return claims.get("userId", Long.class);
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
