import os
import re
from collections import defaultdict

filepath = 'backend/src/main/java/fu/sep490/g23/backend/service/course/impl/OnlineCourseServiceImpl.java'
content = open(filepath, 'r', encoding='utf-8').read()

method_pattern = r'(?:public|private|protected)\s+[\w\<\>\[\]\,\s]+\s+(\w+)\s*\([^)]*\)\s*(?:throws\s+[\w\,\s]+)?\s*\{'
methods = {}
for match in re.finditer(method_pattern, content):
    method_name = match.group(1)
    start_body = match.end() - 1
    count = 0
    end_body = -1
    for i in range(start_body, len(content)):
        if content[i] == '{': count += 1
        elif content[i] == '}':
            count -= 1
            if count == 0:
                end_body = i
                break
    if end_body != -1:
        methods[method_name] = content[start_body:end_body]

calls = defaultdict(set)
for m_name, body in methods.items():
    for other_m in methods.keys():
        if other_m != m_name and re.search(r'\b' + other_m + r'\s*\(', body):
            calls[m_name].add(other_m)

def get_reachable(entry_points):
    to_visit = list(entry_points)
    visited = set(entry_points)
    while to_visit:
        curr = to_visit.pop(0)
        for callee in calls[curr]:
            if callee not in visited:
                visited.add(callee)
                to_visit.append(callee)
    return visited

learner_entry = ['getCourseCertificate', 'getCourseCompletion', 'getEnrolledCourse', 'getMyEnrollments', 
                 'getRecommendedCourses', 'getVocabularyTerms', 'registerCourse', 'updateLessonProgress', 
                 'updateVocabularyProgress']
manager_public_entry = ['getPublicCourses', 'getPublicCourse', 'verifyCourseCertificate', 'getManagerCourses', 
                        'getManagerCourse', 'getManagerCoursePreview', 'reorderLessons', 'getManagerCourseAssessments', 
                        'getManagerAssessmentRubrics', 'saveManagerCourseAssessments', 'getStats', 'createCourse', 
                        'updateCourse', 'publishCourse', 'archiveCourse', 'deleteCourse', 'uploadLessonVideo', 
                        'refreshLessonTranscript']

learner_reachable = get_reachable(learner_entry)
other_reachable = get_reachable(manager_public_entry)

pure_learner = learner_reachable - other_reachable
print('Pure Learner Methods:', sorted(pure_learner))
