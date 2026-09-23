package com.learn.learnE_Backend.reading;

import com.learn.learnE_Backend.auth.User;
import com.learn.learnE_Backend.reading.dto.*;
import com.learn.learnE_Backend.vocabulary.Topic;
import com.learn.learnE_Backend.vocabulary.TopicRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class ReadingService {

    private final ReadingPassageRepository passageRepository;
    private final ReadingQuestionRepository questionRepository;
    private final ReadingAttemptRepository attemptRepository;
    private final TopicRepository topicRepository;

    public ReadingService(
            ReadingPassageRepository passageRepository,
            ReadingQuestionRepository questionRepository,
            ReadingAttemptRepository attemptRepository,
            TopicRepository topicRepository
    ) {
        this.passageRepository = passageRepository;
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
        this.topicRepository = topicRepository;
    }

    @Transactional(readOnly = true)
    public List<ReadingPassageSummaryDto> listPassages(Long userId) {
        return passageRepository.findAll().stream()
                .map(passage -> {
                    long questionCount = questionRepository.countByPassage_Id(passage.getId());
                    Integer bestScore = attemptRepository.findTopByUser_IdAndPassage_IdOrderByScoreDesc(userId, passage.getId())
                            .map(ReadingAttempt::getScore)
                            .orElse(null);
                    long attemptsCount = attemptRepository.countByUser_IdAndPassage_Id(userId, passage.getId());
                    return new ReadingPassageSummaryDto(
                            passage.getId(),
                            passage.getTitle(),
                            passage.getTopic() != null ? passage.getTopic().getName() : null,
                            passage.getLevel(),
                            (int) questionCount,
                            bestScore,
                            (int) attemptsCount
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public ReadingPassageDetailDto getPassage(Long passageId) {
        ReadingPassage passage = passageRepository.findById(passageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reading passage not found"));

        List<ReadingQuestionDto> questions = questionRepository.findByPassage_IdOrderByIdAsc(passageId).stream()
                .map(ReadingQuestionDto::from)
                .toList();

        return new ReadingPassageDetailDto(
                passage.getId(),
                passage.getTitle(),
                passage.getContent(),
                passage.getTopic() != null ? passage.getTopic().getName() : null,
                passage.getLevel(),
                questions
        );
    }

    @Transactional
    public ReadingAttemptResultDto submit(User user, Long passageId, SubmitReadingRequest request) {
        ReadingPassage passage = passageRepository.findById(passageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reading passage not found"));

        List<ReadingQuestion> questions = questionRepository.findByPassage_IdOrderByIdAsc(passageId);
        Map<Long, ReadingQuestion> questionsById = questions.stream()
                .collect(java.util.stream.Collectors.toMap(ReadingQuestion::getId, Function.identity()));

        ReadingAttempt attempt = ReadingAttempt.builder()
                .user(user)
                .passage(passage)
                .totalQuestions(questions.size())
                .build();

        int score = 0;
        List<GradedAnswerDto> graded = new java.util.ArrayList<>();

        for (SubmitAnswerRequest answer : request.answers()) {
            ReadingQuestion question = questionsById.get(answer.questionId());
            if (question == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question does not belong to this passage: " + answer.questionId());
            }
            boolean correct = question.getCorrectOption().equalsIgnoreCase(answer.selectedOption());
            if (correct) score++;

            attempt.getAnswers().add(ReadingAttemptAnswer.builder()
                    .attempt(attempt)
                    .question(question)
                    .selectedOption(answer.selectedOption().toUpperCase())
                    .correct(correct)
                    .build());

            graded.add(new GradedAnswerDto(
                    question.getId(), answer.selectedOption().toUpperCase(), question.getCorrectOption(), correct, question.getExplanation()
            ));
        }

        attempt.setScore(score);
        ReadingAttempt saved = attemptRepository.save(attempt);

        return new ReadingAttemptResultDto(saved.getId(), score, questions.size(), graded);
    }

    // ---- Admin content management ----

    @Transactional(readOnly = true)
    public AdminReadingPassageDto getPassageForAdmin(Long passageId) {
        ReadingPassage passage = passageRepository.findById(passageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reading passage not found"));
        List<AdminReadingQuestionDto> questions = questionRepository.findByPassage_IdOrderByIdAsc(passageId).stream()
                .map(AdminReadingQuestionDto::from)
                .toList();
        return new AdminReadingPassageDto(
                passage.getId(), passage.getTitle(), passage.getContent(),
                passage.getTopic() != null ? passage.getTopic().getId() : null,
                passage.getTopic() != null ? passage.getTopic().getName() : null,
                passage.getLevel(), questions
        );
    }

    @Transactional
    public AdminReadingPassageDto createPassageWithQuestions(CreateReadingPassageRequest request) {
        Topic topic = request.topicId() != null
                ? topicRepository.findById(request.topicId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"))
                : null;

        ReadingPassage passage = passageRepository.save(ReadingPassage.builder()
                .topic(topic)
                .level(request.level())
                .title(request.title())
                .content(request.content())
                .build());

        List<ReadingQuestion> questions = request.questions().stream()
                .map(q -> ReadingQuestion.builder()
                        .passage(passage)
                        .questionText(q.questionText())
                        .optionA(q.optionA())
                        .optionB(q.optionB())
                        .optionC(q.optionC())
                        .optionD(q.optionD())
                        .correctOption(q.correctOption().toUpperCase())
                        .explanation(q.explanation())
                        .build())
                .toList();
        questionRepository.saveAll(questions);

        return getPassageForAdmin(passage.getId());
    }
}
