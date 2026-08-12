import BrandedSelect from '../ui/BrandedSelect';

const methodOptions = [
  { label: 'Dán văn bản', value: 'TEXT' },
  { label: 'Tệp Excel', value: 'EXCEL' },
];

const strategyOptions = [
  { label: 'Thêm vào danh sách hiện tại', value: 'APPEND' },
  { label: 'Thay thế toàn bộ danh sách', value: 'REPLACE' },
];

const delimiterOptions = [
  { label: 'Tab', value: '\t' },
  { label: 'Dấu phẩy', value: ',' },
];

const rowDelimiterOptions = [
  { label: 'Dòng mới', value: '\n' },
  { label: 'Dấu chấm phẩy', value: ';' },
];

export default function FlashcardImportDialog({
  importMethod,
  invalidRows,
  onApply,
  onClose,
  onDownloadTemplate,
  onExcelFileChange,
  onImportMethodChange,
  onRowDelimiterChange,
  onStrategyChange,
  onTermDelimiterChange,
  onTextChange,
  previewCards,
  readingSpreadsheet,
  rowDelimiter,
  spreadsheetFileName,
  strategy,
  termDelimiter,
  text,
}) {
  const validCardCount = previewCards.length;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 p-4" role="dialog" aria-modal="true" aria-labelledby="flashcard-import-title">
      <button aria-label="Đóng" className="absolute inset-0 cursor-default" onClick={onClose} type="button" />
      <section className="relative w-full max-w-2xl rounded-3xl border border-[#dcc0bf]/45 bg-white p-6 shadow-2xl">
        <div className="flex items-start justify-between gap-4">
          <div>
            <p className="text-xs font-bold uppercase tracking-[0.16em] text-[#8b706e]">Nhập bộ thẻ</p>
            <h2 id="flashcard-import-title" className="mt-1 text-2xl font-extrabold text-[#172033]">Thêm thẻ từ dữ liệu có sẵn</h2>
            <p className="mt-2 text-sm text-[#5d6779]">Mỗi dòng gồm thuật ngữ, định nghĩa, ví dụ và lỗi thường gặp.</p>
          </div>
          <button className="rounded-xl border border-[#dfbfbd]/70 px-3 py-2 text-sm font-bold text-[#730014] transition hover:bg-[#fff2f3]" onClick={onClose} type="button">Đóng</button>
        </div>

        <div className="mt-6 grid gap-4 sm:grid-cols-2">
          <Field label="Nguồn nhập"><BrandedSelect value={importMethod} onChange={(event) => onImportMethodChange(event.target.value)} options={methodOptions} /></Field>
          <Field label="Cách cập nhật"><BrandedSelect value={strategy} onChange={(event) => onStrategyChange(event.target.value)} options={strategyOptions} /></Field>
        </div>

        {importMethod === 'EXCEL' ? (
          <div className="mt-4 rounded-2xl border border-dashed border-[#dfbfbd] bg-[#fffafb] p-5">
            <p className="text-sm font-semibold text-[#4b0009]">Tải tệp Excel</p>
            <p className="mt-1 text-sm text-[#6f5553]">Tệp gồm các cột: term, meaning, example, commonError.</p>
            <div className="mt-4 flex flex-wrap items-center gap-3">
              <label className="cursor-pointer rounded-xl bg-[#730014] px-4 py-2.5 text-sm font-bold text-white transition hover:bg-[#59000b]">
                {readingSpreadsheet ? 'Đang đọc tệp...' : 'Chọn tệp .xlsx'}
                <input accept=".xlsx,.xls" className="hidden" disabled={readingSpreadsheet} onChange={(event) => onExcelFileChange(event.target.files?.[0])} type="file" />
              </label>
              <button className="text-sm font-bold text-[#730014] underline underline-offset-4" onClick={onDownloadTemplate} type="button">Tải mẫu Excel</button>
              {spreadsheetFileName ? <span className="text-sm text-[#5d6779]">{spreadsheetFileName}</span> : null}
            </div>
          </div>
        ) : (
          <>
            <div className="mt-4 grid gap-4 sm:grid-cols-2">
              <Field label="Ngăn cách cột"><BrandedSelect value={termDelimiter} onChange={(event) => onTermDelimiterChange(event.target.value)} options={delimiterOptions} /></Field>
              <Field label="Ngăn cách dòng"><BrandedSelect value={rowDelimiter} onChange={(event) => onRowDelimiterChange(event.target.value)} options={rowDelimiterOptions} /></Field>
            </div>
            <label className="mt-4 block text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">
              Dữ liệu thẻ
              <textarea className="mt-2 min-h-40 w-full rounded-2xl border border-[#dfbfbd]/70 bg-[#fcfbfb] p-4 text-sm normal-case tracking-normal text-[#1a1c1c] outline-none transition focus:border-[#730014] focus:bg-white" onChange={(event) => onTextChange(event.target.value)} placeholder={'term\tmeaning\texample\tcommon error'} value={text} />
            </label>
          </>
        )}

        <div className="mt-5 rounded-2xl bg-[#f8f9ff] px-4 py-3 text-sm text-[#40506a]">
          Tìm thấy <strong>{validCardCount}</strong> thẻ hợp lệ.
          {invalidRows.length ? <span className="ml-2 text-amber-700">Bỏ qua dòng: {invalidRows.join(', ')}.</span> : null}
        </div>

        <div className="mt-6 flex justify-end gap-3">
          <button className="rounded-xl border border-[#dfbfbd]/70 px-4 py-2.5 text-sm font-bold text-[#584140] transition hover:bg-[#fff2f3]" onClick={onClose} type="button">Hủy</button>
          <button className="rounded-xl bg-[#730014] px-4 py-2.5 text-sm font-bold text-white transition hover:bg-[#59000b] disabled:cursor-not-allowed disabled:opacity-50" disabled={!validCardCount} onClick={onApply} type="button">Nhập {validCardCount || ''} thẻ</button>
        </div>
      </section>
    </div>
  );
}

function Field({ label, children }) {
  return (
    <label className="block text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">
      {label}
      <div className="mt-2 normal-case tracking-normal">{children}</div>
    </label>
  );
}
