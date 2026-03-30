package com.spire.backend.controller;

import com.spire.backend.dto.*;
import com.spire.backend.entity.Assignment;
import com.spire.backend.entity.Submission;
import com.spire.backend.service.AssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    // ─── Create assignment (instructor owner or admin) ──────────────

    @PostMapping("/api/courses/{courseId}/assignments")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('INSTRUCTOR') and @courseSecurity.isOwner(#courseId, authentication))")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createAssignment(
            @PathVariable Long courseId,
            @Valid @RequestBody AssignmentRequest dto,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        boolean isAdmin = isAdmin(authentication);

        Assignment assignment = assignmentService.createAssignment(courseId, dto, userId, isAdmin);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Assignment created", Map.of(
                "id", assignment.getId(),
                "title", assignment.getTitle(),
                "description", assignment.getDescription() != null ? assignment.getDescription() : "",
                "dueDate", assignment.getDueDate() != null ? assignment.getDueDate().toString() : "",
                "createdAt", assignment.getCreatedAt().toString()
        )));
    }

    // ─── Get assignments for course (enrolled students + instructor + admin) ─

    @GetMapping("/api/courses/{courseId}/assignments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAssignments(
            @PathVariable Long courseId,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        boolean isAdmin = isAdmin(authentication);

        List<Assignment> assignments = assignmentService.getAssignments(courseId, userId, isAdmin);
        List<Map<String, Object>> data = assignments.stream().map(a -> Map.<String, Object>of(
                "id", a.getId(),
                "title", a.getTitle(),
                "description", a.getDescription() != null ? a.getDescription() : "",
                "dueDate", a.getDueDate() != null ? a.getDueDate().toString() : "",
                "courseId", a.getCourse().getId(),
                "createdAt", a.getCreatedAt().toString()
        )).toList();

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    // ─── Submit assignment (student only) ───────────────────────────

    @PostMapping("/api/assignments/{assignmentId}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitAssignment(
            @PathVariable Long assignmentId,
            @Valid @RequestBody SubmissionRequest dto,
            Authentication authentication) {
        Long studentId = extractUserId(authentication);

        Submission submission = assignmentService.submitAssignment(assignmentId, dto, studentId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Assignment submitted", Map.of(
                "id", submission.getId(),
                "assignmentId", submission.getAssignment().getId(),
                "submittedAt", submission.getSubmittedAt().toString()
        )));
    }

    // ─── Get submissions (instructor/admin only) ────────────────────

    @GetMapping("/api/assignments/{assignmentId}/submissions")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSubmissions(
            @PathVariable Long assignmentId,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        boolean isAdmin = isAdmin(authentication);

        List<Submission> submissions = assignmentService.getSubmissions(assignmentId, userId, isAdmin);
        List<Map<String, Object>> data = submissions.stream().map(s -> Map.<String, Object>of(
                "id", s.getId(),
                "studentEmail", s.getStudent().getEmail(),
                "studentName", s.getStudent().getFullName(),
                "content", s.getContent(),
                "submittedAt", s.getSubmittedAt().toString(),
                "grade", s.getGrade() != null ? s.getGrade() : -1,
                "feedback", s.getFeedback() != null ? s.getFeedback() : ""
        )).toList();

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    // ─── Grade submission (instructor/admin only) ───────────────────

    @PutMapping("/api/submissions/{submissionId}/grade")
    @PreAuthorize("hasRole('ADMIN') or hasRole('INSTRUCTOR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> gradeSubmission(
            @PathVariable Long submissionId,
            @Valid @RequestBody GradeRequest dto,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        boolean isAdmin = isAdmin(authentication);

        Submission submission = assignmentService.gradeSubmission(submissionId, dto, userId, isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Submission graded", Map.of(
                "id", submission.getId(),
                "grade", submission.getGrade(),
                "feedback", submission.getFeedback() != null ? submission.getFeedback() : ""
        )));
    }

    // ─── Helpers ────────────────────────────────────────────────────

    private Long extractUserId(Authentication auth) {
        return Long.parseLong(auth.getPrincipal().toString());
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
