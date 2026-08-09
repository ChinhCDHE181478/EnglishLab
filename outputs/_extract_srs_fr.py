# -*- coding: utf-8 -*-
from pathlib import Path
import re

src = Path(r'D:\EngLishLab\EnglishLab\srs_extracted.txt')
text = src.read_text(encoding='utf-8', errors='ignore')
lines = text.splitlines()

out = Path(r'D:\EngLishLab\EnglishLab\outputs\srs_fr_usecase_extract.txt')
chunks = []

# Capture TOC-ish and FR/UC numbered items
pat = re.compile(r'^(UC-\d+[a-d]?|FR-?[A-Z0-9.-]+|3\.\d+|2\.\d+|Use Case|Functional Requirement)', re.I)
for i, line in enumerate(lines):
    s = line.strip()
    if not s:
        continue
    if pat.search(s) or s.startswith('UC-') or 'FR-' in s[:20]:
        chunks.append(f'{i}: {s}')

# Also dump section 3 Functional Requirements window
start = None
for i, line in enumerate(lines):
    if '3. Functional Requirements' in line or line.strip() == '3. Functional Requirements':
        start = i
        break
fr_block = []
if start is not None:
    fr_block = lines[start:start + 2500]

out.write_text('\n'.join(chunks[:800]) + '\n\n===== FR SECTION =====\n' + '\n'.join(fr_block), encoding='utf-8')
print('markers', len(chunks), 'fr_lines', len(fr_block), 'written', out)
