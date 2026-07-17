package com.campus.business.controller;

import com.campus.business.common.ApiResponse;
import com.campus.business.common.BusinessException;
import com.campus.business.security.SecurityUtils;
import com.campus.business.service.ContentBlockValidator;
import com.campus.business.service.JdbcInsertService;
import com.campus.business.service.OperationLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/mini")
@RequiredArgsConstructor
public class AdminMiniContentController {
  private final NamedParameterJdbcTemplate jdbc;
  private final JdbcInsertService inserts;
  private final ContentBlockValidator validator;
  private final ObjectMapper objectMapper;
  private final OperationLogService logs;

  public record CategoryRequest(
      @NotBlank @Size(max = 40) String categoryCode,
      @NotBlank @Size(max = 80) String categoryName,
      @Size(max = 500) String description,
      Integer sortOrder,
      Integer status) {}

  public record ArticleRequest(
      @NotNull Long categoryId,
      @NotBlank @Size(max = 150) String title,
      @Size(max = 200) String subtitle,
      @Size(max = 500) String summary,
      Long coverMediaId,
      @NotBlank String contentBlocksJson,
      Integer sortOrder) {}

  public record PlaceRequest(
      @NotNull Long categoryId,
      @NotBlank @Size(max = 120) String placeName,
      @NotNull BigDecimal longitude,
      @NotNull BigDecimal latitude,
      @NotBlank @Size(max = 300) String address,
      @Size(max = 500) String summary,
      @Size(max = 30) String contactPhone,
      @Size(max = 150) String businessHours,
      Long coverMediaId,
      String detailBlocksJson,
      Integer sortOrder,
      Integer status) {}

  public record HomeConfigRequest(@NotNull Map<String, Object> configs) {}

  @GetMapping("/home-config")
  public ApiResponse<List<Map<String, Object>>> homeConfig() {
    return ApiResponse.ok(
        jdbc.queryForList(
            "SELECT config_key configKey,config_value_json configValue,status,updated_at updatedAt"
                + " FROM mini_home_config ORDER BY id",
            Map.of()));
  }

  @PutMapping("/home-config")
  @Transactional
  public ApiResponse<Void> saveHomeConfig(@Valid @RequestBody HomeConfigRequest request)
      throws Exception {
    for (var item : request.configs().entrySet()) {
      if (!Set.of("site", "notice", "banners", "cards").contains(item.getKey()))
        throw new BusinessException("首页配置项不正确");
      String json = objectMapper.writeValueAsString(item.getValue());
      if (json.length() > 20000) throw new BusinessException("首页配置内容过长");
      jdbc.update(
          "INSERT INTO mini_home_config(config_key,config_value_json,status,updated_by)"
              + " VALUES(:key,:value,1,:userId) ON DUPLICATE KEY UPDATE"
              + " config_value_json=:value,status=1,updated_by=:userId",
          new MapSqlParameterSource()
              .addValue("key", item.getKey())
              .addValue("value", json)
              .addValue("userId", SecurityUtils.current().id()));
    }
    logs.record("MINI_CONTENT", "HOME_CONFIG", null, "修改小程序首页配置");
    return ApiResponse.ok();
  }

  @GetMapping("/categories")
  public ApiResponse<List<Map<String, Object>>> categories() {
    return ApiResponse.ok(
        jdbc.queryForList(
            "SELECT id,category_code categoryCode,category_name categoryName,description,sort_order"
                + " sortOrder,status,created_at createdAt,updated_at updatedAt FROM"
                + " mini_content_category WHERE deleted=0 ORDER BY sort_order,id",
            Map.of()));
  }

  @PostMapping("/categories")
  @Transactional
  public ApiResponse<Map<String, Long>> createCategory(@Valid @RequestBody CategoryRequest r) {
    long id =
        inserts.insert(
            "INSERT INTO"
                + " mini_content_category(category_code,category_name,description,sort_order,status,created_by,updated_by)"
                + " VALUES(:code,:name,:description,:sortOrder,:status,:userId,:userId)",
            categoryParams(r));
    logs.record("MINI_CONTENT", "CREATE_CATEGORY", id, "新增栏目 " + r.categoryName());
    return ApiResponse.ok(Map.of("id", id));
  }

  @PutMapping("/categories/{id}")
  @Transactional
  public ApiResponse<Void> updateCategory(
      @PathVariable Long id, @Valid @RequestBody CategoryRequest r) {
    int n =
        jdbc.update(
            "UPDATE mini_content_category SET"
                + " category_code=:code,category_name=:name,description=:description,sort_order=:sortOrder,status=:status,updated_by=:userId"
                + " WHERE id=:id AND deleted=0",
            categoryParams(r).addValue("id", id));
    required(n, "栏目不存在");
    logs.record("MINI_CONTENT", "UPDATE_CATEGORY", id, "修改栏目 " + r.categoryName());
    return ApiResponse.ok();
  }

  @DeleteMapping("/categories/{id}")
  @Transactional
  public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
    Long count =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM mini_content_article WHERE category_id=:id AND deleted=0",
            Map.of("id", id),
            Long.class);
    if (count != null && count > 0) throw new BusinessException("栏目仍有关联内容，不能删除");
    required(
        jdbc.update(
            "UPDATE mini_content_category SET deleted=1,updated_by=:userId WHERE id=:id AND"
                + " deleted=0",
            Map.of("id", id, "userId", SecurityUtils.current().id())),
        "栏目不存在");
    return ApiResponse.ok();
  }

  @GetMapping("/articles")
  public ApiResponse<List<Map<String, Object>>> articles() {
    return ApiResponse.ok(
        jdbc.queryForList(
            "SELECT a.id,a.article_no articleNo,a.category_id categoryId,c.category_name"
                + " categoryName,a.title,a.subtitle,a.summary,a.cover_media_id"
                + " coverMediaId,a.publish_status publishStatus,a.published_at"
                + " publishedAt,a.sort_order sortOrder,a.updated_at updatedAt FROM"
                + " mini_content_article a JOIN mini_content_category c ON c.id=a.category_id WHERE"
                + " a.deleted=0 ORDER BY a.sort_order,a.id DESC",
            Map.of()));
  }

  @GetMapping("/articles/{id}")
  public ApiResponse<Map<String, Object>> article(@PathVariable Long id) {
    return ApiResponse.ok(
        one(
            "SELECT id,article_no articleNo,category_id"
                + " categoryId,title,subtitle,summary,cover_media_id"
                + " coverMediaId,content_blocks_json contentBlocksJson,publish_status"
                + " publishStatus,published_at publishedAt,sort_order sortOrder FROM"
                + " mini_content_article WHERE id=:id AND deleted=0",
            id,
            "内容不存在"));
  }

  @PostMapping("/articles")
  @Transactional
  public ApiResponse<Map<String, Long>> createArticle(@Valid @RequestBody ArticleRequest r) {
    String blocks = validator.validate(r.contentBlocksJson());
    MapSqlParameterSource p = articleParams(r, blocks);
    p.addValue(
        "articleNo",
        "ART" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
    long id =
        inserts.insert(
            "INSERT INTO"
                + " mini_content_article(article_no,category_id,title,subtitle,summary,cover_media_id,content_blocks_json,publish_status,sort_order,created_by,updated_by)"
                + " VALUES(:articleNo,:categoryId,:title,:subtitle,:summary,:coverMediaId,:blocks,'DRAFT',:sortOrder,:userId,:userId)",
            p);
    logs.record("MINI_CONTENT", "CREATE_ARTICLE", id, "新增图文 " + r.title());
    return ApiResponse.ok(Map.of("id", id));
  }

  @PutMapping("/articles/{id}")
  @Transactional
  public ApiResponse<Void> updateArticle(
      @PathVariable Long id, @Valid @RequestBody ArticleRequest r) {
    String blocks = validator.validate(r.contentBlocksJson());
    MapSqlParameterSource p = articleParams(r, blocks).addValue("id", id);
    required(
        jdbc.update(
            "UPDATE mini_content_article SET"
                + " category_id=:categoryId,title=:title,subtitle=:subtitle,summary=:summary,cover_media_id=:coverMediaId,content_blocks_json=:blocks,sort_order=:sortOrder,updated_by=:userId"
                + " WHERE id=:id AND deleted=0",
            p),
        "内容不存在");
    logs.record("MINI_CONTENT", "UPDATE_ARTICLE", id, "修改图文 " + r.title());
    return ApiResponse.ok();
  }

  @PostMapping("/articles/{id}/publish")
  @Transactional
  public ApiResponse<Void> publish(@PathVariable Long id) {
    publishRecord(id, "PUBLISH", "PUBLISHED");
    return ApiResponse.ok();
  }

  @PostMapping("/articles/{id}/offline")
  @Transactional
  public ApiResponse<Void> offline(@PathVariable Long id) {
    publishRecord(id, "OFFLINE", "OFFLINE");
    return ApiResponse.ok();
  }

  @GetMapping("/publish-records")
  public ApiResponse<List<Map<String, Object>>> records() {
    return ApiResponse.ok(
        jdbc.queryForList(
            "SELECT r.id,r.article_id articleId,a.title,r.version_no versionNo,r.operation_type"
                + " operationType,r.published_by publishedBy,r.published_at publishedAt FROM"
                + " mini_content_publish_record r JOIN mini_content_article a ON a.id=r.article_id"
                + " ORDER BY r.id DESC LIMIT 200",
            Map.of()));
  }

  @GetMapping("/places/categories")
  public ApiResponse<List<Map<String, Object>>> placeCategories() {
    return ApiResponse.ok(
        jdbc.queryForList(
            "SELECT id,category_name categoryName,icon,sort_order sortOrder,status FROM"
                + " campus_place_category WHERE deleted=0 ORDER BY sort_order,id",
            Map.of()));
  }

  @GetMapping("/places")
  public ApiResponse<List<Map<String, Object>>> places() {
    return ApiResponse.ok(
        jdbc.queryForList(
            "SELECT p.id,p.place_no placeNo,p.category_id categoryId,c.category_name"
                + " categoryName,p.place_name"
                + " placeName,p.longitude,p.latitude,p.address,p.summary,p.contact_phone"
                + " contactPhone,p.business_hours businessHours,p.cover_media_id"
                + " coverMediaId,p.sort_order sortOrder,p.status FROM campus_place p JOIN"
                + " campus_place_category c ON c.id=p.category_id WHERE p.deleted=0 ORDER BY"
                + " p.sort_order,p.id",
            Map.of()));
  }

  @GetMapping("/places/{id}")
  public ApiResponse<Map<String, Object>> place(@PathVariable Long id) {
    return ApiResponse.ok(
        one(
            "SELECT id,place_no placeNo,category_id categoryId,place_name"
                + " placeName,longitude,latitude,address,summary,contact_phone"
                + " contactPhone,business_hours businessHours,cover_media_id"
                + " coverMediaId,detail_blocks_json detailBlocksJson,sort_order sortOrder,status"
                + " FROM campus_place WHERE id=:id AND deleted=0",
            id,
            "地点不存在"));
  }

  @PostMapping("/places")
  @Transactional
  public ApiResponse<Map<String, Long>> createPlace(@Valid @RequestBody PlaceRequest r) {
    String blocks = validator.validate(r.detailBlocksJson() == null ? "[]" : r.detailBlocksJson());
    MapSqlParameterSource p =
        placeParams(r, blocks)
            .addValue(
                "placeNo",
                "PLC"
                    + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
    long id =
        inserts.insert(
            "INSERT INTO"
                + " campus_place(place_no,category_id,place_name,longitude,latitude,address,summary,contact_phone,business_hours,cover_media_id,detail_blocks_json,sort_order,status,created_by,updated_by)"
                + " VALUES(:placeNo,:categoryId,:placeName,:longitude,:latitude,:address,:summary,:phone,:hours,:coverMediaId,:blocks,:sortOrder,:status,:userId,:userId)",
            p);
    return ApiResponse.ok(Map.of("id", id));
  }

  @PutMapping("/places/{id}")
  @Transactional
  public ApiResponse<Void> updatePlace(@PathVariable Long id, @Valid @RequestBody PlaceRequest r) {
    String blocks = validator.validate(r.detailBlocksJson() == null ? "[]" : r.detailBlocksJson());
    MapSqlParameterSource p = placeParams(r, blocks).addValue("id", id);
    required(
        jdbc.update(
            "UPDATE campus_place SET"
                + " category_id=:categoryId,place_name=:placeName,longitude=:longitude,latitude=:latitude,address=:address,summary=:summary,contact_phone=:phone,business_hours=:hours,cover_media_id=:coverMediaId,detail_blocks_json=:blocks,sort_order=:sortOrder,status=:status,updated_by=:userId"
                + " WHERE id=:id AND deleted=0",
            p),
        "地点不存在");
    return ApiResponse.ok();
  }

  @DeleteMapping("/places/{id}")
  public ApiResponse<Void> deletePlace(@PathVariable Long id) {
    required(
        jdbc.update(
            "UPDATE campus_place SET deleted=1,updated_by=:userId WHERE id=:id AND deleted=0",
            Map.of("id", id, "userId", SecurityUtils.current().id())),
        "地点不存在");
    return ApiResponse.ok();
  }

  private MapSqlParameterSource categoryParams(CategoryRequest r) {
    return new MapSqlParameterSource()
        .addValue("code", r.categoryCode().trim())
        .addValue("name", r.categoryName().trim())
        .addValue("description", blank(r.description()))
        .addValue("sortOrder", r.sortOrder() == null ? 0 : r.sortOrder())
        .addValue("status", r.status() == null ? 1 : r.status())
        .addValue("userId", SecurityUtils.current().id());
  }

  private MapSqlParameterSource articleParams(ArticleRequest r, String blocks) {
    return new MapSqlParameterSource()
        .addValue("categoryId", r.categoryId())
        .addValue("title", r.title().trim())
        .addValue("subtitle", blank(r.subtitle()))
        .addValue("summary", blank(r.summary()))
        .addValue("coverMediaId", r.coverMediaId())
        .addValue("blocks", blocks)
        .addValue("sortOrder", r.sortOrder() == null ? 0 : r.sortOrder())
        .addValue("userId", SecurityUtils.current().id());
  }

  private MapSqlParameterSource placeParams(PlaceRequest r, String blocks) {
    return new MapSqlParameterSource()
        .addValue("categoryId", r.categoryId())
        .addValue("placeName", r.placeName().trim())
        .addValue("longitude", r.longitude())
        .addValue("latitude", r.latitude())
        .addValue("address", r.address().trim())
        .addValue("summary", blank(r.summary()))
        .addValue("phone", blank(r.contactPhone()))
        .addValue("hours", blank(r.businessHours()))
        .addValue("coverMediaId", r.coverMediaId())
        .addValue("blocks", blocks)
        .addValue("sortOrder", r.sortOrder() == null ? 0 : r.sortOrder())
        .addValue("status", r.status() == null ? 1 : r.status())
        .addValue("userId", SecurityUtils.current().id());
  }

  private void publishRecord(Long id, String operation, String status) {
    Map<String, Object> article =
        one("SELECT * FROM mini_content_article WHERE id=:id AND deleted=0", id, "内容不存在");
    Integer version =
        jdbc.queryForObject(
            "SELECT COALESCE(MAX(version_no),0)+1 FROM mini_content_publish_record WHERE"
                + " article_id=:id",
            Map.of("id", id),
            Integer.class);
    try {
      String snapshot = objectMapper.writeValueAsString(article);
      jdbc.update(
          "INSERT INTO"
              + " mini_content_publish_record(article_id,version_no,content_snapshot_json,operation_type,published_by)"
              + " VALUES(:id,:version,:snapshot,:operation,:userId)",
          new MapSqlParameterSource()
              .addValue("id", id)
              .addValue("version", version)
              .addValue("snapshot", snapshot)
              .addValue("operation", operation)
              .addValue("userId", SecurityUtils.current().id()));
      jdbc.update(
          "UPDATE mini_content_article SET"
              + " publish_status=:status,published_at=:time,updated_by=:userId WHERE id=:id",
          new MapSqlParameterSource()
              .addValue("id", id)
              .addValue("status", status)
              .addValue("time", "PUBLISHED".equals(status) ? LocalDateTime.now() : null)
              .addValue("userId", SecurityUtils.current().id()));
      logs.record("MINI_CONTENT", operation, id, operation + "图文内容");
    } catch (Exception e) {
      throw new BusinessException("保存发布快照失败");
    }
  }

  private Map<String, Object> one(String sql, Long id, String message) {
    List<Map<String, Object>> rows = jdbc.queryForList(sql, Map.of("id", id));
    if (rows.isEmpty()) throw new BusinessException(message);
    return rows.get(0);
  }

  private void required(int count, String message) {
    if (count == 0) throw new BusinessException(message);
  }

  private String blank(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
