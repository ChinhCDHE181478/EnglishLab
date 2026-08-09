# -*- coding: utf-8 -*-
from pathlib import Path

MD = Path(r"C:\Users\phong\Downloads\intergration test\giai thich code\file md")

def apply(path: Path, pairs: list[tuple[str, str]]) -> int:
    text = path.read_text(encoding="utf-8")
    n = 0
    for old, new in pairs:
        count = text.count(old)
        if count:
            text = text.replace(old, new)
            n += count
    path.write_text(text, encoding="utf-8")
    return n

schedule = [
    ("phòng Lark", "phòng Google Meet"),
    ("Phòng học Lark", "Phòng học Google Meet"),
    ("Phòng Lark", "Phòng Google Meet"),
    ("link Lark", "liên kết Google Meet"),
    ("Link Lark", "Liên kết Google Meet"),
    ("tạo phòng Lark", "tạo phòng Google Meet"),
    ("Tạo và mở phòng Lark", "Tạo và mở Google Meet"),
    ("Vào phòng Lark", "Vào Google Meet"),
    ("Xem lại phòng Lark", "Mở lại Google Meet"),
    ("Lớp học trực tuyến (Lark)", "Lớp học trực tuyến (Google Meet)"),
    (" /> Lark</span>", " /> Google Meet</span>"),
    ("online/Lark", "online/Google Meet"),
]

session = [
    ("Đã cập nhật liên kết Lark thành công", "Đã cập nhật liên kết Google Meet thành công"),
    ("Không thể cập nhật liên kết Lark", "Không thể cập nhật liên kết Google Meet"),
    ("Nhập liên kết phòng học Lark mới", "Nhập liên kết Google Meet mới"),
    ("Cập nhật Lark Link", "Cập nhật liên kết Google Meet"),
    ("khối Lark", "khối Google Meet"),
    ("Khối Lark", "Khối Google Meet"),
    ("vùng Lark", "vùng Google Meet"),
    ("phòng Lark", "phòng Google Meet"),
    ("Phòng Lark", "Phòng Google Meet"),
    ("dịch vụ Lark", "dịch vụ Google Meet"),
    ("ô link Lark", "ô link Google Meet"),
    ("Vận hành lớp trực tuyến (Lark)", "Vận hành lớp trực tuyến (Google Meet)"),
    ("Virtual Meeting Operations (Lark)", "Virtual Meeting Operations (Google Meet)"),
]

print("02", apply(MD / "02_TeacherSchedulePage.md", schedule))
print("04", apply(MD / "04_TeacherSessionPage.md", session))
print("10", apply(MD / "10_Huong_dan_thao_tac_tren_web.md", [
    ("phòng Lark", "phòng Google Meet"),
    ("Tạo và mở phòng Lark", "Tạo và mở Google Meet"),
    ("liên kết Lark", "liên kết Google Meet"),
    ("Cập nhật Lark Link", "Cập nhật liên kết Google Meet"),
    ("khối vận hành Lark", "khối vận hành Google Meet"),
    ("Bấm mở phòng Lark", "Bấm mở phòng Google Meet"),
    ("06_ManagerClassroomsPage.md", "06_StaffClassroomsPage.md"),
    ("07_TrainingManagerClassroomDetailPage.md", "07_StaffClassroomDetailPage.md"),
]))
