const fs = require('fs');
const path = require('path');

const SRC = 'C:/Users/MinhDuc/Downloads/de thi';
const OUT = path.join(__dirname, '../../backend/src/main/resources/sheet-data');
const PUBLIC_ROOT = path.join(__dirname, '../../frontend/public/sheet-exams');

const SETS = ['2025de2', '2025de3', '2025de4', '2026de1', '2026de2'];

function decode(html) {
  return String(html || '')
    .replace(/&nbsp;/gi, ' ')
    .replace(/&amp;/gi, '&')
    .replace(/&quot;/gi, '"')
    .replace(/&#39;/g, "'")
    .replace(/&lt;/gi, '<')
    .replace(/&gt;/gi, '>')
    .replace(/&#(\d+);/g, (_, n) => String.fromCharCode(Number(n)));
}

function strip(html) {
  return decode(html)
    .replace(/<script[\s\S]*?<\/script>/gi, ' ')
    .replace(/<style[\s\S]*?<\/style>/gi, ' ')
    .replace(/<br\s*\/?>/gi, ' ')
    .replace(/<\/(p|div|li|tr|h\d)>/gi, ' ')
    .replace(/<[^>]+>/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function pageTitle(html) {
  return strip((html.match(/<title>([^<]+)<\/title>/i) || [])[1] || '')
    .replace(/\s*\|\s*IELTS Online Tests.*/i, '')
    .trim();
}

function pageUrl(html) {
  return (html.match(/property="og:url" content="([^"]+)"/i)
    || html.match(/rel="canonical" href="([^"]+)"/i)
    || html.match(/https?:\/\/[^\s"'<>]*ieltsonlinetests[^\s"'<>]*/i)
    || [])[1] || '';
}

function slugMeta(html) {
  const url = pageUrl(html);
  const match = url.match(/ielts-mock-test-(\d{4})-([a-z]+)-(listening|reading|writing|speaking)-practi[cs]e-test-(\d)/i);
  if (!match) return null;
  const month = match[2].charAt(0).toUpperCase() + match[2].slice(1).toLowerCase();
  return {
    year: match[1],
    month,
    skill: match[3].toLowerCase(),
    test: match[4],
    practise: /practise-test/i.test(url),
  };
}

function skillWord(skill) {
  return { L: 'Listening', R: 'Reading', W: 'Writing', S: 'Speaking' }[skill];
}

function setMeta(files) {
  const bySkill = {};
  for (const skill of ['S', 'R', 'W', 'L']) {
    bySkill[skill] = slugMeta(files[skill]) || null;
  }
  const fallback = bySkill.S || bySkill.R || bySkill.W || bySkill.L;
  return { bySkill, fallback };
}

function examTitle(files, skill, setId) {
  const html = files[skill];
  const title = pageTitle(html);
  if (title && !/^(Full IELTS|Take test)$/i.test(title)) return title;
  const meta = setMeta(files);
  const info = meta.bySkill[skill] || meta.fallback;
  if (info) {
    const practise = info.practise ? 'Practise' : 'Practice';
    return `IELTS Mock Test ${info.year} ${info.month} ${skillWord(skill)} ${practise} Test ${info.test}`;
  }
  return `IELTS mock ${setId} ${skillWord(skill)}`;
}

function examKey(files, skill, setId) {
  const meta = setMeta(files);
  const info = meta.bySkill[skill] || meta.fallback;
  if (info) {
    return `ielts_mock_${info.year}_${info.month.toLowerCase()}_${info.skill}_${info.practise ? 'practise' : 'practice'}_test_${info.test}`
      .replace(/listening|reading|writing|speaking/, skillWord(skill).toLowerCase());
  }
  return `iot_${setId}_${skillWord(skill).toLowerCase()}`;
}

function publicDirName(key) {
  return key.replace(/_/g, '-');
}

function splitGroups(html) {
  const re = /<h4 class="test-panel__question-title">([\s\S]*?)<\/h4>([\s\S]*?)(?=<h4 class="test-panel__question-title">|$)/gi;
  const groups = [];
  let m;
  while ((m = re.exec(html))) {
    const title = strip(m[1]);
    const hasQuestionMarkup = /iot-question|data-num=/.test(m[2]);
    if (!/^Questions?/i.test(title) && !hasQuestionMarkup) continue;
    groups.push({ title: /^Questions?/i.test(title) ? title : strip(m[1]) || 'Questions', body: m[2] });
  }
  return groups;
}

function optionFromSelect(selectHtml) {
  return [...selectHtml.matchAll(/<option[^>]*value="([^"]+)"[^>]*>([^<]*)<\/option>/gi)]
    .map((m) => ({ value: m[1], label: strip(m[2]) || m[1] }))
    .filter((o) => o.value);
}

function matchingBank(body) {
  const rows = [...body.matchAll(/<tr[\s\S]*?<b>([A-Z]\.)<\/b>[\s\S]*?<\/td>\s*<td[^>]*>([\s\S]*?)<\/td>/gi)];
  if (!rows.length) return [];
  return rows.map((m) => ({
    value: m[1].replace('.', ''),
    label: `${m[1].replace('.', '')} ${strip(m[2])}`.trim(),
  }));
}

function parseFillQuestions(body) {
  const questions = [];
  const re = /<span class="test-panel__iotquestion">[\s\S]*?data-num="(\d+)"[\s\S]*?<\/span>/gi;
  let m;
  let cursor = 0;
  const pieces = [];
  while ((m = re.exec(body))) {
    pieces.push({ num: Number(m[1]), before: body.slice(cursor, m.index), afterStart: m.index + m[0].length });
    cursor = m.index + m[0].length;
  }
  for (let i = 0; i < pieces.length; i++) {
    const before = strip(pieces[i].before).slice(-90);
    questions.push({
      number: pieces[i].num,
      promptBefore: before.replace(/^\d+\s*/, '') || `Question ${pieces[i].num}`,
      promptAfter: strip(body.slice(pieces[i].afterStart, pieces[i].afterStart + 140)).slice(0, 80),
    });
  }
  return questions;
}

function parseSelectInline(body) {
  const bank = matchingBank(body);
  const questions = [];
  const re = /<b class="iot-question-number">(\d+)\.?<\/b>\s*<select([\s\S]*?)<\/select>([\s\S]*?)(?=<b class="iot-question-number">|$)/gi;
  let m;
  while ((m = re.exec(body))) {
    const options = bank.length ? bank : optionFromSelect(`<select${m[2]}</select>`);
    questions.push({
      number: Number(m[1]),
      prompt: strip(m[3]).replace(/^[\s.]+/, '').replace(/\s*Part\s+\d+\s*$/i, '') || `Question ${m[1]}`,
      options: options.map((o) => (typeof o === 'string' ? o : o.label || o.value)),
    });
  }
  return { questions, typedOptions: bank };
}

function parseRadios(body) {
  const blocks = [...body.matchAll(/<div class="test-panel__question-sm-group"([^>]*)>([\s\S]*?)(?=<div class="test-panel__question-sm-group"|$)/gi)];
  return blocks.map((block) => {
    const num = Number((block[1].match(/data-num="(\d+)"/) || [])[1] || (block[2].match(/data-num="(\d+)"/) || [])[1]);
    const prompt = strip((block[2].match(/test-panel__question-sm-title[^>]*>([\s\S]*?)<\/div>/) || [])[1] || '');
    const options = [...block[2].matchAll(/<span class="test-panel__answer-option">([A-Z])<\/span>[\s\S]*?<\/span>\s*([\s\S]*?)<\/label>/gi)]
      .map((m) => ({ value: m[1], label: strip(m[2]) }));
    if (!options.length) {
      const alt = [...block[2].matchAll(/value="([A-Z])"[\s\S]*?<\/span>\s*<span class="cb-label">([\s\S]*?)<\/span>/gi)];
      alt.forEach((m) => options.push({ value: m[1], label: strip(m[2]) }));
    }
    return { number: num, prompt: prompt.replace(/^\d+\.\s*/, ''), options };
  }).filter((q) => q.number);
}

function expandNums(value) {
  const text = String(value || '');
  const range = text.match(/(\d+)\s*[-–]\s*(\d+)/);
  if (range) {
    const from = Number(range[1]);
    const to = Number(range[2]);
    return Array.from({ length: to - from + 1 }, (_, index) => from + index);
  }
  const one = Number(text);
  return Number.isFinite(one) && one > 0 ? [one] : [];
}

function parseMulti(body) {
  const rawNums = [...body.matchAll(/data-num="([^"]+)"/gi)].map((m) => m[1]);
  const uniqueRaw = [...new Set(rawNums)];
  const prompt = strip((body.match(/field--name-field-question[\s\S]*?field--item[^>]*>([\s\S]*?)<\/div>/) || [])[1] || '');
  const items = [...body.matchAll(/<span class="test-panel__answer-option">([A-Z])<\/span>[\s\S]*?data-num="([^"]+)"[\s\S]*?<span class="cb-label">([\s\S]*?)<\/span>/gi)];
  const grouped = new Map();
  for (const item of items) {
    const key = item[2];
    if (!grouped.has(key)) grouped.set(key, []);
    grouped.get(key).push({ value: item[1], label: strip(item[3]) });
  }
  if (uniqueRaw.length > 1 && uniqueRaw.every((value) => !String(value).includes('-'))) {
    return {
      mode: 'single',
      questions: uniqueRaw.map((raw) => ({
        number: Number(raw),
        prompt,
        options: grouped.get(raw) || [],
      })),
    };
  }
  const nums = uniqueRaw.flatMap(expandNums);
  const options = grouped.get(uniqueRaw[0]) || items.map((item) => ({ value: item[1], label: strip(item[3]) }));
  return { mode: 'multi', nums, options, prompt };
}

function toGroup(title, body) {
  const instructions = strip(
    (body.match(/field--name-field-question[\s\S]*?field--item[^>]*>([\s\S]*?)<\/div>/) || [])[1]
    || (body.match(/field--name-field-block-description[\s\S]*?field--item[^>]*>([\s\S]*?)<\/div>/) || [])[1]
    || (body.match(/<p><em>([\s\S]*?)<\/em><\/p>/) || [])[1]
    || title
  );
  if (/checkbox-iot/.test(body)) {
    const multi = parseMulti(body);
    if (multi.mode === 'single') {
      return { title, instructions, type: 'single_choice', questions: multi.questions };
    }
    return {
      title,
      instructions,
      type: 'multi_select_letters',
      questionNumbers: multi.nums,
      maxSelections: multi.nums.length || 2,
      options: multi.options,
      questions: multi.nums.map((number) => ({ number, prompt: multi.prompt })),
    };
  }
  if (/test-panel__question-sm-group/.test(body) && /radio-iot/.test(body)) {
    return { title, instructions, type: 'single_choice', questions: parseRadios(body) };
  }
  if (/<select/i.test(body)) {
    const parsed = parseSelectInline(body);
    const options = parsed.typedOptions.length
      ? parsed.typedOptions
      : (parsed.questions[0]?.options || []).map((label) => ({ value: String(label).charAt(0), label: String(label) }));
    return {
      title,
      instructions,
      type: 'select',
      options: options.map((o) => o.label || o.value),
      questions: parsed.questions.map((q) => ({ number: q.number, prompt: q.prompt })),
    };
  }
  if (/iot-question__fill-blank/.test(body)) {
    return { title, instructions, type: 'text', questions: parseFillQuestions(body) };
  }
  return { title, instructions, type: 'text', questions: [] };
}

function groupMinNumber(group) {
  const fromTitle = expandNums(group.title);
  const fromQs = (group.questions || []).map((q) => q.number).filter(Boolean);
  const fromNums = group.questionNumbers || [];
  const all = [...fromQs, ...fromNums, ...fromTitle];
  return all.length ? Math.min(...all) : 0;
}

function assignParts(groups, ranges) {
  return ranges.map((range, index) => ({
    key: `part_${index + 1}`,
    partNumber: index + 1,
    title: range.title,
    questionRange: range.questionRange,
    summary: range.summary || '',
    questionGroups: groups.filter((g) => {
      const n = groupMinNumber(g);
      return n >= range.from && n <= range.to;
    }),
    ...(range.passage ? { passage: range.passage } : {}),
  }));
}

function extractPassages(html) {
  const titles = [...html.matchAll(/field--name-field-subtitle-section[\s\S]*?field--item[^>]*>([^<]+)/gi)].map((m) => strip(m[1]));
  const blocks = [...html.matchAll(/field--name-field-passage field--type-text-long[\s\S]*?field--item[^>]*>([\s\S]*?)<\/div>/gi)].map((m) => m[1]);
  return blocks.map((block, i) => {
    const paragraphs = [];
    const re = /<p>([\s\S]*?)<\/p>/gi;
    let m;
    while ((m = re.exec(block))) {
      const raw = m[1];
      const label = ((raw.match(/<strong>\s*([A-Z])\.?\s*<\/strong>/) || [])[1]) || '';
      const text = strip(raw.replace(/<strong>[\s\S]*?<\/strong>/, ''));
      if (text) paragraphs.push({ label, text });
    }
    return { title: titles[i] || `Reading Passage ${i + 1}`, paragraphs };
  });
}

function extractJsonValue(html, key) {
  const needle = `"${key}":`;
  let start = 0;
  while (true) {
    const idx = html.indexOf(needle, start);
    if (idx < 0) return null;
    let i = idx + needle.length;
    while (html[i] === ' ') i++;
    const open = html[i];
    if (open !== '[' && open !== '{') {
      start = idx + 1;
      continue;
    }
    const close = open === '[' ? ']' : '}';
    let depth = 0;
    const from = i;
    for (; i < html.length; i++) {
      if (html[i] === open) depth++;
      else if (html[i] === close) {
        depth--;
        if (depth === 0) {
          i++;
          break;
        }
      }
    }
    try {
      const value = JSON.parse(html.slice(from, i));
      if (Array.isArray(value) && value[0] && Array.isArray(value[0].questions)) return value;
    } catch {
      /* try next */
    }
    start = idx + 1;
  }
}

function mediaUrl(raw) {
  const url = String(raw || '').trim();
  if (/\.(mp4|mp3|wav|webm)(\?|$)/i.test(url) || /aliyuncs\.com/i.test(url)) return url;
  return '';
}

function htmlParas(html) {
  return [...String(html || '').matchAll(/<p[^>]*>([\s\S]*?)<\/p>/gi)]
    .map((m) => strip(m[1]))
    .filter(Boolean);
}

function listeningJson(html, files, setId) {
  const groups = splitGroups(html).map((g) => toGroup(g.title, g.body));
  const key = examKey(files, 'L', setId);
  return {
    version: 1,
    type: 'ielts_listening_exam',
    key,
    title: examTitle(files, 'L', setId),
    sourceLabel: `Imported from local HTML ${setId}.html`,
    durationMinutes: 32,
    rules: [
      'Complete all four parts in one sitting.',
      'Do not switch tabs or leave the exam window.',
    ],
    parts: assignParts(groups, [
      { title: 'Part 1', questionRange: 'Questions 1-10', from: 1, to: 10 },
      { title: 'Part 2', questionRange: 'Questions 11-20', from: 11, to: 20 },
      { title: 'Part 3', questionRange: 'Questions 21-30', from: 21, to: 30 },
      { title: 'Part 4', questionRange: 'Questions 31-40', from: 31, to: 40 },
    ]),
    answerKey: {},
  };
}

function readingJson(html, files, setId) {
  const groups = splitGroups(html).map((g) => toGroup(g.title, g.body));
  const passages = extractPassages(html);
  const parts = assignParts(groups, [
    { title: 'Reading Passage 1', questionRange: 'Questions 1-13', from: 1, to: 13, passage: passages[0] },
    { title: 'Reading Passage 2', questionRange: 'Questions 14-26', from: 14, to: 26, passage: passages[1] },
    { title: 'Reading Passage 3', questionRange: 'Questions 27-40', from: 27, to: 40, passage: passages[2] },
  ]);
  return {
    version: 1,
    type: 'ielts_reading_exam',
    key: examKey(files, 'R', setId),
    title: examTitle(files, 'R', setId),
    sourceLabel: `Imported from local HTML ${setId}r.html`,
    durationMinutes: 60,
    rules: ['Do not switch tabs or leave the exam screen during the test.'],
    parts,
    answerKey: {},
  };
}

function writingImage(setId, key) {
  const folder = path.join(SRC, `${setId}w_files`);
  if (!fs.existsSync(folder)) return '';
  const img = fs.readdirSync(folder).find((name) => /\.(png|jpe?g|webp)$/i.test(name));
  if (!img) return '';
  const dirName = publicDirName(key);
  const destDir = path.join(PUBLIC_ROOT, dirName);
  fs.mkdirSync(destDir, { recursive: true });
  const ext = path.extname(img).toLowerCase();
  const destName = `writing-task1${ext}`;
  fs.copyFileSync(path.join(folder, img), path.join(destDir, destName));
  return `/sheet-exams/${dirName}/${destName}`;
}

function writingJson(html, files, setId) {
  const text = strip(html);
  const t1 = (text.match(/Writing Task 1([\s\S]*?)Writing Task 2/) || [])[1] || '';
  const t2 = (text.match(/Writing Task 2([\s\S]*?)Words Count/) || [])[1] || '';
  const paras = (chunk) => {
    const parts = chunk
      .split(/(?=(You should spend about|You should write at least|Summarise the information by|Write about the following topic))/i)
      .map((s) => s.trim())
      .filter((s) => s.length > 20);
    return parts.filter((item, index) => !parts.some((other, otherIndex) => otherIndex !== index && other.startsWith(item)));
  };
  const key = examKey(files, 'W', setId);
  const imageUrl = writingImage(setId, key);
  const p1 = paras(t1);
  const p2 = paras(t2);
  return {
    version: 1,
    type: 'ielts_writing_exam',
    key,
    title: examTitle(files, 'W', setId),
    sourceLabel: `Imported from local HTML ${setId}w.html`,
    durationMinutes: 60,
    rules: ['Stay in full-screen mode until you submit the exam.'],
    tasks: [
      {
        key: 'task_1',
        title: 'Task 1',
        heading: 'Writing Task 1',
        summary: 'Complete Task 1 in at least 150 words.',
        recommendedMinutes: 20,
        minimumWords: 150,
        promptParagraphs: p1.length ? p1 : [t1.slice(0, 500) || 'Writing Task 1'],
        ...(imageUrl ? { imageUrl } : {}),
      },
      {
        key: 'task_2',
        title: 'Task 2',
        heading: 'Writing Task 2',
        summary: 'Complete Task 2 in at least 250 words.',
        recommendedMinutes: 40,
        minimumWords: 250,
        promptParagraphs: p2.length ? p2 : [t2.slice(0, 500) || 'Writing Task 2'],
      },
    ],
  };
}

function speakingJson(html, files, setId) {
  const partsRaw = extractJsonValue(html, 'part') || [];
  const parts = partsRaw.map((raw, index) => {
    const questions = (raw.questions || []).map((q) => ({
      text: strip(q.text_question || ''),
      videoUrl: mediaUrl(q.question_video),
    })).filter((q) => q.text);
    const suggestion = htmlParas((raw.questions && raw.questions[0] && raw.questions[0].suggestion_text) || '');
    const isCue = index === 1 || /topic/i.test(raw.title || '') && !/discussion/i.test(raw.title || '');
    if (isCue && questions[0]) {
      return {
        key: `part_${index + 1}`,
        title: `Part ${index + 1} · ${raw.title || 'Cue Card'}`,
        prepSeconds: Number(raw.time?.time_to_think || 60),
        answerSeconds: Number(raw.time?.total_answer_time || 120),
        videoUrl: questions[0].videoUrl || '',
        cueCardTitle: questions[0].text,
        cueCardBullets: suggestion.filter((line) => !/^you should say:?$/i.test(line)),
      };
    }
    return {
      key: `part_${index + 1}`,
      title: `Part ${index + 1} · ${raw.title || 'Questions'}`,
      prepSeconds: 0,
      answerSeconds: Number(raw.time?.total_answer_time || (index === 0 ? 90 : 120)),
      prompts: questions,
    };
  });
  if (parts[1] && Array.isArray(parts[1].cueCardBullets)) {
    parts[1].cueCardBullets = parts[1].cueCardBullets.filter((line) => !/^you should say:?$/i.test(line));
  }
  return {
    version: 1,
    type: 'ielts_speaking_exam',
    key: examKey(files, 'S', setId),
    title: examTitle(files, 'S', setId),
    sourceLabel: `Imported from local HTML ${setId}s.html`,
    durationMinutes: 14,
    parts,
  };
}

function parseAnswerValue(raw) {
  const text = strip(raw);
  if (!text) return '';
  if (/^[A-Za-z](?:\s*,\s*[A-Za-z])+$/.test(text)) {
    return text.split(',').map((item) => item.trim().toUpperCase()).filter(Boolean);
  }
  if (text.includes('/')) {
    const parts = text.split('/').map((item) => item.trim()).filter(Boolean);
    if (parts.length > 1) return parts;
  }
  return text;
}

function parseSysAnswers(html) {
  const key = {};
  const re = /<li class="list-answer-item[^"]*"[\s\S]*?<span class="number">([\s\S]*?)<\/span>\s*<span class="sys-answer">([\s\S]*?)<\/span>/gi;
  let match;
  while ((match = re.exec(html))) {
    const nums = [...match[1].matchAll(/(\d+)/g)].map((item) => Number(item[1]));
    if (!nums.length) continue;
    const value = parseAnswerValue(match[2]);
    const firstKey = String(nums[0]);
    if (Object.prototype.hasOwnProperty.call(key, firstKey) || Object.prototype.hasOwnProperty.call(key, nums.join('-'))) {
      continue;
    }
    if (nums.length === 1) {
      key[firstKey] = value;
      continue;
    }
    const from = Math.min(...nums);
    const to = Math.max(...nums);
    const range = [];
    for (let n = from; n <= to; n++) range.push(n);
    const letters = (Array.isArray(value) ? value : String(value).split(','))
      .map((item) => String(item).trim().toUpperCase())
      .filter((item) => /^[A-Z]$/.test(item));
    const stored = letters.length ? letters : value;
    key[range.join('-')] = stored;
    key[String(range[0])] = stored;
  }
  return key;
}

function solutionSlug(html) {
  const haystack = `${pageUrl(html)}\n${html.slice(0, 2500)}`;
  const match = haystack.match(/ielts-mock-test-(\d{4}-[a-z]+-(?:listening|reading)-practi[cs]e-test-\d+(?:-\d+)?)/i);
  return match ? match[1].replace(/-0$/, '') : '';
}

function loadAnswerKeys() {
  const byJson = {};
  const files = fs.readdirSync(SRC).filter((name) => /a[lr]\.html$/i.test(name));
  for (const name of files) {
    const html = fs.readFileSync(path.join(SRC, name), 'utf8');
    const slug = solutionSlug(html);
    if (!slug) {
      console.log('skip answers, no slug', name);
      continue;
    }
    const jsonName = `ielts-mock-${slug}.json`;
    byJson[jsonName] = { file: name, key: parseSysAnswers(html), slug };
  }
  return byJson;
}

function countQuestions(config) {
  const nums = new Set();
  for (const part of config.parts || []) {
    for (const g of part.questionGroups || []) {
      (g.questions || []).forEach((q) => nums.add(q.number));
      (g.questionNumbers || []).forEach((n) => nums.add(n));
    }
  }
  return [...nums].sort((a, b) => a - b);
}

function writeJson(name, data) {
  const file = path.join(OUT, name);
  fs.writeFileSync(file, JSON.stringify(data, null, 2), 'utf8');
  return `sheet-data/${name}`;
}

function loadSet(setId) {
  const read = (name) => fs.readFileSync(path.join(SRC, name), 'utf8');
  return {
    L: read(`${setId}.html`),
    R: read(`${setId}r.html`),
    W: read(`${setId}w.html`),
    S: read(`${setId}s.html`),
  };
}

fs.mkdirSync(OUT, { recursive: true });
fs.mkdirSync(PUBLIC_ROOT, { recursive: true });

const answerKeys = loadAnswerKeys();
const index = [];
for (const setId of SETS) {
  const files = loadSet(setId);
  const listening = listeningJson(files.L, files, setId);
  const reading = readingJson(files.R, files, setId);
  const writing = writingJson(files.W, files, setId);
  const speaking = speakingJson(files.S, files, setId);

  const lName = `${publicDirName(listening.key)}.json`;
  const rName = `${publicDirName(reading.key)}.json`;
  if (answerKeys[lName]) listening.answerKey = answerKeys[lName].key;
  if (answerKeys[rName]) reading.answerKey = answerKeys[rName].key;

  const lFile = writeJson(lName, listening);
  const rFile = writeJson(rName, reading);
  const wFile = writeJson(`${publicDirName(writing.key)}.json`, writing);
  const sFile = writeJson(`${publicDirName(speaking.key)}.json`, speaking);

  index.push(
    { title: listening.title, skill: 'LISTENING', minutes: 32, resource: lFile, needsKey: true, setId },
    { title: reading.title, skill: 'READING', minutes: 60, resource: rFile, needsKey: true, setId },
    { title: writing.title, skill: 'WRITING', minutes: 60, resource: wFile, needsKey: false, setId },
    { title: speaking.title, skill: 'SPEAKING', minutes: 14, resource: sFile, needsKey: false, setId },
  );

  console.log(setId,
    'L', countQuestions(listening).length, 'ak', Object.keys(listening.answerKey || {}).length, answerKeys[lName]?.file || '-',
    'R', countQuestions(reading).length, 'ak', Object.keys(reading.answerKey || {}).length, answerKeys[rName]?.file || '-',
    listening.title);
}

writeJson('iot-mocks-index.json', index);
console.log('wrote', index.length, 'items');
