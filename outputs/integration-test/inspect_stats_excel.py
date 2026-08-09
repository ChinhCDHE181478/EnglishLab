# -*- coding: utf-8 -*-
"""Inspect Excel formula errors on Test Statistics via Excel COM if available."""
from pathlib import Path

path = Path(r"C:\Users\phong\Downloads\intergration test\SEP490_G23_Report5.2_Integration Test_COMPLETED_v4_FORMATTED.xlsx")

try:
    import win32com.client  # type: ignore
except ImportError:
    print("NO_WIN32COM")
    raise SystemExit(0)

excel = win32com.client.DispatchEx("Excel.Application")
excel.Visible = False
excel.DisplayAlerts = False
wb = excel.Workbooks.Open(str(path))
st = wb.Worksheets("Test Statistics")
print("Evaluating Test Statistics rows 11-16 and 36-39...")
for r in range(11, 17):
    row = []
    for c in range(2, 9):
        cell = st.Cells(r, c)
        row.append(f"{cell.Text}")
    print(f"R{r}", row)

for r in (36, 38, 39):
    row = []
    for c in range(2, 9):
        cell = st.Cells(r, c)
        row.append(f"{cell.Text}")
    print(f"R{r}", row)

# Course sheet counts
course = wb.Worksheets("IT - Course")
print("Course B2", course.Range("B2").Text)
print("Course B4", course.Range("B4").Text)
print("Course B6:E6", [course.Range(x).Text for x in ("B6", "C6", "D6", "E6")])

wb.Close(False)
excel.Quit()
print("DONE")
