# -*- coding: utf-8 -*-
import requests

BASE = "http://localhost:8080"


def login(email):
    r = requests.post(
        f"{BASE}/api/auth/login",
        json={"email": email, "password": "Password123!"},
        timeout=20,
    )
    return {"Authorization": "Bearer " + r.json()["accessToken"]}


h = login("staff@englishlab.vn")
arr = requests.get(BASE + "/api/staff/classrooms", headers=h, timeout=20).json()
o = arr[0]
d = requests.get(BASE + f"/api/staff/classrooms/{o['id']}", headers=h, timeout=20).json()
print("detail deliveryMode", d.get("deliveryMode"), "title", (d.get("title") or "")[:40])
body = {
    "title": d.get("title") or "IT Class",
    "shortDescription": d.get("shortDescription"),
    "description": d.get("description"),
    "deliveryMode": d.get("deliveryMode") or "ONLINE",
    "maxCapacity": d.get("maxCapacity") or 20,
    "price": d.get("price") or 0,
}
for k in (
    "trainingProgramId",
    "curriculumProgramId",
    "entryLevel",
    "startDate",
    "endDate",
    "primaryTeacherId",
    "defaultRoomId",
):
    if d.get(k) is not None:
        body[k] = d.get(k)
rr = requests.put(BASE + f"/api/staff/classrooms/{o['id']}", headers=h, json=body, timeout=20)
print("PUT", rr.status_code, rr.text[:180].encode("ascii", "replace").decode())

# create via POST
print("POST", requests.post(BASE + "/api/staff/classrooms", headers=h, json=body, timeout=20).status_code)

ht = login("classroom.teacher1@englishlab.vn")
items = requests.get(BASE + "/api/teacher/classrooms/assigned", headers=ht, timeout=20).json()
oid = items[0]["id"]
sess = requests.get(BASE + f"/api/teacher/classrooms/{oid}/sessions", headers=ht, timeout=20).json()
print("sessions", len(sess) if isinstance(sess, list) else type(sess))
if sess:
    sid = sess[0]["id"]
    print("open", requests.post(BASE + f"/api/teacher/classrooms/sessions/{sid}/open", headers=ht, timeout=20).status_code)
    hl = login("0386852628z@gmail.com")
    print("join sess", requests.post(BASE + f"/api/student/classrooms/sessions/{sid}/join", headers=hl, timeout=20).status_code)
    print("join class", requests.post(BASE + f"/api/student/classrooms/{oid}/join", headers=hl, timeout=20).status_code)
    print("join fake", requests.post(BASE + "/api/student/classrooms/sessions/999999/join", headers=hl, timeout=20).status_code)
