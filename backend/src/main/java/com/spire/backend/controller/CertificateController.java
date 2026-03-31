package com.spire.backend.controller;

import com.spire.backend.dto.ApiResponse;
import com.spire.backend.entity.Certificate;
import com.spire.backend.service.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;

    // ─── Generate certificate ───────────────────────────────────────

    @PostMapping("/generate/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateCertificate(
            @PathVariable Long courseId,
            Authentication auth) {
        Long userId = Long.parseLong(auth.getPrincipal().toString());
        Certificate cert = certificateService.generateCertificate(courseId, userId);

        return ResponseEntity.ok(ApiResponse.success("Certificate generated", Map.of(
                "id", cert.getId(),
                "certificateId", cert.getCertificateId(),
                "certificateUrl", cert.getCertificateUrl(),
                "issuedAt", cert.getIssuedAt().toString()
        )));
    }

    // ─── Check if certificate exists ────────────────────────────────

    @GetMapping("/check/{courseId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkCertificate(
            @PathVariable Long courseId,
            Authentication auth) {
        Long userId = Long.parseLong(auth.getPrincipal().toString());
        Certificate cert = certificateService.getCertificate(courseId, userId);

        if (cert == null) {
            return ResponseEntity.ok(ApiResponse.success(Map.of("exists", false)));
        }

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "exists", true,
                "certificateId", cert.getCertificateId(),
                "certificateUrl", cert.getCertificateUrl(),
                "issuedAt", cert.getIssuedAt().toString()
        )));
    }

    // ─── Get all user certificates ──────────────────────────────────

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyCertificates(Authentication auth) {
        Long userId = Long.parseLong(auth.getPrincipal().toString());
        List<Certificate> certs = certificateService.getUserCertificates(userId);

        List<Map<String, Object>> data = certs.stream().map(c -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", c.getId());
            m.put("courseTitle", c.getCourse().getTitle());
            m.put("certificateUrl", c.getCertificateUrl());
            m.put("issuedAt", c.getIssuedAt().toString());
            return m;
        }).toList();

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    // ─── Public verification ───────────────────────────────────────

    @GetMapping("/verify/{certificateId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyCertificate(
            @PathVariable String certificateId) {
        return certificateService.findByCertificateId(certificateId)
                .map(cert -> {
                    Map<String, Object> data = new java.util.HashMap<>();
                    data.put("valid", true);
                    data.put("studentName", cert.getUser().getFullName());
                    data.put("courseTitle", cert.getCourse().getTitle());
                    data.put("issuedAt", cert.getIssuedAt().toString());
                    data.put("certificateId", cert.getCertificateId());
                    return ResponseEntity.ok(ApiResponse.success(data));
                })
                .orElse(ResponseEntity.ok(ApiResponse.success(Map.of("valid", false))));
    }

    // ─── Download PDF ───────────────────────────────────────────────

    @GetMapping("/download/{courseId}/{fileName}")
    public ResponseEntity<Resource> downloadCertificate(
            @PathVariable String courseId, @PathVariable String fileName) {
        File file = new File("certificates/" + courseId + "/" + fileName);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }
}
