package com.campus.business.controller;

import com.campus.business.common.ApiResponse;
import com.campus.business.common.BusinessException;
import com.campus.business.security.SecurityUtils;
import com.campus.business.service.ObjectMediaStorageService;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/mini/issues")
@RequiredArgsConstructor
public class MiniIssueImageController {
  private final NamedParameterJdbcTemplate jdbc;
  private final ObjectMediaStorageService files;

  @PostMapping("/{id}/images")
  @Transactional
  public ApiResponse<Map<String, String>> upload(
      @PathVariable Long id, @RequestParam("file") MultipartFile file) {
    Long issueCount =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM support_issue WHERE id=:id AND submitter_id=:userId AND"
                + " deleted=0",
            Map.of("id", id, "userId", SecurityUtils.current().id()),
            Long.class);
    if (issueCount == null || issueCount == 0) {
      throw new BusinessException("问题不存在或无权操作");
    }
    Long imageCount =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM support_issue_image WHERE issue_id=:id",
            Map.of("id", id),
            Long.class);
    if (imageCount != null && imageCount >= 3) {
      throw new BusinessException("每个问题最多上传3张图片");
    }
    String objectKey = files.store(file, "support-issues/" + id).objectKey();
    jdbc.update(
        "INSERT INTO support_issue_image(issue_id,image_url) VALUES(:id,:url)",
        Map.of("id", id, "url", objectKey));
    Long imageId =
        jdbc.queryForObject(
            "SELECT MAX(id) FROM support_issue_image WHERE issue_id=:id",
            Map.of("id", id),
            Long.class);
    return ApiResponse.ok(Map.of("url", "/api/mini/issues/" + id + "/images/" + imageId));
  }

  @org.springframework.web.bind.annotation.GetMapping("/{issueId}/images/{imageId}")
  public void view(
      @PathVariable Long issueId, @PathVariable Long imageId, HttpServletResponse response)
      throws java.io.IOException {
    var user = SecurityUtils.current();
    String ownerFilter =
        Set.of("OWNER", "ADMIN").contains(user.role()) ? "" : " AND i.submitter_id=:userId";
    var parameters =
        new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
            .addValue("issueId", issueId)
            .addValue("imageId", imageId)
            .addValue("userId", user.id());
    var rows =
        jdbc.queryForList(
            "SELECT img.image_url objectKey FROM support_issue_image img JOIN support_issue i ON"
                + " i.id=img.issue_id WHERE img.id=:imageId AND i.id=:issueId AND i.deleted=0"
                + ownerFilter,
            parameters);
    if (rows.isEmpty()) throw new BusinessException("图片不存在或无权查看");
    response.sendRedirect(files.temporaryUrl(String.valueOf(rows.get(0).get("objectKey"))));
  }
}
