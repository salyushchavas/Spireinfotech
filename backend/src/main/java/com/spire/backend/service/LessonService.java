package com.spire.backend.service;

import com.spire.backend.dto.LessonDTO;
import com.spire.backend.dto.LessonRequest;
import com.spire.backend.entity.Course;
import com.spire.backend.entity.Lesson;
import com.spire.backend.exception.ResourceNotFoundException;
import com.spire.backend.exception.UnauthorizedException;
import com.spire.backend.repository.CourseRepository;
import com.spire.backend.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;

    public List<LessonDTO> getLessons(Long courseId, boolean includeVideoUrl) {
        return lessonRepository.findByCourseIdOrderByOrderIndex(courseId).stream()
                .map(l -> LessonDTO.from(l, includeVideoUrl || Boolean.TRUE.equals(l.getIsFree())))
                .collect(Collectors.toList());
    }

    @Transactional
    public LessonDTO createLesson(Long courseId, LessonRequest dto, Long userId, boolean isAdmin) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        if (!isAdmin && !course.getInstructor().getId().equals(userId)) {
            throw new UnauthorizedException("You can only add lessons to your own courses");
        }

        int nextOrder = dto.getOrderIndex() != null ? dto.getOrderIndex()
                : lessonRepository.findByCourseIdOrderByOrderIndex(courseId).size() + 1;

        Lesson lesson = Lesson.builder()
                .course(course)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .videoUrl(dto.getVideoUrl())
                .orderIndex(nextOrder)
                .durationMinutes(dto.getDurationMinutes())
                .isFree(dto.getIsFree() != null ? dto.getIsFree() : false)
                .build();

        Lesson saved = lessonRepository.save(lesson);

        // Update course lesson count
        course.setLessonsCount(lessonRepository.findByCourseIdOrderByOrderIndex(courseId).size());
        courseRepository.save(course);

        return LessonDTO.from(saved, true);
    }

    @Transactional
    public LessonDTO updateLesson(Long lessonId, LessonRequest dto, Long userId, boolean isAdmin) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));

        if (!isAdmin && !lesson.getCourse().getInstructor().getId().equals(userId)) {
            throw new UnauthorizedException("You can only edit lessons in your own courses");
        }

        if (dto.getTitle() != null) lesson.setTitle(dto.getTitle());
        if (dto.getDescription() != null) lesson.setDescription(dto.getDescription());
        if (dto.getVideoUrl() != null) lesson.setVideoUrl(dto.getVideoUrl());
        if (dto.getOrderIndex() != null) lesson.setOrderIndex(dto.getOrderIndex());
        if (dto.getDurationMinutes() != null) lesson.setDurationMinutes(dto.getDurationMinutes());
        if (dto.getIsFree() != null) lesson.setIsFree(dto.getIsFree());

        return LessonDTO.from(lessonRepository.save(lesson), true);
    }

    @Transactional
    public void deleteLesson(Long lessonId, Long userId, boolean isAdmin) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson", "id", lessonId));

        if (!isAdmin && !lesson.getCourse().getInstructor().getId().equals(userId)) {
            throw new UnauthorizedException("You can only delete lessons in your own courses");
        }

        Course course = lesson.getCourse();
        lessonRepository.delete(lesson);

        // Update course lesson count
        course.setLessonsCount(lessonRepository.findByCourseIdOrderByOrderIndex(course.getId()).size());
        courseRepository.save(course);
    }
}
