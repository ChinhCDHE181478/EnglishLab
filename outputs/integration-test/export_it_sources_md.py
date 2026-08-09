# -*- coding: utf-8 -*-
"""Export full original IT Java sources into one Markdown file."""
from pathlib import Path

IT_DIR = Path(r"D:\EngLishLab\EnglishLab\backend\src\test\java\fu\sap490\g23\backend\it")
OUT_MD = Path(r"C:\Users\phong\Downloads\intergration test\CODE_NGUYEN_BAN_tat_ca_file_IT.md")

# Order: foundation first, then all *IT alphabetically
FOUNDATION = [
    "ItSupport.java",
    "EnglishLabIT.java",
    "ItTimezoneInitializer.java",
]

def main() -> None:
    foundation = [IT_DIR / n for n in FOUNDATION if (IT_DIR / n).exists()]
    its = sorted(p for p in IT_DIR.glob("*.java") if p.name not in FOUNDATION)
    files = foundation + its

    parts: list[str] = []
    parts.append("# Code nguyên bản — toàn bộ file Integration Test (`it/`)")
    parts.append("")
    parts.append("Nguồn: `backend/src/test/java/fu/sap490/g23/backend/it/`")
    parts.append("")
    parts.append("File này **chép nguyên** nội dung các file `.java` trong project (không rút gọn).")
    parts.append("")
    parts.append(f"**Tổng số file:** {len(files)}")
    parts.append("")
    parts.append("## Mục lục")
    parts.append("")
    for i, p in enumerate(files, 1):
        anchor = p.name.lower().replace(".", "")
        parts.append(f"{i}. [`{p.name}`](#{anchor})")
    parts.append("")
    parts.append("---")
    parts.append("")

    for p in files:
        code = p.read_text(encoding="utf-8")
        # normalize newlines
        if not code.endswith("\n"):
            code += "\n"
        parts.append(f"## `{p.name}`")
        parts.append("")
        parts.append(f"Đường dẫn: `backend/src/test/java/fu/sap490/g23/backend/it/{p.name}`")
        parts.append("")
        parts.append(f"Số dòng: {len(code.splitlines())}")
        parts.append("")
        parts.append("```java")
        parts.append(code.rstrip("\n"))
        parts.append("```")
        parts.append("")
        parts.append("---")
        parts.append("")

    OUT_MD.write_text("\n".join(parts), encoding="utf-8")
    print(f"Wrote {OUT_MD}")
    print(f"files={len(files)} bytes={OUT_MD.stat().st_size}")


if __name__ == "__main__":
    main()
