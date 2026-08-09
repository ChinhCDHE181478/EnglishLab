# -*- coding: utf-8 -*-
"""Generate beginner-friendly MD explaining all IT Java test files."""
from __future__ import annotations

import re
import shutil
from pathlib import Path

JAVA_DIR = Path(r"D:\EngLishLab\EnglishLab\backend\src\test\java\fu\sap490\g23\backend\it")
OUT_DIR = Path(r"C:\Users\phong\Downloads\intergration test")
PROJ = Path(r"D:\EngLishLab\EnglishLab\outputs\integration-test")
OUT_NAME = "Giai_thich_26_file_code_IT_chi_tiet.md"

ORDER = [
    "ItSupport.java",
    "EnglishLabIT.java",
    "ItTimezoneInitializer.java",
    "AuthIT.java",
    "AuthOtpIT.java",
    "UserIT.java",
    "NotificationIT.java",
    "CommerceIT.java",
    "PaymentIT.java",
    "OnlineCourseIT.java",
    "DiscussionIT.java",
    "ContentManagerCourseIT.java",
    "PackageIT.java",
    "CurriculumIT.java",
    "EnrollmentRequestIT.java",
    "TrainingManagerClassroomIT.java",
    "StudentClassroomIT.java",
    "TeacherClassroomIT.java",
    "ClassroomQuizIT.java",
    "AssessmentIT.java",
    "SupportTicketIT.java",
    "AdminIT.java",
    "LarkIT.java",
    "InfrastructureIT.java",
    "ReportIT.java",
    "ClassroomProposalIT.java",
    "AttendanceDisputeIT.java",
    "LearningNotesIT.java",
]

FILE_INTRO = {
    "ItSupport.java": "Helper dùng chung (login, tài khoản demo, Bearer). **Không có @Test — đừng Run Test file này.**",
    "EnglishLabIT.java": "Annotation gộp `@SpringBootTest` + `@AutoConfigureMockMvc` + timezone initializer. Mọi class `*IT` gắn `@EnglishLabIT`.",
    "ItTimezoneInitializer.java": "Set `Asia/Ho_Chi_Minh` trước khi Spring tạo DataSource (tránh lỗi IDE Asia/Saigon).",
    "AuthIT.java": "Sheet Excel **IT - Auth** — đăng ký, login JWT, negative OTP/password, /me.",
    "AuthOtpIT.java": "IT_AUTH_03 / IT_AUTH_09 — đọc OTP thật từ bảng `auth_tokens` bằng JdbcTemplate.",
    "UserIT.java": "Sheet **IT - User** — hồ sơ /me, đổi mật khẩu negative, avatar.",
    "NotificationIT.java": "Sheet **IT - Notif** — preference + list thông báo.",
    "CommerceIT.java": "Sheet **IT - Commerce** — giỏ hàng / wishlist.",
    "PaymentIT.java": "Sheet **IT - Payment** — PayOS link, quote, webhook, manager orders.",
    "OnlineCourseIT.java": "Sheet **IT - Course** — catalog public + content/progress learner.",
    "DiscussionIT.java": "Sheet **IT - Discuss** — thảo luận / report / CM moderation.",
    "ContentManagerCourseIT.java": "Sheet **IT - Content** — CM list khóa online.",
    "PackageIT.java": "Sheet **IT - Package** — CM packages.",
    "CurriculumIT.java": "Sheet **IT - Curriculum** — programs, bank, rubrics.",
    "EnrollmentRequestIT.java": "Sheet **IT - EnrollReq** — HV tạo form + Staff list.",
    "TrainingManagerClassroomIT.java": "Sheet **IT - Classroom** — TM lớp / registrations / waitlist.",
    "StudentClassroomIT.java": "Sheet **IT - LearnerCls** — lớp của HV, session, homework…",
    "TeacherClassroomIT.java": "Sheet **IT - Teacher** — lớp assigned, điểm danh, gradebook.",
    "ClassroomQuizIT.java": "Sheet **IT - Quiz** — quiz GV/HV.",
    "AssessmentIT.java": "Sheet **IT - Assess** — placement, mock, assessments.",
    "SupportTicketIT.java": "Sheet **IT - Support** — ticket HV + manager.",
    "AdminIT.java": "Sheet **IT - Admin** — users, audit, config.",
    "LarkIT.java": "Sheet **IT - Lark** — webhook + sync.",
    "InfrastructureIT.java": "Sheet **IT - Infra** — campus, room, template.",
    "ReportIT.java": "Sheet **IT - Report** — dashboard / revenue.",
    "ClassroomProposalIT.java": "Sheet **IT - Proposal** — đề xuất lớp (Staff).",
    "AttendanceDisputeIT.java": "Sheet **IT - Dispute** — khiếu nại điểm danh.",
    "LearningNotesIT.java": "Sheet **IT - Notes** — ghi chú học.",
}


def extract_methods(src: str) -> list[tuple[str, str, str]]:
    """Return list of (display_name, method_name, method_body_snippet)."""
    out = []
    # find @DisplayName ... void name(
    pattern = re.compile(
        r'@DisplayName\("([^"]+)"\)\s*void\s+(\w+)\s*\([^)]*\)\s*throws\s+Exception\s*\{',
        re.M,
    )
    for m in pattern.finditer(src):
        out.append((m.group(1), m.group(2), m.group(0)))
    # also void without throws
    pattern2 = re.compile(
        r'@DisplayName\("([^"]+)"\)\s*void\s+(\w+)\s*\([^)]*\)\s*\{',
        re.M,
    )
    if not out:
        for m in pattern2.finditer(src):
            out.append((m.group(1), m.group(2), m.group(0)))
    return out


def explain_method(display: str, name: str, src: str) -> list[str]:
    lines = [f"#### `{display}` → method `{name}()`", ""]
    # slice method body roughly
    idx = src.find(f"void {name}")
    chunk = src[idx : idx + 800] if idx >= 0 else ""

    if "UUID.randomUUID()" in chunk:
        lines.append(
            "- Tạo email/`id` ngẫu nhiên bằng `UUID.randomUUID()` để **không trùng** data khi chạy lại."
        )
    if '""".formatted' in chunk or ".formatted(" in chunk:
        lines.append(
            "- `\"\"\" ... %s ... \"\"\".formatted(...)` = **text block JSON** + chỗ trống `%s` được điền biến (email, password, id…)."
        )
    if "login(mockMvc" in chunk:
        lines.append(
            "- `login(mockMvc, ROLE, PASSWORD)` (từ `ItSupport`) = POST `/api/auth/login` → lấy **accessToken (JWT)**."
        )
    if "bearer(token)" in chunk:
        lines.append(
            "- `bearer(token)` = ghép header `Authorization: Bearer <jwt>` để Security nhận diện user."
        )
    if "post(" in chunk:
        lines.append("- `mockMvc.perform(post(\"...\"))` = giả lập **HTTP POST** vào Controller (như Postman Send).")
    if "get(" in chunk:
        lines.append("- `mockMvc.perform(get(\"...\"))` = giả lập **HTTP GET**.")
    if "put(" in chunk:
        lines.append("- `mockMvc.perform(put(\"...\"))` = giả lập **HTTP PUT**.")
    if "patch(" in chunk:
        lines.append("- `mockMvc.perform(patch(\"...\"))` = giả lập **HTTP PATCH**.")
    if "delete(" in chunk:
        lines.append("- `mockMvc.perform(delete(\"...\"))` = giả lập **HTTP DELETE**.")
    if "is2xxSuccessful" in chunk or "isOk()" in chunk:
        lines.append("- `.andExpect(status().isOk()/is2xxSuccessful())` = kỳ vọng **thành công** (Expected Excel).")
    if "is4xxClientError" in chunk:
        lines.append(
            "- `.andExpect(status().is4xxClientError())` = **negative test**: cố tình sai, hệ thống phải **từ chối** (vẫn có thể Passed)."
        )
    if "jsonPath" in chunk:
        lines.append("- `.andExpect(jsonPath(\"$.field\"))` = kiểm tra **field JSON** trong response.")
    if "jdbcTemplate" in chunk.lower() or "latestOtp" in chunk:
        lines.append("- Đọc OTP từ DB bằng SQL (`JdbcTemplate`) — giống bước “query auth_tokens” trên Excel.")
    if "andReturn()" in chunk:
        lines.append("- `.andReturn()` = lấy response để tự đọc Status/body (không chỉ andExpect).")
    if "assertTrue" in chunk:
        lines.append("- `Assertions.assertTrue(...)` = tự viết điều kiện thêm (vd Status < 500).")
    if "LEARNER" in chunk or "TEACHER" in chunk or "ADMIN" in chunk or "TM" in chunk or "CM" in chunk or "STAFF" in chunk or "MANAGER" in chunk:
        lines.append("- Dùng hằng tài khoản demo trong `ItSupport` (password `Password123!`).")

    if len(lines) == 2:
        lines.append("- Gọi API qua MockMvc rồi `andExpect` theo Expected của mã IT trên Excel.")
    lines.append("")
    return lines


def common_concepts() -> str:
    return r'''## Phần 0 – Khái niệm dùng lại trong MỌI file *IT

### Text block `"""` và `%s`

```java
String body = """
        {"email":"%s","password":"%s"}
        """.formatted(email, PASSWORD);
```

| Thành phần | Ý nghĩa |
|------------|---------|
| `""" ... """` | Chuỗi nhiều dòng (Java text block) — viết JSON cho dễ đọc, **không phải body rỗng** |
| `%s` | Chỗ trống chờ điền **chuỗi** |
| `.formatted(a, b)` | `%s` thứ 1 ← `a`, `%s` thứ 2 ← `b` |

### `UUID.randomUUID()`

Tạo mã ngẫu nhiên dài. Thường ghép vào email (`it.reg.` + UUID + `@englishlab-it.test`) để **mỗi lần chạy test không trùng** user.

### `mockMvc.perform(...)`

Giả lập HTTP vào Spring (như Postman **Send**), nhưng chạy trong JUnit → đi **Controller → Service → Repository** (Integration Test đúng chuẩn).

| Code | Giống Postman |
|------|----------------|
| `perform(...)` | Bấm Send |
| `post/get/put/...("/api/...")` | Method + URL |
| `.contentType(APPLICATION_JSON)` | Header Content-Type |
| `.content(body)` | Body JSON |
| `.header("Authorization", bearer(token))` | Bearer Token |
| `.andExpect(status()...)` | Kiểm tra Status |

### `@EnglishLabIT`

Gồm: `@SpringBootTest` (load app) + `@AutoConfigureMockMvc` + timezone initializer.

### `@DisplayName("IT_xxx ...")`

Tên test = **Test Case ID** trên Excel (map Round Passed/Failed/N/A).

### `import static ItSupport.*`

Dùng ngắn `PASSWORD`, `LEARNER`, `login(...)`, `bearer(...)` mà không viết `ItSupport.` mỗi lần.

---
'''


def explain_itsupport(src: str) -> str:
    return f'''## File: `ItSupport.java`

{FILE_INTRO["ItSupport.java"]}

### Vai trò từng phần

| Code | Giải thích |
|------|------------|
| `static {{ ... TimeZone ...}}` | Set timezone `Asia/Ho_Chi_Minh` sớm — IDE không dùng Maven surefire vẫn chạy được |
| `PASSWORD`, `LEARNER`, `TEACHER`… | Hằng email/mật khẩu demo |
| `private ItSupport()` | Không cho `new ItSupport()` — chỉ dùng static |
| `login(mockMvc, email, password)` | POST `/api/auth/login` → đọc JSON → trả `accessToken` |
| `bearer(token)` | Trả về `"Bearer " + token` để gắn header |
| `mapper()` | Cho class khác dùng chung `ObjectMapper` |

### Hàm `login` chi tiết

1. Ghép JSON login bằng `%s` + `.formatted(email, password)`.
2. `mockMvc.perform(post("/api/auth/login")...)` gửi request.
3. `.andExpect(status().isOk())` — login phải 200.
4. `.andReturn()` lấy body → `MAPPER.readTree` → `accessToken`.

**Không Run Test file này** (không có `@Test`).

```java
{src.strip()}
```

---
'''


def explain_meta(src: str, name: str) -> str:
    return f'''## File: `{name}`

{FILE_INTRO.get(name, "")}

```java
{src.strip()}
```

---
'''


def explain_it_class(path: Path) -> str:
    src = path.read_text(encoding="utf-8")
    name = path.name
    intro = FILE_INTRO.get(name, "Class Integration Test MockMvc.")
    lines = [
        f"## File: `{name}`",
        "",
        intro,
        "",
        "### Khung class (giống hầu hết file *IT)",
        "",
        "| Thành phần | Ý nghĩa |",
        "|------------|---------|",
        "| `@EnglishLabIT` | Bật SpringBootTest + MockMvc + timezone |",
        "| `@Autowired MockMvc mockMvc` | Spring tiêm MockMvc để gọi API |",
        "| Mỗi `@Test` + `@DisplayName` | 1 method = 1 Test Case ID Excel |",
        "",
        "### Từng test method",
        "",
    ]
    methods = extract_methods(src)
    if not methods:
        lines.append("_Không có `@DisplayName` (file helper/meta)._")
        lines.append("")
    for display, mname, _ in methods:
        lines.extend(explain_method(display, mname, src))

    lines.append("### Source đầy đủ")
    lines.append("")
    lines.append("```java")
    lines.append(src.rstrip())
    lines.append("```")
    lines.append("")
    lines.append("---")
    lines.append("")
    return "\n".join(lines)


def build() -> Path:
    parts = [
        "# Giải thích chi tiết code Integration Test (toàn bộ file trong `it/`)\n",
        "\n",
        "Thư mục: `backend/src/test/java/fu/sap490/g23/backend/it/`\n",
        "\n",
        "Đọc theo thứ tự: **ItSupport → EnglishLabIT → AuthIT** (mẫu) → các `*IT` còn lại (cùng pattern).\n",
        "\n",
        "Chạy ví dụ:\n",
        "```powershell\n",
        "cd D:\\EngLishLab\\EnglishLab\\backend\n",
        ".\\mvnw.cmd \"-Dtest=AuthIT\" test\n",
        "```\n",
        "\n",
        "---\n",
        "\n",
        common_concepts(),
    ]

    # toc
    parts.append("## Mục lục file\n\n")
    files = []
    for n in ORDER:
        p = JAVA_DIR / n
        if p.exists():
            files.append(p)
    for p in sorted(JAVA_DIR.glob("*.java")):
        if p not in files:
            files.append(p)
    for i, p in enumerate(files, 1):
        parts.append(f"{i}. [`{p.name}`](#{p.name.lower().replace('.', '')})\n")
    # github md anchors are messy; just list names
    parts.append("\n---\n\n")

    for p in files:
        if p.name == "ItSupport.java":
            parts.append(explain_itsupport(p.read_text(encoding="utf-8")))
        elif p.name in ("EnglishLabIT.java", "ItTimezoneInitializer.java"):
            parts.append(explain_meta(p.read_text(encoding="utf-8"), p.name))
        else:
            parts.append(explain_it_class(p))

    parts.append(
        """## Kết luận nhanh để nói với cô

- Mỗi class `*IT` dùng `@EnglishLabIT` (= Spring Boot Test + MockMvc) để xác minh **Controller–Service–Repository**.
- `mockMvc.perform` = gọi API trong test; `%s` / `formatted` = ghép JSON động; `UUID` = tránh trùng data.
- `@DisplayName("IT_…")` map đúng Excel; `ItSupport` chỉ hỗ trợ login/token.
- Postman không thay các class này — chỉ là tool phụ.
"""
    )

    text = "".join(parts)
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    out = OUT_DIR / OUT_NAME
    out.write_text(text, encoding="utf-8")
    PROJ.mkdir(parents=True, exist_ok=True)
    shutil.copy2(out, PROJ / OUT_NAME)
    # also keep/update the AUTH_01 short file reference
    print(out, "chars", len(text), "files", len(files))
    return out


if __name__ == "__main__":
    build()
