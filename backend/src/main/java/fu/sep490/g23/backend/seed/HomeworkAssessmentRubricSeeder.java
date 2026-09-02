package fu.sep490.g23.backend.seed;

import fu.sep490.g23.backend.entity.assessment.AssessmentRubric;
import fu.sep490.g23.backend.entity.assessment.RubricCriterion;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentType;
import fu.sep490.g23.backend.entity.assessment.enums.AiEvaluationMode;
import fu.sep490.g23.backend.entity.curriculum.AssessmentBankItem;
import fu.sep490.g23.backend.repository.assessment.AssessmentRubricRepository;
import fu.sep490.g23.backend.repository.curriculum.AssessmentBankItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@Order(59)
@RequiredArgsConstructor
public class HomeworkAssessmentRubricSeeder implements CommandLineRunner {

    private final AssessmentRubricRepository rubricRepository;
    private final AssessmentBankItemRepository assessmentBankItemRepository;

    @Override
    @Transactional
    public void run(String... args) {
        AssessmentRubric writingRubric = upsertIeltsWritingRubric();
        AssessmentRubric speakingRubric = upsertIeltsSpeakingRubric();
        upsertIeltsVocabularyRubric();
        upsertIeltsListeningRubric();
        upsertIeltsReadingRubric();
        upsertAiModuleTest(
                "IELTS Writing Task 2 - Opinion Essay",
                AssessmentSkill.WRITING,
                writingRubric,
                "Viết ít nhất 250 từ. Trình bày quan điểm rõ ràng, phát triển lập luận và đưa ví dụ phù hợp.",
                "Some people think online learning can replace classroom learning. To what extent do you agree or disagree?",
                1
        );
        upsertAiModuleTest(
                "IELTS Speaking - Part 2 Long Turn",
                AssessmentSkill.SPEAKING,
                speakingRubric,
                "Chuẩn bị trong 1 phút, sau đó trình bày khoảng 1-2 phút. Học viên có thể nộp transcript hoặc nội dung ghi âm theo giao diện hỗ trợ.",
                "Describe a skill you learned that has been useful in your study or work. Explain how you learned it and why it is useful.",
                2
        );
    }

    private void upsertAiModuleTest(
            String title,
            AssessmentSkill skill,
            AssessmentRubric rubric,
            String instructions,
            String prompt,
            int displayOrder
    ) {
        AssessmentBankItem item = assessmentBankItemRepository.findAllByOrderByUpdatedAtDescIdDesc().stream()
                .filter(candidate -> title.equalsIgnoreCase(candidate.getTitle()))
                .findFirst()
                .orElseGet(() -> AssessmentBankItem.builder().title(title).build());
        item.setDescription("MODULE_TEST hệ thống dành cho bài tập về nhà; giáo viên có thể bật hoặc tắt hỗ trợ chấm điểm AI.");
        item.setType(AssessmentType.MODULE_TEST);
        item.setSkill(skill);
        item.setAiEvaluationMode(AiEvaluationMode.RUBRIC_FEEDBACK);
        item.setRubric(rubric);
        item.setInstructions(instructions);
        item.setUiConfigJson("{\"prompt\":\"" + prompt + "\",\"responseType\":\"TEXT\"}");
        item.setMaxScore(BigDecimal.TEN);
        item.setStatus("PUBLISHED");
        assessmentBankItemRepository.save(item);
    }

    private AssessmentRubric upsertIeltsWritingRubric() {
        return rubricRepository.findByNameIgnoreCaseAndStatus("IELTS Writing Task 2 AI Rubric", "PUBLISHED")
                .orElseGet(() -> rubricRepository.save(buildRubric(
                        "IELTS Writing Task 2 AI Rubric",
                        AssessmentSkill.WRITING,
                        "Writing Task 2",
                        "Estimated IELTS band 0-9",
                        "Chấm Writing theo 4 tiêu chí IELTS Task 2. Dùng cho bài luận, không phải điểm thi chính thức.",
                        new String[][]{
                                {"Task Response", "25", "1", "Trả lời đúng đề, có quan điểm rõ và phát triển ý.", "Band 5: phát triển hạn chế; Band 6: liên quan nhưng chưa sâu; Band 7: quan điểm rõ, ý được triển khai tốt."},
                                {"Coherence and Cohesion", "25", "2", "Mạch lạc, đoạn văn và từ nối hợp lý.", "Band 5: liên kết yếu; Band 6: có tiến trình nhưng hơi cứng; Band 7: mạch lạc, đoạn văn hiệu quả."},
                                {"Lexical Resource", "25", "3", "Từ vựng chủ đề, collocation và độ chính xác.", "Band 5: hạn chế; Band 6: đủ dùng còn lỗi; Band 7: linh hoạt, lỗi ít."},
                                {"Grammatical Range and Accuracy", "25", "4", "Đa dạng cấu trúc câu và độ chính xác ngữ pháp.", "Band 5: lỗi thường xuyên; Band 6: có câu phức nhưng còn lỗi; Band 7: đa dạng và kiểm soát tốt."}
                        }
                )));
    }

    private AssessmentRubric upsertIeltsSpeakingRubric() {
        return rubricRepository.findByNameIgnoreCaseAndStatus("IELTS Speaking AI Rubric", "PUBLISHED")
                .orElseGet(() -> rubricRepository.save(buildRubric(
                        "IELTS Speaking AI Rubric",
                        AssessmentSkill.SPEAKING,
                        "Speaking practice",
                        "Estimated IELTS band 0-9",
                        "Chấm Speaking qua transcript/ghi âm theo 4 tiêu chí IELTS.",
                        new String[][]{
                                {"Fluency and Coherence", "25", "1", "Nói trôi chảy, mạch lạc, ít ngập ngừng dài.", "Band 5: hay dừng/lặp; Band 6: nói được khá dài; Band 7: linh hoạt, mạch lạc."},
                                {"Lexical Resource", "25", "2", "Từ vựng chủ đề, paraphrase, collocation tự nhiên.", "Band 5: hạn chế; Band 6: đủ dùng; Band 7: linh hoạt, chính xác."},
                                {"Grammar Range and Accuracy", "25", "3", "Ngữ pháp đa dạng và chính xác.", "Band 5: lỗi nhiều; Band 6: trộn cấu trúc; Band 7: kiểm soát tốt."},
                                {"Pronunciation", "25", "4", "Dễ nghe, trọng âm, nhịp điệu.", "Band 5: cần nỗ lực nghe; Band 6: nhìn chung rõ; Band 7: tự nhiên, lỗi nhỏ."}
                        }
                )));
    }

    private AssessmentRubric upsertIeltsVocabularyRubric() {
        return rubricRepository.findByNameIgnoreCaseAndStatus("IELTS Vocabulary Usage AI Rubric", "PUBLISHED")
                .orElseGet(() -> rubricRepository.save(buildRubric(
                        "IELTS Vocabulary Usage AI Rubric",
                        AssessmentSkill.VOCABULARY,
                        "Vocabulary output practice",
                        "EnglishLab 0-10 formative score",
                        "Chấm cách học viên dùng từ vựng mục tiêu trong câu/đoạn trả lời.",
                        new String[][]{
                                {"Meaning Accuracy", "35", "1", "Dùng đúng nghĩa từ trong ngữ cảnh.", "Thấp: sai nghĩa; Trung bình: đúng một phần; Cao: tự nhiên, chính xác."},
                                {"Collocation", "30", "2", "Kết hợp từ tự nhiên (collocation).", "Thấp: gượng; Trung bình: chấp nhận được; Cao: academic/natural."},
                                {"Sentence Quality", "20", "3", "Câu rõ nghĩa, ngữ pháp ổn.", "Thấp: khó hiểu; Trung bình: hiểu được; Cao: chính xác, mạch lạc."},
                                {"Topic Relevance", "15", "4", "Gắn từ vựng với chủ đề bài tập.", "Thấp: lạc đề; Trung bình: liên quan; Cao: bám sát chủ đề."}
                        }
                )));
    }

    private AssessmentRubric upsertIeltsListeningRubric() {
        return rubricRepository.findByNameIgnoreCaseAndStatus("IELTS Listening Objective AI Rubric", "PUBLISHED")
                .orElseGet(() -> rubricRepository.save(buildRubric(
                        "IELTS Listening Objective AI Rubric",
                        AssessmentSkill.LISTENING,
                        "Listening answer check",
                        "Accuracy 0-100% mapped to homework score",
                        "Chấm Listening theo đáp án chuẩn (nếu giáo viên cung cấp trong đề) và phân tích lỗi nghe.",
                        new String[][]{
                                {"Answer Accuracy", "40", "1", "Số câu đúng so với đáp án chuẩn.", "Thấp: <50%; Trung bình: 50-75%; Cao: >75%."},
                                {"Spelling and Format", "20", "2", "Chính tả, số, dạng từ theo yêu cầu đề.", "Thấp: nhiều lỗi format; Cao: đúng quy tắc IELTS."},
                                {"Question Type Weakness", "20", "3", "Dạng câu hay sai (form/note/map/multiple choice).", "Ghi nhận dạng câu còn yếu để ôn lại."},
                                {"Listening Strategy", "20", "4", "Gợi ý preview, keyword, distractor.", "Phản hồi chiến lược nghe phù hợp lỗi sai."}
                        }
                )));
    }

    private AssessmentRubric upsertIeltsReadingRubric() {
        return rubricRepository.findByNameIgnoreCaseAndStatus("IELTS Reading Objective AI Rubric", "PUBLISHED")
                .orElseGet(() -> rubricRepository.save(buildRubric(
                        "IELTS Reading Objective AI Rubric",
                        AssessmentSkill.READING,
                        "Reading answer check",
                        "Accuracy 0-100% mapped to homework score",
                        "Chấm Reading theo đáp án chuẩn (nếu có trong đề) và phân tích lỗi đọc hiểu.",
                        new String[][]{
                                {"Answer Accuracy", "40", "1", "Số câu đúng so với đáp án chuẩn.", "Thấp: <50%; Trung bình: 50-75%; Cao: >75%."},
                                {"Evidence Location", "20", "2", "Có trích dẫn/locate thông tin trong passage.", "Thấp: đoán; Cao: bám evidence rõ."},
                                {"Question Type Weakness", "20", "3", "Dạng câu hay sai (T/F/NG, matching, heading).", "Ghi nhận dạng đọc còn yếu."},
                                {"Vocabulary in Context", "20", "4", "Hiểu từ đồng nghĩa, paraphrase trong bài đọc.", "Phản hồi từ/khái niệm cần củng cố."}
                        }
                )));
    }

    private AssessmentRubric buildRubric(
            String name,
            AssessmentSkill skill,
            String taskType,
            String scoringScale,
            String description,
            String[][] criteria
    ) {
        AssessmentRubric rubric = AssessmentRubric.builder()
                .name(name)
                .examType("IELTS")
                .skill(skill)
                .taskType(taskType)
                .scoringScale(scoringScale)
                .description(description)
                .status("PUBLISHED")
                .build();
        for (String[] row : criteria) {
            rubric.addCriterion(RubricCriterion.builder()
                    .name(row[0])
                    .weight(Integer.parseInt(row[1]))
                    .displayOrder(Integer.parseInt(row[2]))
                    .description(row[3])
                    .bandDescriptors(row[4])
                    .build());
        }
        return rubric;
    }
}
