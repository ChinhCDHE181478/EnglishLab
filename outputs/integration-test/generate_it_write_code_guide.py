# -*- coding: utf-8 -*-
"""Generate consolidated IT coding guide markdown for all *IT.java files."""
from __future__ import annotations

import re
from collections import Counter
from pathlib import Path

IT_DIR = Path(r"D:\EngLishLab\EnglishLab\backend\src\test\java\fu\sap490\g23\backend\it")
OUT = Path(r"C:\Users\phong\Downloads\intergration test\Tong_hop_VIET_CODE_tat_ca_file_IT.md")

SKIP = {"EnglishLabIT.java", "ItSupport.java", "ItTimezoneInitializer.java"}

TEST_RE = re.compile(
    r'@DisplayName\("([^"]+)"\)\s*(?:\n\s*@[^\n]+)*\s*\n\s*void\s+(\w+)\s*\([^)]*\)\s*throws[^{]*\{',
    re.MULTILINE,
)
# fallback simpler
TEST_RE2 = re.compile(r'@DisplayName\("([^"]+)"\)\s*\n\s*void\s+(\w+)\s*\(')

EXPECT_RE = re.compile(r"andExpect\(status\(\)\.(\w+)\(\)\)")
HTTP_RE = re.compile(
    r'\b(get|post|put|patch|delete)\(\s*"([^"]+)"'
)


def extract_tests(text: str) -> list[tuple[str, str]]:
    found = TEST_RE.findall(text)
    if found:
        return found
    return TEST_RE2.findall(text)


def extract_endpoints(method_body: str) -> list[tuple[str, str]]:
    return [(m.upper(), path) for m, path in HTTP_RE.findall(method_body)]


def split_methods(text: str) -> dict[str, str]:
    """Map methodName -> body approximate by scanning void method blocks."""
    out = {}
    for m in re.finditer(r"void\s+(\w+)\s*\([^)]*\)[^{]*\{", text):
        name = m.group(1)
        start = m.end() - 1
        depth = 0
        i = start
        while i < len(text):
            ch = text[i]
            if ch == "{":
                depth += 1
            elif ch == "}":
                depth -= 1
                if depth == 0:
                    out[name] = text[start : i + 1]
                    break
            i += 1
    return out


def role_hint(text: str) -> str:
    roles = []
    for const, label in [
        ("ADMIN", "Admin"),
        ("STAFF", "Staff"),
        ("MANAGER", "Manager"),
        ("TEACHER", "Teacher"),
        ("LEARNER", "Learner"),
        ("CM", "Content Manager"),
        ("TM", "Training Manager"),
    ]:
        if re.search(rf"\blogin\s*\(\s*mockMvc\s*,\s*{const}\b", text) or re.search(
            rf"login\(mockMvc,\s*{const}\b", text
        ):
            roles.append(label)
    return ", ".join(roles) if roles else "(public / không login hoặc tự register)"


def status_label(expect_name: str) -> str:
    mapping = {
        "isOk": "200 OK",
        "isCreated": "201 Created",
        "isNoContent": "204 No Content",
        "isBadRequest": "400 Bad Request",
        "isUnauthorized": "401 Unauthorized",
        "isForbidden": "403 Forbidden",
        "isNotFound": "404 Not Found",
        "isConflict": "409 Conflict",
        "is2xxSuccessful": "2xx (thành công)",
        "is3xxRedirection": "3xx",
        "is4xxClientError": "4xx (lỗi client)",
        "is5xxServerError": "5xx",
    }
    return mapping.get(expect_name, expect_name)


def main() -> None:
    support_files = [
        IT_DIR / "ItSupport.java",
        IT_DIR / "EnglishLabIT.java",
        IT_DIR / "ItTimezoneInitializer.java",
    ]
    it_files = sorted(p for p in IT_DIR.glob("*IT.java") if p.name not in SKIP)

    lines: list[str] = []
    a = lines.append

    a("# Tổng hợp CÁCH VIẾT CODE Integration Test — toàn bộ file `*IT`")
    a("")
    a("Thư mục: `backend/src/test/java/fu/sap490/g23/backend/it/`")
    a("")
    a("File này giúp bạn **học thuộc công thức viết** và biết từng file IT đang test gì, expected ở đâu. Không thay thế việc đọc file `.java` thật.")
    a("")
    a("---")
    a("")
    a("## 1. Ba file nền (không có `@Test`)")
    a("")
    a("| File | Vai trò |")
    a("|---|---|")
    a("| `ItSupport.java` | Tài khoản demo + `login()` + `bearer()` — dùng chung mọi IT |")
    a("| `EnglishLabIT.java` | Annotation gộp `@SpringBootTest` + `@AutoConfigureMockMvc` + timezone |")
    a("| `ItTimezoneInitializer.java` | Ép `Asia/Ho_Chi_Minh` khi chạy bằng IDE |")
    a("")
    a("### Tài khoản dùng trong test (`ItSupport`)")
    a("")
    a("| Hằng số | Email | Mật khẩu |")
    a("|---|---|---|")
    a("| `LEARNER` | `0386852628z@gmail.com` | `Password123!` |")
    a("| `TEACHER` | `classroom.teacher1@englishlab.vn` | `Password123!` |")
    a("| `STAFF` | `staff@englishlab.vn` | `Password123!` |")
    a("| `MANAGER` | `classroom.manager@englishlab.vn` | `Password123!` |")
    a("| `TM` | `training.manager@englishlab.vn` | `Password123!` |")
    a("| `CM` | `content.manager@englishlab.vn` | `Password123!` |")
    a("| `ADMIN` | `classroom.admin@englishlab.vn` | `Password123!` |")
    a("")
    a("---")
    a("")
    a("## 2. Công thức viết 1 test case (học thuộc)")
    a("")
    a("```java")
    a("@Test")
    a('@DisplayName(\"IT_XXX_01 mô tả ngắn\")  // = Test Case ID trên Excel')
    a("void itXxx01() throws Exception {")
    a("    // 1) Login (nếu API cần quyền)")
    a("    String token = login(mockMvc, TEACHER, PASSWORD);")
    a("")
    a("    // 2) (tuỳ case) Chuẩn bị body JSON")
    a('    String body = \"\"\"')
    a('            {\"field\":\"value\"}')
    a('            \"\"\";')
    a("")
    a("    // 3) Gọi API như Postman")
    a('    mockMvc.perform(get(\"/api/...\")')
    a('                    .header(\"Authorization\", bearer(token))')
    a("                    // .contentType(MediaType.APPLICATION_JSON)")
    a("                    // .content(body)")
    a("            )")
    a("            // 4) EXPECTED RESULT nằm ở đây")
    a("            .andExpect(status().isOk());")
    a("            // .andExpect(jsonPath(\"$.email\").value(...));")
    a("}")
    a("```")
    a("")
    a("### Expected Result ở đâu?")
    a("")
    a("| Trong code | Ý nghĩa |")
    a("|---|---|")
    a("| `.andExpect(status().isOk())` | Mong đợi **200** |")
    a("| `.andExpect(status().isCreated())` | Mong đợi **201** |")
    a("| `.andExpect(status().is2xxSuccessful())` | Mong đợi **2xx** |")
    a("| `.andExpect(status().is4xxClientError())` | Mong đợi **4xx** (sai input / trùng / không đủ quyền…) |")
    a("| `.andExpect(jsonPath(\"$.x\").value(y))` | Kiểm tra thêm nội dung JSON |")
    a("")
    a("> Panel xanh \"Tests passed\" trong IDE **không** chứa expected — expected là dòng `.andExpect(...)` trong code.")
    a("")
    a("### Khung lớp IT")
    a("")
    a("```java")
    a("@EnglishLabIT")
    a("public class XxxIT {")
    a("    @Autowired")
    a("    private MockMvc mockMvc;")
    a("    // các @Test ...")
    a("}")
    a("```")
    a("")
    a("### Chạy bằng IDE / Maven")
    a("")
    a("- IDE: mở file `*IT.java` → click mũi tên xanh cạnh class hoặc cạnh method.")
    a("- Maven:")
    a("")
    a("```powershell")
    a("cd D:\\EngLishLab\\EnglishLab\\backend")
    a(".\\mvnw.cmd \"-Dtest=AuthIT\" test")
    a(".\\mvnw.cmd \"-Dtest=AdminIT#itAdmin01\" test")
    a("```")
    a("")
    a("---")
    a("")
    a("## 3. Bản đồ toàn bộ file IT")
    a("")

    rows = []
    total_tests = 0
    for p in it_files:
        text = p.read_text(encoding="utf-8")
        tests = extract_tests(text)
        total_tests += len(tests)
        expects = Counter(EXPECT_RE.findall(text))
        roles = role_hint(text)
        rows.append((p, tests, expects, roles, len(text.splitlines())))

    a(f"**Tổng:** {len(it_files)} file `*IT` · **{total_tests}** test method (`@DisplayName`).")
    a("")
    a("| # | File | Số TC | Vai trò hay dùng | Dòng |")
    a("|---|---|---:|---|---:|")
    for i, (p, tests, expects, roles, nlines) in enumerate(rows, 1):
        a(f"| {i} | `{p.name}` | {len(tests)} | {roles} | {nlines} |")
    a("")
    a("---")
    a("")
    a("## 4. Chi tiết từng file — viết gì / expect gì")
    a("")

    for p, tests, expects, roles, nlines in rows:
        text = p.read_text(encoding="utf-8")
        bodies = split_methods(text)
        a(f"### `{p.name}`")
        a("")
        a(f"- **Số test:** {len(tests)}")
        a(f"- **Role thường login:** {roles}")
        if expects:
            exp_s = ", ".join(f"`{k}`×{v}" for k, v in expects.most_common())
            a(f"- **Expected hay dùng:** {exp_s}")
        a("")
        a("| Test Case ID (`@DisplayName`) | Method | API chính | Expected status |")
        a("|---|---|---|---|")
        for display, method in tests:
            body = bodies.get(method, "")
            eps = extract_endpoints(body)
            # skip login endpoint noise if many; keep unique paths except auth login when not AuthIT
            shown = []
            for verb, path in eps:
                if path == "/api/auth/login" and p.name not in {"AuthIT.java", "AuthOtpIT.java"}:
                    continue
                shown.append(f"`{verb} {path}`")
            api_cell = "<br>".join(shown[:4]) if shown else "_(xem trong method)_"
            if len(shown) > 4:
                api_cell += f"<br>… +{len(shown)-4}"
            exps = EXPECT_RE.findall(body)
            if exps:
                exp_cell = ", ".join(f"`{e}` → {status_label(e)}" for e in dict.fromkeys(exps))
            else:
                # maybe assertTrue on status
                if "assertTrue" in body and "getStatus()" in body:
                    exp_cell = "assert status (xem code)"
                else:
                    exp_cell = "_(xem `.andExpect` trong method)_"
            a(f"| `{display}` | `{method}` | {api_cell} | {exp_cell} |")
        a("")
        a(f"**Cách viết lại file này:** tạo class `@EnglishLabIT` → `@Autowired MockMvc` → copy pattern login + `perform` + `andExpect` cho từng ID ở bảng trên.")
        a("")
        a("---")
        a("")

    a("## 5. Checklist khi cô hỏi \"em viết IT thế nào?\"")
    a("")
    a("1. Gắn `@EnglishLabIT` cho class.")
    a("2. Inject `MockMvc`.")
    a("3. `@DisplayName` = đúng Test Case ID Excel.")
    a("4. `login(mockMvc, ROLE, PASSWORD)` rồi `bearer(token)` nếu API có bảo vệ.")
    a("5. `mockMvc.perform(get/post/... )` với URL thật của Controller.")
    a("6. Expected = `.andExpect(status()...)` (+ `jsonPath` nếu cần).")
    a("7. Chạy bằng IDE mũi tên xanh hoặc `mvnw -Dtest=TenClassIT test`.")
    a("")
    a("### Câu nói ngắn")
    a("")
    a("> Dạ em viết Integration Test bằng MockMvc: mỗi method là một Test Case ID, login lấy JWT, gọi đúng endpoint, rồi assert bằng `andExpect`. Expected Result chính là các dòng `andExpect` trong code; panel IDE chỉ báo pass/fail.")
    a("")

    OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Wrote {OUT}")
    print(f"files={len(it_files)} tests={total_tests} lines={len(lines)}")


if __name__ == "__main__":
    main()
