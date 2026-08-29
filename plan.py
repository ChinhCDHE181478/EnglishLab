import os
import re
from collections import defaultdict

CONTROLLER_DIR = 'backend/src/main/java/fu/sep490/g23/backend/controller'
SERVICE_IMPL_DIR = 'backend/src/main/java/fu/sep490/g23/backend/service'

def find_java_files(directory):
    files = []
    for root, _, filenames in os.walk(directory):
        for f in filenames:
            if f.endswith('.java'):
                files.append(os.path.join(root, f))
    return files

controller_files = find_java_files(CONTROLLER_DIR)

learner_controllers = []
non_learner_controllers = []
for f in controller_files:
    basename = os.path.basename(f)
    if basename.startswith('Student') or basename.startswith('PlacementTest'):
        learner_controllers.append(f)
    else:
        non_learner_controllers.append(f)

def extract_service_calls(files):
    # returns mapping: service_class_name -> set of method names
    # (assuming service variable names map directly to service classes, e.g., onlineCourseService -> OnlineCourseServiceImpl)
    calls = defaultdict(set)
    for f in files:
        content = open(f, 'r', encoding='utf-8').read()
        for match in re.finditer(r'\b([a-z]\w*Service)\.([a-z]\w*)\(', content):
            svc = match.group(1)
            method = match.group(2)
            # convert onlineCourseService to OnlineCourseServiceImpl
            impl_name = svc[0].upper() + svc[1:] + 'Impl.java'
            calls[impl_name].add(method)
    return calls

learner_entry_points = extract_service_calls(learner_controllers)
non_learner_entry_points = extract_service_calls(non_learner_controllers)

service_files = find_java_files(SERVICE_IMPL_DIR)
service_file_map = {os.path.basename(f): f for f in service_files}

pure_learner_map = {}

for impl_name, learner_entries in learner_entry_points.items():
    if impl_name not in service_file_map:
        continue
    filepath = service_file_map[impl_name]
    content = open(filepath, 'r', encoding='utf-8').read()
    
    # parse methods
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
            
    # build call graph
    calls = defaultdict(set)
    for m_name, body in methods.items():
        for other_m in methods.keys():
            if other_m != m_name and re.search(r'\b' + other_m + r'\s*\(', body):
                calls[m_name].add(other_m)
                
    def get_reachable(entry_points):
        to_visit = [ep for ep in entry_points if ep in methods]
        visited = set(to_visit)
        while to_visit:
            curr = to_visit.pop(0)
            for callee in calls[curr]:
                if callee not in visited:
                    visited.add(callee)
                    to_visit.append(callee)
        return visited

    learner_reachable = get_reachable(learner_entries)
    
    non_learner_entries = non_learner_entry_points.get(impl_name, set())
    # We must also consider public methods as implicitly callable by non-learners if this is a mixed service?
    # No, just use what is actually called by other controllers. But wait, what if another service calls it?
    # Let's stick to controller entry points.
    other_reachable = get_reachable(non_learner_entries)
    
    pure_learner = learner_reachable - other_reachable
    if pure_learner:
        pure_learner_map[filepath] = list(pure_learner)

# Let's print out what we found
for filepath, methods in pure_learner_map.items():
    print(os.path.basename(filepath) + ':', sorted(methods))

