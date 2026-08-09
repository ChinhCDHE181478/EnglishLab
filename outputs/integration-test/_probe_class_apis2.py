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


H = login("staff@englishlab.vn")
arr = requests.get(BASE + "/api/staff/classrooms", headers=H, timeout=20).json()
progs = requests.get(BASE + "/api/staff/classrooms/training-programs", headers=H, timeout=20)
print("programs", progs.status_code, len(progs.json()) if progs.status_code == 200 else progs.text[:80])

# find offering that PUT accepts: copy full detail fields carefully
for o in arr:
    d = requests.get(BASE + f"/api/staff/classrooms/{o['id']}", headers=H, timeout=20).json()
    if d.get("deliveryMode") not in ("ONLINE", "OFFLINE", "HYBRID", "VIRTUAL"):
        continue
    body = {
        "title": (d.get("title") or "IT") + "",
        "deliveryMode": d.get("deliveryMode"),
        "maxCapacity": d.get("maxCapacity") or 20,
        "price": d.get("price") if d.get("price") is not None else 0,
        "trainingProgramId": d.get("trainingProgramId"),
        "curriculumProgramId": d.get("curriculumProgramId"),
        "shortDescription": d.get("shortDescription") or "IT update",
    }
    # unwrap nested
    if body["trainingProgramId"] is None and isinstance(d.get("trainingProgram"), dict):
        body["trainingProgramId"] = d["trainingProgram"].get("id")
    if body["curriculumProgramId"] is None and isinstance(d.get("curriculumProgram"), dict):
        body["curriculumProgramId"] = d["curriculumProgram"].get("id")
    rr = requests.put(BASE + f"/api/staff/classrooms/{o['id']}", headers=H, json=body, timeout=20)
    print(
        "try",
        o["id"],
        d.get("deliveryMode"),
        "tp",
        body["trainingProgramId"],
        "cp",
        body["curriculumProgramId"],
        "->",
        rr.status_code,
        rr.text[:100].encode("ascii", "replace").decode(),
    )
    if rr.status_code == 200:
        break
