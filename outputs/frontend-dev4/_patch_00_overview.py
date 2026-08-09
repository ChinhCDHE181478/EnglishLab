# -*- coding: utf-8 -*-
from pathlib import Path

p = Path(r"C:\Users\phong\Downloads\intergration test\giai thich code\file md\00_Tong_quan_va_nen_tang_chung.md")
t = p.read_text(encoding="utf-8")

pairs = [
    ("ManagerClassroomsPage.jsx", "StaffClassroomsPage.jsx"),
    ("TrainingManagerClassroomDetailPage.jsx", "StaffClassroomDetailPage.jsx"),
    ("06_ManagerClassroomsPage.md", "06_StaffClassroomsPage.md"),
    ("07_TrainingManagerClassroomDetailPage.md", "07_StaffClassroomDetailPage.md"),
    ("Cả 9 màn hình", "Các màn hình"),
    ("9 file kia", "các file kia"),
    ("không còn là 9 thứ khác nhau", "không còn là nhiều thứ khác nhau"),
    ("Bản đồ 9 file bạn phụ trách", "Bản đồ các file bạn phụ trách"),
    ("TEACHER, TRAINING_MANAGER, MANAGER, ADMIN", "TEACHER, MANAGER, ADMIN"),
    ("| STAFF / TRAINING_MANAGER |", "| STAFF, ADMIN |"),
    ("```136:143:frontend/src/App.jsx", "```157:174:frontend/src/App.jsx"),
]
for a, b in pairs:
    t = t.replace(a, b)

t = t.replace("| STAFF | Duyệt", "| STAFF, ADMIN | Duyệt")
t = t.replace("| STAFF | Soạn", "| STAFF, ADMIN | Soạn")

old_roles = "allowedRoles={['TEACHER', 'TRAINING_MANAGER', 'MANAGER', 'ADMIN']}"
new_roles = "allowedRoles={['TEACHER', 'MANAGER', 'ADMIN']}"
t = t.replace(old_roles, new_roles)

if "StaffRequestsPage.jsx" not in t:
    lines = t.splitlines()
    out = []
    inserted = False
    for line in lines:
        out.append(line)
        if (not inserted) and "StaffClassroomProposalsPage.jsx" in line and line.strip().startswith("|"):
            out.append("| 10 | `StaffRequestsPage.jsx` | `/staff/requests` | STAFF, ADMIN | Duyệt yêu cầu đổi lịch từ giáo viên |")
            inserted = True
    t = "\n".join(out)
    if p.read_text(encoding="utf-8").endswith("\n"):
        t += "\n"

# learning path mentions near end
t = t.replace(
    "4. `03_TeacherClassroomPage.md`, `04_TeacherSessionPage.md`, `07_StaffClassroomDetailPage.md` — trang chi tiết, lấy `id` từ URL.\n5. `06_StaffClassroomsPage.md` — danh sách có nhiều bộ lọc.",
    "4. `03_TeacherClassroomPage.md`, `04_TeacherSessionPage.md`, `07_StaffClassroomDetailPage.md` — trang chi tiết, lấy `id` từ URL.\n5. `06_StaffClassroomsPage.md` — danh sách có nhiều bộ lọc.\n6. `11_StaffRequestsPage.md` — Staff duyệt yêu cầu đổi lịch.",
)

p.write_text(t, encoding="utf-8")
print("ok", "StaffRequestsPage" in t, "ManagerClassrooms" not in t)
