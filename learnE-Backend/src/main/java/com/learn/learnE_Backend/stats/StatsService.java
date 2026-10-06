package com.learn.learnE_Backend.stats;

import com.learn.learnE_Backend.reading.ReadingAttempt;
import com.learn.learnE_Backend.reading.ReadingAttemptRepository;
import com.learn.learnE_Backend.sentence.UserSentenceAttempt;
import com.learn.learnE_Backend.sentence.UserSentenceAttemptRepository;
import com.learn.learnE_Backend.vocabulary.Language;
import com.learn.learnE_Backend.vocabulary.UserCourseEnrollment;
import com.learn.learnE_Backend.vocabulary.UserCourseEnrollmentRepository;
import com.learn.learnE_Backend.vocabulary.UserWordProgressRepository;
import com.learn.learnE_Backend.writing.WritingSubmission;
import com.learn.learnE_Backend.writing.WritingSubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.OptionalDouble;

@Service
public class StatsService {

    private final UserCourseEnrollmentRepository enrollmentRepository;
    private final UserWordProgressRepository wordProgressRepository;
    private final ReadingAttemptRepository readingAttemptRepository;
    private final WritingSubmissionRepository writingSubmissionRepository;
    private final UserSentenceAttemptRepository sentenceAttemptRepository;

    public StatsService(
            UserCourseEnrollmentRepository enrollmentRepository,
            UserWordProgressRepository wordProgressRepository,
            ReadingAttemptRepository readingAttemptRepository,
            WritingSubmissionRepository writingSubmissionRepository,
            UserSentenceAttemptRepository sentenceAttemptRepository
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.wordProgressRepository = wordProgressRepository;
        this.readingAttemptRepository = readingAttemptRepository;
        this.writingSubmissionRepository = writingSubmissionRepository;
        this.sentenceAttemptRepository = sentenceAttemptRepository;
    }

    /** @param language narrows every counter to one language; {@code null} counts everything. */
    @Transactional(readOnly = true)
    public StatsDto getStats(Long userId, Language language) {
        List<UserCourseEnrollment> enrollments = enrollmentRepository.findByUser_Id(userId).stream()
                .filter(e -> language == null || e.getCourse().getLanguage() == language)
                .toList();
        LocalDate today = LocalDate.now();
        // Best of the courses on each measure: one run still going is enough to call the learner
        // on a streak, and the record stands whichever course set it.
        int currentStreak = enrollments.stream().mapToInt(e -> e.currentStreak(today)).max().orElse(0);
        int longestStreak = enrollments.stream().mapToInt(UserCourseEnrollment::getLongestStreak).max().orElse(0);
        long totalWordsLearned;
        long wordsMastered;
        long wordsDueToday;
        if (language == null) {
            totalWordsLearned = wordProgressRepository.countByUser_Id(userId);
            wordsMastered = wordProgressRepository.countFullyMastered(userId);
            wordsDueToday = wordProgressRepository.countByUser_IdAndNextReviewDateLessThanEqual(userId, today);
        } else {
            totalWordsLearned = wordProgressRepository.countByUserAndLanguage(userId, language);
            wordsMastered = wordProgressRepository.countFullyMasteredByLanguage(userId, language);
            wordsDueToday = wordProgressRepository.countDueByLanguage(userId, language, today);
        }

        // Reading and writing are English-only content, so they drop out of the Chinese dashboard.
        boolean includeEnglishSkills = language != Language.CHINESE;
        List<ReadingAttempt> readingAttempts = includeEnglishSkills
                ? readingAttemptRepository.findByUser_Id(userId) : List.of();
        Double readingAverage = average(readingAttempts.stream()
                .mapToDouble(a -> a.getTotalQuestions() == 0 ? 0 : (a.getScore() * 100.0) / a.getTotalQuestions()));

        List<WritingSubmission> writingSubmissions = includeEnglishSkills
                ? writingSubmissionRepository.findByUser_IdOrderBySubmittedAtDesc(userId) : List.of();
        Double writingAverage = average(writingSubmissions.stream()
                .map(WritingSubmission::getScore)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Integer::doubleValue));

        // Sentence translation only exists for Chinese courses.
        boolean includeSentences = language != Language.ENGLISH;
        List<UserSentenceAttempt> sentenceAttempts = includeSentences
                ? sentenceAttemptRepository.findByUser_IdOrderBySubmittedAtDesc(userId) : List.of();
        Double sentenceAverage = average(sentenceAttempts.stream()
                .map(UserSentenceAttempt::getScore)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Integer::doubleValue));

        return new StatsDto(
                language,
                enrollments.size(),
                (int) totalWordsLearned,
                (int) wordsMastered,
                (int) wordsDueToday,
                currentStreak,
                longestStreak,
                readingAttempts.size(),
                readingAverage,
                writingSubmissions.size(),
                writingAverage,
                sentenceAttempts.size(),
                sentenceAverage
        );
    }

    private static Double average(java.util.stream.DoubleStream values) {
        OptionalDouble result = values.average();
        return result.isPresent() ? result.getAsDouble() : null;
    }
}
