package com.learn.learnE_Backend.admin;

import com.learn.learnE_Backend.admin.dto.AdminUserDto;
import com.learn.learnE_Backend.auth.Role;
import com.learn.learnE_Backend.auth.User;
import com.learn.learnE_Backend.auth.UserRepository;
import com.learn.learnE_Backend.auth.UserStatus;
import com.learn.learnE_Backend.reading.ReadingAttemptRepository;
import com.learn.learnE_Backend.sentence.UserSentenceAttemptRepository;
import com.learn.learnE_Backend.skipahead.SkipAheadTestRepository;
import com.learn.learnE_Backend.vocabulary.UserCourseEnrollmentRepository;
import com.learn.learnE_Backend.vocabulary.UserLessonDayProgressRepository;
import com.learn.learnE_Backend.vocabulary.UserWordProgressRepository;
import com.learn.learnE_Backend.writing.WritingSubmissionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final UserWordProgressRepository wordProgressRepository;
    private final UserCourseEnrollmentRepository enrollmentRepository;
    private final UserLessonDayProgressRepository dayProgressRepository;
    private final ReadingAttemptRepository readingAttemptRepository;
    private final WritingSubmissionRepository writingSubmissionRepository;
    private final UserSentenceAttemptRepository sentenceAttemptRepository;
    private final SkipAheadTestRepository skipAheadTestRepository;

    public AdminUserService(
            UserRepository userRepository,
            UserWordProgressRepository wordProgressRepository,
            UserCourseEnrollmentRepository enrollmentRepository,
            UserLessonDayProgressRepository dayProgressRepository,
            ReadingAttemptRepository readingAttemptRepository,
            WritingSubmissionRepository writingSubmissionRepository,
            UserSentenceAttemptRepository sentenceAttemptRepository,
            SkipAheadTestRepository skipAheadTestRepository
    ) {
        this.userRepository = userRepository;
        this.wordProgressRepository = wordProgressRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.dayProgressRepository = dayProgressRepository;
        this.readingAttemptRepository = readingAttemptRepository;
        this.writingSubmissionRepository = writingSubmissionRepository;
        this.sentenceAttemptRepository = sentenceAttemptRepository;
        this.skipAheadTestRepository = skipAheadTestRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminUserDto> list() {
        return userRepository.findAll().stream()
                // Accounts waiting on a decision come first, then newest.
                .sorted(Comparator
                        .comparing((User u) -> u.getStatus() != UserStatus.PENDING)
                        .thenComparing(User::getCreatedAt, Comparator.reverseOrder()))
                .map(user -> new AdminUserDto(
                        user.getId(),
                        user.getEmail(),
                        user.getDisplayName(),
                        user.getRole(),
                        user.getStatus(),
                        user.getCreatedAt(),
                        (int) wordProgressRepository.countByUser_Id(user.getId()),
                        (int) wordProgressRepository.countFullyMastered(user.getId())))
                .toList();
    }

    @Transactional
    public AdminUserDto setStatus(User actor, Long userId, UserStatus status) {
        User user = require(userId);
        // Locking yourself out would leave nobody able to approve anyone.
        if (user.getId().equals(actor.getId()) && status != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể tự khoá tài khoản của mình");
        }
        user.setStatus(status);
        return toDto(userRepository.save(user));
    }

    @Transactional
    public AdminUserDto setRole(User actor, Long userId, Role role) {
        User user = require(userId);
        if (user.getId().equals(actor.getId()) && role != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể tự bỏ quyền quản trị của mình");
        }
        user.setRole(role);
        return toDto(userRepository.save(user));
    }

    /** Removes the account together with everything that references it. */
    @Transactional
    public void delete(User actor, Long userId) {
        User user = require(userId);
        if (user.getId().equals(actor.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể tự xoá tài khoản của mình");
        }

        // Order matters: every table pointing at the user has to go before the user row itself.
        skipAheadTestRepository.deleteAll(skipAheadTestRepository.findByUser_Id(userId));
        sentenceAttemptRepository.deleteAll(
                sentenceAttemptRepository.findByUser_IdOrderBySubmittedAtDesc(userId));
        wordProgressRepository.deleteByUser_Id(userId);
        dayProgressRepository.deleteByUser_Id(userId);
        enrollmentRepository.deleteByUser_Id(userId);
        readingAttemptRepository.deleteAll(readingAttemptRepository.findByUser_Id(userId));
        writingSubmissionRepository.deleteAll(
                writingSubmissionRepository.findByUser_IdOrderBySubmittedAtDesc(userId));
        userRepository.delete(user);
    }

    private User require(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"));
    }

    private AdminUserDto toDto(User user) {
        return new AdminUserDto(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt(),
                (int) wordProgressRepository.countByUser_Id(user.getId()),
                (int) wordProgressRepository.countFullyMastered(user.getId()));
    }
}
