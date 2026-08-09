# -*- coding: utf-8 -*-
from pathlib import Path
import sys

gen = Path(r"D:\EngLishLab\EnglishLab\outputs\integration-test\generate_integration_test_pack.py")
text = gen.read_text(encoding="utf-8")

if "sys.path.insert" not in text:
    text = text.replace(
        "from pathlib import Path\n",
        "from pathlib import Path\nimport sys\n"
        "sys.path.insert(0, str(Path(__file__).resolve().parent))\n",
    )

start = text.index("# ---------------------------------------------------------------------------\n# Modules")
end = text.index("\ndef iter_cases")
new_block = (
    "# ---------------------------------------------------------------------------\n"
    "# Modules imported from full-project coverage definition\n"
    "# ---------------------------------------------------------------------------\n"
    "from full_modules import MODULES  # noqa: E402\n\n"
)
text = text[:start] + new_block + text[end:]
text = text.replace('VERSION = "v1.0"', 'VERSION = "v2.0"')
text = text.replace(
    "Initial filled Integration Test report from source + SRS survey",
    "v2.0 full-project module expansion (all major domains)",
)

gen.write_text(text, encoding="utf-8")
sys.path.insert(0, str(gen.parent))
from full_modules import MODULES, count_cases

print("modules", len(MODULES), "cases", count_cases())
