/**
 * EnglishLab Master Demo Dataset generator.
 * Source of truth builder for workbook + import JSON.
 *
 * DEMO_DATA_SEED = 20260916
 * DEMO_REFERENCE_DATE = 2026-09-16
 * Window: 2026-06-01 → 2026-10-31
 *
 * Usage: node ops/sheet-data/master/generate.mjs
 */
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";
import { createRequire } from "module";
import { createRng } from "./lib/rng.mjs";
import { buildVietnameseNamePool, validateVietnameseNames } from "./lib/vietnamese-names.mjs";
import { buildTableMatrix } from "./lib/table-matrix.mjs";
import { assembleGapWorld } from "./lib/world-gap-fill.mjs";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, "../../..");
const require = createRequire(import.meta.url);
const XLSX = require(path.join(ROOT, "frontend/node_modules/@e965/xlsx"));

const DEMO_DATA_SEED = 20260916;
const REF = "2026-09-16";
const WINDOW_FROM = "2026-06-01";
const WINDOW_TO = "2026-10-31";
const PASSWORD_NOTE = "Password123! (MASTER generated accounts only — never for preserved accounts)";
const MARKER = "MASTER_OPS";
const EMAIL_DOMAIN = "englishlab.vn";
const MASTER_REF_PREFIX = "master-ops://";

const PRESERVED = {
  learner: { email: "0386852628z@gmail.com", role: "LEARNER", purpose: "Showcase learner — KEEP AS-IS" },
  teacher: { email: "alien1062004@gmail.com", role: "TEACHER", purpose: "Showcase teacher / Google Meet — KEEP AS-IS" },
  staff: [
    { email: "classroom.admin@englishlab.vn", role: "ADMIN", purpose: "Preserved admin — rename fullName only", fullNameHint: "Phạm Quốc Bảo" },
    { email: "classroom.manager@englishlab.vn", role: "MANAGER", purpose: "Preserved manager — rename fullName only", fullNameHint: "Hoàng Thu Hà" },
    { email: "content.manager@englishlab.vn", role: "CONTENT_MANAGER", purpose: "Preserved content manager — rename fullName only", fullNameHint: "Đặng Minh Châu" },
    { email: "staff@englishlab.vn", role: "STAFF", purpose: "Preserved staff — keep password", fullNameHint: "Nguyễn Thanh Tùng" },
  ],
};

const PROTECTED_COURSES = [
  { slug: "e2-ielts-practice-tests", title: "E2 IELTS Practice", mode: "PROTECTED" },
  { slug: "ielts-master-vocabulary-band-7-plus", title: "IELTS Master Vocabulary Band 7+", mode: "PROTECTED" },
];

const FORBIDDEN_UI_RE = /\b(demo|sample)\b|(?<![a-z])test(?![a-z])/i;

function assertNoForbiddenUi(label, value) {
  const text = String(value ?? "");
  if (!text) return;
  const lower = text.toLowerCase();
  if (lower.includes("mock test") || lower.includes("placement test") || lower.includes("module test")) {
    if (lower.includes("demo") || lower.includes("sample")) {
      throw new Error(`Forbidden UI token in ${label}: ${text}`);
    }
    return;
  }
  if (FORBIDDEN_UI_RE.test(text) || lower.includes("demo")) {
    throw new Error(`Forbidden UI token in ${label}: ${text}`);
  }
}

function d(iso) {
  return new Date(`${iso}T00:00:00.000Z`);
}

function iso(date) {
  return date.toISOString().slice(0, 10);
}

function addDays(date, days) {
  const next = new Date(date.getTime());
  next.setUTCDate(next.getUTCDate() + days);
  return next;
}

function monthBucket(isoDate) {
  return isoDate.slice(0, 7);
}

function phoneFor(index) {
  const body = String(900000000 + index).slice(0, 9);
  return `09${body}`;
}

function buildAccounts(rng) {
  const names = buildVietnameseNamePool(rng, 450);
  let ni = 0;
  const take = () => names[ni++];

  const accounts = [];
  const push = (partial) => {
    if (partial.fullName) assertNoForbiddenUi(`account.fullName:${partial.email}`, partial.fullName);
    if (partial.email) assertNoForbiddenUi(`account.email:${partial.email}`, partial.email);
    accounts.push({
      marker: partial.preserved ? null : MARKER,
      preserved: Boolean(partial.preserved),
      passwordNote: partial.preserved ? "Do not reset / do not overwrite password" : PASSWORD_NOTE,
      phone: partial.phone || null,
      dateOfBirth: partial.dateOfBirth || null,
      gender: partial.gender || null,
      ...partial,
    });
  };

  // Preserved ops staff (email+password kept; fullNameHint applied only if importer allows rename)
  for (const staff of PRESERVED.staff) {
    push({
      email: staff.email,
      role: staff.role,
      purpose: staff.purpose,
      fullName: staff.fullNameHint,
      preserved: true,
      renameFullNameOnly: true,
    });
  }

  // Preserved Gmail showcase accounts — reference only
  push({ ...PRESERVED.teacher, fullName: "(preserved — do not overwrite)", preserved: true });
  push({ ...PRESERVED.learner, fullName: "(preserved — do not overwrite)", preserved: true });

  // Optional buffer generated staff (exact ownership prefixes) — not replacing preserved emails
  for (let i = 1; i <= 2; i += 1) {
    const s = take();
    push({
      email: `nv.${String(i).padStart(2, "0")}@${EMAIL_DOMAIN}`,
      fullName: s.fullName,
      gender: s.gender,
      role: "STAFF",
      purpose: i === 1 ? "Tuyển sinh & xếp lớp" : "Học phí & payment proof",
      phone: phoneFor(400 + i),
      dateOfBirth: `199${i}-0${i + 2}-15`,
    });
  }

  for (let i = 1; i <= 20; i += 1) {
    const t = take();
    const specs = [
      "IELTS Writing",
      "IELTS Speaking",
      "IELTS Listening",
      "IELTS Reading",
      "TOEIC L&R",
      "General English",
      "Business English",
    ];
    push({
      email: `gv.${String(i).padStart(2, "0")}@${EMAIL_DOMAIN}`,
      fullName: t.fullName,
      gender: t.gender,
      role: "TEACHER",
      purpose: "Instructor-led teaching",
      phone: phoneFor(500 + i),
      dateOfBirth: `198${5 + (i % 5)}-${String((i % 12) + 1).padStart(2, "0")}-10`,
      teacherHeadline: `Giáo viên ${specs[(i - 1) % specs.length]}`,
      yearsOfExperience: 2 + (i % 10),
    });
  }

  // ~320 learners kept for current world scale; cohort signup dates so Sep ≈100 new.
  // (Jun–Aug historical ~73–74/mo; bump to 400×100/mo requires enrollment regen — deferred.)
  const LEARNER_COHORTS = [
    { month: "2026-06", count: 74 },
    { month: "2026-07", count: 73 },
    { month: "2026-08", count: 73 },
    { month: "2026-09", count: 100 },
  ];
  let learnerIndex = 0;
  for (const cohort of LEARNER_COHORTS) {
    for (let j = 1; j <= cohort.count; j += 1) {
      learnerIndex += 1;
      const l = take();
      const day = String(((j - 1) % 27) + 1).padStart(2, "0");
      const hour = String(8 + (j % 10)).padStart(2, "0");
      const minute = String((j * 3) % 60).padStart(2, "0");
      push({
        email: `hs.${String(learnerIndex).padStart(3, "0")}@${EMAIL_DOMAIN}`,
        fullName: l.fullName,
        gender: l.gender,
        role: "LEARNER",
        purpose: "Classroom + online learning journeys",
        phone: phoneFor(1000 + learnerIndex),
        dateOfBirth: `200${learnerIndex % 5}-${String((learnerIndex % 12) + 1).padStart(2, "0")}-${String((learnerIndex % 27) + 1).padStart(2, "0")}`,
        targetExam: learnerIndex % 4 === 0 ? "TOEIC" : "IELTS",
        targetScore: learnerIndex % 4 === 0 ? String(650 + (learnerIndex % 4) * 50) : `6.${learnerIndex % 5}`,
        // Business scale: cohort signup month (not import wall-clock)
        createdAt: `${cohort.month}-${day}T${hour}:${minute}:00`,
        signupCohort: cohort.month,
      });
    }
  }

  return accounts;
}

function buildCatalog() {
  const categories = [
    { code: "IELTS", name: "IELTS", description: "Khóa luyện thi IELTS Academic & General.", mode: "SYSTEM" },
    { code: "TOEIC", name: "TOEIC", description: "Khóa luyện thi TOEIC Listening & Reading.", mode: "SYSTEM" },
    { code: "COMMUNICATION", name: "Giao tiếp", description: "Tiếng Anh giao tiếp công việc và đời sống.", mode: "SYSTEM" },
    { code: "FOUNDATION", name: "Nền tảng", description: "Củng cố nền tảng ngữ pháp và từ vựng.", mode: "SYSTEM" },
    { code: "BUSINESS", name: "Business English", description: "Tiếng Anh thương mại và thuyết trình.", mode: "MASTER" },
  ];

  // Ownership: ilc-* slugs (not demo-*). Titles/UI have no DEMO/TEST/SAMPLE.
  const onlineExtras = [
    { slug: "ilc-ielts-foundation-45-55", title: "IELTS Foundation 4.5–5.5", category: "IELTS", price: 1490000, level: "FOUNDATION", status: "PUBLISHED", duration: "8 tuần", featured: true, thumbnailUrl: "/course-covers/ielts-practice.png" },
    { slug: "ilc-ielts-writing-intensive-60", title: "IELTS Writing Intensive 6.0+", category: "IELTS", price: 1890000, level: "INTERMEDIATE", status: "PUBLISHED", duration: "6 tuần", featured: true, thumbnailUrl: "/course-covers/ielts-writing.png" },
    { slug: "ilc-ielts-speaking-fluency", title: "IELTS Speaking Fluency & Pronunciation", category: "IELTS", price: 1690000, level: "INTERMEDIATE", status: "PUBLISHED", duration: "6 tuần", featured: false, thumbnailUrl: "/course-covers/ielts-speaking.png" },
    { slug: "ilc-ielts-listening-strategies-65", title: "IELTS Listening Strategies 6.5+", category: "IELTS", price: 1590000, level: "UPPER_INTERMEDIATE", status: "PUBLISHED", duration: "5 tuần", featured: false, thumbnailUrl: "/course-covers/ielts-listening.png" },
    { slug: "ilc-toeic-lr-650", title: "TOEIC Listening & Reading 650+", category: "TOEIC", price: 1290000, level: "INTERMEDIATE", status: "PUBLISHED", duration: "8 tuần", featured: true, thumbnailUrl: "/course-covers/toeic-lr.png" },
    { slug: "ilc-toeic-intensive-800", title: "TOEIC Intensive 800+", category: "TOEIC", price: 1790000, level: "ADVANCED", status: "PUBLISHED", duration: "8 tuần", featured: false, thumbnailUrl: "/course-covers/toeic-sw.png" },
    { slug: "ilc-english-communication-work", title: "English Communication for Work", category: "COMMUNICATION", price: 1190000, level: "INTERMEDIATE", status: "PUBLISHED", duration: "6 tuần", featured: true, thumbnailUrl: "/course-covers/communication.png" },
    { slug: "ilc-business-email-presentation", title: "Business Email & Presentation Skills", category: "BUSINESS", price: 1390000, level: "INTERMEDIATE", status: "PUBLISHED", duration: "5 tuần", featured: false, thumbnailUrl: "/course-covers/classroom-online.png" },
    { slug: "ilc-academic-english-foundations", title: "Academic English Foundations", category: "FOUNDATION", price: 990000, level: "FOUNDATION", status: "PUBLISHED", duration: "6 tuần", featured: false, thumbnailUrl: "/course-covers/grammar.png" },
    { slug: "ilc-ielts-reading-skimming", title: "IELTS Reading Skimming & Scanning", category: "IELTS", price: 990000, level: "INTERMEDIATE", status: "DRAFT", duration: "4 tuần", featured: false, thumbnailUrl: "/course-covers/ielts-reading.png" },
    { slug: "ilc-toeic-vocab-builder", title: "TOEIC Vocabulary Builder 500 Words", category: "TOEIC", price: 790000, level: "FOUNDATION", status: "DRAFT", duration: "4 tuần", featured: false, thumbnailUrl: "/course-covers/ielts-vocab.png" },
  ];

  const instructorLed = [
    { code: "ILC_IELTS_FOUNDATION", title: "IELTS Foundation Evening", category: "IELTS", tuition: 4690000, totalSessions: 36 },
    { code: "ILC_IELTS_65", title: "IELTS 6.5 Intensive", category: "IELTS", tuition: 5290000, totalSessions: 36 },
    { code: "ILC_IELTS_SPEAKING", title: "IELTS Speaking Studio", category: "IELTS", tuition: 3990000, totalSessions: 36 },
    { code: "ILC_TOEIC_650", title: "TOEIC 650 Evening", category: "TOEIC", tuition: 4290000, totalSessions: 36 },
    { code: "ILC_TOEIC_800", title: "TOEIC 800 Intensive", category: "TOEIC", tuition: 4890000, totalSessions: 36 },
    { code: "ILC_COMM_WORK", title: "English for Workplace", category: "COMMUNICATION", tuition: 3590000, totalSessions: 36 },
  ];

  for (const c of onlineExtras) {
    assertNoForbiddenUi(`online.slug:${c.slug}`, c.slug);
    assertNoForbiddenUi(`online.title:${c.slug}`, c.title);
  }
  for (const p of instructorLed) {
    assertNoForbiddenUi(`ilc.code:${p.code}`, p.code);
    assertNoForbiddenUi(`ilc.title:${p.code}`, p.title);
  }

  return { categories, onlineExtras, instructorLed, protectedCourses: PROTECTED_COURSES };
}

function buildRooms() {
  // 10 rooms: 8 physical + 2 online studios (center capacity)
  const physical = [
    { code: "ILC_R01", name: "Phòng 101", capacity: 20 },
    { code: "ILC_R02", name: "Phòng 102", capacity: 18 },
    { code: "ILC_R03", name: "Phòng 103", capacity: 16 },
    { code: "ILC_R04", name: "Phòng 201", capacity: 22 },
    { code: "ILC_R05", name: "Phòng 202", capacity: 18 },
    { code: "ILC_R06", name: "Phòng 203", capacity: 20 },
    { code: "ILC_R07", name: "Phòng 301", capacity: 24 },
    { code: "ILC_R08", name: "Phòng 302", capacity: 16 },
  ].map((r) => ({
    marker: MARKER,
    ...r,
    locationName: "EnglishLab Hai Bà Trưng",
    locationAddress: "123 Phố Huế, Hai Bà Trưng, Hà Nội",
    type: "PHYSICAL",
    active: true,
  }));
  const virtual = [
    { code: "ILC_R09", name: "Studio Online A", capacity: 30 },
    { code: "ILC_R10", name: "Studio Online B", capacity: 30 },
  ].map((r) => ({
    marker: MARKER,
    ...r,
    locationName: "EnglishLab Virtual",
    locationAddress: "Online",
    type: "ONLINE",
    active: true,
  }));
  const rooms = [...physical, ...virtual];
  for (const r of rooms) assertNoForbiddenUi(`room.name:${r.code}`, r.name);
  return rooms;
}

function classTitle(program, intakeLabel, scheduleLabel, evening) {
  return `${program.title} - ${intakeLabel} ${scheduleLabel} ${evening ? "Ca tối 2" : "Ca tối 1"}`;
}

function buildStoryWorld(rng, accounts, catalog, rooms) {
  const teachers = accounts.filter((a) => a.role === "TEACHER" && !a.preserved);
  const learners = accounts.filter((a) => a.role === "LEARNER" && !a.preserved);
  const staff = accounts.filter((a) => a.role === "STAFF");
  const managers = accounts.filter((a) => a.role === "MANAGER");
  const contentManagers = accounts.filter((a) => a.role === "CONTENT_MANAGER");
  const admins = accounts.filter((a) => a.role === "ADMIN");

  const ref = d(REF);
  // Steady ACTIVE at REF: JUL+AUG+SEP = 30 classes (24 offline + 6 virtual). JUN completed, OCT upcoming.
  const intakes = [
    { key: "JUN", label: "June 2026", start: "2026-06-02", end: "2026-08-22", statusHint: "COMPLETED", ym: "2506" },
    { key: "JUL", label: "July 2026", start: "2026-07-01", end: "2026-09-19", statusHint: "ACTIVE", ym: "2507" },
    { key: "AUG", label: "August 2026", start: "2026-08-04", end: "2026-10-24", statusHint: "ACTIVE", ym: "2508" },
    { key: "SEP", label: "September 2026", start: "2026-09-01", end: "2026-11-21", statusHint: "ACTIVE", ym: "2509" },
    { key: "OCT", label: "October 2026", start: "2026-10-06", end: "2026-12-26", statusHint: "UPCOMING", ym: "2510" },
  ];

  const classes = [];
  let classIndex = 0;
  const schedulePatterns = [
    { days: [1, 3, 5], label: "T2-4-6", evening: false, start: "18:00", end: "19:30" },
    { days: [1, 3, 5], label: "T2-4-6", evening: true, start: "19:45", end: "21:15" },
    { days: [2, 4, 6], label: "T3-5-7", evening: false, start: "18:00", end: "19:30" },
    { days: [2, 4, 6], label: "T3-5-7", evening: true, start: "19:45", end: "21:15" },
  ];

  const physicalRooms = rooms.filter((r) => r.type !== "ONLINE");
  const onlineRooms = rooms.filter((r) => r.type === "ONLINE");

  // 32 unique offline (room, pattern) slots — enough for 24 concurrent ACTIVE offline classes
  const offlineSlots = [];
  for (const room of physicalRooms) {
    for (const pattern of schedulePatterns) {
      offlineSlots.push({ room, pattern });
    }
  }

  // 50 classes: 5 months × (8 offline + 2 virtual). Slot index keeps concurrent months on distinct room/pattern pairs.
  for (let intakeIdx = 0; intakeIdx < intakes.length; intakeIdx += 1) {
    const intake = intakes[intakeIdx];
    for (let o = 0; o < 8; o += 1) {
      classIndex += 1;
      const program = catalog.instructorLed[(classIndex - 1) % catalog.instructorLed.length];
      const slot = offlineSlots[(intakeIdx * 8 + o) % offlineSlots.length];
      const start = d(intake.start);
      const plannedEnd = iso(addDays(start, 84));
      const code = `ilc-${intake.ym}-${String(o + 1).padStart(2, "0")}`;
      assertNoForbiddenUi(`class.code:${code}`, code);
      const name = classTitle(program, intake.label, slot.pattern.label, slot.pattern.evening);
      assertNoForbiddenUi(`class.name:${code}`, name);
      classes.push({
        marker: MARKER,
        code,
        name,
        programCode: program.code,
        deliveryMode: "OFFLINE",
        status: intake.statusHint === "UPCOMING" ? "UPCOMING" : intake.statusHint === "COMPLETED" ? "COMPLETED" : "ACTIVE",
        startDate: intake.start,
        plannedEndDate: plannedEnd < intake.end ? plannedEnd : intake.end,
        tuitionFeeVnd: program.tuition,
        capacity: 16,
        primaryTeacherEmail: null, // filled by greedy assign below
        roomCode: slot.room.code,
        schedule: slot.pattern,
        intakeKey: intake.key,
        _slotKey: `${slot.room.code}|${slot.pattern.days.join(",")}|${slot.pattern.start}`,
      });
    }
    for (let v = 0; v < 2; v += 1) {
      classIndex += 1;
      const program = catalog.instructorLed[(classIndex - 1) % catalog.instructorLed.length];
      const pattern = schedulePatterns[(intakeIdx + v) % schedulePatterns.length];
      const room = onlineRooms[v % onlineRooms.length];
      const start = d(intake.start);
      const plannedEnd = iso(addDays(start, 84));
      const code = `ilc-${intake.ym}-v${String(v + 1).padStart(2, "0")}`;
      assertNoForbiddenUi(`class.code:${code}`, code);
      const name = classTitle(program, intake.label, pattern.label, pattern.evening);
      assertNoForbiddenUi(`class.name:${code}`, name);
      classes.push({
        marker: MARKER,
        code,
        name,
        programCode: program.code,
        deliveryMode: "VIRTUAL",
        status: intake.statusHint === "UPCOMING" ? "UPCOMING" : intake.statusHint === "COMPLETED" ? "COMPLETED" : "ACTIVE",
        startDate: intake.start,
        plannedEndDate: plannedEnd < intake.end ? plannedEnd : intake.end,
        tuitionFeeVnd: program.tuition,
        capacity: 20,
        primaryTeacherEmail: null,
        roomCode: room.code,
        schedule: pattern,
        intakeKey: intake.key,
        _slotKey: `${room.code}|${pattern.days.join(",")}|${pattern.start}`,
      });
    }
  }

  // Greedy teacher assign: no teacher holds two date-overlapping classes on the same (days, start)
  const teacherLoad = teachers.map((t) => ({ email: t.email, assignments: [] }));
  const rangesOverlap = (aStart, aEnd, bStart, bEnd) => aStart <= bEnd && bStart <= aEnd;
  for (const cls of classes) {
    const aStart = cls.startDate;
    const aEnd = cls.plannedEndDate;
    const schedKey = `${cls.schedule.days.join(",")}|${cls.schedule.start}`;
    let picked = null;
    for (const t of teacherLoad) {
      const clash = t.assignments.some((prev) => {
        if (prev.schedKey !== schedKey) return false;
        return rangesOverlap(aStart, aEnd, prev.start, prev.end);
      });
      if (!clash) {
        picked = t;
        break;
      }
    }
    if (!picked) {
      throw new Error(`No free teacher for class ${cls.code} slot ${schedKey}`);
    }
    picked.assignments.push({ schedKey, start: aStart, end: aEnd });
    cls.primaryTeacherEmail = picked.email;
    delete cls._slotKey;
  }

  // Showcase VIRTUAL (SEP ACTIVE): bind preserved alien teacher — Meet 2B target after rebuild
  const showcaseClass = classes.find((c) => c.deliveryMode === "VIRTUAL" && c.status === "ACTIVE" && c.intakeKey === "SEP");
  if (showcaseClass) {
    showcaseClass.primaryTeacherEmail = PRESERVED.teacher.email;
    showcaseClass.showcaseMeet = true;
    showcaseClass.name = `IELTS Speaking Studio - September 2026 Showcase Ca tối 1`;
    assertNoForbiddenUi(`class.name:${showcaseClass.code}`, showcaseClass.name);
  }

  // Conflict-free: one teacher per class slot, room not shared same slot
  const sessions = [];
  const enrollments = [];
  const attendance = [];
  const homework = [];
  const submissions = [];
  const gradebook = [];
  const usedLearnerSlots = new Map(); // email -> list of {day, start}

  let learnerCursor = 0;
  for (const cls of classes) {
    const targetSize = cls.deliveryMode === "VIRTUAL" ? 14 : 12;
    const classLearners = [];
    let guard = 0;
    while (classLearners.length < targetSize && guard < learners.length * 3) {
      guard += 1;
      const learner = learners[learnerCursor % learners.length];
      learnerCursor += 1;
      const key = learner.email;
      const conflicts = usedLearnerSlots.get(key) || [];
      const clash = conflicts.some((c) => c.days.some((day) => cls.schedule.days.includes(day)) && c.start === cls.schedule.start);
      if (clash) continue;
      if (classLearners.some((x) => x.email === learner.email)) continue;
      classLearners.push(learner);
      conflicts.push({ days: cls.schedule.days, start: cls.schedule.start });
      usedLearnerSlots.set(key, conflicts);
    }

    // Preserved showcase learner must be enrolled in alien's VIRTUAL class
    if (cls.showcaseMeet) {
      const preservedLearner = {
        email: PRESERVED.learner.email,
        role: "LEARNER",
        preserved: true,
      };
      if (!classLearners.some((x) => x.email === preservedLearner.email) && classLearners.length < cls.capacity) {
        classLearners.push(preservedLearner);
      }
    }

    for (const learner of classLearners) {
      const enrolledAt = iso(addDays(d(cls.startDate), -rng.int(3, 14)));
      enrollments.push({
        marker: MARKER,
        classCode: cls.code,
        learnerEmail: learner.email,
        // Placed on a class_section → ASSIGNED (learning access). UPCOMING deposits stay PARTIALLY_PAID.
        registrationStatus: cls.status === "UPCOMING" ? "PARTIALLY_PAID" : "ASSIGNED",
        agreedTuitionFeeVnd: cls.tuitionFeeVnd,
        tuitionAmountDue: cls.tuitionFeeVnd,
        tuitionAmountPaid: cls.status === "UPCOMING" ? Math.round(cls.tuitionFeeVnd * 0.5) : cls.tuitionFeeVnd,
        enrolledAt: `${enrolledAt}T10:00:00`,
      });
    }

    // Generate sessions 3×/week, no Sunday, up to 36
    let cursor = d(cls.startDate);
    const end = d(cls.plannedEndDate);
    let sessionNo = 0;
    while (cursor <= end && sessionNo < 36) {
      const dow = cursor.getUTCDay(); // 0 Sun — never schedule
      if (dow !== 0 && cls.schedule.days.includes(dow)) {
        sessionNo += 1;
        const sessionDate = iso(cursor);
        let status = "SCHEDULED";
        if (sessionDate < REF) status = "COMPLETED";
        else if (sessionDate === REF) status = "IN_PROGRESS";
        sessions.push({
          marker: MARKER,
          naturalKey: `${cls.code}|${sessionDate}|${cls.schedule.start}`,
          classCode: cls.code,
          sessionDate,
          startTime: cls.schedule.start,
          endTime: cls.schedule.end,
          teacherEmail: cls.primaryTeacherEmail,
          roomCode: cls.roomCode,
          status,
          title: cls.name,
        });

        // Attendance subset of completed sessions
        if ((status === "COMPLETED" || status === "IN_PROGRESS") && sessionNo % 3 === 1) {
          for (let li = 0; li < classLearners.length; li += 1) {
            const roll = rng.next();
            let att = "PRESENT";
            if (roll > 0.93) att = "ABSENT";
            else if (roll > 0.88) att = "LATE";
            else if (roll > 0.84) att = "EXCUSED";
            attendance.push({
              marker: MARKER,
              classCode: cls.code,
              sessionDate,
              startTime: cls.schedule.start,
              learnerEmail: classLearners[li].email,
              status: att,
              recordedAt: `${sessionDate}T${cls.schedule.end}:00`,
            });
          }
        }
      }
      cursor = addDays(cursor, 1);
    }

    // Homework 2–4 per in-progress/completed class
    if (cls.status !== "UPCOMING") {
      const hwCount = rng.int(2, 4);
      const topics = [
        "Writing Task 2 - Education and Technology",
        "Listening Section 3 Practice",
        "Vocabulary Review - Environment",
        "Speaking Part 2 - Describe a Skill",
        "Reading Matching Headings Drill",
        "Grammar Focus - Conditional Sentences",
      ];
      for (let h = 0; h < hwCount; h += 1) {
        const publish = iso(addDays(d(cls.startDate), 7 + h * 10));
        if (publish > REF && cls.status === "COMPLETED") continue;
        const due = iso(addDays(d(publish), 7));
        const hwKey = `${cls.code}|hw-${h + 1}`;
        homework.push({
          marker: MARKER,
          naturalKey: hwKey,
          classCode: cls.code,
          title: topics[(classIndex + h) % topics.length],
          publishedAt: `${publish}T18:00:00`,
          dueAt: `${due}T23:59:00`,
          maxScore: 10,
        });
        for (const learner of classLearners) {
          if (due > REF && rng.bool(0.4)) continue;
          if (publish > REF) continue;
          const submittedAt = iso(addDays(d(publish), rng.int(1, 6)));
          if (submittedAt > REF) continue;
          const score = Math.max(4, Math.min(10, 7 + rng.int(-2, 3)));
          submissions.push({
            marker: MARKER,
            homeworkKey: hwKey,
            learnerEmail: learner.email,
            status: "GRADED",
            score,
            submittedAt: `${submittedAt}T20:15:00`,
            gradedAt: `${iso(addDays(d(submittedAt), 1))}T09:30:00`,
            feedback: score >= 8 ? "Lập luận rõ, từ vựng tốt." : "Cần bổ sung ví dụ và kiểm soát ngữ pháp.",
          });
        }
      }

      for (const learner of classLearners) {
        const learnerSubs = submissions.filter((s) => s.learnerEmail === learner.email && s.homeworkKey.startsWith(cls.code));
        const avgHw = learnerSubs.length
          ? learnerSubs.reduce((sum, s) => sum + s.score, 0) / learnerSubs.length
          : null;
        const attRows = attendance.filter((a) => a.classCode === cls.code && a.learnerEmail === learner.email);
        const present = attRows.filter((a) => a.status === "PRESENT" || a.status === "LATE").length;
        const attPct = attRows.length ? Math.round((present / attRows.length) * 1000) / 10 : null;
        gradebook.push({
          marker: MARKER,
          classCode: cls.code,
          learnerEmail: learner.email,
          homeworkScore: avgHw == null ? null : Math.round(avgHw * 10) / 10,
          attendancePercent: attPct,
          finalResult: avgHw == null || attPct == null ? null : Math.round((avgHw * 0.6 + (attPct / 10) * 0.4) * 10) / 10,
          status: cls.status === "COMPLETED" ? "PUBLISHED" : "DRAFT",
        });
      }
    }
  }

  // Online courses (extras only — protected courses referenced, not regenerated)
  const onlineCourses = catalog.onlineExtras.map((course) => ({
    marker: MARKER,
    ...course,
    shortDescription: course.shortDescription || `${course.title}: lộ trình luyện tập có mục tiêu đầu ra rõ ràng theo chuẩn trung tâm.`,
    description: course.description || `${course.title} gồm mục tiêu đầu ra, tiến trình module theo kỹ năng và bài luyện gắn band/điểm mục tiêu. Nội dung không dùng placeholder chung cho mọi khóa.`,
    createdByEmail: contentManagers[0].email,
  }));

  // Online enrollments for master learners (not preserved showcase)
  const onlineEnrollments = [];
  const lessonProgressPlan = [];
  for (let i = 0; i < 180; i += 1) {
    const learner = learners[i % learners.length];
    const course = onlineCourses[i % onlineCourses.length];
    if (course.status !== "PUBLISHED") continue;
    // Spread across Jun–Sep (no future enrollments after REF)
    const enrolledOn = iso(addDays(d(WINDOW_FROM), rng.int(0, 100)));
    if (enrolledOn > REF) continue;
    const progress = Math.min(100, rng.int(10, 95));
    onlineEnrollments.push({
      marker: MARKER,
      learnerEmail: learner.email,
      courseSlug: course.slug,
      status: progress >= 100 ? "COMPLETED" : "ACTIVE",
      progressPercent: progress,
      enrolledAt: `${enrolledOn}T11:00:00`,
      orderCode: 900000 + i,
    });
    lessonProgressPlan.push({
      marker: MARKER,
      learnerEmail: learner.email,
      courseSlug: course.slug,
      completedLessonCount: Math.max(1, Math.floor(progress / 20)),
      progressPercent: progress,
    });
  }

  // Payments linked to online enrollments
  const payments = onlineEnrollments.map((enr, idx) => {
    const course = onlineCourses.find((c) => c.slug === enr.courseSlug);
    const discount = idx % 5 === 0 ? 100000 : 0;
    const amount = Math.max(0, (course?.price || 0) - discount);
    return {
      marker: MARKER,
      orderCode: enr.orderCode,
      learnerEmail: enr.learnerEmail,
      courseSlug: enr.courseSlug,
      originalAmount: course?.price || 0,
      couponDiscountAmount: discount,
      finalAmount: amount,
      status: "PAID",
      createdAt: enr.enrolledAt,
      paidAt: enr.enrolledAt,
    };
  });

  // Support tickets
  const ticketSubjects = [
    "Không truy cập được video bài học Module 3",
    "Thanh toán thành công nhưng khóa học chưa được kích hoạt",
    "Xin đổi lớp do trùng lịch học đại học",
    "Yêu cầu xuất hóa đơn VAT cho học phí tháng 8",
    "Lỗi tải tệp bài tập Writing Task 2",
  ];
  const tickets = [];
  for (let i = 0; i < 28; i += 1) {
    const created = iso(addDays(d(WINDOW_FROM), rng.int(0, 100)));
    if (created > REF) continue;
    const learner = learners[i % learners.length];
    const closed = created < "2026-09-01" || rng.bool(0.55);
    tickets.push({
      marker: MARKER,
      naturalKey: `ilc-ticket-${String(i + 1).padStart(3, "0")}`,
      learnerEmail: learner.email,
      assigneeEmail: staff[i % staff.length].email,
      subject: ticketSubjects[i % ticketSubjects.length],
      status: closed ? "RESOLVED" : "OPEN",
      createdAt: `${created}T09:20:00`,
      closedAt: closed ? `${iso(addDays(d(created), rng.int(1, 5)))}T16:00:00` : null,
    });
  }

  // Proposals / registration / change requests
  const proposals = [];
  for (let i = 0; i < 20; i += 1) {
    const created = iso(addDays(d(WINDOW_FROM), rng.int(0, 95)));
    const approved = created <= REF && rng.bool(0.7);
    proposals.push({
      marker: MARKER,
      naturalKey: `ilc-proposal-${String(i + 1).padStart(2, "0")}`,
      programCode: catalog.instructorLed[i % catalog.instructorLed.length].code,
      createdByEmail: staff[i % staff.length].email,
      reviewedByEmail: managers[i % managers.length].email,
      status: created > REF ? "DRAFT" : approved ? "APPROVED" : "REJECTED",
      createdAt: `${created}T10:00:00`,
      reviewedAt: created > REF ? null : `${iso(addDays(d(created), 2))}T14:00:00`,
      title: `Đề xuất mở lớp ${catalog.instructorLed[i % catalog.instructorLed.length].title} — đợt ${i + 1}`,
    });
  }

  const registrations = [];
  for (let i = 0; i < 30; i += 1) {
    const created = iso(addDays(d(WINDOW_FROM), rng.int(0, 100)));
    registrations.push({
      marker: MARKER,
      naturalKey: `ilc-reg-${String(i + 1).padStart(3, "0")}`,
      learnerEmail: learners[(i + 20) % learners.length].email,
      programCode: catalog.instructorLed[i % catalog.instructorLed.length].code,
      status: created > REF ? "SUBMITTED" : rng.pick(["APPROVED", "ASSIGNED", "REJECTED", "WAITING_PAYMENT"]),
      createdAt: `${created}T08:30:00`,
      note: "Hồ sơ tư vấn đầu vào — nguyện vọng ca tối.",
    });
  }

  const evaluations = [];
  for (let i = 0; i < 20; i += 1) {
    const periodEnd = iso(addDays(d("2026-06-30"), i * 7));
    if (periodEnd > REF) continue;
    evaluations.push({
      marker: MARKER,
      naturalKey: `ilc-eval-${String(i + 1).padStart(2, "0")}`,
      teacherEmail: teachers[i % teachers.length].email,
      evaluatorEmail: managers[i % managers.length].email,
      periodFrom: iso(addDays(d(periodEnd), -30)),
      periodTo: periodEnd,
      score: 7 + (i % 3), // numeric(3,2) max 9.99
      status: "COMPLETED",
      comment: "Giáo viên duy trì chuyên cần tốt, phản hồi bài tập đúng hạn.",
    });
  }

  const materials = [];
  for (let i = 0; i < 40; i += 1) {
    materials.push({
      marker: MARKER,
      naturalKey: `ilc-material-${String(i + 1).padStart(3, "0")}`,
      title: [
        "Syllabus IELTS Foundation — Tuần 1-4",
        "Handout Writing Task 2 — Education",
        "Listening Transcript Pack — Section 3",
        "Vocabulary List — Environment & Climate",
        "TOEIC Mini Drill — Parts 1-4",
      ][i % 5],
      classCode: classes[i % classes.length].code,
      uploadedByEmail: teachers[i % teachers.length].email,
      uploadedAt: `${iso(addDays(d(WINDOW_FROM), rng.int(0, 90)))}T12:00:00`,
    });
  }

  const discountCodes = [
    { marker: MARKER, code: "ILC_SUMMER10", percentOff: 10, active: true, startsOn: "2026-06-01", endsOn: "2026-08-31" },
    { marker: MARKER, code: "ILC_SEP15", percentOff: 15, active: true, startsOn: "2026-09-01", endsOn: "2026-09-30" },
    { marker: MARKER, code: "ILC_WELCOME5", percentOff: 5, active: true, startsOn: "2026-06-01", endsOn: "2026-12-31" },
  ];

  const baseWorld = {
    classes,
    sessions,
    enrollments,
    attendance,
    homework,
    submissions,
    gradebook,
    onlineCourses,
    onlineEnrollments,
    lessonProgressPlan,
    payments,
    tickets,
    proposals,
    registrations,
    evaluations,
    materials,
    discountCodes,
  };

  const gap = assembleGapWorld({
    rng,
    MARKER,
    REF,
    WINDOW_FROM,
    teachers,
    learners,
    staff,
    managers,
    contentManagers,
    admins,
    classes,
    enrollments,
    attendance,
    onlineCourses,
    onlineEnrollments,
    lessonProgressPlan,
    payments,
    tickets,
    instructorLedPrograms: catalog.instructorLed,
  });

  return {
    ...baseWorld,
    ...gap,
    meta: {
      staffEmails: staff.map((s) => s.email),
      managerEmails: managers.map((m) => m.email),
      contentManagerEmails: contentManagers.map((c) => c.email),
    },
  };
}

function timelineRows(world) {
  const buckets = {
    "2026-06": [],
    "2026-07": [],
    "2026-08": [],
    "2026-09": [],
    "2026-10": [],
  };
  const push = (date, domain, summary) => {
    const key = monthBucket(date);
    if (!buckets[key]) return;
    buckets[key].push({ date, domain, summary });
  };
  world.sessions.forEach((s) => push(s.sessionDate, "session", `${s.classCode} ${s.status}`));
  world.payments.forEach((p) => push(p.createdAt.slice(0, 10), "payment", `order ${p.orderCode} ${p.finalAmount}`));
  world.tickets.forEach((t) => push(t.createdAt.slice(0, 10), "support", t.subject));
  world.homework.forEach((h) => push(h.publishedAt.slice(0, 10), "homework", h.title));
  world.enrollments.forEach((e) => push(e.enrolledAt.slice(0, 10), "class_enrollment", e.classCode));
  return buckets;
}

function countByMonth(items, dateField) {
  const out = { "2026-06": 0, "2026-07": 0, "2026-08": 0, "2026-09": 0, "2026-10": 0 };
  for (const item of items) {
    const raw = item[dateField];
    if (!raw) continue;
    const key = String(raw).slice(0, 7);
    if (out[key] != null) out[key] += 1;
  }
  return out;
}

function semanticValidate(dataset) {
  const issues = [];
  issues.push(...validateVietnameseNames(dataset.accounts).map((i) => ({ type: "invalid_name", ...i })));

  const sessionKeys = new Set();
  for (const s of dataset.world.sessions) {
    if (sessionKeys.has(s.naturalKey)) issues.push({ type: "duplicate_session", key: s.naturalKey });
    sessionKeys.add(s.naturalKey);
    if (s.status === "COMPLETED" && s.sessionDate > REF) {
      issues.push({ type: "future_completed_session", key: s.naturalKey });
    }
  }

  // room conflicts
  const roomBusy = new Map();
  for (const s of dataset.world.sessions) {
    if (!s.roomCode) continue;
    const key = `${s.roomCode}|${s.sessionDate}|${s.startTime}`;
    if (roomBusy.has(key)) issues.push({ type: "room_conflict", key, classes: [roomBusy.get(key), s.classCode] });
    else roomBusy.set(key, s.classCode);
  }

  // teacher conflicts
  const teacherBusy = new Map();
  for (const s of dataset.world.sessions) {
    const key = `${s.teacherEmail}|${s.sessionDate}|${s.startTime}`;
    if (teacherBusy.has(key)) issues.push({ type: "teacher_conflict", key, classes: [teacherBusy.get(key), s.classCode] });
    else teacherBusy.set(key, s.classCode);
  }

  for (const a of dataset.world.attendance) {
    if (a.sessionDate > REF && a.status === "PRESENT") {
      issues.push({ type: "future_attendance", ...a });
    }
  }

  for (const sub of dataset.world.submissions) {
    if (sub.gradedAt && sub.submittedAt && sub.gradedAt < sub.submittedAt) {
      issues.push({ type: "grade_before_submit", ...sub });
    }
  }

  for (const p of dataset.world.payments) {
    if (p.finalAmount < 0 || p.finalAmount > p.originalAmount) {
      issues.push({ type: "payment_amount", orderCode: p.orderCode });
    }
    if (p.status === "PAID" && !p.paidAt) {
      issues.push({ type: "payment_paid_missing_paidAt", orderCode: p.orderCode });
    }
  }

  const placeholderRe = /^(abc|xyz|test|hello|lorem ipsum|sample text)$/i;
  for (const disc of dataset.world.discussions || []) {
    if (placeholderRe.test(String(disc.title).trim()) || placeholderRe.test(String(disc.content).trim())) {
      issues.push({ type: "discussion_placeholder", key: disc.naturalKey });
    }
  }

  const reactionKeys = new Set();
  for (const r of dataset.world.discussionReactions || []) {
    const uk = `${r.postNaturalKey}|${r.userEmail}`;
    if (reactionKeys.has(uk)) issues.push({ type: "duplicate_reaction", key: uk });
    reactionKeys.add(uk);
  }

  for (const rep of dataset.world.discussionReports || []) {
    if (rep.status !== "PENDING" && rep.reviewedAt && rep.createdAt && rep.reviewedAt <= rep.createdAt) {
      issues.push({ type: "report_review_chronology", key: rep.naturalKey });
    }
  }

  for (const n of dataset.world.notifications || []) {
    if (n.read && !n.readAt) issues.push({ type: "notification_read_missing_readAt", key: n.naturalKey });
    if (!n.read && n.readAt) issues.push({ type: "notification_unread_has_readAt", key: n.naturalKey });
  }

  for (const proof of dataset.world.tuitionProofs || []) {
    if (proof.status === "PENDING" && (proof.reviewedByEmail || proof.reviewedAt)) {
      issues.push({ type: "tuition_proof_pending_has_review", key: proof.naturalKey });
    }
    if (proof.status !== "PENDING" && (!proof.reviewedByEmail || !proof.reviewedAt)) {
      issues.push({ type: "tuition_proof_review_incomplete", key: proof.naturalKey });
    }
  }

  for (const b of dataset.world.broadcasts || []) {
    if (b.scheduledAt && b.scheduledAt.slice(0, 10) > REF && b.status === "SENT") {
      issues.push({ type: "broadcast_future_marked_sent", key: b.naturalKey });
    }
    if (b.status !== "SENT" && b.sentAt) {
      issues.push({ type: "broadcast_unsent_has_sentAt", key: b.naturalKey });
    }
    if (b.inAppSuccessCount > b.recipientCount) {
      issues.push({ type: "broadcast_inapp_over_recipients", key: b.naturalKey });
    }
  }

  const completed100 = (dataset.world.onlineEnrollments || []).filter((e) => e.status === "COMPLETED" && e.progressPercent === 100);
  if (completed100.length < 5) {
    issues.push({ type: "insufficient_completed_enrollments", count: completed100.length });
  }

  // capacity
  const byClass = new Map();
  for (const e of dataset.world.enrollments) {
    byClass.set(e.classCode, (byClass.get(e.classCode) || 0) + 1);
  }
  for (const cls of dataset.world.classes) {
    const n = byClass.get(cls.code) || 0;
    if (n > cls.capacity) issues.push({ type: "over_capacity", classCode: cls.code, n, capacity: cls.capacity });
  }

  const placeholders = /^(abc|xyz|test|demo data|sample text|lorem ipsum)$/i;
  for (const cls of dataset.world.classes) {
    if (placeholders.test(cls.name) || /test course|demo course|course abc|sample course/i.test(cls.name)) {
      issues.push({ type: "placeholder_text", field: "class.name", value: cls.name });
    }
  }

  // Scale gates (center mid-size)
  if (dataset.rooms.length !== 10) issues.push({ type: "scale_rooms", actual: dataset.rooms.length, expected: 10 });
  if (dataset.world.classes.length !== 50) issues.push({ type: "scale_classes", actual: dataset.world.classes.length, expected: 50 });
  const active = dataset.world.classes.filter((c) => c.status === "ACTIVE");
  const activeOffline = active.filter((c) => c.deliveryMode === "OFFLINE").length;
  const activeVirtual = active.filter((c) => c.deliveryMode === "VIRTUAL").length;
  if (activeOffline !== 24 || activeVirtual !== 6) {
    issues.push({ type: "scale_active_mix", activeOffline, activeVirtual, expected: "24+6" });
  }
  const teacherCount = dataset.accounts.filter((a) => a.role === "TEACHER" && !a.preserved).length;
  if (teacherCount < 18 || teacherCount > 20) issues.push({ type: "scale_teachers", actual: teacherCount });
  const learnerCount = dataset.accounts.filter((a) => a.role === "LEARNER" && !a.preserved).length;
  if (learnerCount < 300 || learnerCount > 330) issues.push({ type: "scale_learners", actual: learnerCount });

  for (const s of dataset.world.sessions) {
    const dow = d(s.sessionDate).getUTCDay();
    if (dow === 0) issues.push({ type: "sunday_session", key: s.naturalKey });
  }

  const sessionsByClass = new Map();
  for (const s of dataset.world.sessions) {
    sessionsByClass.set(s.classCode, (sessionsByClass.get(s.classCode) || 0) + 1);
  }
  for (const cls of dataset.world.classes) {
    const n = sessionsByClass.get(cls.code) || 0;
    if (n < 30 || n > 36) issues.push({ type: "scale_sessions_per_class", classCode: cls.code, n });
  }

  const scanForbidden = (label, value) => {
    try {
      assertNoForbiddenUi(label, value);
    } catch (err) {
      issues.push({ type: "forbidden_ui", label, value: String(value), message: err.message });
    }
  };
  for (const a of dataset.accounts) {
    if (a.preserved) continue;
    scanForbidden(`email:${a.email}`, a.email);
    scanForbidden(`fullName:${a.email}`, a.fullName);
  }
  for (const c of dataset.world.onlineCourses || []) {
    scanForbidden(`online.slug:${c.slug}`, c.slug);
    scanForbidden(`online.title:${c.slug}`, c.title);
  }
  for (const c of dataset.catalog.instructorLed || []) {
    scanForbidden(`ilc.code:${c.code}`, c.code);
    scanForbidden(`ilc.title:${c.code}`, c.title);
  }
  for (const cls of dataset.world.classes) {
    scanForbidden(`class.code:${cls.code}`, cls.code);
    scanForbidden(`class.name:${cls.code}`, cls.name);
  }
  for (const dc of dataset.world.discountCodes || []) {
    scanForbidden(`discount:${dc.code}`, dc.code);
  }

  return issues;
}

function buildOwnershipManifest(dataset) {
  const generatedEmails = dataset.accounts.filter((a) => !a.preserved).map((a) => a.email).sort();
  const preservedEmails = [
    PRESERVED.learner.email,
    PRESERVED.teacher.email,
    ...PRESERVED.staff.map((s) => s.email),
  ];
  return {
    marker: MARKER,
    emailDomain: EMAIL_DOMAIN,
    neverCleanupByDomainAlone: true,
    neverCleanupRule: "Do NOT delete all @englishlab.vn — only exact owned prefixes/codes listed here plus legacy patterns",
    generatedEmailPrefixes: ["gv.", "hs.", "nv.", "ql.", "cm.", "ad."],
    generatedEmails,
    generatedEmailCount: generatedEmails.length,
    preservedEmailsNeverDelete: preservedEmails,
    protectedCourseSlugs: PROTECTED_COURSES.map((c) => c.slug),
    classCodePrefix: "ilc-",
    classCodes: dataset.world.classes.map((c) => c.code).sort(),
    courseSlugPrefix: "ilc-",
    courseSlugs: (dataset.world.onlineCourses || []).map((c) => c.slug).sort(),
    ilcCodes: dataset.catalog.instructorLed.map((c) => c.code),
    roomCodes: dataset.rooms.map((r) => r.code),
    roomNames: dataset.rooms.map((r) => r.name),
    discountCodes: (dataset.world.discountCodes || []).map((c) => c.code),
    flashcardCodePrefix: "ilc-fc-",
    exerciseCodePrefix: "ilc-ex-",
    masterRefPrefix: MASTER_REF_PREFIX,
    legacyCleanup: {
      emailPatterns: [
        "^demo\\..+@englishlab\\.local$",
        "^gv\\.sheet\\.\\d+@englishlab\\.vn$",
        "^hs\\.(sheet|consult)\\.\\d+@englishlab\\.vn$",
        "^review\\..+@englishlab\\.vn$",
      ],
      classCodePrefixes: ["demo-class-", "center-sheet-class-"],
      courseSlugPrefix: "demo-",
      ilcCodePrefix: "DEMO_",
      roomCodePrefix: "demo-room-",
      roomNamePrefix: "Phòng học ",
      discountPrefix: "DEMO_",
      flashcardPrefix: "demo-fc-",
      exercisePrefix: "demo-ex-",
      masterRefPrefix: "master-demo://",
    },
    counts: {
      classes: dataset.world.classes.length,
      activeClasses: dataset.world.classes.filter((c) => c.status === "ACTIVE").length,
      rooms: dataset.rooms.length,
      teachers: dataset.accounts.filter((a) => a.role === "TEACHER" && !a.preserved).length,
      learners: dataset.accounts.filter((a) => a.role === "LEARNER" && !a.preserved).length,
      sessions: dataset.world.sessions.length,
      classEnrollments: dataset.world.enrollments.length,
    },
  };
}

function sheetFromRows(rows) {
  return XLSX.utils.json_to_sheet(rows.length ? rows : [{ note: "empty" }]);
}

function writeWorkbook(dataset, outPath) {
  const wb = XLSX.utils.book_new();
  const readme = [
    { Mục: "Tên", "Chi tiết": "EnglishLab Master Demo Dataset" },
    { Mục: "Seed", "Chi tiết": String(DEMO_DATA_SEED) },
    { Mục: "Reference date", "Chi tiết": REF },
    { Mục: "Window", "Chi tiết": `${WINDOW_FROM} → ${WINDOW_TO}` },
    { Mục: "Priority", "Chi tiết": "DATA QUALITY > BUSINESS COVERAGE > RECORD COUNT" },
    { Mục: "Preserved learner", "Chi tiết": PRESERVED.learner.email },
    { Mục: "Preserved teacher", "Chi tiết": PRESERVED.teacher.email },
    { Mục: "Protected courses", "Chi tiết": PROTECTED_COURSES.map((c) => c.slug).join(", ") },
    { Mục: "Master marker", "Chi tiết": MARKER },
    { Mục: "Flag", "Chi tiết": "app.seed.master.enabled=false by default" },
    { Mục: "Import JSON", "Chi tiết": "backend/src/main/resources/seed/master/master-dataset.json" },
  ];
  XLSX.utils.book_append_sheet(wb, sheetFromRows(readme), "00_README");
  XLSX.utils.book_append_sheet(wb, sheetFromRows(dataset.tableMatrix), "01_TABLE_MATRIX");
  XLSX.utils.book_append_sheet(
    wb,
    sheetFromRows(dataset.accounts.map((a) => ({
      email: a.email,
      fullName: a.fullName,
      role: a.role,
      purpose: a.purpose,
      preserved: a.preserved ? "PRESERVED" : "MASTER",
      loginNote: a.passwordNote,
      gender: a.gender || "",
      phone: a.phone || "",
    }))),
    "02_ACCOUNTS",
  );

  const timelines = timelineRows(dataset.world);
  XLSX.utils.book_append_sheet(wb, sheetFromRows(timelines["2026-06"].slice(0, 200)), "03_TIMELINE_JUN_2026");
  XLSX.utils.book_append_sheet(wb, sheetFromRows(timelines["2026-07"].slice(0, 200)), "04_TIMELINE_JUL_2026");
  XLSX.utils.book_append_sheet(wb, sheetFromRows(timelines["2026-08"].slice(0, 200)), "05_TIMELINE_AUG_2026");
  XLSX.utils.book_append_sheet(wb, sheetFromRows(timelines["2026-09"].slice(0, 200)), "06_TIMELINE_SEP_2026");
  XLSX.utils.book_append_sheet(wb, sheetFromRows(timelines["2026-10"].slice(0, 200)), "07_TIMELINE_OCT_2026");

  XLSX.utils.book_append_sheet(wb, sheetFromRows(dataset.catalog.categories), "10_CATEGORIES");
  XLSX.utils.book_append_sheet(wb, sheetFromRows([
    ...dataset.catalog.protectedCourses.map((c) => ({ ...c, source: "PROTECTED" })),
    ...dataset.world.onlineCourses.map((c) => ({ slug: c.slug, title: c.title, category: c.category, price: c.price, status: c.status, source: "MASTER" })),
  ]), "11_ONLINE_COURSES");
  XLSX.utils.book_append_sheet(wb, sheetFromRows(dataset.catalog.instructorLed), "12_INSTRUCTOR_LED");
  XLSX.utils.book_append_sheet(wb, sheetFromRows(dataset.rooms), "13_ROOMS");
  XLSX.utils.book_append_sheet(wb, sheetFromRows(dataset.world.classes), "14_CLASS_SECTIONS");
  XLSX.utils.book_append_sheet(wb, sheetFromRows(dataset.world.enrollments), "15_CLASS_ENROLLMENTS");
  XLSX.utils.book_append_sheet(wb, sheetFromRows(dataset.world.sessions.slice(0, 500)), "16_SESSIONS_SAMPLE");
  XLSX.utils.book_append_sheet(wb, sheetFromRows(dataset.world.homework), "17_HOMEWORK");
  XLSX.utils.book_append_sheet(wb, sheetFromRows(dataset.world.payments), "18_PAYMENTS");
  XLSX.utils.book_append_sheet(wb, sheetFromRows(dataset.world.tickets), "19_SUPPORT_TICKETS");
  XLSX.utils.book_append_sheet(wb, sheetFromRows(dataset.world.proposals), "20_PROPOSALS");
  XLSX.utils.book_append_sheet(wb, sheetFromRows(dataset.world.discussions || []), "21_DISCUSSIONS");
  XLSX.utils.book_append_sheet(wb, sheetFromRows(dataset.world.announcements || []), "22_ANNOUNCEMENTS");
  XLSX.utils.book_append_sheet(wb, sheetFromRows(dataset.world.teacherFeedback || []), "23_TEACHER_FEEDBACK");
  XLSX.utils.book_append_sheet(wb, sheetFromRows(dataset.counts), "99_COUNTS");

  const buf = XLSX.write(wb, { type: "buffer", bookType: "xlsx" });
  fs.writeFileSync(outPath, buf);
}

function main() {
  const rng = createRng(DEMO_DATA_SEED);
  const accounts = buildAccounts(rng);
  const catalog = buildCatalog();
  const rooms = buildRooms();
  const world = buildStoryWorld(rng, accounts, catalog, rooms);

  const counts = {
    users_master: accounts.filter((a) => !a.preserved).length,
    users_by_role: {
      ADMIN: accounts.filter((a) => a.role === "ADMIN").length,
      MANAGER: accounts.filter((a) => a.role === "MANAGER").length,
      CONTENT_MANAGER: accounts.filter((a) => a.role === "CONTENT_MANAGER").length,
      STAFF: accounts.filter((a) => a.role === "STAFF").length,
      TEACHER: accounts.filter((a) => a.role === "TEACHER" && !a.preserved).length,
      LEARNER: accounts.filter((a) => a.role === "LEARNER" && !a.preserved).length,
    },
    online_courses_master: world.onlineCourses.length,
    online_courses_protected: PROTECTED_COURSES.length,
    online_courses_total_target: world.onlineCourses.length + PROTECTED_COURSES.length,
    instructor_led_courses: catalog.instructorLed.length,
    classrooms: world.classes.length,
    class_enrollments: world.enrollments.length,
    sessions: world.sessions.length,
    attendance: world.attendance.length,
    homework: world.homework.length,
    homework_submissions: world.submissions.length,
    gradebook: world.gradebook.length,
    online_enrollments: world.onlineEnrollments.length,
    payments: world.payments.length,
    notifications: world.notifications.length,
    support_tickets: world.tickets.length,
    proposals: world.proposals.length,
    registrations: world.registrations.length,
    evaluations: world.evaluations.length,
    materials: world.materials.length,
    rooms: rooms.length,
    discussions: world.discussions?.length || 0,
    discussionReplies: world.discussionReplies?.length || 0,
    discussionReactions: world.discussionReactions?.length || 0,
    discussionReports: world.discussionReports?.length || 0,
    teacherFeedback: world.teacherFeedback?.length || 0,
    announcements: world.announcements?.length || 0,
    broadcasts: world.broadcasts?.length || 0,
    ticketMessages: world.ticketMessages?.length || 0,
    changeRequests: world.changeRequests?.length || 0,
    attendanceDisputes: world.attendanceDisputes?.length || 0,
    courseListItems: world.courseListItems?.length || 0,
    tuitionPayments: world.tuitionPayments?.length || 0,
    tuitionProofs: world.tuitionProofs?.length || 0,
    teacherCredentials: world.teacherCredentials?.length || 0,
    practiceAttempts: world.practiceAttempts?.length || 0,
    flashcardSets: world.flashcardSets?.length || 0,
    centerLibrary: world.centerLibrary?.length || 0,
    lessonNotes: world.lessonNotes?.length || 0,
    courseUnits: world.courseUnits?.length || 0,
    courseLessons: (world.courseUnits || []).reduce((sum, u) => sum + (u.lessons?.length || 0), 0),
    online_enrollments_completed_100: world.onlineEnrollments.filter((e) => e.status === "COMPLETED" && e.progressPercent === 100).length,
    course_reviews: world.onlineEnrollments.filter((e) => e.reviewRating != null).length,
    month_sessions: countByMonth(world.sessions, "sessionDate"),
    month_payments: countByMonth(world.payments, "createdAt"),
    month_attendance: countByMonth(world.attendance, "sessionDate"),
  };

  const dataset = {
    meta: {
      seed: DEMO_DATA_SEED,
      referenceDate: REF,
      windowFrom: WINDOW_FROM,
      windowTo: WINDOW_TO,
      marker: MARKER,
      generatedAt: new Date().toISOString(),
      preserved: PRESERVED,
      protectedCourses: PROTECTED_COURSES,
    },
    accounts,
    catalog,
    rooms,
    world,
    counts,
  };

  dataset.tableMatrix = buildTableMatrix(counts);
  dataset.ownership = buildOwnershipManifest(dataset);
  const issues = semanticValidate(dataset);
  const report = {
    ok: issues.length === 0,
    issueCount: issues.length,
    issues: issues.slice(0, 200),
    counts,
    ownershipSummary: {
      generatedEmailCount: dataset.ownership.generatedEmailCount,
      classCodes: dataset.ownership.classCodes.length,
      neverCleanupByDomainAlone: true,
    },
    referenceDate: REF,
    seed: DEMO_DATA_SEED,
  };

  const xlsxPath = path.join(ROOT, "ops/sheet-data/EnglishLab-Master-SheetData.xlsx");
  const jsonPath = path.join(ROOT, "backend/src/main/resources/seed/master/master-dataset.json");
  const reportPath = path.join(ROOT, "ops/sheet-data/master-data-validation-report.json");
  const reportClasspath = path.join(ROOT, "backend/src/main/resources/seed/master/master-data-validation-report.json");
  const ownershipPath = path.join(ROOT, "docs/demo-data/09-ownership-manifest.json");

  fs.mkdirSync(path.dirname(jsonPath), { recursive: true });
  fs.mkdirSync(path.dirname(ownershipPath), { recursive: true });
  writeWorkbook(dataset, xlsxPath);
  fs.writeFileSync(jsonPath, JSON.stringify(dataset, null, 2), "utf8");
  fs.writeFileSync(reportPath, JSON.stringify(report, null, 2), "utf8");
  fs.writeFileSync(reportClasspath, JSON.stringify(report, null, 2), "utf8");
  fs.writeFileSync(ownershipPath, JSON.stringify(dataset.ownership, null, 2), "utf8");

  console.log(JSON.stringify({
    xlsxPath,
    jsonPath,
    reportPath,
    ownershipPath,
    ok: report.ok,
    issueCount: report.issueCount,
    counts,
  }, null, 2));

  if (!report.ok) {
    console.error("Semantic validation failed");
    process.exitCode = 1;
  }
}

main();
