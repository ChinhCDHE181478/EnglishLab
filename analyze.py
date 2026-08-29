import os
import re
from collections import defaultdict

filepath = 'backend/src/main/java/fu/sep490/g23/backend/service/course/impl/OnlineCourseServiceImpl.java'
content = open(filepath, 'r', encoding='utf-8').read()

# Find all methods and their bodies
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
        body = content[start_body:end_body]
        methods[method_name] = body

# Build call graph
calls = defaultdict(set)
for m_name, body in methods.items():
    for other_m in methods.keys():
        if other_m != m_name and re.search(r'\b' + other_m + r'\s*\(', body):
            calls[m_name].add(other_m)

learner_entry = ['getCourseCertificate', 'getCourseCompletion', 'getEnrolledCourse', 'getMyEnrollments', 
                 'getRecommendedCourses', 'getVocabularyTerms', 'registerCourse', 'updateLessonProgress', 
                 'updateVocabularyProgress']

to_visit = list(learner_entry)
visited = set(learner_entry)

while to_visit:
    curr = to_visit.pop(0)
    for callee in calls[curr]:
        if callee not in visited:
            visited.add(callee)
            to_visit.append(callee)

print('Learner reachable methods:', sorted(visited))
