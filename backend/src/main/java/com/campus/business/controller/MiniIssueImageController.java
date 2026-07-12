package com.campus.business.controller;

import com.campus.business.common.ApiResponse;
import com.campus.business.common.BusinessException;
import com.campus.business.security.SecurityUtils;
import com.campus.business.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/mini/issues")
@RequiredArgsConstructor
public class MiniIssueImageController {
    private final NamedParameterJdbcTemplate jdbc;
    private final FileStorageService files;

    @PostMapping("/{id}/images")
    @Transactional
    public ApiResponse<Map<String, String>> upload(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        Long issueCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM support_issue WHERE id=:id AND submitter_id=:userId AND deleted=0",
                Map.of("id", id, "userId", SecurityUtils.current().id()),
                Long.class
        );
        if (issueCount == null || issueCount == 0) {
            throw new BusinessException("问题不存在或无权操作");
        }
        Long imageCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM support_issue_image WHERE issue_id=:id",
                Map.of("id", id),
                Long.class
        );
        if (imageCount != null && imageCount >= 3) {
            throw new BusinessException("每个问题最多上传3张图片");
        }
        String url = files.store(file);
        jdbc.update(
                "INSERT INTO support_issue_image(issue_id,image_url) VALUES(:id,:url)",
                Map.of("id", id, "url", url)
        );
        return ApiResponse.ok(Map.of("url", url));
    }
}
