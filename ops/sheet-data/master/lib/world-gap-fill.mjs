/**
 * Gap-domain builders for MASTER demo world (discussions, ops, commerce edges, etc.)
 */

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

function ts(dateIso, time = "10:00:00") {
  return `${dateIso}T${time}`;
}

const REACTION_TYPES = ["LIKE", "LOVE", "CARE", "LAUGH", "WOW", "SAD", "ANGRY"];
const REPORT_CATEGORIES = ["SPAM", "INAPPROPRIATE_LANGUAGE", "OFF_TOPIC", "HARASSMENT", "OTHER"];
const CHANGE_TYPES = [
  "RESCHEDULE_SESSION",
  "CHANGE_ROOM",
  "CHANGE_TEACHER",
  "CANCEL_SESSION",
  "TRANSFER_STUDENT",
  "TRANSFER_CLASS",
];

const DISCUSSION_TITLES = [
  "Nên chia đoạn Introduction Writing Task 2 thế nào cho band 6.5?",
  "Cách luyện nghe Section 3 khi bị lạc từ khóa?",
  "Part 2 Speaking: nói quá 2 phút có bị trừ điểm không?",
  "Collocation thường gặp trong đề Environment có gợi ý không?",
  "Nên học từ vựng theo chủ đề hay theo bài đọc?",
  "Luyện phát âm /θ/ và /ð/ có bài tập nào hiệu quả?",
  "Reading Matching Headings — làm sao tránh bẫy đảo thứ tự?",
  "TOEIC Part 7: đọc nhanh mà vẫn bắt được ý chính?",
  "Khi nào nên dùng present perfect trong Writing?",
  "Gợi ý outline cho đề Education & Technology?",
  "Lớp online có nên ghi chép tay hay gõ trực tiếp?",
  "Cách tự sửa bài Writing trước khi nộp?",
];

const DISCUSSION_BODIES = [
  "Em đang học khóa Writing Intensive, phần mở bài em hay viết quá dài. Các anh chị có mẫu câu hook ngắn gọn không ạ?",
  "Buổi nghe hôm qua em bị lạc ở đoạn giáo sư và sinh viên thảo luận. Mọi người có tip ghi chú nhanh không?",
  "Em hay hết ý ở phút 1:20. Muốn hỏi cách mở rộng ý bằng ví dụ cá nhân mà vẫn academic.",
  "Em thấy từ sustainable và renewable hay bị nhầm. Ai có list collocation hay cho chủ đề môi trường không?",
  "Em mới chuyển từ lớp foundation sang, không biết nên ôn từ theo tuần trong syllabus hay đọc thêm báo.",
  "Phát âm thin/thank em hay lẫn. Có bài luyện nào trong khóa áp dụng được hàng ngày không?",
  "Em làm đề reading tuần trước, đoạn C hay chọn sai heading vì từ đồng nghĩa. Mọi người check keyword thế nào?",
  "Part 7 em đọc hết passage rồi mới nhìn câu hỏi nên không kịp. Có chiến lược skim hợp lý không?",
  "Bài em bị sửa 'tense mixing' ở body 2. Khi nào được dùng hiện tại hoàn thành trong luận giải nguyên nhân?",
  "Đề tuần này về technology in education — em muốn xin ý outline 4 đoạn, không nhờ viết hộ ạ.",
  "Em học trên điện thoại hay bị mất tập trung. Ai có cách chia block 25 phút hiệu quả?",
  "Trước khi nộp em tự check grammar bằng checklist. Có ai chia sẻ checklist band 6.5 không?",
];

const REPLY_BODIES_TEACHER = [
  "Bạn thử giới hạn câu mở đầu trong 2 câu, câu 2 nêu rõ quan điểm. Xem lại handout tuần 2 nhé.",
  "Section 3 nên ghi keyword trước, bắt tín hiệu chuyển ý (However, Actually). Luyện lại bài 3.2 trong khóa.",
  "Part 2 nên có khung PEEL cá nhân: mở đề → mô tả → lý do → cảm xúc. 2 phút là đủ nếu triển khai đủ ý.",
  "Sustainable development thường đi với policies/measures. Renewable energy hay dùng với source/transition.",
];

const REPLY_BODIES_LEARNER = [
  "Cảm ơn bạn, em thử outline 4 đoạn như gợi ý và thấy mạch rõ hơn.",
  "Em cũng bị lạc Section 3, để em luyện shadowing 10 phút mỗi ngày xem sao.",
  "Mình note thêm ví dụ du lịch địa phương vào Part 2 thì đủ thời gian.",
  "List collocation bạn gửi rất hữu ích, em save lại rồi.",
];

export function patchOnlineEnrollmentsAndPayments(rng, WINDOW_FROM, onlineCourses, onlineEnrollments, lessonProgressPlan, payments, learners, REF) {
  const published = onlineCourses.filter((c) => c.status === "PUBLISHED");
  let orderSeq = 900000;

  // Ensure ~5 COMPLETED at 100% on distinct learner/course pairs
  const completedTargets = [];
  const existingPairs = new Set(onlineEnrollments.map((e) => `${e.learnerEmail}|${e.courseSlug}`));
  for (let i = 0; i < 5 && i < learners.length; i += 1) {
    const learner = learners[i];
    const course = published[i % published.length];
    const pair = `${learner.email}|${course.slug}`;
    if (existingPairs.has(pair)) {
      const row = onlineEnrollments.find((e) => `${e.learnerEmail}|${e.courseSlug}` === pair);
      if (row) {
        row.status = "COMPLETED";
        row.progressPercent = 100;
      }
      continue;
    }
    const enrolledOn = iso(addDays(d("2026-06-15"), i * 5));
    if (enrolledOn > REF) continue;
    orderSeq += 1;
    existingPairs.add(pair);
    const row = {
      marker: "MASTER_DEMO",
      learnerEmail: learner.email,
      courseSlug: course.slug,
      status: "COMPLETED",
      progressPercent: 100,
      enrolledAt: ts(enrolledOn, "11:00:00"),
      orderCode: orderSeq,
    };
    onlineEnrollments.unshift(row);
    lessonProgressPlan.unshift({
      marker: "MASTER_DEMO",
      learnerEmail: learner.email,
      courseSlug: course.slug,
      completedLessonCount: 12,
      progressPercent: 100,
    });
    payments.unshift({
      marker: "MASTER_DEMO",
      orderCode: orderSeq,
      learnerEmail: learner.email,
      courseSlug: course.slug,
      originalAmount: course.price,
      couponDiscountAmount: 0,
      finalAmount: course.price,
      status: "PAID",
      createdAt: row.enrolledAt,
      paidAt: row.enrolledAt,
    });
    completedTargets.push(row);
  }

  // Course reviews ~35–40% of enrollments with progress >= 30 (not every enrollment)
  const reviewComments = [
    "Các video Listening dễ theo dõi, phần giải thích đáp án giúp mình nhận ra lỗi về keyword.",
    "Nội dung tốt nhưng mình mong có thêm bài luyện Speaking theo đề thi thật.",
    "Khóa học phù hợp ôn tập nhưng một số module khá dài so với lịch làm việc.",
    "Video ngắn gọn, bài tập gắn sát đề thi. Em tăng được band Writing.",
    "Giảng viên phản hồi nhanh trên diễn đàn. Nội dung speaking rất thực tế.",
    "Lộ trình tuần giúp em duy trì thói quen. Recommend cho bạn bè.",
    "Phần từ vựng cần thêm ví dụ câu. Còn lại ổn.",
    "Em hoàn thành phần lớn khóa rồi, cảm thấy tự tin hơn khi làm mock.",
  ];
  for (const enr of onlineEnrollments) {
    if (enr.progressPercent < 30) continue;
    if (!rng.bool(0.55)) continue;
    const enrolledDate = enr.enrolledAt.slice(0, 10);
    const reviewDay = iso(addDays(d(enrolledDate), rng.int(7, 45)));
    if (reviewDay > REF) continue;
    const roll = rng.next();
    enr.reviewRating = roll > 0.82 ? 3 : roll > 0.35 ? 4 : 5;
    enr.reviewComment = rng.pick(reviewComments);
    enr.reviewedAt = ts(reviewDay, "19:30:00");
  }

  // Payment edge cases (~12 extra, not happy-path enrollments)
  const edgeLearners = learners.slice(10, 22);
  const edgeStatuses = [
    { status: "FAILED", paidAt: null },
    { status: "PAID", paidAt: "retry" },
    { status: "CANCELLED", paidAt: null },
    { status: "EXPIRED", paidAt: null },
  ];
  for (let i = 0; i < 12; i += 1) {
    const learner = edgeLearners[i % edgeLearners.length];
    const course = published[i % published.length];
    const createdDay = iso(addDays(d(WINDOW_FROM), rng.int(20, 95)));
    if (createdDay > REF) continue;
    orderSeq += 1;
    let status;
    let paidAt = null;
    if (i % 4 === 0) {
      // FAILED then separate retry PAID
      payments.push({
        marker: "MASTER_DEMO",
        orderCode: orderSeq,
        learnerEmail: learner.email,
        courseSlug: course.slug,
        originalAmount: course.price,
        couponDiscountAmount: 0,
        finalAmount: course.price,
        status: "FAILED",
        createdAt: ts(createdDay, "14:00:00"),
        paidAt: null,
        note: "Cổng thanh toán timeout — học viên thử lại sau.",
      });
      orderSeq += 1;
      const retryDay = iso(addDays(d(createdDay), rng.int(1, 3)));
      payments.push({
        marker: "MASTER_DEMO",
        orderCode: orderSeq,
        learnerEmail: learner.email,
        courseSlug: course.slug,
        originalAmount: course.price,
        couponDiscountAmount: 0,
        finalAmount: course.price,
        status: "PAID",
        createdAt: ts(retryDay, "09:15:00"),
        paidAt: ts(retryDay, "09:16:00"),
        note: "Thanh toán lại thành công sau lỗi mạng.",
      });
      continue;
    }
    const pick = edgeStatuses[(i % 3) + 1];
    status = pick.status;
    if (status === "PAID") paidAt = ts(createdDay, "15:00:00");
    payments.push({
      marker: "MASTER_DEMO",
      orderCode: orderSeq,
      learnerEmail: learner.email,
      courseSlug: course.slug,
      originalAmount: course.price,
      couponDiscountAmount: i % 2 === 0 ? 50000 : 0,
      finalAmount: Math.max(0, course.price - (i % 2 === 0 ? 50000 : 0)),
      status,
      createdAt: ts(createdDay, "14:00:00"),
      paidAt,
      note: status === "CANCELLED" ? "Học viên hủy trước khi hoàn tất." : status === "EXPIRED" ? "Phiên QR hết hạn." : null,
    });
  }

  return { completedTargets };
}

export function buildDiscussions(rng, MARKER, REF, publishedCourses, learners, teachers, contentManagers) {
  const discussions = [];
  const discussionReplies = [];
  const target = 50;
  for (let i = 0; i < target; i += 1) {
    const created = iso(addDays(d("2026-06-05"), rng.int(0, 100)));
    if (created > REF) continue;
    const nk = `demo-disc-${String(i + 1).padStart(3, "0")}`;
    const course = publishedCourses[i % publishedCourses.length];
    const author = learners[(i * 3) % learners.length];
    const resolved = rng.bool(0.35);
    discussions.push({
      marker: MARKER,
      naturalKey: nk,
      courseSlug: course.slug,
      authorEmail: author.email,
      title: DISCUSSION_TITLES[i % DISCUSSION_TITLES.length],
      content: DISCUSSION_BODIES[i % DISCUSSION_BODIES.length],
      status: resolved ? "RESOLVED" : "OPEN",
      createdAt: ts(created, "16:20:00"),
    });
    const replyCount = rng.int(1, 4);
    for (let r = 0; r < replyCount; r += 1) {
      const replyDay = iso(addDays(d(created), rng.int(1, 14)));
      if (replyDay > REF) continue;
      const isStaff = r === 0 && rng.bool(0.55);
      const replyAuthor = isStaff
        ? rng.pick([...contentManagers, ...teachers]).email
        : learners[(i + r + 5) % learners.length].email;
      discussionReplies.push({
        marker: MARKER,
        naturalKey: `${nk}|reply-${r + 1}`,
        parentNaturalKey: nk,
        authorEmail: replyAuthor,
        content: isStaff ? rng.pick(REPLY_BODIES_TEACHER) : rng.pick(REPLY_BODIES_LEARNER),
        createdAt: ts(replyDay, "18:45:00"),
      });
    }
  }
  return { discussions, discussionReplies };
}

export function buildDiscussionReactions(rng, MARKER, discussions, discussionReplies, learners, teachers, contentManagers) {
  const discussionReactions = [];
  const posts = [
    ...discussions.map((d) => ({ naturalKey: d.naturalKey, createdAt: d.createdAt })),
    ...discussionReplies.map((r) => ({ naturalKey: r.naturalKey, createdAt: r.createdAt })),
  ];
  const allUsers = [...learners, ...teachers, ...contentManagers];
  const used = new Set();
  for (const post of posts) {
    const count = rng.int(0, 12);
    const shuffled = rng.shuffle(allUsers);
    for (let i = 0; i < count && i < shuffled.length; i += 1) {
      const email = shuffled[i].email;
      const key = `${post.naturalKey}|${email}`;
      if (used.has(key)) continue;
      used.add(key);
      discussionReactions.push({
        marker: MARKER,
        postNaturalKey: post.naturalKey,
        userEmail: email,
        reactionType: rng.pick(REACTION_TYPES),
        helpful: rng.bool(0.25),
      });
    }
  }
  return discussionReactions;
}

export function buildDiscussionReports(rng, MARKER, REF, discussions, discussionReplies, learners, contentManagers) {
  const discussionReports = [];
  const posts = [
    ...discussions.map((d) => d.naturalKey),
    ...discussionReplies.slice(0, 15).map((r) => r.naturalKey),
  ];
  const reasons = [
    "Nội dung quảng cáo khóa học bên ngoài, không liên quan bài học.",
    "Ngôn từ thiếu tôn trọng khi phản bác ý kiến.",
    "Thảo luận lệch sang chủ đề chính trị.",
    "Spam link Telegram nhiều lần.",
    "Gửi tin nhắn riêng gây khó chịu sau khi đã từ chối.",
  ];
  for (let i = 0; i < 12 && i < posts.length; i += 1) {
    const created = iso(addDays(d("2026-06-10"), rng.int(0, 90)));
    if (created > REF) continue;
    const status = rng.pick(["PENDING", "DISMISSED", "ACTION_TAKEN"]);
    const reviewer = status === "PENDING" ? null : contentManagers[i % contentManagers.length].email;
    const reviewedDay = status === "PENDING" ? null : iso(addDays(d(created), rng.int(1, 5)));
    discussionReports.push({
      marker: MARKER,
      naturalKey: `demo-disc-report-${String(i + 1).padStart(2, "0")}`,
      postNaturalKey: posts[i],
      reporterEmail: learners[(i + 7) % learners.length].email,
      reason: reasons[i % reasons.length],
      reasonCategory: REPORT_CATEGORIES[i % REPORT_CATEGORIES.length],
      status,
      reviewedByEmail: reviewer,
      reviewedAt: reviewedDay ? ts(reviewedDay, "11:00:00") : null,
      actionNote: status === "PENDING" ? null : status === "DISMISSED" ? "Nội dung trong phạm vi trao đổi học thuật." : "Đã ẩn bài và nhắc nhở thành viên.",
      createdAt: ts(created, "08:10:00"),
    });
  }
  return discussionReports;
}

export function buildTeacherFeedback(rng, MARKER, REF, classes, enrollments) {
  const teacherFeedback = [];
  const strengths = [
    "Giảng dễ hiểu, ví dụ gần gũi với học viên đi làm.",
    "Tạo không khí lớp thoải mái, khuyến khích speaking.",
    "Chấm bài chi tiết, chỉ rõ lỗi ngữ pháp và cách sửa.",
  ];
  const improvements = [
    "Nên thêm 5 phút recap cuối buổi cho học viên mới vào giữa khóa.",
    "Một số buổi slide hơi dày, có thể chia nhỏ activity.",
    "Phần listening nên có thêm một lần chép chính tả ngắn.",
  ];
  const paceOptions = ["TOO_SLOW", "JUST_RIGHT", "TOO_FAST"];
  let n = 0;
  for (const cls of classes) {
    if (cls.status === "UPCOMING") continue;
    const classEnr = enrollments.filter((e) => e.classCode === cls.code);
    for (const enr of classEnr) {
      if (n >= 30) break;
      if (!rng.bool(0.35)) continue;
      const submitted = iso(addDays(d(cls.startDate), rng.int(20, 55)));
      if (submitted > REF) continue;
      teacherFeedback.push({
        marker: MARKER,
        naturalKey: `demo-tfb-${String(n + 1).padStart(3, "0")}`,
        classCode: cls.code,
        learnerEmail: enr.learnerEmail,
        teacherEmail: cls.primaryTeacherEmail,
        clarityScore: rng.int(3, 5),
        engagementScore: rng.int(3, 5),
        learnerSupportScore: rng.int(3, 5),
        feedbackTimelinessScore: rng.int(3, 5),
        professionalismScore: rng.int(4, 5),
        pace: rng.pick(paceOptions),
        wouldRecommend: rng.bool(0.82),
        strengths: rng.pick(strengths),
        improvementSuggestions: rng.pick(improvements),
        additionalComment: rng.bool(0.4) ? "Em sẽ tiếp tục học khóa nâng cao tại trung tâm." : null,
        submittedAt: ts(submitted, "21:00:00"),
      });
      n += 1;
    }
    if (n >= 30) break;
  }
  return teacherFeedback;
}

export function buildAnnouncements(rng, MARKER, REF, classes) {
  const announcements = [];
  const templates = [
    { title: "Nhắc nộp bài Writing tuần này", content: "Hạn nộp 23:59 Chủ nhật. Nộp trên hệ thống, không gửi email riêng." },
    { title: "Đổi phòng học buổi {{date}}", content: "Buổi tối nay chuyển sang phòng lớn hơn do bảo trì máy lạnh. Vui lòng đến sớm 10 phút." },
    { title: "Bổ sung tài liệu Listening", content: "Giáo viên đã tải thêm transcript Part 3 trong mục Tài liệu lớp." },
    { title: "Lịch nghỉ lễ Quốc khánh", content: "Lớp không học ngày 02/09. Buổi bù sẽ thông báo sau trên lịch lớp." },
    { title: "Mock test giữa khóa", content: "Tuần sau có bài mock 60 phút. Mang laptop đầy pin hoặc dùng máy trung tâm." },
  ];
  let n = 0;
  for (const cls of classes) {
    const count = rng.int(2, 4);
    for (let a = 0; a < count && n < 45; a += 1) {
      const created = iso(addDays(d(cls.startDate), rng.int(3, 40)));
      if (created > REF) continue;
      const tpl = templates[(n + a) % templates.length];
      announcements.push({
        marker: MARKER,
        naturalKey: `demo-ann-${String(n + 1).padStart(3, "0")}`,
        classCode: cls.code,
        title: tpl.title,
        content: tpl.content.replace("{{date}}", created),
        createdByEmail: cls.primaryTeacherEmail,
        createdAt: ts(created, "17:00:00"),
      });
      n += 1;
    }
  }
  return announcements;
}

export function buildBroadcasts(rng, MARKER, REF, admins) {
  const broadcasts = [];
  const admin = admins[0]?.email || `demo.admin.01@englishlab.local`;
  const items = [
    { title: "Khai giảng khóa mùa thu", message: "Chào mừng học viên đợt September 2026. Kiểm tra lịch lớp trên portal.", targetRole: "LEARNER", status: "SENT" },
    { title: "Cập nhật quy trình học phí", message: "Từ 01/09, biên lai chuyển khoản cần ghi rõ mã lớp.", targetRole: "STAFF", status: "SENT" },
    { title: "Nhắc giáo viên điểm danh", message: "Vui lòng chốt điểm danh trong 24h sau buổi học.", targetRole: "TEACHER", status: "SENT" },
    { title: "Bảo trì hệ thống", message: "Portal offline 02:00–04:00 ngày 20/09.", targetRole: null, status: "SCHEDULED", scheduledAt: "2026-09-20T02:00:00" },
    { title: "Workshop CM tháng 10", message: "Họp rà soát khóa online draft.", targetRole: "STAFF", status: "DRAFT" },
    { title: "Ưu đãi tháng 9", message: "Mã DEMO_SEP15 giảm 15% khóa online.", targetRole: "LEARNER", status: "SCHEDULED", scheduledAt: "2026-09-25T08:00:00" },
    { title: "Thông báo nội bộ Q4", message: "Kế hoạch mở lớp Q4 — draft.", targetRole: "STAFF", status: "DRAFT" },
    { title: "Kết quả khảo sát giáo viên", message: "Cảm ơn giáo viên đã phản hồi khảo sát tháng 8.", targetRole: "TEACHER", status: "SENT" },
  ];
  for (let i = 0; i < items.length; i += 1) {
    const it = items[i];
    const created = iso(addDays(d("2026-06-01"), i * 10));
    const recipientCount = rng.int(40, 120);
    const inApp = it.status === "SENT" ? rng.int(Math.floor(recipientCount * 0.7), recipientCount) : 0;
    broadcasts.push({
      marker: MARKER,
      naturalKey: `demo-bcast-${String(i + 1).padStart(2, "0")}`,
      title: it.title,
      message: it.message,
      targetRole: it.targetRole,
      status: it.status,
      scheduledAt: it.scheduledAt || (it.status === "SCHEDULED" ? `${created}T08:00:00` : null),
      sentAt: it.status === "SENT" ? ts(created, "09:00:00") : null,
      recipientCount,
      inAppSuccessCount: inApp,
      createdByEmail: admin,
      createdAt: ts(created, "08:30:00"),
    });
  }
  return broadcasts;
}

export function buildTicketMessages(rng, MARKER, tickets, staff) {
  const ticketMessages = [];
  const learnerBodies = [
    "Em đã thử đăng xuất và vào lại nhưng video vẫn báo lỗi 403.",
    "Mã order em là {{order}}, ngân hàng đã trừ tiền sáng nay.",
    "Em có thể chuyển sang ca T3-5-7 ca 2 không ạ?",
  ];
  const staffBodies = [
    "Chào bạn, mình đã reset quyền truy cập khóa học. Bạn thử lại giúp nhé.",
    "Mình đã đối soát — khóa học đã kích hoạt. Nếu vẫn lỗi gửi screenshot.",
    "Lịch ca 2 hiện full, mình ghi nhận và sẽ gọi tư vấn trong hôm nay.",
    "Ticket đã xử lý xong. Cảm ơn bạn đã kiên nhẫn phối hợp.",
  ];
  for (const ticket of tickets) {
    const msgCount = rng.int(2, 4);
    const startDay = ticket.createdAt.slice(0, 10);
    for (let m = 0; m < msgCount; m += 1) {
      const day = iso(addDays(d(startDay), m));
      const fromLearner = m % 2 === 0;
      const body = fromLearner
        ? rng.pick(learnerBodies).replace("{{order}}", String(900000 + rng.int(1, 50)))
        : staffBodies[Math.min(m, staffBodies.length - 1)];
      if (!fromLearner && ticket.status === "RESOLVED" && m === msgCount - 1) {
        ticketMessages.push({
          marker: MARKER,
          naturalKey: `${ticket.naturalKey}|msg-${m + 1}`,
          ticketNaturalKey: ticket.naturalKey,
          authorEmail: staff[rng.int(0, staff.length - 1)].email,
          body: staffBodies[3],
          createdAt: ticket.closedAt || ts(day, "15:30:00"),
        });
      } else {
        ticketMessages.push({
          marker: MARKER,
          naturalKey: `${ticket.naturalKey}|msg-${m + 1}`,
          ticketNaturalKey: ticket.naturalKey,
          authorEmail: fromLearner ? ticket.learnerEmail : ticket.assigneeEmail,
          body,
          createdAt: ts(day, fromLearner ? "10:00:00" : "14:20:00"),
        });
      }
    }
  }
  return ticketMessages;
}

export function buildChangeRequests(rng, MARKER, REF, WINDOW_FROM, classes, enrollments, learners, teachers, staff, managers) {
  const changeRequests = [];
  const reasons = [
    "Sinh viên đại học đổi lịch thi giữa kỳ, cần chuyển ca học.",
    "Phòng hiện tại quá nóng, đề nghị phòng có máy lạnh ổn định hơn.",
    "Giáo viên bận công tác tuần 38, đề nghị giáo viên thay thế tạm thời.",
    "Buổi ngày 15/08 trùng lịch họp công ty, xin hủy buổi và học bù.",
    "Học viên chuyển công tác sang chi nhánh khác, xin chuyển lớp cùng chương trình.",
  ];
  for (let i = 0; i < 20; i += 1) {
    const cls = classes[i % classes.length];
    const created = iso(addDays(d(WINDOW_FROM), rng.int(10, 95)));
    if (created > REF) continue;
    const type = CHANGE_TYPES[i % CHANGE_TYPES.length];
    const rolePick = rng.pick(["TEACHER", "STAFF", "LEARNER"]);
    let requesterEmail;
    if (rolePick === "TEACHER") requesterEmail = cls.primaryTeacherEmail;
    else if (rolePick === "STAFF") requesterEmail = staff[i % staff.length].email;
    else {
      const enr = enrollments.find((e) => e.classCode === cls.code);
      requesterEmail = enr?.learnerEmail || learners[i % learners.length].email;
    }
    const status = rng.pick(["PENDING", "APPROVED", "REJECTED", "APPLIED"]);
    const reviewer = status === "PENDING" ? null : (staff[(i + 1) % staff.length].email || managers[0].email);
    const reviewedDay = status === "PENDING" ? null : iso(addDays(d(created), rng.int(1, 4)));
    changeRequests.push({
      marker: MARKER,
      naturalKey: `demo-cr-${String(i + 1).padStart(3, "0")}`,
      requestType: type,
      requesterEmail,
      requesterRole: rolePick,
      classCode: cls.code,
      reason: reasons[i % reasons.length],
      status,
      reviewerEmail: reviewer,
      reviewedAt: reviewedDay ? ts(reviewedDay, "13:00:00") : null,
      reviewNote: status === "PENDING" ? null : status === "REJECTED" ? "Không đủ điều kiện chuyển lớp trong tuần này." : "Đã sắp xếp theo yêu cầu.",
      createdAt: ts(created, "09:40:00"),
    });
  }
  return changeRequests;
}

export function buildAttendanceDisputes(rng, MARKER, REF, attendance, classes) {
  const attendanceDisputes = [];
  const absentRows = attendance.filter((a) => a.status === "ABSENT");
  const picked = rng.shuffle(absentRows).slice(0, 12);
  const reasons = [
    "Em đã gửi đơn xin phép vì ốm nhưng hệ thống vẫn ghi vắng.",
    "Em có mặt nhưng quên check-in QR — có log camera cửa lớp.",
    "Buổi đó em tham gia online do trùng lịch công tác đột xuất.",
  ];
  for (let i = 0; i < picked.length; i += 1) {
    const row = picked[i];
    const cls = classes.find((c) => c.code === row.classCode);
    const status = rng.pick(["PENDING", "APPROVED", "REJECTED"]);
    const created = row.sessionDate;
    const reviewedDay = status === "PENDING" ? null : iso(addDays(d(created), rng.int(1, 3)));
    attendanceDisputes.push({
      marker: MARKER,
      attendanceNaturalKey: `${row.classCode}|${row.sessionDate}|${row.startTime}|${row.learnerEmail}`,
      disputeReason: reasons[i % reasons.length],
      disputeStatus: status,
      disputeReviewNote: status === "PENDING" ? null : status === "APPROVED" ? "Đã xác nhận có đơn xin phép hợp lệ." : "Không có minh chứng bổ sung.",
      disputeReviewedByEmail: status === "PENDING" ? null : cls?.primaryTeacherEmail,
      disputeReviewedAt: reviewedDay ? ts(reviewedDay, "10:00:00") : null,
    });
  }
  return attendanceDisputes;
}

export function buildCourseListItems(rng, MARKER, REF, WINDOW_FROM, publishedCourses, onlineEnrollments, learners) {
  const courseListItems = [];
  const enrolledPairs = new Set(onlineEnrollments.map((e) => `${e.learnerEmail}|${e.courseSlug}`));
  let wi = 0;
  let ci = 0;
  for (let i = 0; i < 25; i += 1) {
    const learner = learners[(i * 2) % learners.length];
    const course = publishedCourses[i % publishedCourses.length];
    const key = `${learner.email}|${course.slug}`;
    if (enrolledPairs.has(key) && rng.bool(0.7)) continue;
    const added = iso(addDays(d(WINDOW_FROM), rng.int(5, 100)));
    if (added > REF) continue;
    courseListItems.push({
      marker: MARKER,
      naturalKey: `demo-wish-${String(wi + 1).padStart(3, "0")}`,
      learnerEmail: learner.email,
      courseSlug: course.slug,
      listType: "WISHLIST",
      addedAt: ts(added, "12:00:00"),
    });
    wi += 1;
  }
  for (let i = 0; i < 12; i += 1) {
    const learner = learners[(i * 5 + 3) % learners.length];
    const course = publishedCourses[(i + 2) % publishedCourses.length];
    const added = iso(addDays(d(WINDOW_FROM), rng.int(10, 90)));
    if (added > REF) continue;
    courseListItems.push({
      marker: MARKER,
      naturalKey: `demo-cart-${String(ci + 1).padStart(3, "0")}`,
      learnerEmail: learner.email,
      courseSlug: course.slug,
      listType: "CART",
      addedAt: ts(added, "20:00:00"),
    });
    ci += 1;
  }
  return courseListItems;
}

export function buildTuition(rng, MARKER, REF, enrollments, classes, staff) {
  const tuitionPayments = [];
  const tuitionProofs = [];
  const kinds = ["DEPOSIT", "PARTIAL", "FULL", "MANUAL_CONFIRMATION"];
  let pi = 0;
  for (let i = 0; i < enrollments.length && tuitionPayments.length < 60; i += 1) {
    const enr = enrollments[i];
    const cls = classes.find((c) => c.code === enr.classCode);
    if (!cls) continue;
    const created = enr.enrolledAt.slice(0, 10);
    const kind = kinds[i % kinds.length];
    const amount =
      kind === "FULL" || kind === "MANUAL_CONFIRMATION"
        ? cls.tuitionFeeVnd
        : kind === "DEPOSIT"
          ? Math.round(cls.tuitionFeeVnd * 0.3)
          : Math.round(cls.tuitionFeeVnd * 0.5);
    tuitionPayments.push({
      marker: MARKER,
      naturalKey: `demo-tuit-${String(tuitionPayments.length + 1).padStart(3, "0")}`,
      classCode: enr.classCode,
      learnerEmail: enr.learnerEmail,
      amount,
      paymentKind: kind,
      note: kind === "MANUAL_CONFIRMATION" ? "Xác nhận chuyển khoản qua sao kê." : null,
      recordedByEmail: staff[i % staff.length].email,
      createdAt: enr.enrolledAt,
    });
    if (pi < 28 && (kind === "DEPOSIT" || kind === "PARTIAL" || kind === "FULL") && rng.bool(0.55)) {
      const status = rng.pick(["PENDING", "CONFIRMED", "REJECTED"]);
      const proofDay = iso(addDays(d(created), rng.int(0, 3)));
      tuitionProofs.push({
        marker: MARKER,
        naturalKey: `demo-tuit-proof-${String(pi + 1).padStart(3, "0")}`,
        tuitionPaymentNaturalKey: tuitionPayments[tuitionPayments.length - 1].naturalKey,
        fileUrl: `master-demo://tuition-proof/${enr.classCode}/${enr.learnerEmail}/${pi + 1}.jpg`,
        status,
        reviewedByEmail: status === "PENDING" ? null : staff[(i + 1) % staff.length].email,
        reviewedAt: status === "PENDING" ? null : ts(proofDay, "16:00:00"),
        createdAt: ts(proofDay, "11:00:00"),
      });
      pi += 1;
    }
  }
  return { tuitionPayments, tuitionProofs };
}

export function buildTeacherCredentials(rng, MARKER, teachers, managers) {
  const teacherCredentials = [];
  const templates = [
    { type: "CERTIFICATE", title: "CELTA", issuer: "Cambridge Assessment English" },
    { type: "CERTIFICATE", title: "TESOL 120h", issuer: "International TEFL Academy" },
    { type: "DEGREE", title: "Cử nhân Sư phạm Tiếng Anh", issuer: "ĐH Ngoại ngữ - ĐHQG Hà Nội" },
    { type: "LANGUAGE_SCORE", title: "IELTS 8.0", issuer: "British Council" },
    { type: "LANGUAGE_SCORE", title: "TOEIC 950", issuer: "ETS" },
  ];
  for (let i = 0; i < 16; i += 1) {
    const teacher = teachers[i % teachers.length];
    const tpl = templates[i % templates.length];
    const issued = iso(addDays(d("2015-01-01"), i * 120));
    const verified = rng.bool(0.75);
    teacherCredentials.push({
      marker: MARKER,
      naturalKey: `demo-cred-${String(i + 1).padStart(2, "0")}`,
      teacherEmail: teacher.email,
      type: tpl.type,
      title: tpl.title,
      issuer: tpl.issuer,
      issuedDate: issued,
      verificationStatus: verified ? "VERIFIED" : "PENDING",
      verifiedByEmail: verified ? managers[i % managers.length].email : null,
    });
  }
  return teacherCredentials;
}

export function buildPracticeAttempts(rng, MARKER, REF, classes, enrollments) {
  const practiceAttempts = [];
  let n = 0;
  for (const cls of classes) {
    if (cls.status === "UPCOMING") continue;
    const enrList = enrollments.filter((e) => e.classCode === cls.code).slice(0, 4);
    for (const enr of enrList) {
      const attempts = rng.int(1, 3);
      let score = rng.int(45, 60);
      for (let a = 1; a <= attempts && n < 50; a += 1) {
        const totalQ = 20;
        score = Math.min(95, score + rng.int(5, 15));
        const correct = Math.round((score / 100) * totalQ);
        const day = iso(addDays(d(cls.startDate), 10 + a * 7));
        if (day > REF) continue;
        practiceAttempts.push({
          marker: MARKER,
          naturalKey: `${cls.code}|${enr.learnerEmail}|attempt-${a}`,
          classCode: cls.code,
          learnerEmail: enr.learnerEmail,
          attemptNumber: a,
          scorePercent: score,
          totalQuestions: totalQ,
          correctAnswers: correct,
          completedAt: ts(day, "19:00:00"),
        });
        n += 1;
      }
    }
  }
  return practiceAttempts;
}

export function buildFlashcardSets(MARKER, contentManagers) {
  const topics = [
    { code: "demo-fc-ielts-env", title: "IELTS Environment Collocations", examCategory: "IELTS", skill: "WRITING" },
    { code: "demo-fc-ielts-edu", title: "IELTS Education Vocabulary", examCategory: "IELTS", skill: "READING" },
    { code: "demo-fc-toeic-office", title: "TOEIC Office Phrases", examCategory: "TOEIC", skill: "LISTENING" },
    { code: "demo-fc-ielts-speaking", title: "IELTS Speaking Part 2 Cues", examCategory: "IELTS", skill: "SPEAKING" },
    { code: "demo-fc-toeic-travel", title: "TOEIC Travel & Logistics", examCategory: "TOEIC", skill: "READING" },
    { code: "demo-fc-comm-email", title: "Business Email Openers", examCategory: "COMMUNICATION", skill: "WRITING" },
    { code: "demo-fc-ielts-listening", title: "IELTS Listening Map Labels", examCategory: "IELTS", skill: "LISTENING" },
    { code: "demo-fc-foundation-grammar", title: "Foundation Phrasal Verbs", examCategory: "FOUNDATION", skill: "GRAMMAR" },
  ];
  const flashcardSets = topics.map((t, idx) => {
    const cards = [];
    const baseTerms = [
      ["sustainable development", "phát triển bền vững", "Governments promote sustainable development."],
      ["urban sprawl", "đô thị hóa lan rộng", "Urban sprawl increases commute times."],
      ["biodiversity", "đa dạng sinh học", "Forests protect biodiversity."],
      ["emission", "khí thải", "Factories must cut emissions."],
      ["conservation", "bảo tồn", "Wildlife conservation requires funding."],
      ["infrastructure", "cơ sở hạ tầng", "Public infrastructure needs investment."],
      ["legislation", "pháp luật", "New legislation targets plastic waste."],
      ["mitigate", "giảm nhẹ", "Trees mitigate flood risk."],
      ["advocate", "ủng hộ", "She advocates for green policies."],
      ["implement", "triển khai", "Schools implement recycling programs."],
    ];
    for (let c = 0; c < rngCardCount(idx); c += 1) {
      const [term, meaning, example] = baseTerms[(idx + c) % baseTerms.length];
      cards.push({ term: `${term}-${c + 1}`, meaning, example });
    }
    return {
      marker: MARKER,
      code: t.code,
      title: t.title,
      examCategory: t.examCategory,
      skill: t.skill,
      status: "PUBLISHED",
      createdByEmail: contentManagers[idx % contentManagers.length].email,
      cards,
    };
  });
  return flashcardSets;
}

function rngCardCount(idx) {
  return 8 + (idx % 5);
}

export function buildCenterLibrary(rng, MARKER, REF, WINDOW_FROM, contentManagers) {
  const centerLibrary = [];
  const items = [
    { title: "IELTS Writing Task 2 Band Descriptors (VN)", fileType: "PDF", examCategory: "IELTS", skill: "WRITING" },
    { title: "TOEIC Part 5 Grammar Cheat Sheet", fileType: "PDF", examCategory: "TOEIC", skill: "GRAMMAR" },
    { title: "Listening Note-taking Template", fileType: "DOCX", examCategory: "IELTS", skill: "LISTENING" },
    { title: "Office Small Talk Audio Pack", fileType: "ZIP", examCategory: "COMMUNICATION", skill: "SPEAKING" },
  ];
  for (let i = 0; i < 25; i += 1) {
    const tpl = items[i % items.length];
    const created = iso(addDays(d(WINDOW_FROM), rng.int(0, 80)));
    centerLibrary.push({
      marker: MARKER,
      naturalKey: `demo-lib-${String(i + 1).padStart(3, "0")}`,
      title: `${tpl.title} — bản ${i + 1}`,
      description: "Tài liệu dùng chung cho giáo viên và học viên tại trung tâm.",
      fileUrl: `master-demo://center-library/${i + 1}.${tpl.fileType.toLowerCase()}`,
      fileType: tpl.fileType,
      examCategory: tpl.examCategory,
      skill: tpl.skill,
      status: "PUBLISHED",
      createdByEmail: contentManagers[i % contentManagers.length].email,
      createdAt: ts(created, "10:00:00"),
    });
  }
  return centerLibrary;
}

export function buildLessonNotes(rng, MARKER, REF, onlineEnrollments) {
  const lessonNotes = [];
  const snippets = [
    "Ghi nhớ: topic sentence phải trả lời trực tiếp câu hỏi đề.",
    "Collocation: heavy traffic / rush hour — không dùng strong traffic.",
    "Part 2: dùng cụm 'What left a deep impression on me was...'",
    "Listening: chú ý paraphrase — affordable ≈ budget-friendly.",
  ];
  const eligible = onlineEnrollments.filter((e) => e.progressPercent >= 15);
  for (let i = 0; i < 30; i += 1) {
    const enr = eligible[i % eligible.length];
    if (!enr) break;
    const created = iso(addDays(d(enr.enrolledAt.slice(0, 10)), rng.int(3, 30)));
    if (created > REF) continue;
    lessonNotes.push({
      marker: MARKER,
      naturalKey: `demo-note-${String(i + 1).padStart(3, "0")}`,
      learnerEmail: enr.learnerEmail,
      courseSlug: enr.courseSlug,
      lessonIndex: 1 + (i % 8),
      content: snippets[i % snippets.length],
      createdAt: ts(created, "22:10:00"),
    });
  }
  return lessonNotes;
}

export function buildRichNotifications(rng, MARKER, REF, WINDOW_FROM, learners, classes, onlineCourses) {
  const notifications = [];
  const templates = [
    { title: "Ghi danh lớp thành công", body: "Bạn đã được xếp vào lớp {{class}}. Xem lịch trong mục Lớp học.", actionPath: "/learner/classrooms", kind: "enrollment" },
    { title: "Thanh toán khóa online thành công", body: "Khóa {{course}} đã mở. Bắt đầu từ bài 1 trong workspace.", actionPath: "/learner/courses", kind: "payment" },
    { title: "Giáo viên đã chấm bài tập", body: "Bài Writing tuần này đã có điểm. Vào Bài tập để xem nhận xét.", actionPath: "/learner/notifications", kind: "homework" },
    { title: "Phản hồi ticket hỗ trợ", body: "Nhân viên đã trả lời ticket của bạn. Mở Hỗ trợ để xem chi tiết.", actionPath: "/learner/support", kind: "support" },
    { title: "Thông báo mới từ lớp", body: "{{class}}: {{snippet}}", actionPath: "/learner/classrooms", kind: "announcement" },
    { title: "Nhắc deadline bài tập", body: "Hạn nộp bài Listening drill là 23:59 tối nay.", actionPath: "/learner/notifications", kind: "homework" },
    { title: "Khóa học sắp hoàn thành", body: "Bạn đã đạt {{pct}}% khóa {{course}}. Hoàn thành bài cuối để nhận chứng nhận.", actionPath: "/learner/courses", kind: "enrollment" },
    { title: "Cập nhật điểm danh", body: "Buổi học hôm qua đã được cập nhật. Kiểm tra nếu cần khiếu nại.", actionPath: "/learner/notifications", kind: "announcement" },
  ];
  for (let i = 0; i < 100; i += 1) {
    const created = iso(addDays(d(WINDOW_FROM), rng.int(0, 105)));
    if (created > REF) continue;
    const learner = learners[i % learners.length];
    const tpl = templates[i % templates.length];
    const cls = classes[i % classes.length];
    const course = onlineCourses[i % onlineCourses.length];
    const read = rng.bool(0.55);
    const body = tpl.body
      .replace("{{class}}", cls.code)
      .replace("{{course}}", course.title)
      .replace("{{pct}}", String(rng.int(70, 95)))
      .replace("{{snippet}}", "Bổ sung tài liệu tuần 5 trong mục Thông báo lớp.");
    notifications.push({
      marker: MARKER,
      naturalKey: `demo-notif-${String(i + 1).padStart(3, "0")}`,
      recipientEmail: learner.email,
      title: tpl.title,
      body,
      actionPath: tpl.actionPath,
      eventKind: tpl.kind,
      createdAt: ts(created, "07:00:00"),
      read,
      readAt: read ? ts(iso(addDays(d(created), rng.int(0, 2))), "12:00:00") : null,
    });
  }
  return notifications;
}

export function buildInstructorLedSyllabus(MARKER, instructorLedPrograms) {
  const ieltsUnits = [
    ["Unit 1 · Listening Section 1-2", "Form completion, map labelling, hội thoại đời thường.", "Nghe lấy thông tin cụ thể và điền form đúng spelling."],
    ["Unit 2 · Listening Section 3-4", "Academic dialogue và lecture notes.", "Ghi chú bài giảng, nhận biết paraphrase."],
    ["Unit 3 · Reading True/False/Not Given", "Skimming, scanning, phân biệt NG với False.", "Đối chiếu proposition với đoạn văn."],
    ["Unit 4 · Reading Matching Headings", "Matching headings, summary completion.", "Tóm ý đoạn và loại heading nhiễu."],
    ["Unit 5 · Writing Task 1", "Biểu đồ, quy trình, so sánh số liệu.", "Viết overview và chọn số liệu then chốt."],
    ["Unit 6 · Writing Task 2", "Opinion, discussion, problem-solution.", "Lập dàn ý 4 đoạn và paraphrase đề."],
    ["Unit 7 · Speaking Part 1-2", "Câu hỏi đời thường và cue card 2 phút.", "Kéo dài câu trả lời bằng example và reason."],
    ["Unit 8 · Speaking Part 3 & Mock", "Câu hỏi trừu tượng, mock 4 kỹ năng.", "Phản hồi abstract idea và tự chấm theo band."],
  ];
  const toeicUnits = [
    ["Unit 1 · Photographs & Q-R", "Part 1 photographs, Part 2 question-response.", "Loại đáp án nhiễu về thì và từ đồng âm."],
    ["Unit 2 · Conversations", "Part 3 hội thoại, bảng biểu.", "Nghe mục đích, chi tiết và implied meaning."],
    ["Unit 3 · Talks", "Part 4 announcement, voicemail.", "Bắt topic sentence và số liệu trong talk."],
    ["Unit 4 · Incomplete sentences", "Part 5 grammar: thì, giới từ, word form.", "Chọn từ loại đúng trong 20 giây/câu."],
    ["Unit 5 · Text completion", "Part 6 điền từ trong email/thông báo.", "Đọc ngữ cảnh trước-sau chỗ trống."],
    ["Unit 6 · Reading single passages", "Part 7 bài đơn: email, article, advert.", "Câu inference và vocabulary in context."],
    ["Unit 7 · Reading double-triple", "Part 7 bộ 2-3 văn bản.", "Nối thông tin giữa các văn bản."],
    ["Unit 8 · Mini mock 650+", "Đề rút gọn Listening + Reading.", "Quản lý thời gian và review lỗi sai."],
  ];
  const communicationUnits = [
    ["Unit 1 · Small talk & introductions", "Chào hỏi, giới thiệu bản thân trong môi trường công sở.", "Dùng câu mở đầu tự nhiên và lịch sự."],
    ["Unit 2 · Meetings basics", "Agenda, turn-taking, summarizing.", "Tóm tắt quyết định sau cuộc họp."],
    ["Unit 3 · Email writing", "Subject line, tone, request rõ ràng.", "Viết email ngắn gọn, action-oriented."],
    ["Unit 4 · Phone & video calls", "Clarify, confirm, follow-up.", "Xác nhận thông tin và kết thúc cuộc gọi chuyên nghiệp."],
    ["Unit 5 · Presenting ideas", "Structure, signposting, Q&A.", "Trình bày 3 điểm chính trong 3 phút."],
    ["Unit 6 · Negotiation soft skills", "Propose, concede, close.", "Đưa đề xuất và xử lý phản đối lịch sự."],
  ];

  const courseUnits = [];
  for (const program of instructorLedPrograms || []) {
    const cat = (program.category || "IELTS").toUpperCase();
    let catalog = ieltsUnits;
    if (cat.includes("TOEIC")) catalog = toeicUnits;
    else if (cat.includes("COMM") || cat.includes("BUSINESS")) catalog = communicationUnits;

    // Speaking studio: leaner syllabus focused on speaking units
    if ((program.code || "").includes("SPEAKING")) {
      catalog = [
        ["Unit 1 · Fluency warm-up", "Tăng tốc độ nói và giảm pause.", "Nói liền mạch 1 phút không dừng đột ngột."],
        ["Unit 2 · Pronunciation focus", "Stress, linking, /θ/ /ð/.", "Phát âm rõ keyword trong câu trả lời."],
        ["Unit 3 · Part 1 routines", "Câu hỏi đời thường, mở rộng ý.", "Trả lời 3 câu trong 40–50 giây/câu."],
        ["Unit 4 · Part 2 cue cards", "Outline 1 phút, nói 2 phút.", "Dùng example cá nhân hợp lý."],
        ["Unit 5 · Part 3 discussion", "Abstract ideas, compare/contrast.", "Đưa quan điểm có lý do và ví dụ."],
        ["Unit 6 · Mock speaking test", "Full mock với feedback.", "Tự đánh giá fluency/coherence."],
      ];
    }

    const lessonFocuses = ["Warm-up & input", "Guided practice", "Review & homework"];
    for (let u = 0; u < catalog.length; u += 1) {
      const [title, description, objective] = catalog[u];
      const baseSkill = title.replace(/^Unit \d+ · /, "");
      const lessons = [];
      for (let s = 1; s <= lessonFocuses.length; s += 1) {
        lessons.push({
          sequenceNumber: s,
          title: `${baseSkill} · ${lessonFocuses[s - 1]}`,
          description,
          learningObjectives: objective,
          plannedSessionCount: 1,
        });
      }
      courseUnits.push({
        marker: MARKER,
        programCode: program.code,
        sequenceNumber: u + 1,
        title,
        description,
        learningObjectives: objective,
        lessons,
      });
    }
  }
  return courseUnits;
}

export function assembleGapWorld(ctx) {
  const {
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
    instructorLedPrograms,
  } = ctx;

  const publishedCourses = onlineCourses.filter((c) => c.status === "PUBLISHED");

  patchOnlineEnrollmentsAndPayments(rng, WINDOW_FROM, onlineCourses, onlineEnrollments, lessonProgressPlan, payments, learners, REF);

  const { discussions, discussionReplies } = buildDiscussions(
    rng,
    MARKER,
    REF,
    publishedCourses,
    learners,
    teachers,
    contentManagers,
  );
  const discussionReactions = buildDiscussionReactions(
    rng,
    MARKER,
    discussions,
    discussionReplies,
    learners,
    teachers,
    contentManagers,
  );
  const discussionReports = buildDiscussionReports(
    rng,
    MARKER,
    REF,
    discussions,
    discussionReplies,
    learners,
    contentManagers,
  );
  const teacherFeedback = buildTeacherFeedback(rng, MARKER, REF, classes, enrollments);
  const announcements = buildAnnouncements(rng, MARKER, REF, classes);
  const broadcasts = buildBroadcasts(rng, MARKER, REF, admins);
  const ticketMessages = buildTicketMessages(rng, MARKER, tickets, staff);
  const changeRequests = buildChangeRequests(rng, MARKER, REF, WINDOW_FROM, classes, enrollments, learners, teachers, staff, managers);
  const attendanceDisputes = buildAttendanceDisputes(rng, MARKER, REF, attendance, classes);
  const courseListItems = buildCourseListItems(rng, MARKER, REF, WINDOW_FROM, publishedCourses, onlineEnrollments, learners);
  const { tuitionPayments, tuitionProofs } = buildTuition(rng, MARKER, REF, enrollments, classes, staff);
  const teacherCredentials = buildTeacherCredentials(rng, MARKER, teachers, managers);
  const practiceAttempts = buildPracticeAttempts(rng, MARKER, REF, classes, enrollments);
  const flashcardSets = buildFlashcardSets(MARKER, contentManagers);
  const centerLibrary = buildCenterLibrary(rng, MARKER, REF, WINDOW_FROM, contentManagers);
  const lessonNotes = buildLessonNotes(rng, MARKER, REF, onlineEnrollments);
  const notifications = buildRichNotifications(rng, MARKER, REF, WINDOW_FROM, learners, classes, onlineCourses);
  const courseUnits = buildInstructorLedSyllabus(MARKER, instructorLedPrograms || []);

  return {
    discussions,
    discussionReplies,
    discussionReactions,
    discussionReports,
    teacherFeedback,
    announcements,
    broadcasts,
    ticketMessages,
    changeRequests,
    attendanceDisputes,
    courseListItems,
    tuitionPayments,
    tuitionProofs,
    teacherCredentials,
    practiceAttempts,
    flashcardSets,
    centerLibrary,
    lessonNotes,
    notifications,
    courseUnits,
  };
}
