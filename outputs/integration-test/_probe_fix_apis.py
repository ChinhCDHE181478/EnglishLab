# -*- coding: utf-8 -*-
"""Probe APIs needed after CLASS/GMEET/BROADCAST runner fixes."""
import json
import time
import requests

BASE = "http://localhost:8080"
PWD = "Password123!"


def login(email):
    r = requests.post(BASE + "/api/auth/login", json={"email": email, "password": PWD}, timeout=20)
    r.raise_for_status()
    return {"Authorization": "Bearer " + r.json()["accessToken"]}


def dump(label, r):
    t = r.text[:180].encode("ascii", "replace").decode()
    print(f"{label}: {r.status_code} {t}")


staff = login("staff@englishlab.vn")
teacher = login("classroom.teacher1@englishlab.vn")
learner = login("0386852628z@gmail.com")
admin = login("classroom.admin@englishlab.vn")

# CLASS list/detail/update
r = requests.get(BASE + "/api/staff/classrooms", headers=staff, timeout=20)
dump("CLASS list", r)
arr = r.json() if r.status_code == 200 else []
oid = None
for o in arr:
    d = requests.get(BASE + f"/api/staff/classrooms/{o['id']}", headers=staff, timeout=20).json()
    if d.get("deliveryMode") == "OFFLINE" and not d.get("trainingProgramId"):
        oid = o["id"]
        break
if not oid and arr:
    oid = arr[0]["id"]
print("picked oid", oid)

# teachers
rt = requests.get(BASE + "/api/staff/classrooms/teachers", headers=staff, timeout=20)
dump("teachers", rt)
teachers = rt.json() if rt.status_code == 200 else []
tid = None
if isinstance(teachers, list) and teachers:
    tid = teachers[0].get("id") or teachers[0].get("userId")
elif isinstance(teachers, dict):
    content = teachers.get("content") or []
    if content:
        tid = content[0].get("id") or content[0].get("userId")
print("teacherId", tid)

if oid and tid:
    ra = requests.post(
        BASE + f"/api/staff/classrooms/{oid}/teachers/{tid}/assign",
        headers=staff,
        timeout=20,
    )
    dump("assign", ra)

# proposal create - need courseOfferingId
# try list public offerings
for path in [
    "/api/classroom-offerings",
    "/api/online-courses",
    "/api/staff/classrooms",
]:
    rr = requests.get(BASE + path, headers=staff, timeout=20)
    print("path", path, rr.status_code)

# BROADCAST
body = {
    "title": f"IT Broadcast {int(time.time())}",
    "message": "Integration test broadcast",
    "sendInApp": True,
    "sendEmail": False,
}
rb = requests.post(BASE + "/api/admin/broadcasts", headers=admin, json=body, timeout=20)
dump("broadcast create", rb)

# GMEET teacher assigned + open
ra2 = requests.get(BASE + "/api/teacher/classrooms/assigned", headers=teacher, timeout=20)
dump("teacher assigned", ra2)
items = ra2.json() if ra2.status_code == 200 else []
if isinstance(items, dict):
    items = items.get("content") or items.get("items") or []
sid = None
cid = None
for it in items[:5]:
    cid = it.get("id") or it.get("classroomId") or it.get("offeringId")
    rs = requests.get(BASE + f"/api/teacher/classrooms/{cid}/sessions", headers=teacher, timeout=20)
    print("sessions", cid, rs.status_code, len(rs.json()) if rs.status_code == 200 else rs.text[:60])
    if rs.status_code == 200:
        sess = rs.json() if isinstance(rs.json(), list) else (rs.json().get("content") or [])
        if sess:
            sid = sess[0].get("id")
            break
print("session", sid, "class", cid)
if sid:
    ro = requests.post(BASE + f"/api/teacher/classrooms/sessions/{sid}/open", headers=teacher, timeout=20)
    dump("open", ro)

# learner join
rm = requests.get(BASE + "/api/student/classrooms/my-classrooms", headers=learner, timeout=20)
dump("my-classrooms", rm)
mitems = rm.json() if rm.status_code == 200 else []
if isinstance(mitems, dict):
    mitems = mitems.get("content") or []
if mitems:
    lid = mitems[0].get("id") or mitems[0].get("offeringId")
    rs2 = requests.get(BASE + f"/api/student/classrooms/{lid}/sessions", headers=learner, timeout=20)
    dump(f"learner sessions {lid}", rs2)
    if rs2.status_code == 200:
        sess = rs2.json() if isinstance(rs2.json(), list) else (rs2.json().get("content") or [])
        if sess:
            sj = sess[0].get("id")
            rj = requests.post(BASE + f"/api/student/classrooms/sessions/{sj}/join", headers=learner, timeout=20)
            dump("join session", rj)
        rj2 = requests.post(BASE + f"/api/student/classrooms/{lid}/join", headers=learner, timeout=20)
        dump("join class", rj2)

# forbidden join
rf = requests.post(BASE + "/api/student/classrooms/sessions/999999/join", headers=learner, timeout=20)
dump("join fake", rf)
