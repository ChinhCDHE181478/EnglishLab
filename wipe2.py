import os
import re

target_methods = {
    'backend/src/main/java/fu/sep490/g23/backend/service/course/impl/OnlineCourseServiceImpl.java': [
        'activateEnrollment', 'applyVocabularyProgress', 'buildRecommendationReason', 'decimalToDouble', 'ensureEnrolled', 'findPublishedCourseForEnrollment', 'formatBand', 'getCourseCertificate', 'getCourseCompletion', 'getEnrolledCourse', 'getMyEnrollments', 'getRecommendedCourses', 'getVocabularyTerms', 'isBandCompatible', 'isCompletedEnrollment', 'isExamCompatible', 'isFreeCourse', 'parseBand', 'recommendCourses', 'registerCourse', 'safe', 'scoreRecommendation', 'skillLabel', 'updateLessonProgress', 'updateVocabularyProgress'
    ],
    'backend/src/main/java/fu/sep490/g23/backend/service/assessment/impl/PlacementRecommendationServiceImpl.java': [
        'canBuildRecommendations', 'focusSkills', 'getRecommendations', 'levelLabel', 'readinessMessage', 'recommendTrainingPrograms', 'skillLabel', 'toTrainingResponse', 'trainingProgramScore'
    ],
    'backend/src/main/java/fu/sep490/g23/backend/service/assessment/impl/PlacementTestServiceImpl.java': [
        'collectQuestionNumbers', 'countKeywordHits', 'countWords', 'evaluateProductiveSkills', 'evaluateSpeakingEvidence', 'evaluateWritingEvidence', 'extractBand', 'fallbackFeedback', 'getTest', 'listeningBand', 'matches', 'normalizeBand', 'normalizeExamType', 'normalizeForRelevance', 'placementBankItem', 'readBand', 'readingBand', 'resolveSelectedSkills', 'resolveStoredExamType', 'scoreObjective', 'scoreToeicSection', 'skillAssessmentFeedback', 'submit', 'submitSkillAssessment', 'submitToeicPlacement', 'toeicLevel', 'toeicQuestionNumbers', 'toeicScaledScore', 'validateSkillAssessmentSubmission', 'validateSubmission', 'validateToeicSubmission'
    ],
    'backend/src/main/java/fu/sep490/g23/backend/service/assessment/impl/AiAssessmentServiceImpl.java': [
        'applySpeakingEvidenceGuard', 'applyVocabularyRelevanceGuard', 'buildInsufficientWritingResult', 'buildMultiSelectFeedback', 'buildObjectiveFeedback', 'buildPartSuggestions', 'buildRubricPrompt', 'buildSubmittedContent', 'createCriterionNode', 'createPartFeedbackNode', 'ensureArray', 'ensureEnrolled', 'estimateObjectiveBand', 'evaluateObjectiveAssessment', 'evaluateObjectiveAssessmentWithoutAnswerKey', 'extractDisplayInstructions', 'extractTargetVocabulary', 'extractUiConfigJson', 'formatObjectiveAnswers', 'getCourseAssessments', 'isAudioReferenceOnlySpeakingSubmission', 'isInsufficientWritingSubmission', 'isObjectiveAssessmentSkill', 'normalizeAnswer', 'normalizeAssessmentRubricCompatibility', 'normalizeEstimatedScore', 'normalizeEvaluationResult', 'normalizeFeedbackJson', 'resolveExpectedAnswerSet', 'resolveExpectedAnswers', 'resolveSubmissionStatus', 'rewriteSpeakingEvidenceFeedback', 'rewriteVocabularyGuardFeedback', 'sanitizeUiConfigJson', 'scoreObjectiveAssessment', 'skillEvaluationPolicy', 'skillSubmissionGuidance', 'submitAssessment', 'toResponse', 'toRubricResponse', 'toSubmissionResponse', 'upsertCriterion', 'usesObjectiveAnswerKey', 'validateSkillAssessmentConfiguration'
    ],
    'backend/src/main/java/fu/sep490/g23/backend/service/classroom/impl/ClassroomHomeworkServiceImpl.java': [
        'findHomework', 'isLearnerInClass', 'listForClass', 'listForLearner', 'submit', 'syncHomeworkScoreToGradebook'
    ],
    'backend/src/main/java/fu/sep490/g23/backend/service/classroom/impl/ClassroomPracticeServiceImpl.java': [
        'complete', 'latestAttempts', 'listAllForLearner', 'listAttempts', 'listForLearner', 'practiceRefs', 'requireLearnerAccess', 'requireOffering', 'requirePracticeRef', 'resolveClassroomTitle', 'score', 'submitAttempt', 'toAttemptResponse', 'toResponse', 'validateSubmission'
    ],
    'backend/src/main/java/fu/sep490/g23/backend/service/classroom/impl/ClassroomGradebookServiceImpl.java': [
        'buildResponse', 'getMyGradebook', 'resolveHomeworkStatus', 'toHomeworkResponse'
    ],
    'backend/src/main/java/fu/sep490/g23/backend/service/classroom/impl/ClassroomContentServiceImpl.java': [
        'assertLearnerPortalAccess', 'getLearnerAnnouncements', 'getLearnerMaterials', 'getLearnerSyllabus'
    ],
    'backend/src/main/java/fu/sep490/g23/backend/service/course/impl/FlashcardPracticeServiceImpl.java': [
        'applyProgress', 'extractFlashcardSet', 'extractLessonContent', 'extractTerms', 'getEnrolledVersionTerms', 'getPracticeTerms', 'initializeCourse', 'resolveCourses', 'safeFlashcardSets', 'safeLessons', 'safeModules'
    ],
    'backend/src/main/java/fu/sep490/g23/backend/service/classroom/impl/ClassroomAttendanceServiceImpl.java': [
        'getByClassForStudent'
    ]
}

controllers = [
    'backend/src/main/java/fu/sep490/g23/backend/controller/course/StudentOnlineCourseController.java',
    'backend/src/main/java/fu/sep490/g23/backend/controller/course/StudentLearningExperienceController.java',
    'backend/src/main/java/fu/sep490/g23/backend/controller/course/StudentFlashcardPracticeController.java',
    'backend/src/main/java/fu/sep490/g23/backend/controller/assessment/PlacementTestController.java',
    'backend/src/main/java/fu/sep490/g23/backend/controller/assessment/StudentAssessmentController.java',
    'backend/src/main/java/fu/sep490/g23/backend/controller/classroom/StudentClassroomController.java',
    'backend/src/main/java/fu/sep490/g23/backend/controller/classroom/StudentEnrollmentRequestController.java',
    'backend/src/main/java/fu/sep490/g23/backend/controller/classroom/StudentNotificationController.java'
]

def remove_all_methods(filepath):
    if not os.path.exists(filepath): return
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    match = re.search(r'\bclass\s+\w+.*?\{', content, re.DOTALL)
    if match:
        start_index = match.end()
        end_index = content.rfind('}')
        if start_index < end_index:
            content = content[:start_index] + '\n\n' + content[end_index:]
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)

def remove_specific_methods(filepath, methods):
    if not os.path.exists(filepath): return
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Sort methods by length descending to match longest first if there are conflicts, 
    # but here method names are unique strings.
    for method in methods:
        pattern = r'\b(?:public|protected|private)\s+[\w\<\>\[\]\?\,\s]+\s+' + method + r'\s*\([^)]*\)\s*(?:throws\s+[\w\,\s]+)?\s*\{'
        # We need a while loop because there might be overloaded methods
        while True:
            match = re.search(pattern, content)
            if not match:
                break
                
            start_sig = match.start()
            prefix = content[:start_sig]
            last_brace = max(prefix.rfind('}'), prefix.rfind(';'), prefix.rfind('{'))
            start_delete = last_brace + 1 if last_brace != -1 else start_sig
            
            start_body = match.end() - 1
            count = 0
            end_delete = -1
            for i in range(start_body, len(content)):
                if content[i] == '{': count += 1
                elif content[i] == '}':
                    count -= 1
                    if count == 0:
                        end_delete = i
                        break
            
            if end_delete != -1:
                content = content[:start_delete] + '\n' + content[end_delete+1:]
            else:
                break # To prevent infinite loop if parsing fails
                
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

for c in controllers:
    remove_all_methods(c)

for s, methods in target_methods.items():
    remove_specific_methods(s, methods)

print('Done applying recursive methods wipes')
