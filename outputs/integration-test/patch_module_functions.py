# -*- coding: utf-8 -*-
"""Insert function/controller names into full_modules.py."""
from __future__ import annotations

import importlib.util
import re
from pathlib import Path

FUNCS = {
    "AUTH": "AuthController",
    "USER": "UserController",
    "NOTIF": "StudentNotificationController",
    "COMMERCE": "StudentCommerceController",
    "PAYMENT": "StudentPaymentController",
    "COURSE": "PublicOnlineCourseController",
    "DISCUSS": "CourseDiscussionController",
    "CONTENT": "ContentManagerOnlineCourseController",
    "PACKAGE": "LearningPackageManagementController",
    "CURRICULUM": "ContentManagerCurriculumController",
    "ENROLLREQ": "ManagerEnrollmentController",
    "CLASS": "TrainingManagerClassroomController",
    "LEARNERCLS": "StudentClassroomController",
    "TEACH": "TeacherClassroomController",
    "QUIZ": "ClassroomQuizController",
    "ASSESS": "PlacementTestController",
    "SUPPORT": "StudentSupportTicketController",
    "ADMIN": "AdminUserController",
    "LARK": "LarkWebhookController",
    "INFRA": "TrainingManagerInfrastructureController",
    "REPORT": "TrainingManagerDashboardController",
    "PROPOSAL": "ManagerClassroomController",
    "DISPUTE": "ClassroomAttendanceDisputeController",
    "NOTES": "StudentClassroomController",
}

p = Path(__file__).with_name("full_modules.py")
text = p.read_text(encoding="utf-8")
if '"function"' in text:
    print("already patched")
else:
    pattern = re.compile(
        r'("code": "([A-Z]+)",\n\s*"sheet": "[^"]+",\n\s*"name": "[^"]+",)'
    )

    def repl(m: re.Match[str]) -> str:
        code = m.group(2)
        return m.group(1) + f'\n        "function": "{FUNCS[code]}",'

    text, n = pattern.subn(repl, text)
    p.write_text(text, encoding="utf-8")
    print("patched", n)

spec = importlib.util.spec_from_file_location("fm", p)
fm = importlib.util.module_from_spec(spec)
spec.loader.exec_module(fm)
missing = [m["code"] for m in fm.MODULES if "function" not in m]
print("modules", len(fm.MODULES), "missing", missing)
print([(m["code"], m["function"]) for m in fm.MODULES])
