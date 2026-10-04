package com.learn.learnE_Backend.writing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.learnE_Backend.ai.GeminiClient;
import com.learn.learnE_Backend.auth.User;
import com.learn.learnE_Backend.vocabulary.Topic;
import com.learn.learnE_Backend.vocabulary.TopicRepository;
import com.learn.learnE_Backend.writing.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class WritingService {

    private static final Logger log = LoggerFactory.getLogger(WritingService.class);

    private final WritingPromptRepository promptRepository;
    private final WritingSubmissionRepository submissionRepository;
    private final GeminiClient geminiClient;
    private final TopicRepository topicRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WritingService(
            WritingPromptRepository promptRepository,
            WritingSubmissionRepository submissionRepository,
            GeminiClient geminiClient,
            TopicRepository topicRepository
    ) {
        this.promptRepository = promptRepository;
        this.submissionRepository = submissionRepository;
        this.geminiClient = geminiClient;
        this.topicRepository = topicRepository;
    }

    @Transactional(readOnly = true)
    public List<WritingPromptSummaryDto> listPrompts(Long userId) {
        return promptRepository.findAll().stream()
                .map(prompt -> {
                    Integer bestScore = submissionRepository.findTopByUser_IdAndPrompt_IdOrderByScoreDesc(userId, prompt.getId())
                            .map(WritingSubmission::getScore)
                            .orElse(null);
                    long submissionsCount = submissionRepository.countByUser_IdAndPrompt_Id(userId, prompt.getId());
                    return new WritingPromptSummaryDto(
                            prompt.getId(),
                            prompt.getTitle(),
                            prompt.getTopic() != null ? prompt.getTopic().getName() : null,
                            prompt.getLevel(),
                            prompt.getMinWords(),
                            bestScore,
                            (int) submissionsCount
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public WritingPromptDetailDto getPrompt(Long promptId) {
        WritingPrompt prompt = promptRepository.findById(promptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Writing prompt not found"));
        return new WritingPromptDetailDto(
                prompt.getId(), prompt.getTitle(), prompt.getInstructions(),
                prompt.getTopic() != null ? prompt.getTopic().getName() : null,
                prompt.getLevel(), prompt.getMinWords()
        );
    }

    @Transactional(readOnly = true)
    public List<WritingSubmissionDto> listSubmissions(Long userId) {
        return submissionRepository.findByUser_IdOrderBySubmittedAtDesc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public WritingSubmissionDto submit(User user, Long promptId, SubmitWritingRequest request) {
        WritingPrompt prompt = promptRepository.findById(promptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Writing prompt not found"));

        int wordCount = countWords(request.content());
        WritingFeedbackDto feedback = gradeWithAi(prompt, request.content());

        WritingSubmission submission = WritingSubmission.builder()
                .user(user)
                .prompt(prompt)
                .content(request.content())
                .wordCount(wordCount)
                .score(feedback.score())
                .feedbackJson(writeJson(feedback))
                .build();

        WritingSubmission saved = submissionRepository.save(submission);
        return toDto(saved);
    }

    private WritingFeedbackDto gradeWithAi(WritingPrompt prompt, String content) {
        if (!geminiClient.isConfigured()) {
            return WritingFeedbackDto.unavailable("Chưa cấu hình AI chấm bài (thiếu GEMINI_API_KEY). Bài viết đã được lưu lại.");
        }

        String aiPrompt = """
                Bạn là một giáo viên tiếng Anh đang chấm bài viết của một học sinh người Việt trình độ %s.

                Đề bài: %s

                Bài làm của học sinh:
                \"\"\"
                %s
                \"\"\"

                Hãy chấm điểm và trả lời DUY NHẤT bằng JSON theo đúng cấu trúc sau, không thêm text nào khác:
                {
                  "score": <số nguyên từ 0 đến 10>,
                  "overallComment": "<nhận xét tổng quan ngắn gọn bằng tiếng Việt, giọng khích lệ>",
                  "corrections": [
                    {"original": "<đoạn văn bản gốc bị sai>", "suggestion": "<cách sửa đúng>", "explanation": "<giải thích ngắn bằng tiếng Việt>"}
                  ]
                }

                Chỉ liệt kê tối đa 8 lỗi quan trọng nhất trong "corrections". Nếu bài viết không có lỗi, để "corrections" là mảng rỗng.
                """.formatted(prompt.getLevel(), prompt.getInstructions(), content);

        try {
            // Marking a learner's work, so it follows the same model choice as sentence grading.
            String rawJson = geminiClient.generateJson(aiPrompt, geminiClient.gradingModel());
            return objectMapper.readValue(rawJson, WritingFeedbackDto.class);
        } catch (Exception ex) {
            log.warn("Gemini writing feedback failed", ex);
            return WritingFeedbackDto.unavailable(
                    "Không chấm được bằng AI: " + ex.getMessage() + " (Bài viết của bạn vẫn đã được lưu lại.)");
        }
    }

    private WritingSubmissionDto toDto(WritingSubmission submission) {
        WritingFeedbackDto feedback = readJson(submission.getFeedbackJson());
        return new WritingSubmissionDto(
                submission.getId(),
                submission.getPrompt().getId(),
                submission.getPrompt().getTitle(),
                submission.getContent(),
                submission.getWordCount(),
                submission.getScore(),
                feedback,
                submission.getSubmittedAt()
        );
    }

    private String writeJson(WritingFeedbackDto feedback) {
        try {
            return objectMapper.writeValueAsString(feedback);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize writing feedback", ex);
        }
    }

    private WritingFeedbackDto readJson(String json) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, WritingFeedbackDto.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private static int countWords(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return 0;
        return trimmed.split("\\s+").length;
    }

    // ---- Admin content management ----

    @Transactional
    public WritingPromptDetailDto createPrompt(CreateWritingPromptRequest request) {
        Topic topic = request.topicId() != null
                ? topicRepository.findById(request.topicId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"))
                : null;

        WritingPrompt prompt = WritingPrompt.builder()
                .topic(topic)
                .level(request.level())
                .title(request.title())
                .instructions(request.instructions())
                .minWords(request.minWords())
                .build();

        WritingPrompt saved = promptRepository.save(prompt);
        return getPrompt(saved.getId());
    }
}
