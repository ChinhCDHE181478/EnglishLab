const normalizeHeader = (value) => String(value || '')
  .normalize('NFD')
  .replace(/[\u0300-\u036f]/g, '')
  .replace(/đ/g, 'd')
  .replace(/Đ/g, 'D')
  .trim()
  .toLowerCase();

const headerAliases = {
  courseTitle: ['ten khoa hoc', 'ten chuong trinh'],
  unitTitle: ['ten unit'],
  unitDescription: ['mo ta unit'],
  lessonNumber: ['thu tu bai hoc', 'so bai hoc', 'so buoi'],
  lessonTitle: ['ten bai hoc', 'ten buoi hoc'],
  lessonDescription: ['mo ta bai hoc', 'mo ta buoi hoc'],
  learningObjectives: ['muc tieu hoc tap'],
};

const requiredHeaders = {
  courseTitle: 'ten khoa hoc',
  unitTitle: 'ten unit',
  unitDescription: 'mo ta unit',
  lessonNumber: 'thu tu bai hoc',
  lessonTitle: 'ten bai hoc',
  lessonDescription: 'mo ta bai hoc',
  learningObjectives: 'muc tieu hoc tap',
};

const errorMessage = (error, fallback) => error?.response?.data?.message || error?.message || fallback;

export const parseInstructorLedCourseExcelRows = (rows, fileName = '') => {
  if (!Array.isArray(rows) || rows.length < 2) {
    throw new Error('Tệp Excel không chứa dữ liệu hoặc sai định dạng.');
  }

  const normalizedHeaders = (rows[0] || []).map(normalizeHeader);
  const columns = Object.fromEntries(Object.entries(headerAliases).map(([key, aliases]) => {
    const index = aliases.reduce((found, alias) => (found >= 0 ? found : normalizedHeaders.indexOf(alias)), -1);
    return [key, index];
  }));
  const missingHeaders = Object.entries(columns)
    .filter(([, index]) => index < 0)
    .map(([key]) => requiredHeaders[key]);
  if (missingHeaders.length) {
    throw new Error(`Tệp Excel thiếu cột: ${missingHeaders.join(', ')}.`);
  }

  let title = '';
  let currentUnitTitle = '';
  const unitsByTitle = new Map();
  const lessonNumbers = new Set();

  rows.slice(1).forEach((row, rowIndex) => {
    const excelRow = rowIndex + 2;
    if (!row || row.every((cell) => !String(cell || '').trim())) return;

    const rowCourseTitle = String(row[columns.courseTitle] || '').trim();
    if (rowCourseTitle) {
      if (title && title.toLowerCase() !== rowCourseTitle.toLowerCase()) {
        throw new Error(`Dòng ${excelRow}: tệp chỉ được chứa một khóa học.`);
      }
      title = rowCourseTitle;
    }

    const rowUnitTitle = String(row[columns.unitTitle] || '').trim();
    if (rowUnitTitle) currentUnitTitle = rowUnitTitle;
    if (!currentUnitTitle) {
      throw new Error(`Dòng ${excelRow}: tên Unit không được để trống.`);
    }

    const rawLessonNumber = String(row[columns.lessonNumber] || '').trim();
    const lessonNumber = Number(rawLessonNumber);
    if (!Number.isInteger(lessonNumber) || lessonNumber < 1) {
      throw new Error(`Dòng ${excelRow}: thứ tự bài học phải là số nguyên bắt đầu từ 1.`);
    }
    if (lessonNumbers.has(lessonNumber)) {
      throw new Error(`Dòng ${excelRow}: bài học ${lessonNumber} bị trùng trong khóa học.`);
    }

    const lessonTitle = String(row[columns.lessonTitle] || '').trim();
    if (!lessonTitle) {
      throw new Error(`Dòng ${excelRow}: tên bài học không được để trống.`);
    }
    lessonNumbers.add(lessonNumber);

    const unitKey = currentUnitTitle.toLowerCase();
    let unit = unitsByTitle.get(unitKey);
    if (!unit) {
      unit = {
        displayOrder: unitsByTitle.size + 1,
        title: currentUnitTitle,
        description: String(row[columns.unitDescription] || '').trim() || null,
        lessons: [],
        sourceRow: excelRow,
      };
      unitsByTitle.set(unitKey, unit);
    } else if (!unit.description) {
      unit.description = String(row[columns.unitDescription] || '').trim() || null;
    }

    unit.lessons.push({
      sessionNumber: lessonNumber,
      displayOrder: unit.lessons.length,
      title: lessonTitle,
      description: String(row[columns.lessonDescription] || '').trim() || null,
      learningObjectives: String(row[columns.learningObjectives] || '').trim() || null,
      sourceRow: excelRow,
    });
  });

  if (!title) throw new Error('Không tìm thấy tên khóa học trong tệp Excel.');
  const units = [...unitsByTitle.values()];
  if (!units.length) throw new Error('Tệp Excel chưa có Unit hoặc buổi học nào.');

  return { title, units, fileName };
};

export const parseInstructorLedCourseExcelFile = async (file) => {
  const XLSX = await import('@e965/xlsx');
  const workbook = XLSX.read(await file.arrayBuffer(), { type: 'array' });
  const sheet = workbook.Sheets[workbook.SheetNames[0]];
  const rows = XLSX.utils.sheet_to_json(sheet, { header: 1, defval: '', raw: false });
  return parseInstructorLedCourseExcelRows(rows, file.name);
};

export const downloadInstructorLedCourseExcelTemplate = async () => {
  const XLSX = await import('@e965/xlsx');
  const rows = [
    ['Tên khóa học', 'Tên Unit', 'Mô tả Unit', 'Thứ tự bài học', 'Tên bài học', 'Mô tả bài học', 'Mục tiêu học tập'],
    ['IELTS Reading Foundation', 'Reading Fundamentals', 'Các kỹ năng đọc nền tảng', 1, 'Reading Overview + Skimming', 'Tổng quan IELTS Reading và kỹ thuật đọc lướt', 'Nắm format và áp dụng skimming'],
    ['', 'Reading Fundamentals', 'Các kỹ năng đọc nền tảng', 2, 'Scanning + Keywords', 'Xác định thông tin chi tiết và từ khóa', 'Áp dụng scanning để tìm thông tin'],
    ['', 'Reading Question Types', 'Chiến thuật cho từng dạng câu hỏi', 3, 'True / False / Not Given', 'Nhận diện và xử lý dạng câu hỏi', 'Phân biệt False và Not Given'],
  ];
  const worksheet = XLSX.utils.aoa_to_sheet(rows);
  worksheet['!cols'] = [
    { wch: 30 }, { wch: 28 }, { wch: 38 }, { wch: 14 }, { wch: 38 }, { wch: 48 }, { wch: 48 },
  ];
  worksheet['!autofilter'] = { ref: 'A1:G4' };
  const workbook = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(workbook, worksheet, 'Khung_Khoa_Hoc');
  XLSX.writeFile(workbook, 'Mau_Import_Khoa_Hoc.xlsx');
};

export const importCourseUnitsWithLessons = async (api, instructorLedCourseId, units) => {
  const result = { createdUnits: 0, createdLessons: 0, failures: [] };
  for (const unit of units) {
    let savedUnit;
    try {
      savedUnit = await api.createCourseUnit(instructorLedCourseId, {
        displayOrder: unit.displayOrder,
        title: unit.title,
        description: unit.description,
      });
      result.createdUnits += 1;
    } catch (error) {
      result.failures.push(`Dòng ${unit.sourceRow}: ${errorMessage(error, `Không tạo được Unit “${unit.title}”.`)}`);
      continue;
    }

    for (const lesson of unit.lessons) {
      try {
        await api.createCourseLesson(savedUnit.id, {
          sessionNumber: lesson.sessionNumber,
          displayOrder: lesson.displayOrder,
          title: lesson.title,
          description: lesson.description,
          learningObjectives: lesson.learningObjectives,
        });
        result.createdLessons += 1;
      } catch (error) {
        result.failures.push(`Dòng ${lesson.sourceRow}: ${errorMessage(error, `Không tạo được bài học ${lesson.sessionNumber}.`)}`);
      }
    }
  }
  return result;
};
