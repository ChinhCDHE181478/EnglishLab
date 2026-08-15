const MONTHS = [
  ['january', 1, 'Tháng 1'],
  ['february', 2, 'Tháng 2'],
  ['march', 3, 'Tháng 3'],
  ['april', 4, 'Tháng 4'],
  ['may', 5, 'Tháng 5'],
  ['june', 6, 'Tháng 6'],
  ['july', 7, 'Tháng 7'],
  ['august', 8, 'Tháng 8'],
  ['september', 9, 'Tháng 9'],
  ['october', 10, 'Tháng 10'],
  ['november', 11, 'Tháng 11'],
  ['december', 12, 'Tháng 12'],
];

const SET_META = {
  '2025de2': { year: 2025, monthKey: 'january', testNumber: 2 },
  '2025de3': { year: 2025, monthKey: 'february', testNumber: 1 },
  '2025de4': { year: 2025, monthKey: 'february', testNumber: 2 },
  '2026de1': { year: 2026, monthKey: 'january', testNumber: 1 },
  '2026de2': { year: 2026, monthKey: 'january', testNumber: 2 },
};

const SKILL_ORDER = ['LISTENING', 'READING', 'WRITING', 'SPEAKING'];

export function readMockConfig(item) {
  const raw = item?.uiConfigJson;
  if (raw && typeof raw === 'object') return raw;
  try {
    const parsed = JSON.parse(String(raw || '{}'));
    return parsed && typeof parsed === 'object' ? parsed : {};
  } catch {
    return {};
  }
}

function monthFromTitle(title) {
  const text = String(title || '');
  return MONTHS.find(([key]) => new RegExp(`\\b${key}\\b`, 'i').test(text)) || null;
}

function setIdFromItem(item) {
  const config = readMockConfig(item);
  const label = String(config.sourceLabel || item.sourceLabel || '');
  const match = label.match(/(\d{4}de\d+)/i);
  return match ? match[1].toLowerCase() : '';
}

export function parseMockTestMeta(item = {}) {
  const title = String(item.title || '');
  const setId = setIdFromItem(item);
  const fromSet = SET_META[setId] || null;
  const yearMatch = title.match(/20\d{2}/);
  const month = monthFromTitle(title);
  const testMatch = title.match(/practi[sc]e\s+test\s*(\d+)/i);
  const year = fromSet?.year || (yearMatch ? Number(yearMatch[0]) : null);
  const monthKey = fromSet?.monthKey || month?.[0] || null;
  const monthIndex = fromSet ? MONTHS.find((entry) => entry[0] === fromSet.monthKey)?.[1] : month?.[1] || null;
  const monthLabel = monthKey ? MONTHS.find((entry) => entry[0] === monthKey)?.[2] : null;
  const testNumber = fromSet?.testNumber || (testMatch ? Number(testMatch[1]) : null);
  const skill = String(item.skill || 'MIXED').toUpperCase();
  const dated = Boolean(year && monthKey && testNumber);
  return {
    dated,
    year,
    monthKey,
    monthIndex,
    monthLabel,
    testNumber: testNumber || 1,
    skill,
    setId,
    title,
  };
}

export function splitMockTests(tests = []) {
  const libraryTests = [];
  const practiceTests = [];
  tests.forEach((item) => {
    const meta = parseMockTestMeta(item);
    if (meta.dated) libraryTests.push({ ...item, meta });
    else practiceTests.push({ ...item, meta });
  });
  return { libraryTests, practiceTests };
}

export function buildMockLibrary(tests = []) {
  const years = new Map();
  splitMockTests(tests).libraryTests.forEach((item) => {
    const { year, monthKey, monthIndex, monthLabel, testNumber, skill } = item.meta;
    if (!years.has(year)) {
      years.set(year, {
        year,
        title: `IELTS Mock Test ${year}`,
        months: new Map(),
      });
    }
    const yearEntry = years.get(year);
    if (!yearEntry.months.has(monthKey)) {
      yearEntry.months.set(monthKey, {
        monthKey,
        monthIndex,
        monthLabel,
        packs: new Map(),
      });
    }
    const monthEntry = yearEntry.months.get(monthKey);
    if (!monthEntry.packs.has(testNumber)) {
      monthEntry.packs.set(testNumber, {
        testNumber,
        title: `Đề ${testNumber}`,
        skills: {},
        testsList: [],
      });
    }
    const pack = monthEntry.packs.get(testNumber);
    pack.skills[skill] = item;
    pack.testsList.push(item);
  });

  return Array.from(years.values())
    .sort((a, b) => b.year - a.year)
    .map((yearEntry) => ({
      year: yearEntry.year,
      title: yearEntry.title,
      months: Array.from(yearEntry.months.values())
        .sort((a, b) => a.monthIndex - b.monthIndex)
        .map((monthEntry) => ({
          monthKey: monthEntry.monthKey,
          monthIndex: monthEntry.monthIndex,
          monthLabel: monthEntry.monthLabel,
          packs: Array.from(monthEntry.packs.values()).sort((a, b) => a.testNumber - b.testNumber),
        })),
    }));
}

export function packProgress(pack, completedScoresMap = {}) {
  const completed = SKILL_ORDER.filter((skill) => pack.skills[skill] && completedScoresMap[pack.skills[skill].id]).length;
  const total = SKILL_ORDER.filter((skill) => pack.skills[skill]).length;
  const percent = total ? Math.round((completed / total) * 100) : 0;
  const nextSkill = SKILL_ORDER.find((skill) => pack.skills[skill] && !completedScoresMap[pack.skills[skill].id])
    || SKILL_ORDER.find((skill) => pack.skills[skill]);
  return { completed, total, percent, nextTest: nextSkill ? pack.skills[nextSkill] : pack.testsList[0] || null };
}

export function monthProgress(month, completedScoresMap = {}) {
  const totals = (month?.packs || []).reduce(
    (acc, pack) => {
      const progress = packProgress(pack, completedScoresMap);
      acc.completed += progress.completed;
      acc.total += progress.total;
      return acc;
    },
    { completed: 0, total: 0 }
  );
  return {
    ...totals,
    percent: totals.total ? Math.round((totals.completed * 100) / totals.total) : 0,
  };
}

export function monthSkillPresence(month) {
  return SKILL_ORDER.filter((skill) => (month?.packs || []).some((pack) => pack.skills[skill]));
}

export function filterLibrary(library, { keyword = '', skill = 'ALL' } = {}) {
  const query = String(keyword || '').trim().toLowerCase();
  return library
    .map((yearEntry) => ({
      ...yearEntry,
      months: yearEntry.months
        .map((monthEntry) => ({
          ...monthEntry,
          packs: monthEntry.packs.filter((pack) => {
            const matchesSkill = skill === 'ALL' || pack.skills[skill];
            const haystack = [
              yearEntry.title,
              monthEntry.monthLabel,
              pack.title,
              ...pack.testsList.map((item) => item.title),
            ].join(' ').toLowerCase();
            const matchesKeyword = !query || haystack.includes(query);
            return matchesSkill && matchesKeyword;
          }),
        }))
        .filter((monthEntry) => monthEntry.packs.length),
    }))
    .filter((yearEntry) => yearEntry.months.length);
}

export { SKILL_ORDER };
