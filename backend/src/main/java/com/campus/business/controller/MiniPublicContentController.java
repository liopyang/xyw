package com.campus.business.controller;

import com.campus.business.common.ApiResponse;
import com.campus.business.common.BusinessException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mini")
@RequiredArgsConstructor
public class MiniPublicContentController {
  private final NamedParameterJdbcTemplate jdbc;

  @GetMapping("/public/home")
  public ApiResponse<Map<String, Object>> home() {
    List<Map<String, Object>> configs =
        jdbc.queryForList(
            "SELECT config_key configKey,config_value_json configValue FROM mini_home_config WHERE"
                + " status=1",
            Map.of());
    List<Map<String, Object>> articles =
        jdbc.queryForList(
            "SELECT a.id,a.article_no articleNo,a.title,a.subtitle,a.summary,a.cover_media_id"
                + " coverMediaId,c.category_code categoryCode,c.category_name categoryName FROM"
                + " mini_content_article a JOIN mini_content_category c ON c.id=a.category_id WHERE"
                + " a.deleted=0 AND c.deleted=0 AND a.publish_status='PUBLISHED' AND c.status=1"
                + " ORDER BY a.sort_order,a.id",
            Map.of());
    return ApiResponse.ok(Map.of("configs", configs, "articles", articles));
  }

  @GetMapping("/public/content/categories")
  public ApiResponse<List<Map<String, Object>>> categories() {
    return ApiResponse.ok(
        jdbc.queryForList(
            "SELECT id,category_code categoryCode,category_name categoryName,description "
                + "FROM mini_content_category WHERE deleted=0 AND status=1 ORDER BY sort_order,id",
            Map.of()));
  }

  @GetMapping("/public/content/articles")
  public ApiResponse<List<Map<String, Object>>> articles(
      @RequestParam(required = false) Long categoryId) {
    String filter = categoryId == null ? "" : " AND a.category_id=:categoryId";
    return ApiResponse.ok(
        jdbc.queryForList(
            "SELECT a.id,a.article_no articleNo,a.category_id"
                + " categoryId,a.title,a.subtitle,a.summary,a.cover_media_id"
                + " coverMediaId,a.published_at publishedAt FROM mini_content_article a WHERE"
                + " a.deleted=0 AND a.publish_status='PUBLISHED'"
                + filter
                + " ORDER BY a.sort_order,a.id",
            categoryId == null ? Map.of() : Map.of("categoryId", categoryId)));
  }

  @GetMapping("/public/content/articles/{id}")
  public ApiResponse<Map<String, Object>> article(@PathVariable Long id) {
    List<Map<String, Object>> rows =
        jdbc.queryForList(
            "SELECT a.id,a.article_no articleNo,a.title,a.subtitle,a.summary,a.cover_media_id"
                + " coverMediaId,a.content_blocks_json contentBlocks,a.published_at"
                + " publishedAt,c.category_name categoryName FROM mini_content_article a JOIN"
                + " mini_content_category c ON c.id=a.category_id WHERE a.id=:id AND a.deleted=0"
                + " AND a.publish_status='PUBLISHED' AND c.deleted=0 AND c.status=1",
            Map.of("id", id));
    if (rows.isEmpty()) throw new BusinessException("内容不存在或已下线");
    return ApiResponse.ok(rows.get(0));
  }

  @GetMapping("/public/places/categories")
  public ApiResponse<List<Map<String, Object>>> placeCategories() {
    return ApiResponse.ok(
        jdbc.queryForList(
            "SELECT id,category_name categoryName,icon FROM campus_place_category "
                + "WHERE deleted=0 AND status=1 ORDER BY sort_order,id",
            Map.of()));
  }

  @GetMapping("/public/places")
  public ApiResponse<List<Map<String, Object>>> places(
      @RequestParam(required = false) Long categoryId) {
    String filter = categoryId == null ? "" : " AND p.category_id=:categoryId";
    return ApiResponse.ok(
        jdbc.queryForList(
            "SELECT p.id,p.place_no placeNo,p.category_id categoryId,c.category_name"
                + " categoryName,p.place_name"
                + " placeName,p.longitude,p.latitude,p.address,p.summary,p.contact_phone"
                + " contactPhone,p.business_hours businessHours,p.cover_media_id coverMediaId FROM"
                + " campus_place p JOIN campus_place_category c ON c.id=p.category_id WHERE"
                + " p.deleted=0 AND p.status=1 AND c.deleted=0 AND c.status=1"
                + filter
                + " ORDER BY p.sort_order,p.id",
            categoryId == null ? Map.of() : Map.of("categoryId", categoryId)));
  }

  @GetMapping("/public/places/{id}")
  public ApiResponse<Map<String, Object>> place(@PathVariable Long id) {
    List<Map<String, Object>> rows =
        jdbc.queryForList(
            "SELECT p.id,p.place_no placeNo,p.place_name"
                + " placeName,p.longitude,p.latitude,p.address,p.summary,p.contact_phone"
                + " contactPhone,p.business_hours businessHours,p.cover_media_id"
                + " coverMediaId,p.detail_blocks_json detailBlocks,c.category_name categoryName"
                + " FROM campus_place p JOIN campus_place_category c ON c.id=p.category_id WHERE"
                + " p.id=:id AND p.deleted=0 AND p.status=1",
            Map.of("id", id));
    if (rows.isEmpty()) throw new BusinessException("地点不存在或已停用");
    return ApiResponse.ok(rows.get(0));
  }
}
