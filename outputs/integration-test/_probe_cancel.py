# -*- coding: utf-8 -*-
import time
import requests

BASE = "http://localhost:8080"
tok = requests.post(
    BASE + "/api/auth/login",
    json={"email": "classroom.admin@englishlab.vn", "password": "Password123!"},
).json()["accessToken"]
H = {"Authorization": "Bearer " + tok}
b = {
    "title": "IT cancel " + str(int(time.time())),
    "message": "x",
    "sendInApp": True,
    "sendEmail": False,
}
r = requests.post(BASE + "/api/admin/broadcasts", headers=H, json=b)
print("create", r.status_code, r.json().get("id"), r.json().get("status"))
bid = r.json()["id"]
c = requests.post(BASE + f"/api/admin/broadcasts/{bid}/cancel", headers=H)
print("cancel", c.status_code, c.text.encode("ascii", "replace").decode())
# try schedule endpoint if any
for path in [
    f"/api/admin/broadcasts/{bid}/schedule",
    f"/api/admin/broadcasts/{bid}/send",
]:
    rr = requests.post(BASE + path, headers=H, json={}, timeout=20)
    print(path, rr.status_code, rr.text[:120].encode("ascii", "replace").decode())
