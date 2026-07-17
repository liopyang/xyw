package com.campus.business.controller;

import com.campus.business.common.ApiResponse;
import com.campus.business.common.BusinessException;
import com.campus.business.security.SecurityUtils;
import com.campus.business.service.JdbcInsertService;
import com.campus.business.service.ObjectMediaStorageService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class MiniMediaController {
  private final NamedParameterJdbcTemplate jdbc;
  private final JdbcInsertService inserts;
  private final ObjectMediaStorageService storage;

  @GetMapping("/api/admin/mini/media")
  public ApiResponse<List<Map<String, Object>>> list() {
    return ApiResponse.ok(
        jdbc.queryForList(
            "SELECT id,object_key objectKey,original_filename originalFilename,content_type"
                + " contentType,file_size fileSize,usage_status usageStatus,created_at createdAt"
                + " FROM media_asset WHERE deleted=0 ORDER BY id DESC",
            Map.of()));
  }

  @PostMapping("/api/admin/mini/media")
  @Transactional
  public ApiResponse<Map<String, Object>> upload(@RequestParam MultipartFile file) {
    var object = storage.store(file, "mini-content");
    long id =
        inserts.insert(
            "INSERT INTO"
                + " media_asset(object_key,original_filename,content_type,file_size,storage_provider,uploaded_by)"
                + " VALUES(:key,:name,:type,:size,'S3',:userId)",
            new MapSqlParameterSource()
                .addValue("key", object.objectKey())
                .addValue("name", object.originalFilename())
                .addValue("type", object.contentType())
                .addValue("size", object.fileSize())
                .addValue("userId", SecurityUtils.current().id()));
    return ApiResponse.ok(Map.of("id", id, "url", "/api/mini/public/media/" + id));
  }

  @DeleteMapping("/api/admin/mini/media/{id}")
  @Transactional
  public ApiResponse<Void> delete(@PathVariable Long id) {
    Map<String, Object> media = one(id);
    Long refs =
        jdbc.queryForObject(
            "SELECT (SELECT COUNT(*) FROM mini_content_article WHERE cover_media_id=:id AND"
                + " deleted=0)+(SELECT COUNT(*) FROM campus_place WHERE cover_media_id=:id AND"
                + " deleted=0)",
            Map.of("id", id),
            Long.class);
    if (refs != null && refs > 0) throw new BusinessException("素材仍被内容或地点使用，不能删除");
    storage.delete(String.valueOf(media.get("objectKey")));
    jdbc.update("UPDATE media_asset SET deleted=1 WHERE id=:id", Map.of("id", id));
    return ApiResponse.ok();
  }

  @GetMapping("/api/mini/public/media/{id}")
  public ResponseEntity<Void> publicMedia(@PathVariable Long id) {
    Map<String, Object> media = one(id);
    return ResponseEntity.status(HttpStatus.FOUND)
        .header(HttpHeaders.LOCATION, storage.temporaryUrl(String.valueOf(media.get("objectKey"))))
        .build();
  }

  private Map<String, Object> one(Long id) {
    List<Map<String, Object>> rows =
        jdbc.queryForList(
            "SELECT id,object_key objectKey FROM media_asset WHERE id=:id AND deleted=0",
            Map.of("id", id));
    if (rows.isEmpty()) throw new BusinessException("素材不存在");
    return rows.get(0);
  }
}
