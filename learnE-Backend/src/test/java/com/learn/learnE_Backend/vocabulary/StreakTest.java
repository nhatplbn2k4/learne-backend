package com.learn.learnE_Backend.vocabulary;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Telling a running streak from a stored one.
 *
 * <p>The count is only recomputed when a day is finished, so the stored number outlives the
 * streak it describes: a learner who stopped three weeks ago still carries the figure they
 * stopped on. The dashboard was showing exactly that, a lit flame over a run that had been dead
 * for a fortnight. Nothing runs at midnight to retire it, so the lapse has to be worked out on
 * read, which is what these tests pin down.
 */
class StreakTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 6);

    private static UserCourseEnrollment enrollment(int streak, LocalDate lastStudy) {
        return UserCourseEnrollment.builder()
                .streakCount(streak)
                .lastStudyDate(lastStudy)
                .build();
    }

    @Test
    void studiedTodayMeansTheStreakIsRunning() {
        assertThat(enrollment(5, TODAY).currentStreak(TODAY)).isEqualTo(5);
    }

    /** One night does not break it — the learner still has all of today to carry on. */
    @Test
    void studiedYesterdayMeansTheStreakIsStillAlive() {
        assertThat(enrollment(5, TODAY.minusDays(1)).currentStreak(TODAY)).isEqualTo(5);
    }

    @Test
    void missingADayEndsIt() {
        assertThat(enrollment(5, TODAY.minusDays(2)).currentStreak(TODAY)).isZero();
    }

    /** The case that was on screen: a fortnight idle, still showing a lit flame. */
    @Test
    void aLongAbsenceEndsItHoweverBigTheStoredCount() {
        assertThat(enrollment(2, TODAY.minusDays(21)).currentStreak(TODAY)).isZero();
    }

    @Test
    void neverStudiedHasNoStreak() {
        assertThat(enrollment(0, null).currentStreak(TODAY)).isZero();
    }

    /** A lapsed run leaves the stored count alone; only the reading of it changes. */
    @Test
    void lapsingDoesNotEraseTheStoredCount() {
        UserCourseEnrollment lapsed = enrollment(7, TODAY.minusDays(30));

        assertThat(lapsed.currentStreak(TODAY)).isZero();
        assertThat(lapsed.getStreakCount()).isEqualTo(7);
    }

    /** What separates the record from the current run: it has to survive the break. */
    @Test
    void theRecordOutlivesTheRunThatSetIt() {
        UserCourseEnrollment lapsed = UserCourseEnrollment.builder()
                .streakCount(2)
                .longestStreak(9)
                .lastStudyDate(TODAY.minusDays(21))
                .build();

        assertThat(lapsed.currentStreak(TODAY)).isZero();
        assertThat(lapsed.getLongestStreak()).isEqualTo(9);
    }

    @Test
    void anEnrollmentStartsWithNoStreakAtAll() {
        UserCourseEnrollment fresh = UserCourseEnrollment.builder().build();

        assertThat(fresh.getStreakCount()).isZero();
        assertThat(fresh.getLongestStreak()).isZero();
        assertThat(fresh.currentStreak(TODAY)).isZero();
    }
}
