package com.campus.business.controller;

import com.campus.business.common.ApiResponse;
import com.campus.business.common.BusinessException;
import com.campus.business.common.PageResult;
import com.campus.business.security.SecurityUtils;
import com.campus.business.service.FileStorageService;
import com.campus.business.service.JdbcInsertService;
import com.campus.business.service.OperationLogService;
import com.campus.business.service.ReferenceNumberService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/issues")
@RequiredArgsConstructor
public class IssueController {
    private static final Set<String> ISSUE_TYPES = Set.of(
            "CAMPUS_CARD", "CAMPUS_NETWORK", "DRIVING_SCHOOL", "RENEWAL", "ACCOUNT", "OTHER"
    );
    private static final Set<String> STATUSES = Set.of("PENDING", "PROCESSING", "RESOLVED", "CLOSED");
    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcInsertService inserts;
    private final OperationLogService logs;
    private final FileStorageService files;
    private final ReferenceNumberService numbers;

    public record IssueRequest(
            @NotBlank String submitterName,
            @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "手机号格式错误") String contactPhone,
            @NotBlank String issueType,
            @NotBlank @Size(max = 5000, message = "问题描述不能超过5000个字符") String description,
            String businessNumber
    ) {
    }

    public record StatusRequest(
            @NotBlank String status,
            @Size(max = 1000, message = "处理备注不能超过1000个字符") String processRemark
    ) {
    }

    @GetMapping
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String issueType,
            @RequestParam(required = false) String keyword
    ) {
        page = Math.max(1, page);
        pageSize = Math.min(100, Math.max(1, pageSize));
        StringBuilder where = new StringBuilder(" WHERE i.deleted=0");
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        if (status != null && !status.isBlank()) {
            where.append(" AND i.status=:status");
            parameters.addValue("status", status);
        }
        if (issueType != null && !issueType.isBlank()) {
            where.append(" AND i.issue_type=:issueType");
            parameters.addValue("issueType", issueType);
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (i.issue_no LIKE :keyword OR i.submitter_name LIKE :keyword OR " +
                    "i.contact_phone LIKE :keyword OR i.description LIKE :keyword OR i.business_number LIKE :keyword)");
            parameters.addValue("keyword", "%" + keyword.trim() + "%");
        }
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM support_issue i" + where, parameters, Long.class);
        parameters.addValue("offset", (page - 1) * pageSize).addValue("size", pageSize);
        String sql = "SELECT i.id,i.issue_no issueNo,i.submitter_name submitterName,i.submitter_type submitterType," +
                "i.contact_phone contactPhone,i.issue_type issueType,i.description,i.business_number businessNumber," +
                "i.status,i.process_remark processRemark,u.real_name processorName,i.submitted_at submittedAt," +
                "i.processed_at processedAt FROM support_issue i LEFT JOIN sys_user u ON u.id=i.processor_id" +
                where + " ORDER BY i.submitted_at DESC,i.id DESC LIMIT :offset,:size";
        return ApiResponse.ok(new PageResult<>(jdbc.queryForList(sql, parameters), total, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT i.*,u.real_name processorName FROM support_issue i " +
                        "LEFT JOIN sys_user u ON u.id=i.processor_id WHERE i.id=:id AND i.deleted=0",
                Map.of("id", id)
        );
        if (rows.isEmpty()) {
            throw new BusinessException("问题不存在");
        }
        Map<String, Object> result = new LinkedHashMap<>(rows.get(0));
        result.put("images", jdbc.queryForList(
                "SELECT id,image_url imageUrl FROM support_issue_image WHERE issue_id=:id ORDER BY id",
                Map.of("id", id)
        ));
        return ApiResponse.ok(result);
    }

    @PostMapping
    @Transactional
    public ApiResponse<Map<String, Long>> create(@Valid @RequestBody IssueRequest request) {
        validateIssueType(request.issueType());
        var user = SecurityUtils.current();
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("issueNo", numbers.issueNumber())
                .addValue("userId", user.id())
                .addValue("name", request.submitterName().trim())
                .addValue("phone", request.contactPhone())
                .addValue("issueType", request.issueType())
                .addValue("description", request.description().trim())
                .addValue("businessNumber", blankToNull(request.businessNumber()));
        long id = inserts.insert(
                "INSERT INTO support_issue(issue_no,submitter_id,submitter_type,submitter_name,contact_phone," +
                        "issue_type,description,business_number,status) " +
                        "VALUES(:issueNo,:userId,'AGENT',:name,:phone,:issueType,:description,:businessNumber,'PENDING')",
                parameters
        );
        return ApiResponse.ok(Map.of("id", id));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> status(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        if (!STATUSES.contains(request.status())) {
            throw new BusinessException("问题状态不正确");
        }
        int updated = jdbc.update(
                "UPDATE support_issue SET status=:status,process_remark=:remark,processor_id=:userId," +
                        "processed_at=CASE WHEN :status IN ('RESOLVED','CLOSED') THEN NOW() ELSE NULL END " +
                        "WHERE id=:id AND deleted=0",
                new MapSqlParameterSource()
                        .addValue("status", request.status())
                        .addValue("remark", blankToNull(request.processRemark()))
                        .addValue("userId", SecurityUtils.current().id())
                        .addValue("id", id)
        );
        if (updated == 0) {
            throw new BusinessException("问题不存在");
        }
        logs.record("ISSUE", "STATUS", id, "问题状态修改为 " + request.status());
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/images")
    @Transactional
    public ApiResponse<Map<String, String>> image(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        ensureImageSlot(id);
        String url = files.store(file);
        jdbc.update(
                "INSERT INTO support_issue_image(issue_id,image_url) VALUES(:id,:url)",
                Map.of("id", id, "url", url)
        );
        return ApiResponse.ok(Map.of("url", url));
    }

    private void ensureImageSlot(Long id) {
        Long issueCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM support_issue WHERE id=:id AND deleted=0",
                Map.of("id", id),
                Long.class
        );
        if (issueCount == null || issueCount == 0) {
            throw new BusinessException("问题不存在");
        }
        Long imageCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM support_issue_image WHERE issue_id=:id",
                Map.of("id", id),
                Long.class
        );
        if (imageCount != null && imageCount >= 3) {
            throw new BusinessException("每个问题最多上传3张图片");
        }
    }

    private void validateIssueType(String issueType) {
        if (!ISSUE_TYPES.contains(issueType)) {
            throw new BusinessException("问题类型不正确");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
