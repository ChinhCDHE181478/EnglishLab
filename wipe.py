import os
import re

target_methods = {
    'backend/src/main/java/fu/sep490/g23/backend/service/course/impl/OnlineCourseServiceImpl.java': [
        'getCourseCertificate', 'getCourseCompletion', 'getEnrolledCourse', 'getMyEnrollments', 
        'getRecommendedCourses', 'getVocabularyTerms', 'registerCourse', 'updateLessonProgress', 
        'updateVocabularyProgress'
    ],
    'backend/src/main/java/fu/sep490/g23/backend/service/course/impl/LearnerLearningExperienceServiceImpl.java': [
        'addReviewFlag', 'createNote', 'deleteNote', 'getNotes', 'getReviewFlags', 'removeReviewFlag', 'updateNote'
    ],
    'backend/src/main/java/fu/sep490/g23/backend/service/course/impl/FlashcardPracticeServiceImpl.java': [
        'getPracticeTerms'
    ],
    'backend/src/main/java/fu/sep490/g23/backend/service/assessment/impl/PlacementTestServiceImpl.java': [
        'getTest', 'submit'
    ],
    'backend/src/main/java/fu/sep490/g23/backend/service/assessment/impl/AiAssessmentServiceImpl.java': [
        'getCourseAssessments', 'submitAssessment'
    ],
    'backend/src/main/java/fu/sep490/g23/backend/service/classroom/impl/ClassroomAttendanceServiceImpl.java': [
        'getByClassForStudent'
    ],
    'backend/src/main/java/fu/sep490/g23/backend/service/classroom/impl/ClassroomHomeworkServiceImpl.java': [
        'listForClass', 'listForLearner', 'submit'
    ],
    'backend/src/main/java/fu/sep490/g23/backend/service/classroom/impl/ClassroomGradebookServiceImpl.java': [
        'getMyGradebook'
    ],
    'backend/src/main/java/fu/sep490/g23/backend/service/classroom/impl/ClassroomPracticeServiceImpl.java': [
        'complete', 'listAllForLearner', 'listAttempts', 'listForLearner', 'submitAttempt'
    ],
    'backend/src/main/java/fu/sep490/g23/backend/service/classroom/impl/ClassroomContentServiceImpl.java': [
        'getLearnerAnnouncements', 'getLearnerMaterials', 'getLearnerSyllabus'
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
    
    for method in methods:
        pattern = r'\b(?:public|protected|private)\s+[\w\<\>\[\]\?\,\s]+\s+' + method + r'\s*\([^)]*\)\s*(?:throws\s+[\w\,\s]+)?\s*\{'
        match = re.search(pattern, content)
        if match:
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
                
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

for c in controllers:
    remove_all_methods(c)
for s, methods in target_methods.items():
    remove_specific_methods(s, methods)

print('Done')
