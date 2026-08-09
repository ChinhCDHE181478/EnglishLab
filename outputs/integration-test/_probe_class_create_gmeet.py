# -*- coding: utf-8 -*-
from datetime import date, timedelta
import requests

BASE = "http://localhost:8080"
PWD = "Password123!"


def login(email):
    r = requests.post(BASE + "/api/auth/login", json={"email": email, "password": PWD}, timeout=20)
    r.raise_for_status()
    return {"Authorization": "Bearer " + r.json()["accessToken"]}


def dump(label, r):
    print(label, r.status_code, r.text[:220].encode("ascii", "replace").decode())


staff = login("staff@englishlab.vn")
teacher = login("classroom.teacher1@englishlab.vn")

progs = requests.get(BASE + "/api/staff/classrooms/training-programs", headers=staff, timeout=20)
dump("programs", progs)
pid = None
if progs.status_code == 200:
    arr = progs.json()
    if isinstance(arr, list) and arr:
        pid = arr[0].get("id")
    elif isinstance(arr, dict):
        c = arr.get("content") or []
        if c:
            pid = c[0].get("id")
print("programId", pid)

start = date.today() + timedelta(days=14)
end = start + timedelta(days=28)
body = {
    "title": f"IT Proposal {int(start.toordinal())}",
    "courseOfferingId": pid,
    "capacity": 20,
    "plannedStartDate": start.isoformat(),
    "plannedEndDate": end.isoformat(),
    "weekdays": ["MONDAY", "WEDNESDAY"],
    "sessionStartTime": "18:00:00",
    "sessionEndTime": "20:00:00",
    "primaryTeacherId": 28,
    "note": "IT create classroom via proposal",
}
rp = requests.post(BASE + "/api/staff/classroom-proposals", headers=staff, json=body, timeout=20)
dump("proposal create", rp)

# find VIRTUAL classroom for teacher
ra = requests.get(BASE + "/api/teacher/classrooms/assigned", headers=teacher, timeout=20)
items = ra.json() if ra.status_code == 200 else []
for it in items:
    mode = it.get("deliveryMode")
    cid = it.get("id")
    print("assigned", cid, mode, (it.get("title") or "")[:40].encode("ascii", "replace").decode())
    if mode not in ("VIRTUAL", "ONLINE", "HYBRID"):
        continue
    rs = requests.get(BASE + f"/api/teacher/classrooms/{cid}/sessions", headers=teacher, timeout=20)
    if rs.status_code != 200:
        continue
    sess = rs.json() if isinstance(rs.json(), list) else []
    for s in sess[:3]:
        sid = s.get("id")
        ro = requests.post(BASE + f"/api/teacher/classrooms/sessions/{sid}/open", headers=teacher, timeout=20)
        dump(f"open virtual sess {sid} class {cid}", ro)

# UPDATE OFFLINE class 14
d = requests.get(BASE + "/api/staff/classrooms/14", headers=staff, timeout=20).json()
body_u = {
    "title": d.get("title") or "IT update",
    "deliveryMode": d.get("deliveryMode"),
    "maxCapacity": d.get("maxCapacity") or 20,
    "price": float(d.get("price") or 0),
    "shortDescription": (d.get("shortDescription") or "IT")[:100],
}
ru = requests.put(BASE + "/api/staff/classrooms/14", headers=staff, json=body_u, timeout=20)
dump("update 14", ru)
