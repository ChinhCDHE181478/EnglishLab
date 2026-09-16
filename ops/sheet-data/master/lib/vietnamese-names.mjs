/** Deterministic Vietnamese full-name pool (Họ + tên đệm + tên). No numbers in fullName. */

const HO = [
  "Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Huỳnh", "Phan", "Vũ", "Võ", "Đặng",
  "Bùi", "Đỗ", "Hồ", "Ngô", "Dương", "Đinh", "Lý", "Trịnh", "Mai", "Cao",
];

const DEM_NAM = [
  "Minh", "Hoàng", "Quốc", "Đức", "Quang", "Gia", "Thành", "Anh", "Văn", "Hữu",
  "Công", "Tuấn", "Nhật", "Bảo", "Khắc", "Xuân", "Thanh", "Hải", "Đình", "Phúc",
];

const TEN_NAM = [
  "Anh", "Bảo", "Dũng", "Huy", "Khang", "Long", "Nam", "Phát", "Quân", "Sơn",
  "Tùng", "Việt", "Đạt", "Khôi", "Lâm", "Phong", "Thịnh", "Trí", "Tuấn", "Hào",
];

const DEM_NU = [
  "Thị", "Ngọc", "Thu", "Minh", "Khánh", "Phương", "Quỳnh", "Thảo", "Mai", "Lan",
  "Hồng", "Kim", "Bích", "Thanh", "Diễm", "Tuệ", "Ánh", "Hạ", "Yến", "Gia",
];

const TEN_NU = [
  "Anh", "Chi", "Hà", "Hân", "Linh", "Mai", "My", "Nhi", "Trang", "Vy",
  "Uyên", "Yến", "Thư", "Ngân", "Oanh", "Phương", "Quỳnh", "Tâm", "Vân", "Dung",
];

const BANNED_NAME_PATTERNS = [
  /quản lý/i,
  /nhân viên/i,
  /content manager/i,
  /classroom admin/i,
  /staff englishlab/i,
  /giáo viên\s*\d/i,
  /học viên\s*\d/i,
  /user demo/i,
  /test student/i,
  /admin user/i,
  /teacher demo/i,
  /\d{2,}/,
  /lorem ipsum/i,
];

export function isInvalidDemoFullName(name) {
  const value = String(name || "").trim();
  if (!value || value.split(/\s+/).length < 2) return true;
  return BANNED_NAME_PATTERNS.some((pattern) => pattern.test(value));
}

export function buildVietnameseNamePool(rng, count) {
  const used = new Set();
  const names = [];
  let guard = 0;
  while (names.length < count && guard < count * 40) {
    guard += 1;
    const female = rng.bool(0.52);
    const ho = rng.pick(HO);
    const dem = rng.pick(female ? DEM_NU : DEM_NAM);
    const ten = rng.pick(female ? TEN_NU : TEN_NAM);
    const full = `${ho} ${dem} ${ten}`;
    if (used.has(full) || isInvalidDemoFullName(full)) continue;
    used.add(full);
    names.push({ fullName: full, gender: female ? "FEMALE" : "MALE" });
  }
  if (names.length < count) {
    throw new Error(`Không tạo đủ ${count} tên Việt Nam duy nhất (got ${names.length}).`);
  }
  return names;
}

export function validateVietnameseNames(accounts) {
  const issues = [];
  for (const account of accounts) {
    if (account.preserved) continue;
    if (isInvalidDemoFullName(account.fullName)) {
      issues.push({ email: account.email, fullName: account.fullName, reason: "invalid_vietnamese_fullname" });
    }
  }
  return issues;
}
