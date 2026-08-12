import { useState } from 'react';
import { Download, FileUp, Plus, Trash2, X } from 'lucide-react';
import BrandedSelect from '../ui/BrandedSelect';
import FlashcardDictionaryAssistant from '../flashcard/FlashcardDictionaryAssistant';

const draftKey = () => `${Date.now()}-${Math.random().toString(36).slice(2)}`;

export const createEmptyQuestion = () => ({
  _key: draftKey(),
  prompt: '',
  options: ['', '', '', ''],
  correctAnswer: 'A',
});

export const createEmptyWritingTask = () => ({
  _key: draftKey(),
  title: '',
  question: '',
  minimumWords: '150',
  recommendedMinutes: '40',
});

export const createEmptySpeakingPart = () => ({
  _key: draftKey(),
  title: '',
  prompts: [''],
  answerSeconds: '120',
});

export const createEmptyFlashcard = () => ({
  _key: draftKey(),
  term: '',
  meaning: '',
  example: '',
  commonMistake: '',
});

const flashcardSpreadsheetHeaders = ['Thuật ngữ', 'Định nghĩa', 'Ví dụ', 'Lỗi thường gặp'];

const parseFlashcardSpreadsheetRows = (rows) => {
  const firstRow = rows[0] || [];
  const normalizedHeaders = firstRow.map((value) => String(value || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]/g, ''));
  const hasHeader = ['thuatngu', 'term'].includes(normalizedHeaders[0])
    && ['dinhnghia', 'nghia', 'definition', 'meaning'].includes(normalizedHeaders[1]);
  const cards = [];
  const invalidRows = [];

  (hasHeader ? rows.slice(1) : rows).forEach((row, index) => {
    if (!Array.isArray(row) || row.every((cell) => !String(cell ?? '').trim())) return;
    const [term, meaning, example = '', commonMistake = ''] = row.map((cell) => String(cell ?? '').trim());
    if (!term || !meaning) {
      invalidRows.push(index + (hasHeader ? 2 : 1));
      return;
    }
    cards.push({ _key: draftKey(), term, meaning, example, commonMistake });
  });

  return { cards, invalidRows };
};

const validFlashcards = (cards) => cards.filter((card) => (
  card.term.trim() || card.meaning.trim() || card.example.trim() || card.commonMistake.trim()
));

const parseConfig = (value) => {
  try {
    const parsed = JSON.parse(value || '{}');
    return parsed && typeof parsed === 'object' ? parsed : {};
  } catch {
    return {};
  }
};

export const parseHomeworkBuilderDrafts = (value) => {
  const config = parseConfig(value);
  const questions = (config.questions || []).map((question, index) => ({
    _key: draftKey(),
    prompt: question.prompt || question.question || '',
    options: (question.options || []).map((option) => (
      typeof option === 'object' ? String(option.label || option.value || '') : String(option)
    )).concat(['', '', '', '']).slice(0, 4),
    correctAnswer: config.answerKey?.[String(question.number || index + 1)] || question.correctAnswer || 'A',
  }));
  const writingTasks = (config.tasks || []).map((task) => ({
    _key: draftKey(),
    title: task.title || '',
    question: task.question || task.prompt || '',
    minimumWords: String(task.minimumWords || task.minWords || 150),
    recommendedMinutes: String(task.recommendedMinutes || config.durationMinutes || 40),
  }));
  const speakingParts = (config.parts || []).map((part) => ({
    _key: draftKey(),
    title: part.title || '',
    prompts: Array.isArray(part.prompts) && part.prompts.length ? part.prompts.map(String) : [''],
    answerSeconds: String(part.answerSeconds || 120),
  }));
  const flashcards = (Array.isArray(config.flashcards) ? config.flashcards : [])
    .filter((card) => card && typeof card === 'object')
    .map((card) => ({
      _key: draftKey(),
      term: card.term || card.front || '',
      meaning: card.meaning || card.definition || card.back || '',
      example: card.example || '',
      commonMistake: card.commonMistake || '',
    }));

  return {
    questions: questions.length ? questions : [createEmptyQuestion()],
    writingTasks: writingTasks.length ? writingTasks : [createEmptyWritingTask()],
    speakingParts: speakingParts.length ? speakingParts : [createEmptySpeakingPart()],
    flashcards: flashcards.length ? flashcards : [createEmptyFlashcard()],
  };
};

export const buildHomeworkActivityConfig = ({
  activityType,
  skill,
  questions,
  writingTasks,
  speakingParts,
  flashcards,
}) => {
  if (activityType === 'SKILL_PRACTICE') {
    return JSON.stringify({
      questions: questions.map((question, index) => ({
        number: index + 1,
        prompt: question.prompt.trim(),
        options: question.options.map((option, optionIndex) => ({
          value: String.fromCharCode(65 + optionIndex),
          label: option.trim(),
        })),
      })),
      answerKey: Object.fromEntries(questions.map((question, index) => [String(index + 1), question.correctAnswer])),
    });
  }

  if (activityType === 'FLASHCARD_REVIEW') {
    return JSON.stringify({
      flashcards: flashcards.map(({ term, meaning, example, commonMistake }) => ({
        term: term.trim(),
        meaning: meaning.trim(),
        example: example.trim(),
        commonMistake: commonMistake.trim(),
      })),
    });
  }

  if (activityType === 'TEXT_RESPONSE' || activityType === 'MIXED') {
    if (skill === 'SPEAKING') {
      return JSON.stringify({
        parts: speakingParts.map((part, index) => ({
          key: `part_${index + 1}`,
          title: part.title.trim() || `Phần ${index + 1}`,
          prompts: part.prompts.map((prompt) => prompt.trim()),
          answerSeconds: Number(part.answerSeconds) || 120,
        })),
      });
    }
    return JSON.stringify({
      durationMinutes: Math.max(1, ...writingTasks.map((task) => Number(task.recommendedMinutes) || 40)),
      tasks: writingTasks.map((task, index) => ({
        key: `task_${index + 1}`,
        title: task.title.trim() || `Đề ${index + 1}`,
        question: task.question.trim(),
        minimumWords: Number(task.minimumWords) || 0,
        recommendedMinutes: Number(task.recommendedMinutes) || 40,
      })),
    });
  }

  return '';
};

const SectionHeader = ({ description, onAdd, title, addLabel }) => (
  <div className="flex flex-wrap items-center justify-between gap-3">
    <div>
      <p className="text-sm font-extrabold text-[#2b2828]">{title}</p>
      <p className="mt-1 text-xs leading-5 text-[#8b706e]">{description}</p>
    </div>
    <button
      className="inline-flex items-center gap-1.5 rounded-lg border border-[#dfbfbd] bg-white px-3 py-2 text-xs font-extrabold text-[#730014]"
      onClick={onAdd}
      type="button"
    >
      <Plus className="h-3.5 w-3.5" />
      {addLabel}
    </button>
  </div>
);

const RemoveButton = ({ label, onClick }) => (
  <button
    aria-label={label}
    className="rounded-lg p-2 text-red-600 transition hover:bg-red-50"
    onClick={onClick}
    title={label}
    type="button"
  >
    <Trash2 className="h-4 w-4" />
  </button>
);

export default function TeacherHomeworkContentBuilder({
  activityType,
  skill,
  questions,
  setQuestions,
  writingTasks,
  setWritingTasks,
  speakingParts,
  setSpeakingParts,
  flashcards,
  setFlashcards,
}) {
  if (activityType === 'FILE_RESPONSE') return null;

  if (activityType === 'SKILL_PRACTICE') {
    return (
      <div className="space-y-4 md:col-span-2">
        <SectionHeader
          addLabel="Thêm câu hỏi"
          description="Nhập nội dung, bốn lựa chọn và đáp án đúng. Hệ thống sẽ tự chấm khi học viên nộp."
          onAdd={() => setQuestions((current) => [...current, createEmptyQuestion()])}
          title="Biên soạn đề trắc nghiệm"
        />
        {questions.map((question, questionIndex) => (
          <div className="space-y-3 rounded-xl border border-[#e5e7eb] bg-[#fffafb] p-4" key={question._key}>
            <div className="flex items-center justify-between gap-3">
              <span className="text-xs font-extrabold text-[#730014]">Câu {questionIndex + 1}</span>
              {questions.length > 1 ? <RemoveButton label={`Xóa câu ${questionIndex + 1}`} onClick={() => setQuestions((current) => current.filter((item) => item._key !== question._key))} /> : null}
            </div>
            <textarea
              className="min-h-[84px] w-full rounded-xl border border-[#e5e7eb] bg-white px-4 py-3 text-sm outline-none focus:border-[#730014]"
              onChange={(event) => setQuestions((current) => current.map((item) => item._key === question._key ? { ...item, prompt: event.target.value } : item))}
              placeholder="Nhập nội dung câu hỏi"
              value={question.prompt}
            />
            <div className="grid gap-3 sm:grid-cols-2">
              {question.options.map((option, optionIndex) => (
                <label className="flex items-center gap-2" key={`${question._key}-${optionIndex}`}>
                  <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-[#fff0f1] text-xs font-extrabold text-[#730014]">{String.fromCharCode(65 + optionIndex)}</span>
                  <input
                    className="w-full rounded-xl border border-[#e5e7eb] bg-white px-3 py-2.5 text-xs outline-none focus:border-[#730014]"
                    onChange={(event) => setQuestions((current) => current.map((item) => {
                      if (item._key !== question._key) return item;
                      const options = [...item.options];
                      options[optionIndex] = event.target.value;
                      return { ...item, options };
                    }))}
                    placeholder={`Lựa chọn ${String.fromCharCode(65 + optionIndex)}`}
                    value={option}
                  />
                </label>
              ))}
            </div>
            <label className="block max-w-xs space-y-2">
              <span className="text-xs font-bold text-[#8b706e]">Đáp án đúng</span>
              <BrandedSelect
                onChange={(event) => setQuestions((current) => current.map((item) => item._key === question._key ? { ...item, correctAnswer: event.target.value } : item))}
                options={['A', 'B', 'C', 'D'].map((value) => ({ label: `Đáp án ${value}`, value }))}
                value={question.correctAnswer}
              />
            </label>
          </div>
        ))}
      </div>
    );
  }

  if (activityType === 'FLASHCARD_REVIEW') {
    return (
      <FlashcardReviewBuilder flashcards={flashcards} setFlashcards={setFlashcards} />
    );
  }

  if (skill === 'SPEAKING') {
    return (
      <div className="space-y-4 md:col-span-2">
        <SectionHeader addLabel="Thêm phần nói" description="Mỗi phần có thể chứa nhiều câu hỏi; học viên nghe hoặc đọc câu hỏi rồi ghi âm câu trả lời." onAdd={() => setSpeakingParts((current) => [...current, createEmptySpeakingPart()])} title="Biên soạn đề Speaking" />
        {speakingParts.map((part, partIndex) => (
          <div className="space-y-3 rounded-xl border border-[#e5e7eb] bg-[#fffafb] p-4" key={part._key}>
            <div className="flex items-center justify-between gap-3">
              <span className="text-xs font-extrabold text-[#730014]">Phần {partIndex + 1}</span>
              {speakingParts.length > 1 ? <RemoveButton label={`Xóa phần ${partIndex + 1}`} onClick={() => setSpeakingParts((current) => current.filter((item) => item._key !== part._key))} /> : null}
            </div>
            <div className="grid gap-3 sm:grid-cols-[1fr_180px]">
              <input className="rounded-xl border border-[#e5e7eb] bg-white px-4 py-3 text-sm outline-none focus:border-[#730014]" onChange={(event) => setSpeakingParts((current) => current.map((item) => item._key === part._key ? { ...item, title: event.target.value } : item))} placeholder="Tên phần nói" value={part.title} />
              <label className="space-y-1"><span className="text-[11px] font-bold text-[#8b706e]">Thời gian trả lời (giây)</span><input className="w-full rounded-xl border border-[#e5e7eb] bg-white px-4 py-3 text-sm outline-none focus:border-[#730014]" min="10" onChange={(event) => setSpeakingParts((current) => current.map((item) => item._key === part._key ? { ...item, answerSeconds: event.target.value } : item))} type="number" value={part.answerSeconds} /></label>
            </div>
            {part.prompts.map((prompt, promptIndex) => (
              <div className="flex items-start gap-2" key={`${part._key}-prompt-${promptIndex}`}>
                <span className="mt-3 text-xs font-extrabold text-[#730014]">{promptIndex + 1}.</span>
                <textarea className="min-h-[72px] flex-1 rounded-xl border border-[#e5e7eb] bg-white px-4 py-3 text-sm outline-none focus:border-[#730014]" onChange={(event) => setSpeakingParts((current) => current.map((item) => item._key === part._key ? { ...item, prompts: item.prompts.map((value, index) => index === promptIndex ? event.target.value : value) } : item))} placeholder="Nhập câu hỏi Speaking" value={prompt} />
                {part.prompts.length > 1 ? <RemoveButton label={`Xóa câu hỏi ${promptIndex + 1}`} onClick={() => setSpeakingParts((current) => current.map((item) => item._key === part._key ? { ...item, prompts: item.prompts.filter((_, index) => index !== promptIndex) } : item))} /> : null}
              </div>
            ))}
            <button className="inline-flex items-center gap-1.5 text-xs font-extrabold text-[#730014]" onClick={() => setSpeakingParts((current) => current.map((item) => item._key === part._key ? { ...item, prompts: [...item.prompts, ''] } : item))} type="button"><Plus className="h-3.5 w-3.5" />Thêm câu hỏi</button>
          </div>
        ))}
      </div>
    );
  }

  return (
    <div className="space-y-4 md:col-span-2">
      <SectionHeader addLabel="Thêm đề" description="Nhập đầy đủ yêu cầu học viên sẽ thấy trong phòng làm bài." onAdd={() => setWritingTasks((current) => [...current, createEmptyWritingTask()])} title={skill === 'WRITING' ? 'Biên soạn đề Writing' : 'Biên soạn đề tự luận'} />
      {writingTasks.map((task, index) => (
        <div className="space-y-3 rounded-xl border border-[#e5e7eb] bg-[#fffafb] p-4" key={task._key}>
          <div className="flex items-center justify-between gap-3"><span className="text-xs font-extrabold text-[#730014]">Đề {index + 1}</span>{writingTasks.length > 1 ? <RemoveButton label={`Xóa đề ${index + 1}`} onClick={() => setWritingTasks((current) => current.filter((item) => item._key !== task._key))} /> : null}</div>
          <input className="w-full rounded-xl border border-[#e5e7eb] bg-white px-4 py-3 text-sm outline-none focus:border-[#730014]" onChange={(event) => setWritingTasks((current) => current.map((item) => item._key === task._key ? { ...item, title: event.target.value } : item))} placeholder="Tên đề / dạng bài" value={task.title} />
          <textarea className="min-h-[130px] w-full rounded-xl border border-[#e5e7eb] bg-white px-4 py-3 text-sm outline-none focus:border-[#730014]" onChange={(event) => setWritingTasks((current) => current.map((item) => item._key === task._key ? { ...item, question: event.target.value } : item))} placeholder="Nhập nội dung đề bài và yêu cầu cần hoàn thành" value={task.question} />
          <div className="grid gap-3 sm:grid-cols-2">
            <label className="space-y-1"><span className="text-[11px] font-bold text-[#8b706e]">Số từ tối thiểu</span><input className="w-full rounded-xl border border-[#e5e7eb] bg-white px-4 py-3 text-sm outline-none focus:border-[#730014]" min="0" onChange={(event) => setWritingTasks((current) => current.map((item) => item._key === task._key ? { ...item, minimumWords: event.target.value } : item))} type="number" value={task.minimumWords} /></label>
            <label className="space-y-1"><span className="text-[11px] font-bold text-[#8b706e]">Thời gian gợi ý (phút)</span><input className="w-full rounded-xl border border-[#e5e7eb] bg-white px-4 py-3 text-sm outline-none focus:border-[#730014]" min="1" onChange={(event) => setWritingTasks((current) => current.map((item) => item._key === task._key ? { ...item, recommendedMinutes: event.target.value } : item))} type="number" value={task.recommendedMinutes} /></label>
          </div>
        </div>
      ))}
    </div>
  );
}

function FlashcardReviewBuilder({ flashcards, setFlashcards }) {
  const [importOpen, setImportOpen] = useState(false);
  const [readingSpreadsheet, setReadingSpreadsheet] = useState(false);
  const [importStrategy, setImportStrategy] = useState('APPEND');
  const [spreadsheetImport, setSpreadsheetImport] = useState({ cards: [], invalidRows: [], fileName: '' });
  const [notice, setNotice] = useState('');

  const downloadTemplate = async () => {
    try {
      const XLSX = await import('@e965/xlsx');
      const worksheet = XLSX.utils.aoa_to_sheet([
        flashcardSpreadsheetHeaders,
        ['immediate family', 'gia đình ruột thịt gần gũi nhất', 'My immediate family lives in Hanoi.', 'Không dùng để chỉ họ hàng xa.'],
      ]);
      worksheet['!cols'] = [{ wch: 28 }, { wch: 38 }, { wch: 55 }, { wch: 45 }];
      worksheet['!autofilter'] = { ref: 'A1:D2' };
      const workbook = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(workbook, worksheet, 'Mẫu thẻ ghi nhớ');
      XLSX.writeFile(workbook, 'mau-import-the-ghi-nho-englishlab.xlsx');
    } catch {
      setNotice('Không tạo được bản mẫu Excel. Hãy thử lại.');
    }
  };

  const readSpreadsheet = async (file) => {
    if (!file) return;
    if (!/\.(xlsx|xls)$/i.test(file.name)) {
      setNotice('Chỉ hỗ trợ tệp Excel .xlsx hoặc .xls.');
      return;
    }

    setReadingSpreadsheet(true);
    setNotice('');
    try {
      const XLSX = await import('@e965/xlsx');
      const workbook = XLSX.read(await file.arrayBuffer(), { type: 'array' });
      const sheet = workbook.Sheets[workbook.SheetNames[0]];
      const rows = XLSX.utils.sheet_to_json(sheet, { header: 1, defval: '', raw: false });
      setSpreadsheetImport({ ...parseFlashcardSpreadsheetRows(rows), fileName: file.name });
    } catch {
      setSpreadsheetImport({ cards: [], invalidRows: [], fileName: file.name });
      setNotice('Không đọc được tệp Excel. Hãy kiểm tra tệp có bị hỏng hoặc đặt mật khẩu hay không.');
    } finally {
      setReadingSpreadsheet(false);
    }
  };

  const applyImport = () => {
    if (!spreadsheetImport.cards.length) return;
    setFlashcards((current) => (
      importStrategy === 'REPLACE'
        ? spreadsheetImport.cards
        : [...validFlashcards(current), ...spreadsheetImport.cards]
    ));
    setImportOpen(false);
    setNotice(`Đã đưa ${spreadsheetImport.cards.length} thẻ từ Excel vào trình chỉnh sửa.`);
  };

  const exportSpreadsheet = async () => {
    const cards = validFlashcards(flashcards);
    if (!cards.length) {
      setNotice('Cần có ít nhất một thẻ trước khi xuất Excel.');
      return;
    }
    try {
      const XLSX = await import('@e965/xlsx');
      const worksheet = XLSX.utils.aoa_to_sheet([
        flashcardSpreadsheetHeaders,
        ...cards.map((card) => [card.term.trim(), card.meaning.trim(), card.example.trim(), card.commonMistake.trim()]),
      ]);
      worksheet['!cols'] = [{ wch: 28 }, { wch: 38 }, { wch: 55 }, { wch: 45 }];
      worksheet['!autofilter'] = { ref: `A1:D${cards.length + 1}` };
      const workbook = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(workbook, worksheet, 'Thẻ ghi nhớ');
      XLSX.writeFile(workbook, 'flashcard-lop-hoc-englishlab.xlsx');
      setNotice(`Đã xuất ${cards.length} thẻ ra Excel.`);
    } catch {
      setNotice('Không tạo được tệp Excel. Hãy thử lại.');
    }
  };

  return (
    <div className="space-y-4 md:col-span-2">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-sm font-extrabold text-[#2b2828]">Biên soạn bộ flashcard</p>
          <p className="mt-1 text-xs leading-5 text-[#8b706e]">Dùng cùng chuẩn bốn trường với kho Flashcard Content Manager.</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button className="inline-flex items-center gap-1.5 rounded-lg border border-[#dfbfbd] bg-white px-3 py-2 text-xs font-extrabold text-[#730014]" onClick={() => { setImportOpen(true); setNotice(''); }} type="button">
            <FileUp className="h-3.5 w-3.5" /> Nhập Excel
          </button>
          <button className="inline-flex items-center gap-1.5 rounded-lg border border-[#dfbfbd] bg-white px-3 py-2 text-xs font-extrabold text-[#730014]" onClick={exportSpreadsheet} type="button">
            <Download className="h-3.5 w-3.5" /> Xuất Excel
          </button>
          <button className="inline-flex items-center gap-1.5 rounded-lg border border-[#dfbfbd] bg-white px-3 py-2 text-xs font-extrabold text-[#730014]" onClick={() => setFlashcards((current) => [...current, createEmptyFlashcard()])} type="button">
            <Plus className="h-3.5 w-3.5" /> Thêm thẻ
          </button>
        </div>
      </div>

      {notice ? <p className={`rounded-xl border px-4 py-3 text-sm ${notice.startsWith('Đã ') ? 'border-emerald-200 bg-emerald-50 text-emerald-800' : 'border-rose-200 bg-rose-50 text-rose-800'}`}>{notice}</p> : null}

      {flashcards.map((card, index) => (
        <div className="space-y-3 rounded-xl border border-[#e5e7eb] bg-[#fffafb] p-4" key={card._key}>
          <div className="flex items-center justify-between gap-3">
            <span className="text-xs font-extrabold text-[#730014]">Thẻ {index + 1}</span>
            {flashcards.length > 1 ? <RemoveButton label={`Xóa thẻ ${index + 1}`} onClick={() => setFlashcards((current) => current.filter((item) => item._key !== card._key))} /> : null}
          </div>
          <div className="grid gap-3 sm:grid-cols-2">
            <input
              className="rounded-xl border border-[#e5e7eb] bg-white px-4 py-3 text-sm outline-none focus:border-[#730014]"
              onChange={(event) => setFlashcards((current) => current.map((item) => item._key === card._key ? { ...item, term: event.target.value } : item))}
              placeholder="Thuật ngữ"
              value={card.term}
            />
            <FlashcardDictionaryAssistant
              example={card.example}
              meaning={card.meaning}
              onExampleChange={(example) => setFlashcards((current) => current.map((item) => item._key === card._key ? { ...item, example } : item))}
              onMeaningChange={(meaning) => setFlashcards((current) => current.map((item) => item._key === card._key ? { ...item, meaning } : item))}
              term={card.term}
            />
            <textarea className="min-h-[82px] rounded-xl border border-[#e5e7eb] bg-white px-4 py-3 text-sm outline-none focus:border-[#730014]" onChange={(event) => setFlashcards((current) => current.map((item) => item._key === card._key ? { ...item, commonMistake: event.target.value } : item))} placeholder="Lỗi thường gặp" value={card.commonMistake} />
          </div>
        </div>
      ))}

      {importOpen ? (
        <FlashcardExcelImportDialog
          fileName={spreadsheetImport.fileName}
          invalidRows={spreadsheetImport.invalidRows}
          onApply={applyImport}
          onClose={() => setImportOpen(false)}
          onDownloadTemplate={downloadTemplate}
          onFileChange={readSpreadsheet}
          onStrategyChange={setImportStrategy}
          previewCards={spreadsheetImport.cards}
          reading={readingSpreadsheet}
          strategy={importStrategy}
        />
      ) : null}
    </div>
  );
}

function FlashcardExcelImportDialog({
  fileName,
  invalidRows,
  onApply,
  onClose,
  onDownloadTemplate,
  onFileChange,
  onStrategyChange,
  previewCards,
  reading,
  strategy,
}) {
  return (
    <div className="fixed inset-0 z-[60] flex items-center justify-center bg-[#1a0004]/45 p-4 backdrop-blur-sm" role="dialog" aria-modal="true">
      <div className="max-h-[92dvh] w-full max-w-3xl overflow-y-auto rounded-2xl border border-[#dcc0bf] bg-white p-6 shadow-[0_30px_90px_rgba(48,0,8,0.28)]">
        <div className="flex items-center justify-between gap-4">
          <div>
            <h2 className="font-['Manrope'] text-xl font-extrabold text-[#4b0009]">Nhập Flashcard từ Excel</h2>
            <p className="mt-1 text-sm text-[#584140]">Dùng bốn cột: Thuật ngữ, Định nghĩa, Ví dụ, Lỗi thường gặp.</p>
          </div>
          <button aria-label="Đóng" className="rounded-lg border border-[#dcc0bf]/50 p-2 text-[#4b0009] hover:bg-[#eff4ff]" onClick={onClose} type="button"><X className="h-5 w-5" /></button>
        </div>
        <div className="mt-5 rounded-xl border border-dashed border-[#dcc0bf] bg-[#f8f9ff] p-6 text-center">
          <FileUp className="mx-auto h-8 w-8 text-[#4b0009]" />
          <p className="mt-3 font-bold text-[#4b0009]">Chọn tệp Excel có bốn cột</p>
          <label className="mt-4 inline-flex cursor-pointer items-center gap-2 rounded-lg bg-[#4b0009] px-4 py-3 text-sm font-bold text-white">
            <FileUp className="h-4 w-4" /> {reading ? 'Đang đọc tệp...' : 'Chọn tệp .xlsx hoặc .xls'}
            <input accept=".xlsx,.xls" className="sr-only" disabled={reading} onChange={(event) => { onFileChange(event.target.files?.[0]); event.target.value = ''; }} type="file" />
          </label>
          <button className="ml-2 mt-4 inline-flex items-center gap-2 rounded-lg border border-[#dcc0bf] bg-white px-4 py-3 text-sm font-bold text-[#4b0009] hover:bg-[#eff4ff]" onClick={onDownloadTemplate} type="button"><Download className="h-4 w-4" /> Tải bản mẫu Excel</button>
          {fileName ? <p className="mt-3 text-sm font-bold text-[#4b0009]">{fileName}</p> : null}
        </div>
        <div className="mt-5">
          <p className="mb-2 text-xs font-bold uppercase tracking-[0.12em] text-[#564241]">Cách nhập</p>
          <div className="grid grid-cols-2 gap-2 rounded-xl border border-[#dcc0bf]/30 bg-[#eff4ff] p-1">
            {[{ label: 'Nối thêm', value: 'APPEND' }, { label: 'Thay thế bộ hiện tại', value: 'REPLACE' }].map((option) => (
              <button className={`rounded-lg px-4 py-3 text-sm font-bold transition ${strategy === option.value ? 'bg-[#4b0009] text-white' : 'text-[#4b0009] hover:bg-white'}`} key={option.value} onClick={() => onStrategyChange(option.value)} type="button">{option.label}</button>
            ))}
          </div>
        </div>
        <div className="mt-5 rounded-xl border border-[#dcc0bf]/30 bg-[#f8f9ff] p-4">
          <div className="flex items-center justify-between gap-3"><p className="font-bold text-[#4b0009]">Xem trước</p><span className="text-sm text-[#564241]">{previewCards.length} thẻ hợp lệ</span></div>
          {invalidRows.length ? <p className="mt-2 text-sm text-[#93000a]">Dòng chưa đúng định dạng: {invalidRows.join(', ')}</p> : null}
          <div className="mt-3 max-h-56 space-y-2 overflow-y-auto">
            {previewCards.slice(0, 20).map((card) => <div className="grid gap-2 rounded-lg bg-white px-3 py-2 text-sm sm:grid-cols-2" key={card._key}><span className="font-bold text-[#0b1c30]">{card.term}</span><span className="text-[#564241]">{card.meaning}</span><span className="text-[#564241]">{card.example || 'Chưa có ví dụ'}</span><span className="text-[#564241]">{card.commonMistake || 'Chưa có lỗi thường gặp'}</span></div>)}
            {!previewCards.length ? <p className="text-sm text-[#564241]">Chưa có dữ liệu hợp lệ để xem trước.</p> : null}
          </div>
        </div>
        <div className="mt-5 flex justify-end gap-3"><button className="rounded-lg border border-[#dcc0bf] px-4 py-3 text-sm font-bold text-[#4b0009]" onClick={onClose} type="button">Hủy</button><button className="rounded-lg bg-[#4b0009] px-5 py-3 text-sm font-bold text-white disabled:opacity-50" disabled={!previewCards.length} onClick={onApply} type="button">Nhập {previewCards.length || ''} thẻ</button></div>
      </div>
    </div>
  );
}
