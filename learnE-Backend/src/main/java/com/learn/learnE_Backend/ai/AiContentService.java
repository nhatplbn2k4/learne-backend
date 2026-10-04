package com.learn.learnE_Backend.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.learnE_Backend.ai.dto.*;
import com.learn.learnE_Backend.grammar.dto.GrammarPointDto;
import com.learn.learnE_Backend.sentence.dto.SentenceFeedbackDto;
import com.learn.learnE_Backend.vocabulary.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AiContentService {

    private static final Logger log = LoggerFactory.getLogger(AiContentService.class);

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiContentService(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    /**
     * Generates one lesson day: a sub-theme title plus its vocabulary. The titles and words already
     * used elsewhere in the course are passed in so the AI picks a fresh angle instead of repeating.
     */
    public GeneratedDayWordsDto generateDayWords(
            Language language,
            String topicName,
            String level,
            int dayNumber,
            String existingTitle,
            List<String> usedTitles,
            List<String> wordsAlreadyInThisDay,
            List<String> wordsElsewhereInCourse,
            int count
    ) {
        String titleInstruction = existingTitle == null || existingTitle.isBlank()
                ? "Hãy tự đặt một tiêu đề (chủ đề con) tiếng Việt ngắn gọn cho ngày học này, "
                  + "khác hẳn các tiêu đề đã dùng bên dưới."
                : "Tiêu đề ngày học đã có sẵn là \"%s\" — giữ nguyên tiêu đề này trong kết quả trả về."
                        .formatted(existingTitle);

        String prompt = """
                %s

                Khoá học chủ đề "%s", trình độ %s. Đây là ngày học số %d.

                %s

                Các tiêu đề ngày học ĐÃ DÙNG trong khoá này (không được trùng hoặc gần giống):
                %s

                Từ vựng đã có ở CÁC NGÀY KHÁC của khoá này — tránh dùng lại:
                %s

                ==========  DANH SÁCH CẤM  ==========
                NGÀY HỌC NÀY ĐÃ CÓ SẴN những từ sau. Đây là ràng buộc quan trọng nhất:
                %s
                =====================================

                Hãy tạo TỐI ĐA %d từ vựng %s cho ngày học này, bám sát tiêu đề của ngày.

                QUY TẮC BẮT BUỘC:
                1. TUYỆT ĐỐI không sinh lại bất kỳ từ nào trong DANH SÁCH CẤM ở trên, kể cả khi
                   viết hoa/thường khác đi hay nghĩa tiếng Việt diễn đạt khác đi.
                2. Không sinh hai từ giống nhau trong chính kết quả trả về.
                3. Không sinh biến thể hiển nhiên của một từ đã có (số nhiều, chia thì, thêm dấu
                   gạch nối) — chúng bị coi là trùng.
                4. Nếu không tìm đủ %d từ mới phù hợp mà không phạm quy tắc 1-3, hãy trả về ÍT TỪ
                   HƠN. Trả về ít từ là ĐÚNG; lặp lại từ đã có là SAI.
                5. Trước khi trả lời, hãy tự rà lại danh sách vừa tạo: nếu thấy từ nào trùng với
                   DANH SÁCH CẤM hoặc trùng nhau, bỏ hẳn từ đó ra khỏi kết quả.

                Trả lời DUY NHẤT bằng JSON theo đúng cấu trúc:
                {
                  "dayTitle": "<tiêu đề ngày học bằng tiếng Việt>",
                  "words": [
                %s
                  ]
                }

                Không thêm giải thích nào ngoài JSON.
                """.formatted(
                personaFor(language), topicName, level, dayNumber, titleInstruction,
                formatList(usedTitles, "(chưa có ngày nào được đặt tên)"),
                formatList(wordsElsewhereInCourse, "(các ngày khác chưa có từ nào)"),
                formatList(wordsAlreadyInThisDay, "(ngày này chưa có từ nào - danh sách cấm trống)"),
                count, languageNameFor(language), count, wordSchemaFor(language));

        // Bulk vocabulary runs many requests, so use the lighter model with the bigger free quota.
        return callAndParse(prompt, GeneratedDayWordsDto.class, geminiClient.bulkModel());
    }

    private static String personaFor(Language language) {
        return language == Language.CHINESE
                ? "Bạn là chuyên gia biên soạn giáo trình tiếng Trung cho người Việt."
                : "Bạn là chuyên gia biên soạn giáo trình tiếng Anh cho người Việt.";
    }

    private static String languageNameFor(Language language) {
        return language == Language.CHINESE ? "tiếng Trung" : "tiếng Anh";
    }

    /** The word object inside the JSON schema — Chinese needs pinyin, âm Hán Việt and usage notes. */
    private static String wordSchemaFor(Language language) {
        if (language == Language.CHINESE) {
            return """
                        {
                          "term": "<từ bằng chữ Hán giản thể>",
                          "phonetic": "<pinyin CÓ DẤU THANH, ví dụ: xué xí>",
                          "partOfSpeech": "<loại từ: danh từ/động từ/tính từ/...>",
                          "vietnameseMeaning": "<nghĩa tiếng Việt>",
                          "hanViet": "<âm Hán Việt của từng chữ, ví dụ: HỌC TẬP>",
                          "usageNote": "<ghi chú ngữ pháp/cách dùng ngắn gọn bằng tiếng Việt, để chuỗi rỗng nếu không có gì đáng lưu ý>",
                          "exampleSentenceTarget": "<câu ví dụ bằng chữ Hán, đơn giản, phù hợp trình độ>",
                          "exampleSentenceVi": "<bản dịch câu ví dụ>"
                        }\
                    """;
        }
        return """
                    {
                      "term": "<từ tiếng Anh>",
                      "phonetic": "<phiên âm IPA, có dấu / />",
                      "partOfSpeech": "<loại từ: noun/verb/adjective/...>",
                      "vietnameseMeaning": "<nghĩa tiếng Việt>",
                      "hanViet": "",
                      "usageNote": "<ghi chú cách dùng ngắn gọn bằng tiếng Việt, để chuỗi rỗng nếu không có gì đáng lưu ý>",
                      "exampleSentenceTarget": "<câu ví dụ tiếng Anh, đơn giản, phù hợp trình độ>",
                      "exampleSentenceVi": "<bản dịch câu ví dụ>"
                    }\
                """;
    }

    /**
     * Sentences to translate from Vietnamese into the target language. {@code dayWords} must be
     * used; {@code earlierWords} may be. Anything else has to come back in {@code newWords} so the
     * learner gets the 汉字 and pinyin as a hint rather than being stuck.
     */
    public GeneratedSentencesDto generateSentences(
            Language language,
            String dayLabel,
            List<String> dayWords,
            List<String> earlierWords,
            int count,
            List<GrammarPointDto> catalogue
    ) {
        String prompt = """
                %s

                Bài học: "%s".

                TỪ VỰNG CỦA BÀI NÀY (mỗi câu phải dùng ít nhất một từ trong danh sách này):
                %s

                TỪ VỰNG ĐÃ HỌC TRƯỚC ĐÓ (được phép dùng thoải mái):
                %s

                Hãy soạn %d câu tiếng Việt để học viên dịch sang %s. Yêu cầu:
                - Câu ngắn, tự nhiên, đúng trình độ của bài; độ dài và cấu trúc đa dạng.
                - Câu tiếng Việt phải dịch được MỘT cách rõ ràng. Cần ngôi thứ ba thì viết rõ
                  "anh ấy" hoặc "cô ấy", KHÔNG dùng "bạn ấy"/"người ấy" — tiếng Trung buộc phải
                  chọn 他 hay 她, mà đề như vậy thì không có cơ sở để chọn.
                - Ưu tiên chỉ dùng từ trong hai danh sách trên.
                - Nếu BẮT BUỘC phải dùng một từ không có trong hai danh sách, hãy liệt kê từ đó trong
                  "newWords" của chính câu đó (kèm chữ viết, phiên âm và nghĩa tiếng Việt). Nếu câu chỉ
                  dùng từ đã cho thì "newWords" là mảng rỗng.

                %s
                Với mỗi câu, liệt kê trong "grammarCodes" những mã ngữ pháp mà bản dịch mẫu thực sự
                dùng tới. Chỉ lấy mã có trong danh mục; không có mã nào phù hợp thì để mảng rỗng.
                TUYỆT ĐỐI không tự bịa mã mới.

                Trả lời DUY NHẤT bằng JSON theo đúng cấu trúc:
                {
                  "sentences": [
                    {
                      "promptVi": "<câu tiếng Việt cần dịch>",
                      "answerTarget": "<bản dịch mẫu%s>",
                      "answerPhonetic": "<%s>",
                      "newWords": [
                        {"term": "<từ>", "phonetic": "<phiên âm>", "meaning": "<nghĩa tiếng Việt>"}
                      ],
                      "grammarCodes": ["<mã ngữ pháp lấy từ danh mục trên>"]
                    }
                  ]
                }
                """.formatted(
                personaFor(language),
                dayLabel,
                formatList(dayWords, "(chưa có từ nào)"),
                formatList(earlierWords, "(đây là bài đầu tiên)"),
                count,
                languageNameFor(language),
                grammarCatalogueBlock(catalogue),
                language == Language.CHINESE ? " bằng chữ Hán giản thể" : "",
                language == Language.CHINESE ? "pinyin có dấu thanh của bản dịch" : "để trống");

        return callAndParse(prompt, GeneratedSentencesDto.class, geminiClient.bulkModel());
    }

    /**
     * Works out which grammar points a batch of existing sentences relies on.
     *
     * <p>A batch at a time on purpose: the sentences already in the database would be hundreds of
     * separate requests otherwise, well past a day's free-tier quota. Only the target-language
     * sentence is sent — the Vietnamese prompt adds nothing to identifying Chinese grammar.
     */
    public TaggedSentencesDto tagSentenceGrammar(
            Language language, List<String> sentences, List<GrammarPointDto> catalogue) {
        StringBuilder numbered = new StringBuilder();
        for (int i = 0; i < sentences.size(); i++) {
            numbered.append(i + 1).append(". ").append(sentences.get(i)).append('\n');
        }

        String prompt = """
                Bạn là giáo viên %s. Dưới đây là các câu %s. Hãy xác định mỗi câu dùng những điểm
                ngữ pháp nào trong danh mục.

                %s

                CÁC CÂU:
                %s

                QUY TẮC:
                - Chỉ dùng mã có trong danh mục trên. TUYỆT ĐỐI không tự bịa mã mới.
                - Chỉ gắn khi câu KHỚP ĐÚNG CÔNG THỨC của điểm ngữ pháp đó. Na ná thì KHÔNG gắn.
                  Câu có chữ "了" không có nghĩa là dùng "太…了"; câu nhắc tới một địa điểm không có
                  nghĩa là dùng trạng ngữ chỉ địa điểm — phải có đủ thành phần trong công thức.
                - Với mỗi mã, chép nguyên văn ("evidence") đoạn chữ TRONG CHÍNH CÂU ĐÓ tạo nên cấu
                  trúc, ít nhất 2 chữ. Không chép được đoạn nào thì đừng gắn mã đó.
                - Câu không dùng điểm ngữ pháp nào trong danh mục thì để "grammar" là mảng rỗng.
                  Để mảng rỗng là ĐÚNG; gắn bừa là SAI.
                - Trả về đủ %d phần tử, "index" đánh số từ 1 theo đúng thứ tự trên.

                Trả lời DUY NHẤT bằng JSON:
                {
                  "results": [
                    {"index": <số thứ tự câu>, "grammar": [{"code": "<mã>", "evidence": "<đoạn chữ trong câu>"}]}
                  ]
                }
                """.formatted(
                languageNameFor(language),
                languageNameFor(language),
                grammarCatalogueBlock(catalogue, true),
                numbered.toString(),
                sentences.size());

        return callAndParse(prompt, TaggedSentencesDto.class, geminiClient.bulkModel());
    }

    /**
     * Sentences for a skip-ahead test. Unlike practice generation this forbids any word outside the
     * studied range — the test must not hand the learner vocabulary they were supposed to know.
     */
    public GeneratedSentencesDto generateTestSentences(
            Language language,
            String scopeLabel,
            List<String> recentWords,
            List<String> earlierWords,
            int count
    ) {
        String prompt = """
                %s

                Bạn đang ra ĐỀ KIỂM TRA VƯỢT CẤP cho học viên muốn nhảy tới %s. Đề phải đủ khó để
                chứng minh họ thực sự nắm được toàn bộ phần đã qua.

                TỪ VỰNG GIAI ĐOẠN GẦN ĐÂY (trọng tâm của đề):
                %s

                TỪ VỰNG GIAI ĐOẠN TRƯỚC ĐÓ:
                %s

                Hãy soạn %d câu tiếng Việt để học viên dịch sang %s. Yêu cầu BẮT BUỘC:
                - TUYỆT ĐỐI chỉ dùng từ nằm trong hai danh sách trên. Không được dùng bất kỳ từ nào
                  ngoài phạm vi đó, vì đề thi không có phần chú thích từ mới.
                - Mỗi câu dùng ít nhất 2 từ trong danh sách; cả đề phải chạm tới càng nhiều từ khác
                  nhau càng tốt, không lặp đi lặp lại một nhóm từ.
                - Khoảng 60%% số câu lấy trọng tâm từ danh sách GIAI ĐOẠN GẦN ĐÂY.
                - Trộn độ dài: khoảng 40%% câu ngắn (5-8 chữ), 40%% câu trung bình (9-14 chữ),
                  20%% câu dài có hai mệnh đề để kiểm tra khả năng ghép câu.
                - Câu phải tự nhiên, đúng ngữ pháp, và có một đáp án dịch rõ ràng.

                Trả lời DUY NHẤT bằng JSON theo đúng cấu trúc:
                {
                  "sentences": [
                    {
                      "promptVi": "<câu tiếng Việt cần dịch>",
                      "answerTarget": "<bản dịch mẫu%s>",
                      "answerPhonetic": "<%s>",
                      "newWords": []
                    }
                  ]
                }
                """.formatted(
                personaFor(language),
                scopeLabel,
                formatList(recentWords, "(không có)"),
                formatList(earlierWords, "(không có)"),
                count,
                languageNameFor(language),
                language == Language.CHINESE ? " bằng chữ Hán giản thể" : "",
                language == Language.CHINESE ? "pinyin có dấu thanh của bản dịch" : "để trống");

        return callAndParse(prompt, GeneratedSentencesDto.class, geminiClient.bulkModel());
    }

    /**
     * Drafts a grammar lesson from nothing but the name of the point and a learner's mistake.
     *
     * <p>Used when grading reports a gap in the catalogue: the admin has a name and an example of
     * someone getting it wrong, and wants the explanation written rather than typing it out. The
     * result is a draft to review, not something saved on the model's say-so — unlike the slide
     * import there is no source text to check the examples against.
     */
    public GeneratedGrammarLessonDto generateGrammarLesson(
            Language language,
            String grammarName,
            String examplePrompt,
            String wrongAnswer,
            String explanation,
            List<GrammarPointDto> existing
    ) {
        String prompt = """
                %s

                Hãy soạn MỘT bài học về điểm ngữ pháp sau, cho học viên người Việt ở trình độ sơ cấp.

                Tên điểm ngữ pháp: %s
                %s

                %s

                QUY TẮC BẮT BUỘC:
                1. "code" là slug chữ thường không dấu, chỉ gồm chữ, số và gạch ngang. KHÔNG được
                   trùng với bất kỳ mã nào trong danh mục ở trên.
                2. Công thức phải viết rõ từng thành phần, ví dụ: 太 + tính từ + 了.
                3. Cho 3-5 câu ví dụ ĐƠN GIẢN, đúng trình độ sơ cấp, kèm pinyin có dấu thanh và
                   bản dịch tiếng Việt.
                4. "notes" nêu lỗi người Việt hay mắc với điểm ngữ pháp này, nếu có.
                5. "difficulty" từ 1 đến 5 theo số trường hợp cần nhớ.

                Trả lời DUY NHẤT bằng JSON:
                {
                  "code": "<slug>",
                  "title": "<tên bài, giữ chữ Hán kèm chú tiếng Việt>",
                  "summary": "<1-2 câu nói điểm ngữ pháp này dùng để làm gì>",
                  "formula": "<công thức>",
                  "components": [{"part": "<thành phần>", "meaning": "<giải nghĩa tiếng Việt>"}],
                  "examples": [{"target": "<câu%s>", "phonetic": "<%s>", "vi": "<nghĩa tiếng Việt>",
                                "note": "<lưu ý, để rỗng nếu không có>"}],
                  "notes": "<lưu ý/lỗi thường gặp, để rỗng nếu không có>",
                  "difficulty": <1-5>
                }
                """.formatted(
                personaFor(language),
                grammarName,
                describeMistake(examplePrompt, wrongAnswer, explanation),
                grammarCatalogueBlock(existing),
                language == Language.CHINESE ? " bằng chữ Hán giản thể" : " tiếng Anh",
                language == Language.CHINESE ? "pinyin có dấu thanh" : "phiên âm IPA");

        return callAndParse(prompt, GeneratedGrammarLessonDto.class, geminiClient.bulkModel());
    }

    /** The reported mistake, when there is one — it is the clearest hint at what to explain. */
    private static String describeMistake(String promptVi, String wrongAnswer, String explanation) {
        if (promptVi == null && wrongAnswer == null && explanation == null) {
            return "";
        }
        StringBuilder block = new StringBuilder("Một học viên đã mắc lỗi này khi làm bài dịch:\n");
        if (promptVi != null && !promptVi.isBlank()) {
            block.append("- Câu tiếng Việt cần dịch: ").append(promptVi).append('\n');
        }
        if (wrongAnswer != null && !wrongAnswer.isBlank()) {
            block.append("- Bài làm sai: ").append(wrongAnswer).append('\n');
        }
        if (explanation != null && !explanation.isBlank()) {
            block.append("- Nhận xét của giáo viên: ").append(explanation).append('\n');
        }
        return block.toString();
    }

    /**
     * Drill sentences for one grammar pattern.
     *
     * <p>The count comes from the lesson's difficulty (5 for the simplest pattern, 25 for one with
     * many cases), and the prompt insists the sentences spread across those cases rather than
     * rehearsing the easiest one twenty times.
     */
    public GeneratedSentencesDto generateGrammarExercises(
            Language language,
            String lessonTitle,
            String formula,
            String components,
            String examples,
            int count
    ) {
        String prompt = """
                %s

                Hãy soạn %d câu bài tập DỊCH VIỆT → %s để luyện riêng điểm ngữ pháp sau.

                Tên điểm ngữ pháp: %s
                Công thức: %s
                Các thành phần: %s
                Ví dụ mẫu: %s

                QUY TẮC BẮT BUỘC:
                1. MỌI câu đều phải dùng đúng điểm ngữ pháp trên. Câu nào không dùng tới nó là SAI.
                2. Trải đều qua các TRƯỜNG HỢP khác nhau của điểm ngữ pháp (khẳng định, phủ định,
                   nghi vấn, các biến thể...) thay vì lặp lại một dạng. Ghi rõ trường hợp vào "caseLabel".
                3. Từ vựng dùng ở mức cơ bản, độ dài câu vừa phải (6-15 chữ).
                   Câu tiếng Việt phải dịch được MỘT cách rõ ràng: cần ngôi thứ ba thì viết rõ
                   "anh ấy" hoặc "cô ấy", không dùng "bạn ấy" — tiếng Trung buộc phải chọn 他/她.
                4. Không tạo hai câu có cùng cấu trúc và chỉ khác mỗi danh từ.
                5. Từ nào khó hoặc ngoài trình độ sơ cấp thì phải liệt kê trong "newWords".

                Trả lời DUY NHẤT bằng JSON, không thêm text nào khác:
                {
                  "sentences": [
                    {
                      "promptVi": "<câu tiếng Việt cần dịch>",
                      "answerTarget": "<bản dịch tham khảo%s>",
                      "answerPhonetic": "<%s>",
                      "caseLabel": "<trường hợp của ngữ pháp mà câu này luyện, bằng tiếng Việt>",
                      "newWords": [{"term": "<từ>", "phonetic": "<phiên âm>", "meaning": "<nghĩa tiếng Việt>"}]
                    }
                  ]
                }
                """.formatted(
                personaFor(language),
                count,
                languageNameFor(language).toUpperCase(),
                lessonTitle,
                formula,
                components,
                examples,
                language == Language.CHINESE ? " bằng chữ Hán giản thể" : "",
                language == Language.CHINESE ? "pinyin có dấu thanh của bản dịch" : "để trống");

        return callAndParse(prompt, GeneratedSentencesDto.class, geminiClient.bulkModel());
    }

    /**
     * Grades one translated sentence. Shared by vocabulary practice and grammar drills so both get
     * the same marking and the same grammar cross-references.
     *
     * <p>Unlike everything else here this <em>never throws</em>: the learner has already written an
     * answer, and losing it because the AI was busy would be worse than showing no score. A failure
     * comes back as {@link SentenceFeedbackDto#unavailable} with a null score.
     *
     * @param catalogue grammar points that have a lesson, so the AI can name one instead of
     *                  inventing a label. The codes it returns are still validated against the
     *                  database afterwards — see {@code GrammarCatalogService}.
     */
    public SentenceFeedbackDto gradeOneSentence(
            Language language,
            String promptVi,
            String referenceAnswer,
            String answer,
            List<GrammarPointDto> catalogue
    ) {
        if (!geminiClient.isConfigured()) {
            return SentenceFeedbackDto.unavailable(
                    "Chưa cấu hình AI chấm bài (thiếu GEMINI_API_KEY). Câu trả lời đã được lưu lại.");
        }

        boolean isChinese = language == Language.CHINESE;
        String prompt = """
                Bạn là giáo viên %s đang chấm một câu dịch của học viên người Việt.

                Câu tiếng Việt cần dịch: %s
                Bản dịch tham khảo: %s

                Bài làm của học viên:
                \"\"\"
                %s
                \"\"\"

                Hãy chấm theo Ý NGHĨA và NGỮ PHÁP, không bắt buộc trùng khít với bản tham khảo — nhiều
                cách diễn đạt khác vẫn đúng.

                KHÔNG TRỪ ĐIỂM CHO THỨ MÀ ĐỀ BÀI KHÔNG XÁC ĐỊNH ĐƯỢC. Đây là câu dịch đứng riêng,
                không có ngữ cảnh nào khác ngoài câu tiếng Việt. Tiếng Việt không phân biệt một số
                thứ mà tiếng Trung buộc phải chọn:
                - Giới tính ngôi thứ ba: "bạn ấy", "người ấy" → 他 và 她 ĐỀU ĐÚNG.
                - Số ít/số nhiều khi tiếng Việt không nói rõ.
                - Mức độ trang trọng, cách xưng hô khi đề không nêu.
                Gặp những chỗ đó: giữ nguyên điểm, KHÔNG đưa vào "corrections", KHÔNG bình luận
                "tuỳ ngữ cảnh". Bản dịch tham khảo chỉ là MỘT cách chọn, không phải đáp án duy nhất.

                KÉM MƯỢT HƠN KHÔNG PHẢI LÀ SAI. "corrections" chỉ dành cho lỗi thật sự làm câu sai;
                một cách nói đúng ngữ pháp nhưng kém tự nhiên hơn thì KHÔNG được đưa vào đó và
                KHÔNG bị trừ điểm. Nhiều thành phần trong tiếng Trung là tuỳ chọn:
                - 的 giữa danh từ và phương vị từ hai âm tiết: 超市的西边 và 超市西边 ĐỀU ĐÚNG.
                - Lượng từ khi có nhiều lựa chọn chấp nhận được: 一个书店 và 一家书店 đều dùng được.
                - 边 / 面 / 边儿, có hay không nhi hoá.
                - Trật tự trạng ngữ khi cả hai trật tự đều tự nhiên.
                Muốn gợi ý cách nói mượt hơn thì đặt vào "betterVersion" — đó mới là chỗ dành cho
                văn phong. Trước khi viết một mục "corrections", tự hỏi: người bản ngữ có coi câu
                này là SAI không, hay chỉ là họ sẽ nói khác đi? Nếu chỉ là nói khác đi thì bỏ qua.

                %s

                Trả lời DUY NHẤT bằng JSON theo đúng cấu trúc, không thêm text nào khác:
                {
                  "score": <số nguyên từ 0 đến 10>,
                  "comment": "<nhận xét ngắn gọn bằng tiếng Việt, giọng khích lệ>",
                  "corrections": [
                    {
                      "original": "<phần sai>",
                      "suggestion": "<sửa lại>",
                      "explanation": "<giải thích ngắn bằng tiếng Việt>",
                      "grammarCode": "<mã lấy ĐÚNG từ danh mục trên; để chuỗi rỗng nếu không mã nào khớp>",
                      "grammarName": "<tên điểm ngữ pháp bằng tiếng Việt>"
                    }
                  ],
                  "betterVersion": "<cách nói tự nhiên hơn%s, để chuỗi rỗng nếu bài làm đã tự nhiên>",
                  "betterVersionPhonetic": "<%s>"
                }

                Quy tắc cho "grammarCode" và "grammarName":
                - "grammarName" phải là TÊN MỘT ĐIỂM NGỮ PHÁP cụ thể, ví dụ "Trợ từ 了", "Câu chữ 把",
                  "Bổ ngữ kết quả", "Trạng ngữ chỉ địa điểm".
                - TUYỆT ĐỐI không điền loại lỗi vào "grammarName". Những thứ sau KHÔNG phải điểm ngữ
                  pháp, gặp thì để TRỐNG cả hai: dịch sai nghĩa, dùng sai từ, thiếu từ, thừa từ,
                  chính tả, sai chữ Hán, cách diễn đạt chưa tự nhiên.
                - Nếu chỉ cần THAY MỘT TỪ bằng một từ khác cùng loại thì đó là dùng sai từ, KHÔNG
                  phải lỗi ngữ pháp — ví dụ 他 (anh ấy) phải sửa thành 她 (cô ấy). Để TRỐNG cả hai.
                - Chỉ coi là lỗi ngữ pháp khi sai về CẤU TRÚC: trật tự từ, thiếu/thừa thành phần bắt
                  buộc, dùng sai trợ từ hay giới từ của một mẫu câu.
                - "grammarCode" chỉ được lấy nguyên văn từ danh mục ở trên. TUYỆT ĐỐI không tự bịa mã mới.
                - Nếu không mã nào trong danh mục khớp, để "grammarCode" là chuỗi rỗng nhưng vẫn điền "grammarName".

                Chỉ liệt kê tối đa 5 lỗi quan trọng nhất. Nếu không có lỗi, "corrections" là mảng rỗng.
                """.formatted(
                languageNameFor(language),
                promptVi,
                referenceAnswer,
                answer,
                grammarCatalogueBlock(catalogue),
                isChinese ? " bằng chữ Hán" : "",
                isChinese ? "pinyin có dấu thanh của betterVersion, để trống nếu không có" : "để trống");

        try {
            // The learner is watching a spinner until this returns, so it runs on the grading model.
            return objectMapper.readValue(
                    geminiClient.generateJson(prompt, geminiClient.gradingModel()),
                    SentenceFeedbackDto.class);
        } catch (Exception ex) {
            log.warn("Gemini sentence grading failed", ex);
            return SentenceFeedbackDto.unavailable(
                    "Không chấm được bằng AI: " + ex.getMessage() + " (Câu trả lời của bạn vẫn đã được lưu lại.)");
        }
    }

    /** Renders the catalogue for a prompt, or says plainly that there is none to choose from. */
    static String grammarCatalogueBlock(List<GrammarPointDto> catalogue) {
        return grammarCatalogueBlock(catalogue, false);
    }

    /**
     * @param withFormulas include each pattern's formula. Worth the extra prompt length when the
     *                     model has to judge whether a sentence really uses a pattern: given only
     *                     the title "trạng ngữ chỉ địa điểm" it tags any sentence that mentions a
     *                     place, where the formula "在 + địa điểm + V" rules most of them out.
     */
    static String grammarCatalogueBlock(List<GrammarPointDto> catalogue, boolean withFormulas) {
        if (catalogue == null || catalogue.isEmpty()) {
            return "Hệ thống chưa có bài học ngữ pháp nào, nên luôn để \"grammarCode\" là chuỗi rỗng.";
        }
        StringBuilder block = new StringBuilder(withFormulas
                ? "DANH MỤC NGỮ PHÁP ĐÃ CÓ BÀI HỌC (mã — tên — công thức):\n"
                : "DANH MỤC NGỮ PHÁP ĐÃ CÓ BÀI HỌC (mã — tên):\n");
        for (GrammarPointDto point : catalogue) {
            block.append("- ").append(point.code()).append(" — ").append(point.title());
            if (withFormulas && point.formula() != null && !point.formula().isBlank()) {
                block.append(" — CÔNG THỨC: ").append(point.formula());
            }
            block.append('\n');
        }
        return block.toString();
    }

    /**
     * Grades a whole test paper in a single request. Doing all questions at once costs one call
     * instead of twenty, which matters on the free tier, and suits a test that only reveals its
     * results at the end anyway.
     */
    public GradedSentencesDto gradeSentenceBatch(
            Language language, String paper, List<GrammarPointDto> catalogue) {
        boolean isChinese = language == Language.CHINESE;
        String prompt = """
                Bạn là giáo viên %s đang chấm bài kiểm tra dịch của học viên người Việt.

                Dưới đây là các câu, mỗi câu gồm: đề tiếng Việt, bản dịch tham khảo, và bài làm.

                %s

                Chấm theo Ý NGHĨA và NGỮ PHÁP, không bắt buộc trùng khít bản tham khảo — cách diễn
                đạt khác mà đúng vẫn được điểm cao. Bài để trống chấm 0.

                KHÔNG TRỪ ĐIỂM CHO THỨ MÀ ĐỀ BÀI KHÔNG XÁC ĐỊNH ĐƯỢC: câu dịch đứng riêng, không có
                ngữ cảnh. "bạn ấy"/"người ấy" thì 他 và 她 đều đúng; số ít/số nhiều, cách xưng hô khi
                đề không nói rõ cũng vậy. Đừng đưa vào "corrections", đừng bình luận "tuỳ ngữ cảnh".

                KÉM MƯỢT HƠN KHÔNG PHẢI LÀ SAI. "corrections" chỉ dành cho lỗi thật sự làm câu sai.
                Thành phần tuỳ chọn thì không tính là lỗi: 的 giữa danh từ và phương vị từ hai âm
                tiết (超市的西边 và 超市西边 ĐỀU ĐÚNG), lượng từ có nhiều lựa chọn được (一个书店 /
                一家书店), 边 / 面 / 边儿. Gợi ý cách nói mượt hơn thì để vào "betterVersion".

                %s

                Trả lời DUY NHẤT bằng JSON, đúng %s phần tử, theo thứ tự "index" tăng dần:
                {
                  "results": [
                    {
                      "index": <số thứ tự câu>,
                      "score": <số nguyên 0-10>,
                      "comment": "<nhận xét ngắn bằng tiếng Việt>",
                      "corrections": [
                        {
                          "original": "<phần sai>",
                          "suggestion": "<sửa lại>",
                          "explanation": "<giải thích ngắn>",
                          "grammarCode": "<mã lấy ĐÚNG từ danh mục trên; chuỗi rỗng nếu không mã nào khớp>",
                          "grammarName": "<tên điểm ngữ pháp bằng tiếng Việt, để trống nếu không phải lỗi ngữ pháp>"
                        }
                      ],
                      "betterVersion": "<cách nói tự nhiên hơn%s, chuỗi rỗng nếu bài làm đã ổn>"
                    }
                  ]
                }

                Mỗi câu tối đa 2 lỗi trong "corrections" để câu trả lời không quá dài.
                TUYỆT ĐỐI không tự bịa mã ngữ pháp ngoài danh mục.
                """.formatted(
                isChinese ? "tiếng Trung" : "tiếng Anh",
                paper,
                grammarCatalogueBlock(catalogue),
                "đủ số",
                isChinese ? " bằng chữ Hán" : "");

        return callAndParse(prompt, GradedSentencesDto.class, geminiClient.gradingModel());
    }

    private static String formatList(List<String> items, String emptyText) {
        if (items == null || items.isEmpty()) {
            return emptyText;
        }
        return String.join(", ", items);
    }

    public GeneratedReadingDto generateReadingPassage(GenerateReadingRequest request) {
        String prompt = """
                Bạn là chuyên gia biên soạn giáo trình tiếng Anh cho người Việt.

                Hãy viết một bài đọc tiếng Anh ngắn (khoảng 100-150 từ) chủ đề "%s", trọng tâm về "%s", trình độ %s,
                kèm theo 5 câu hỏi trắc nghiệm kiểm tra khả năng đọc hiểu.

                Trả lời DUY NHẤT bằng JSON theo đúng cấu trúc:
                {
                  "title": "<tiêu đề bài đọc bằng tiếng Anh>",
                  "content": "<nội dung bài đọc bằng tiếng Anh>",
                  "questions": [
                    {
                      "questionText": "<câu hỏi bằng tiếng Anh>",
                      "optionA": "...", "optionB": "...", "optionC": "...", "optionD": "...",
                      "correctOption": "<A|B|C|D>",
                      "explanation": "<giải thích ngắn bằng tiếng Việt, trích dẫn câu trong bài đọc>"
                    }
                  ]
                }
                """.formatted(request.topicName(), request.theme(), request.level());

        return callAndParse(prompt, GeneratedReadingDto.class);
    }

    public GeneratedWritingPromptDto generateWritingPrompt(GenerateWritingPromptRequest request) {
        String prompt = """
                Bạn là chuyên gia biên soạn giáo trình tiếng Anh cho người Việt.

                Hãy soạn một đề bài luyện viết tiếng Anh chủ đề "%s", trình độ %s, phù hợp để học viên viết một
                đoạn văn ngắn.

                Trả lời DUY NHẤT bằng JSON theo đúng cấu trúc:
                {
                  "title": "<tiêu đề đề bài bằng tiếng Việt, ngắn gọn>",
                  "instructions": "<mô tả yêu cầu bài viết bằng tiếng Anh, nêu rõ cần viết ít nhất bao nhiêu từ>",
                  "minWords": <số nguyên, số từ tối thiểu, gợi ý 50-80 cho beginner, 100-150 cho intermediate, 150-250 cho advanced>
                }
                """.formatted(request.topicName(), request.level());

        return callAndParse(prompt, GeneratedWritingPromptDto.class);
    }

    private <T> T callAndParse(String prompt, Class<T> type) {
        return callAndParse(prompt, type, null);
    }

    private <T> T callAndParse(String prompt, Class<T> type, String model) {
        if (!geminiClient.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Chưa cấu hình GEMINI_API_KEY");
        }
        String rawJson;
        try {
            rawJson = model == null ? geminiClient.generateJson(prompt) : geminiClient.generateJson(prompt, model);
        } catch (GeminiException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, ex.getMessage());
        }

        try {
            return objectMapper.readValue(rawJson, type);
        } catch (Exception ex) {
            log.warn("Could not parse Gemini JSON. Raw response: {}", rawJson, ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "AI trả về JSON không đúng định dạng (thường do nội dung bị cắt vì quá dài) — thử giảm số từ mỗi lần sinh");
        }
    }
}
