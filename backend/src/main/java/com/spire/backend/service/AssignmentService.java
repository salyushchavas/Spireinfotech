package com.spire.backend.service;

import com.spire.backend.dto.AssignmentRequest;
import com.spire.backend.dto.GradeRequest;
import com.spire.backend.dto.SubmissionRequest;
import com.spire.backend.entity.*;
import com.spire.backend.exception.ResourceNotFoundException;
import com.spire.backend.exception.UnauthorizedException;
import com.spire.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;

    // ─── Assignments ────────────────────────────────────────────────

    @Transactional
    public Assignment createAssignment(Long courseId, AssignmentRequest dto, Long userId, boolean isAdmin) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        if (!isAdmin && !course.getInstructor().getId().equals(userId)) {
            throw new UnauthorizedException("You can only create assignments for your own courses");
        }

        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Assignment assignment = Assignment.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .dueDate(dto.getDueDate())
                .course(course)
                .createdBy(creator)
                .build();

        return assignmentRepository.save(assignment);
    }

    public List<Assignment> getAssignments(Long courseId, Long userId, boolean isAdmin) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        // Admin and course instructor can always view
        boolean isInstructor = course.getInstructor().getId().equals(userId);
        if (!isAdmin && !isInstructor) {
            // Students must be enrolled
            if (!enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
                throw new UnauthorizedException("You must be enrolled to view assignments");
            }
        }

        return assignmentRepository.findByCourseIdOrderByCreatedAtDesc(courseId);
    }

    // ─── Submissions ────────────────────────────────────────────────

    @Transactional
    public Submission submitAssignment(Long assignmentId, SubmissionRequest dto, Long studentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", "id", assignmentId));

        // Must be enrolled in the course
        if (!enrollmentRepository.existsByUserIdAndCourseId(studentId, assignment.getCourse().getId())) {
            throw new UnauthorizedException("You must be enrolled to submit assignments");
        }

        // Prevent duplicate submissions
        if (submissionRepository.existsByAssignmentIdAndStudentId(assignmentId, studentId)) {
            throw new IllegalArgumentException("You have already submitted this assignment");
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", studentId));

        Submission submission = Submission.builder()
                .assignment(assignment)
                .student(student)
                .content(dto.getContent())
                .build();

        return submissionRepository.save(submission);
    }

    public List<Submission> getSubmissions(Long assignmentId, Long userId, boolean isAdmin) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", "id", assignmentId));

        // Only instructor (owner) or admin can see all submissions
        if (!isAdmin && !assignment.getCourse().getInstructor().getId().equals(userId)) {
            throw new UnauthorizedException("Only the course instructor or admin can view submissions");
        }

        return submissionRepository.findByAssignmentId(assignmentId);
    }

    // ─── Grading ────────────────────────────────────────────────────

    @Transactional
    public Submission gradeSubmission(Long submissionId, GradeRequest dto, Long userId, boolean isAdmin) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission", "id", submissionId));

        // Only course instructor or admin can grade
        if (!isAdmin && !submission.getAssignment().getCourse().getInstructor().getId().equals(userId)) {
            throw new UnauthorizedException("Only the course instructor or admin can grade submissions");
        }

        submission.setGrade(dto.getGrade());
        submission.setFeedback(dto.getFeedback());

        return submissionRepository.save(submission);
    }
}
