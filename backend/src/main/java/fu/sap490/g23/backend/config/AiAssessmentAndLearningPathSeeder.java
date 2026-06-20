package fu.sap490.g23.backend.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sap490.g23.backend.entity.assessment.*;
import fu.sap490.g23.backend.entity.assessment.enums.*;
import fu.sap490.g23.backend.entity.assessment.enums.*;
import fu.sap490.g23.backend.entity.course.CourseModule;
import fu.sap490.g23.backend.entity.course.OnlineCourse;
import fu.sap490.g23.backend.repository.assessment.AssessmentRubricRepository;
import fu.sap490.g23.backend.repository.assessment.CourseAssessmentRepository;
import fu.sap490.g23.backend.repository.course.LearningPackageRepository;
import fu.sap490.g23.backend.repository.course.OnlineCourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
@Order(60)
@RequiredArgsConstructor
public class AiAssessmentAndLearningPathSeeder implements CommandLineRunner {
    private static final String UI_CONFIG_MARKER = "\n\n[ENGLISHLAB_UI_CONFIG]\n";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AssessmentRubricRepository rubricRepository;
    private final CourseAssessmentRepository courseAssessmentRepository;
    private final LearningPackageRepository learningPackageRepository;
    private final OnlineCourseRepository onlineCourseRepository;

    @Value("${app.seed.enabled:false}")
    private boolean seedEnabled;

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled) {
            return;
        }

        AssessmentRubric writingRubric = upsertIeltsWritingRubric();
        AssessmentRubric speakingRubric = upsertIeltsSpeakingRubric();
        AssessmentRubric vocabularyRubric = upsertVocabularyRubric();

        learningPackageRepository.findBySlugAndDeletedFalse("ielts-master-vocabulary-band-7-plus")
                .flatMap(onlineCourseRepository::findByLearningPackage)
                .ifPresent(course -> {
                    configurePath(course, 1, "IELTS 5.5 to 7.0 Self-Paced Path", 5.5, 6.5, 7.0,
                            "Learner can use band-7 topic vocabulary, collocations, and examples in IELTS Speaking/Writing responses.",
                            "e2-ielts-practice-tests");
                    seedVocabularyAssessments(course, vocabularyRubric);
                });

        learningPackageRepository.findBySlugAndDeletedFalse("e2-ielts-practice-tests")
                .flatMap(onlineCourseRepository::findByLearningPackage)
                .ifPresent(course -> {
                    configurePath(course, 2, "IELTS 5.5 to 7.0 Self-Paced Path", 5.5, 6.5, 7.0,
                            "Learner can complete IELTS-style practice tests, analyze mistakes, and follow AI recommendations for final review.",
                            null);
                    seedPracticeTestAssessments(course, writingRubric, speakingRubric);
                });
    }

    private void configurePath(OnlineCourse course, int order, String name, double minBand, double maxBand, double targetBand, String outcome, String nextSlug) {
        course.setLearningPathCode("IELTS_BAND_55_TO_70");
        course.setLearningPathName(name);
        course.setLearningPathOrder(order);
        course.setRecommendedCurrentBandMin(minBand);
        course.setRecommendedCurrentBandMax(maxBand);
        course.setTargetBand(targetBand);
        course.setTargetOutcome(outcome);
        course.setRecommendedNextCourseSlug(nextSlug);
        onlineCourseRepository.save(course);
    }

    private AssessmentRubric upsertIeltsWritingRubric() {
        return rubricRepository.findByNameIgnoreCaseAndActiveTrue("IELTS Writing Task 2 AI Rubric")
                .orElseGet(() -> {
                    AssessmentRubric rubric = AssessmentRubric.builder()
                            .name("IELTS Writing Task 2 AI Rubric")
                            .examType("IELTS")
                            .skill(AssessmentSkill.WRITING)
                            .taskType("Writing Task 2")
                            .scoringScale("Estimated IELTS band 0-9")
                            .description("Rubric used by EnglishLab AI for formative Writing Task 2 feedback. It is not an official IELTS score.")
                            .active(true)
                            .build();
                    rubric.addCriterion(criterion("Task Response", 25, 1, "Addresses the prompt, presents a clear position, and develops relevant ideas.", "Band 5: limited development; Band 6: relevant but sometimes underdeveloped; Band 7: clear position and well-developed support."));
                    rubric.addCriterion(criterion("Coherence and Cohesion", 25, 2, "Organizes ideas logically with paragraphing and cohesive devices.", "Band 5: weak progression; Band 6: overall progression but mechanical linking; Band 7: clear progression and effective paragraphing."));
                    rubric.addCriterion(criterion("Lexical Resource", 25, 3, "Uses topic vocabulary, collocations, and precise wording.", "Band 5: limited range; Band 6: adequate range with errors; Band 7: flexible range with less frequent errors."));
                    rubric.addCriterion(criterion("Grammatical Range and Accuracy", 25, 4, "Uses accurate simple and complex sentence structures.", "Band 5: frequent errors; Band 6: mix of simple/complex forms with errors; Band 7: variety and good control."));
                    return rubricRepository.save(rubric);
                });
    }

    private AssessmentRubric upsertIeltsSpeakingRubric() {
        return rubricRepository.findByNameIgnoreCaseAndActiveTrue("IELTS Speaking AI Rubric")
                .orElseGet(() -> {
                    AssessmentRubric rubric = AssessmentRubric.builder()
                            .name("IELTS Speaking AI Rubric")
                            .examType("IELTS")
                            .skill(AssessmentSkill.SPEAKING)
                            .taskType("Speaking practice")
                            .scoringScale("Estimated IELTS band 0-9")
                            .description("Rubric used by EnglishLab AI to evaluate speaking transcript and fluency indicators. Audio pronunciation scoring can be integrated later.")
                            .active(true)
                            .build();
                    rubric.addCriterion(criterion("Fluency and Coherence", 25, 1, "Responds at length, connects ideas, and avoids long pauses.", "Band 5: pauses and repetition; Band 6: willing to speak at length; Band 7: flexible and coherent."));
                    rubric.addCriterion(criterion("Lexical Resource", 25, 2, "Uses topic vocabulary, paraphrase, and natural collocations.", "Band 5: limited vocabulary; Band 6: adequate but sometimes inaccurate; Band 7: flexible and precise."));
                    rubric.addCriterion(criterion("Grammar Range and Accuracy", 25, 3, "Uses accurate sentence forms and complex structures.", "Band 5: frequent errors; Band 6: mixed range; Band 7: good range and control."));
                    rubric.addCriterion(criterion("Pronunciation", 25, 4, "Shows intelligibility, stress, rhythm, and sound control.", "Band 5: effort required; Band 6: generally clear; Band 7: clear and natural with minor lapses."));
                    return rubricRepository.save(rubric);
                });
    }

    private AssessmentRubric upsertVocabularyRubric() {
        return rubricRepository.findByNameIgnoreCaseAndActiveTrue("IELTS Vocabulary Usage AI Rubric")
                .orElseGet(() -> {
                    AssessmentRubric rubric = AssessmentRubric.builder()
                            .name("IELTS Vocabulary Usage AI Rubric")
                            .examType("IELTS")
                            .skill(AssessmentSkill.VOCABULARY)
                            .taskType("Vocabulary output practice")
                            .scoringScale("EnglishLab 0-10 formative score")
                            .description("Rubric for checking whether learners can use target vocabulary naturally in sentences and short answers.")
                            .active(true)
                            .build();
                    rubric.addCriterion(criterion("Meaning Accuracy", 35, 1, "Uses the target word with the correct meaning in context.", "Low: wrong meaning; Medium: partly correct; High: accurate and natural."));
                    rubric.addCriterion(criterion("Collocation", 30, 2, "Combines target vocabulary with natural collocations.", "Low: awkward combinations; Medium: acceptable; High: natural academic collocations."));
                    rubric.addCriterion(criterion("Sentence Quality", 20, 3, "Produces grammatically clear and meaningful sentences.", "Low: unclear sentence; Medium: understandable; High: accurate and well-formed."));
                    rubric.addCriterion(criterion("Topic Relevance", 15, 4, "Connects vocabulary to the IELTS topic of the module.", "Low: unrelated; Medium: related; High: clearly topic-specific."));
                    return rubricRepository.save(rubric);
                });
    }

    private RubricCriterion criterion(String name, int weight, int order, String description, String descriptors) {
        return RubricCriterion.builder()
                .name(name)
                .weight(weight)
                .displayOrder(order)
                .description(description)
                .bandDescriptors(descriptors)
                .build();
    }

    private void seedVocabularyAssessments(OnlineCourse course, AssessmentRubric rubric) {
        List<CourseModule> modules = course.getModules().stream()
                .sorted(Comparator.comparing(CourseModule::getDisplayOrder))
                .toList();
        List<CourseAssessment> existingAssessments = courseAssessmentRepository.findByOnlineCourseAndActiveTrueOrderByDisplayOrderAscIdAsc(course);
        for (CourseModule module : modules) {
            CourseAssessment assessment = findSeededModuleAssessment(existingAssessments, module, "AI Vocabulary Output Check - ")
                    .orElseGet(() -> CourseAssessment.builder()
                            .onlineCourse(course)
                            .module(module)
                            .type(AssessmentType.MODULE_TEST)
                            .active(true)
                            .build());
            assessment.setRubric(rubric);
            assessment.setTitle("AI Vocabulary Output Check - " + module.getTitle());
            assessment.setDescription("Write 5-7 sentences using the target vocabulary from this module. AI checks meaning, collocation, sentence quality, and topic relevance.");
            assessment.setSkill(AssessmentSkill.VOCABULARY);
            assessment.setAiEvaluationMode(AiEvaluationMode.RUBRIC_FEEDBACK);
            assessment.setInstructions("Use at least five target words from the module in IELTS-style sentences. Do not copy examples from the lesson.");
            assessment.setPassingScore(BigDecimal.valueOf(7.0));
            assessment.setMaxScore(BigDecimal.TEN);
            assessment.setTimeLimitMinutes(20);
            assessment.setDisplayOrder(module.getDisplayOrder());
            assessment.setActive(true);
            CourseAssessment savedAssessment = courseAssessmentRepository.save(assessment);
            if (existingAssessments.stream().noneMatch(item -> item.getId() != null && item.getId().equals(savedAssessment.getId()))) {
                existingAssessments.add(savedAssessment);
            }
        }
    }

    private void seedPracticeTestAssessments(OnlineCourse course, AssessmentRubric writingRubric, AssessmentRubric speakingRubric) {
        List<CourseModule> modules = course.getModules().stream()
                .sorted(Comparator.comparing(CourseModule::getDisplayOrder))
                .toList();
        List<CourseAssessment> existingAssessments = courseAssessmentRepository.findByOnlineCourseAndActiveTrueOrderByDisplayOrderAscIdAsc(course);
        for (CourseModule module : modules) {
            AssessmentSkill skill = detectSkill(module.getTitle());
            AssessmentRubric rubric = resolvePracticeRubric(skill, writingRubric, speakingRubric);
            AiEvaluationMode evaluationMode = resolvePracticeEvaluationMode(skill);
            CourseAssessment assessment = findSeededModuleAssessment(existingAssessments, module, "Module AI Check - ")
                    .orElseGet(() -> CourseAssessment.builder()
                            .onlineCourse(course)
                            .module(module)
                            .type(AssessmentType.MODULE_TEST)
                            .active(true)
                            .build());
            assessment.setRubric(rubric);
            assessment.setTitle("Bài đánh giá cuối mô-đun - " + module.getTitle());
            assessment.setDescription(practiceDescription(skill));
            assessment.setSkill(skill);
            assessment.setAiEvaluationMode(evaluationMode);
            assessment.setInstructions(practiceInstructions(skill, module));
            if (skill == AssessmentSkill.LISTENING || skill == AssessmentSkill.READING) {
                assessment.setObjectiveAnswerKey(loadObjectiveAnswerKey(skill));
                assessment.setPassingScore(BigDecimal.valueOf(28));
                assessment.setMaxScore(BigDecimal.valueOf(40));
            } else {
                assessment.setObjectiveAnswerKey(null);
                assessment.setPassingScore(usesBandScoring(evaluationMode) ? BigDecimal.valueOf(6.0) : null);
                assessment.setMaxScore(usesBandScoring(evaluationMode) ? BigDecimal.valueOf(9.0) : null);
            }
            assessment.setTimeLimitMinutes(skill == AssessmentSkill.READING ? 60 : (skill == AssessmentSkill.SPEAKING ? 15 : 40));
            assessment.setDisplayOrder(module.getDisplayOrder());
            assessment.setActive(true);
            CourseAssessment savedAssessment = courseAssessmentRepository.save(assessment);
            if (existingAssessments.stream().noneMatch(item -> item.getId() != null && item.getId().equals(savedAssessment.getId()))) {
                existingAssessments.add(savedAssessment);
            }
        }
        CourseAssessment finalMock = findSeededMockAssessment(existingAssessments)
                .orElseGet(() -> CourseAssessment.builder()
                        .onlineCourse(course)
                        .type(AssessmentType.MOCK_TEST)
                        .active(true)
                        .build());
        finalMock.setRubric(null);
        finalMock.setTitle("Tổng kết sau bài thi thử");
        finalMock.setDescription("Sau khi hoàn thành các bài luyện thi, hãy gửi nhật ký lỗi và phần tự đánh giá. Hệ thống sẽ xác định kỹ năng yếu và gợi ý mô-đun cần ôn lại.");
        finalMock.setSkill(AssessmentSkill.MIXED);
        finalMock.setAiEvaluationMode(AiEvaluationMode.EXPLAIN_ONLY);
        finalMock.setInstructions("Tóm tắt lỗi sai, dạng câu hỏi khó và mục tiêu tiếp theo. Hệ thống sẽ tạo kế hoạch ôn tập.");
        finalMock.setPassingScore(null);
        finalMock.setMaxScore(null);
        finalMock.setTimeLimitMinutes(30);
        finalMock.setDisplayOrder(999);
        finalMock.setActive(true);
        courseAssessmentRepository.save(finalMock);
    }

    private AssessmentSkill detectSkill(String moduleTitle) {
        String title = moduleTitle == null ? "" : moduleTitle.toLowerCase();
        if (title.contains("listening")) {
            return AssessmentSkill.LISTENING;
        }
        if (title.contains("reading")) {
            return AssessmentSkill.READING;
        }
        if (title.contains("speaking")) {
            return AssessmentSkill.SPEAKING;
        }
        if (title.contains("writing")) {
            return AssessmentSkill.WRITING;
        }
        return AssessmentSkill.MIXED;
    }

    private AssessmentRubric resolvePracticeRubric(AssessmentSkill skill, AssessmentRubric writingRubric, AssessmentRubric speakingRubric) {
        if (skill == AssessmentSkill.SPEAKING) {
            return speakingRubric;
        }
        if (skill == AssessmentSkill.WRITING) {
            return writingRubric;
        }
        return null;
    }

    private AiEvaluationMode resolvePracticeEvaluationMode(AssessmentSkill skill) {
        return switch (skill) {
            case LISTENING, READING, SPEAKING, WRITING -> AiEvaluationMode.ESTIMATED_BAND;
            case VOCABULARY -> AiEvaluationMode.RUBRIC_FEEDBACK;
            default -> AiEvaluationMode.EXPLAIN_ONLY;
        };
    }

    private boolean usesBandScoring(AiEvaluationMode evaluationMode) {
        return evaluationMode == AiEvaluationMode.ESTIMATED_BAND;
    }

    private String practiceDescription(AssessmentSkill skill) {
        return switch (skill) {
            case LISTENING -> "Nộp đáp án Listening theo từng câu. Hệ thống chấm bằng đáp án chuẩn, phân tích lỗi sai và gợi ý phần cần ôn lại.";
            case READING -> "Nộp đáp án Reading theo từng câu. Hệ thống chấm bằng đáp án chuẩn, phân tích evidence, lỗi đọc hiểu và dạng câu còn yếu.";
            case SPEAKING -> "Ghi âm câu trả lời Speaking. Hệ thống nhận xét theo tiêu chí Speaking, dùng âm thanh khi có để đánh giá độ trôi chảy và phát âm.";
            case WRITING -> "Nộp bài viết IELTS Writing. Hệ thống đánh giá theo bộ tiêu chí Writing phù hợp với task.";
            default -> "Nộp nhật ký lỗi, đáp án hoặc phần tự đánh giá để nhận kế hoạch ôn tập tiếp theo.";
        };
    }

    private String practiceInstructions(AssessmentSkill skill, CourseModule module) {
        return switch (skill) {
            case LISTENING -> listeningInstructions();
            case READING -> readingInstructions();
            case SPEAKING -> speakingInstructions(module);
            case WRITING -> writingInstructions();
            default -> "Tóm tắt đáp án, lỗi sai, dạng câu hỏi khó và mục tiêu tiếp theo. Hệ thống sẽ tạo kế hoạch ôn tập.";
        };
    }

    private String listeningInstructions() {
        return "Hoàn thành bài thi mô phỏng IELTS Listening. Bài thi có trình phát âm thanh, bốn phần và ô nhập đáp án cho đủ 40 câu."
                + UI_CONFIG_MARKER
                + loadResourceText("assessment-data/ielts_mock_2025_january_listening_test_1.json");
    }

    private String readingInstructions() {
        return "Hoàn thành bài thi mô phỏng IELTS Reading. Bài thi có bố cục bài đọc và câu hỏi riêng, đồng hồ 60 phút và ô nhập đáp án cho đủ 40 câu."
                + UI_CONFIG_MARKER
                + loadResourceText("assessment-data/ielts_mock_2025_january_reading_test_1.json");
    }

    private String writingInstructions() {
        return "Hoàn thành bài thi mô phỏng IELTS Writing. Bài thi gồm hai task, đồng hồ chung, mục tiêu số từ tối thiểu và luồng nộp bài riêng."
                + UI_CONFIG_MARKER
                + loadResourceText("assessment-data/ielts_mock_2025_january_writing_test_1.json");
    }

    private String loadObjectiveAnswerKey(AssessmentSkill skill) {
        String resourcePath = switch (skill) {
            case LISTENING -> "assessment-data/ielts_mock_2025_january_listening_test_1.json";
            case READING -> "assessment-data/ielts_mock_2025_january_reading_test_1.json";
            default -> null;
        };
        if (resourcePath == null) {
            return null;
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(new ClassPathResource(resourcePath).getInputStream());
            JsonNode answerKey = root.path("answerKey");
            if (!answerKey.isObject()) {
                return null;
            }
            return OBJECT_MAPPER.writeValueAsString(answerKey);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load objective answer key: " + resourcePath, ex);
        }
    }

    private String loadResourceText(String path) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load seed resource: " + path, ex);
        }
    }

    private String speakingInstructions(CourseModule module) {
        String plainInstructions = "Kiểm tra micro trước, sau đó làm lần lượt IELTS Speaking Part 1, Part 2 và Part 3 trước khi nộp bản ghi âm.";
        return plainInstructions + UI_CONFIG_MARKER + speakingUiConfig(module);
    }

    private String speakingUiConfig(CourseModule module) {
        String title = module == null || module.getTitle() == null ? "" : module.getTitle().toLowerCase();
        if (title.contains("100 ielts speaking questions")) {
            return """
                    {
                      "version": 1,
                      "type": "speaking_topic_bank",
                      "flow": ["mic_check", "briefing", "topic_bank", "recording", "submit"],
                      "briefing": {
                        "title": "Luyện Speaking Part 1 theo topic bank",
                        "summary": "Kiểm tra micro trước, đọc nhanh topic, rồi ghi âm trả lời liên tiếp nhiều câu như một buổi warm-up Speaking Part 1."
                      },
                      "topicBank": {
                        "sourceLabel": "Module 6 - 100 IELTS Speaking Questions",
                        "topics": [
                          {
                            "title": "Hometown and Living Place",
                            "prompts": ["Where are you from?", "What do you like most about your hometown?", "Has your hometown changed much in recent years?", "Would you like to live there in the future?"]
                          },
                          {
                            "title": "Work or Study",
                            "prompts": ["Do you work or are you a student?", "Why did you choose this course or job?", "What is the most interesting part of your daily routine?", "Is there anything you would like to change about your work or study?"]
                          },
                          {
                            "title": "Shopping",
                            "prompts": ["Do you enjoy shopping?", "Who usually does the shopping in your family?", "What kinds of shops do young people like?", "Is online shopping more popular than before?"]
                          },
                          {
                            "title": "Films and Entertainment",
                            "prompts": ["How often do you watch films?", "What kinds of films do you enjoy most?", "Do you prefer watching films alone or with other people?", "Would you ever like to be in a film?"]
                          }
                        ]
                      }
                    }
                    """;
        }
        return """
                {
                  "version": 1,
                  "type": "speaking_mock_test",
                  "flow": ["mic_check", "briefing", "mock_test", "recording", "submit"],
                  "briefing": {
                    "title": "IELTS Speaking Mock Test",
                    "summary": "Bắt đầu bằng bước kiểm tra micro, sau đó lần lượt làm Part 1, Part 2 và Part 3 như một bài Speaking mô phỏng."
                  },
                  "variants": [
                    {
                      "key": "jan_2025_test_1",
                      "label": "Mock Test 1",
                      "sourceLabel": "January Practice Test 1",
                      "parts": [
                        {
                          "key": "part_1",
                          "label": "Part 1",
                          "caption": "Introduction and Interview",
                          "description": "Trả lời ngắn gọn, tự nhiên và trực tiếp.",
                          "prepSeconds": 0,
                          "answerSeconds": 300,
                          "prompts": [{"text": "Where are you from?", "videoUrl": "https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%201/Part%201-%20Q1-Where%20are%20you%20from.mp4"}, {"text": "Where do you live now?", "videoUrl": "https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%201/Part%201-%20Q2-Where%20do%20you%20live%20now.mp4"}, {"text": "How long have you lived there?", "videoUrl": "https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%201/Part%201-%20Q3-How%20long%20have%20you%20lived%20there.mp4"}, {"text": "Who do you live with?", "videoUrl": "https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%201/Part%201-%20Q4-Who%20do%20you%20live%20with.mp4"}, {"text": "Do you plan to live there for a long time?", "videoUrl": "https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%201/Part%201-%20Q5-Do%20you%20plan%20to%20live%20there%20for%20a%20long%20time.mp4"}, {"text": "Do you like watching films?", "videoUrl": "https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%201/Part%201-%20Q6-Do%20you%20like%20watching%20films.mp4"}, {"text": "What kinds of movies do you like best?", "videoUrl": "https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%201/Part%201-%20Q7-What%20kinds%20of%20movies%20do%20you%20like%20best.mp4"}, {"text": "How often do you watch films?", "videoUrl": "https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%201/Part%201-%20Q8-How%20often%20do%20you%20watch%20films.mp4"}, {"text": "Do you like to watch movies alone or with your friends?", "videoUrl": "http://link.intergreat.com/7o2T1"}, {"text": "Would you like to be in a movie?", "videoUrl": "https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%201/Part%201-%20Q10-Would%20you%20like%20to%20be%20in%20a%20movie.mp4"}, {"text": "How often do you drink water?", "videoUrl": "https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%201/Part%201-%20Q11-How%20often%20do%20you%20drink%20water.mp4"}, {"text": "What kinds of water do you like to drink?", "videoUrl": "https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Test%202023/1/Test%201/Part%201%20-%20Q12%20-%20What%20kinds%20of%20water%20do%20you%20like%20to%20drink.mp4"}, {"text": "Do you drink bottled water or water from water machines?", "videoUrl": "http://link.intergreat.com/5gEnA"}]
                        },
                        {
                          "key": "part_2",
                          "label": "Part 2",
                          "caption": "Cue Card",
                          "description": "Bạn có 1 phút chuẩn bị và khoảng 2 phút nói liên tục.",
                          "videoUrl": "http://link.intergreat.com/RSi4I",
                          "prepSeconds": 60,
                          "answerSeconds": 120,
                          "cueCardTitle": "Describe an activity you would do when you are alone in your free time.",
                          "cueCardBullets": ["What you do", "How often you do it", "Why you like to do this activity", "How you feel when you do it"]
                        },
                        {
                          "key": "part_3",
                          "label": "Part 3",
                          "caption": "Topic Discussion",
                          "description": "Mở rộng ý, phân tích và so sánh sâu hơn.",
                          "prepSeconds": 0,
                          "answerSeconds": 300,
                          "prompts": [{"text": "Do you think people should only focus on work?", "videoUrl": "https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%201/Part%203-%20Q1-Do%20you%20think%20people%20should%20only%20focus%20on%20work.mp4"}, {"text": "Do you ever think how much time we will spend at work in a week?", "videoUrl": "http://link.intergreat.com/M93tA"}, {"text": "Should parents plan the children’s leisure time and activities?", "videoUrl": "http://link.intergreat.com/LwsAe"}, {"text": "Do you think the activities of the young generation are different from those of the older generation?", "videoUrl": "http://link.intergreat.com/6SSIw"}]
                        }
                      ]
                    },
                    {
                      "key": "jan_2025_test_2",
                      "label": "Mock Test 2",
                      "sourceLabel": "January Practice Test 2",
                      "parts": [
                        {
                          "key": "part_1",
                          "label": "Part 1",
                          "caption": "Introduction and Interview",
                          "description": "Giữ phản xạ tự nhiên và tránh học thuộc câu trả lời.",
                          "prepSeconds": 0,
                          "answerSeconds": 300,
                          "prompts": [{"text": "Are you a student or do you work now?", "videoUrl": "https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%202/Part%201-%20Q1-Are%20you%20a%20student%20or%20do%20you%20work%20now.mp4"}, {"text": "Why did you choose this course or job?", "videoUrl": "https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%202/Part%201-%20Q2-Why%20did%20you%20choose%20this%20coursejob.mp4"}, {"text": "Talk about your daily routine.", "videoUrl": "https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%202/Part%201-%20Q3-Talk%20about%20your%20daily%20routine.mp4"}, {"text": "Is there anything about your course or job you would like to change?", "videoUrl": "http://link.intergreat.com/D8I5B"}, {"text": "I’d like to move on and ask you some questions about shopping.", "videoUrl": "http://link.intergreat.com/iP0zN"}, {"text": "Who does most of the shopping in your household?", "videoUrl": "https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%202/Part%201-%20Q6-Who%20does%20most%20of%20the%20shopping%20in%20your%20household.mp4"}, {"text": "What type of shopping do you like? Why?", "videoUrl": "https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%202/Part%201-%20Q7-What%20type%20of%20shopping%20do%20you%20like%20%28Why%29.mp4"}, {"text": "Is shopping a popular activity in your country? Why or why not?", "videoUrl": "http://link.intergreat.com/bYnoU"}, {"text": "What type of shops do teenagers like best in your country?", "videoUrl": "http://link.intergreat.com/lOMN3"}, {"text": "Let’s talk about films. How often do you go to the cinema?", "videoUrl": "https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%202/Part%201-%20Q10-Let%E2%80%99s%20talk%20about%20films..mp4"}, {"text": "What type of films do you like best? Why?", "videoUrl": "https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%202/Part%201-%20Q11-What%20type%20of%20films%20do%20you%20like%20best%20%28Why%29.mp4"}, {"text": "What type of films don’t you like? Why not?", "videoUrl": "http://link.intergreat.com/3FbDt"}]
                        },
                        {
                          "key": "part_2",
                          "label": "Part 2",
                          "caption": "Cue Card",
                          "description": "Chuẩn bị nhanh, ghi ý chính và nói liền mạch trong khoảng 2 phút.",
                          "videoUrl": "https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%202/Part%202-Describe%20an%20important%20event%20in%20your%20life..mp4",
                          "prepSeconds": 60,
                          "answerSeconds": 120,
                          "cueCardTitle": "Describe an important event in your life.",
                          "cueCardBullets": ["When it happened", "Who you were with", "What happened", "Why you feel it was important"]
                        },
                        {
                          "key": "part_3",
                          "label": "Part 3",
                          "caption": "Topic Discussion",
                          "description": "Trả lời theo góc nhìn xã hội, xu hướng và tác động rộng hơn.",
                          "prepSeconds": 0,
                          "answerSeconds": 300,
                          "prompts": [{"text": "What days are important in your country?", "videoUrl": "https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%202/Part%203-%20Q1-What%20days%20are%20important%20in%20your%20country.mp4"}, {"text": "Why is it important to have national celebrations?", "videoUrl": "https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%202/Part%203-%20Q2-Why%20it%20is%20important%20to%20have%20national%20celebrations.mp4"}, {"text": "How are national celebrations now different from those in the past?", "videoUrl": "http://link.intergreat.com/Q2kcd"}, {"text": "Do you think any new national celebrations will appear in the future?", "videoUrl": "http://link.intergreat.com/2W3Bd"}, {"text": "Are there any celebrations from other countries that people celebrate in your country?", "videoUrl": "http://link.intergreat.com/PAQjN"}, {"text": "What are the benefits of having events that many people around the world celebrate on the same day?", "videoUrl": "http://link.intergreat.com/lATrc"}]
                        }
                      ]
                    }
                  ]
                }
                """;
    }

    private Optional<CourseAssessment> findSeededModuleAssessment(List<CourseAssessment> assessments, CourseModule module, String seededTitlePrefix) {
        List<CourseAssessment> matches = assessments.stream()
                .filter(item -> item.getModule() != null
                        && item.getModule().getId() != null
                        && module.getId() != null
                        && item.getModule().getId().equals(module.getId())
                        && item.getType() == AssessmentType.MODULE_TEST)
                .toList();
        return matches.stream()
                .filter(item -> item.getTitle() != null && item.getTitle().startsWith(seededTitlePrefix))
                .findFirst()
                .or(() -> matches.size() == 1 ? Optional.of(matches.get(0)) : Optional.empty());
    }

    private Optional<CourseAssessment> findSeededMockAssessment(List<CourseAssessment> assessments) {
        List<CourseAssessment> matches = assessments.stream()
                .filter(item -> item.getType() == AssessmentType.MOCK_TEST)
                .toList();
        return matches.stream()
                .filter(item -> "Final AI Mock Reflection".equalsIgnoreCase(item.getTitle()))
                .findFirst()
                .or(() -> matches.size() == 1 ? Optional.of(matches.get(0)) : Optional.empty());
    }
}
