# -*- coding: utf-8 -*-
"""Kiểm tra các code reference trong tài liệu có khớp số dòng file thật không."""
import re
from pathlib import Path

BASE = Path(__file__).resolve().parents[2]
OUT = Path(r"C:\Users\phong\Downloads\intergration test\giai thich code\file md")
PAT = re.compile(r"^```(\d+):(\d+):(\S+)$")

for md in sorted(OUT.glob("*.md")):
    total = 0
    problems = []
    for line in md.read_text(encoding="utf-8").splitlines():
        m = PAT.match(line.strip())
        if not m:
            continue
        total += 1
        start, end, rel = int(m.group(1)), int(m.group(2)), m.group(3)
        target = BASE / rel
        if not target.exists():
            problems.append(f"thiếu file {rel}")
            continue
        n = len(target.read_text(encoding="utf-8").splitlines())
        if start < 1 or end > n or start > end:
            problems.append(f"{rel} {start}:{end} (file có {n} dòng)")
    print(f"{md.name}: {total} tham chiếu, {len(problems)} lỗi")
    for p in problems[:5]:
        print("   -", p)
