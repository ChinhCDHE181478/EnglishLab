export default function ExamSectionChangeDialog({
  currentLabel,
  missingCount,
  onCancel,
  onConfirm,
  targetLabel,
  unitLabel = 'câu',
}) {
  return (
    <div className="fixed inset-0 z-[145] flex items-center justify-center bg-[#261112]/55 px-4">
      <div className="w-full max-w-md rounded-[28px] bg-white p-6 shadow-2xl">
        <p className="text-[11px] font-black uppercase tracking-[0.18em] text-[#b26a00]">Chưa hoàn thành phần hiện tại</p>
        <h3 className="mt-2 font-['Manrope'] text-2xl font-black text-[#341c1d]">
          Vẫn chuyển sang {targetLabel}?
        </h3>
        <p className="mt-3 text-sm leading-7 text-[#584140]">
          {currentLabel} còn thiếu {missingCount} {unitLabel} để đạt yêu cầu hoàn thành.
          Câu trả lời hiện tại vẫn được giữ và bạn có thể quay lại sau.
        </p>
        <div className="mt-5 flex gap-3">
          <button
            className="flex-1 rounded-2xl border border-[#dfbfbd] px-5 py-3 text-sm font-bold text-[#8a0018]"
            onClick={onCancel}
            type="button"
          >
            Ở lại hoàn thành
          </button>
          <button
            className="flex-1 rounded-2xl bg-[#8a0018] px-5 py-3 text-sm font-black text-white"
            onClick={onConfirm}
            type="button"
          >
            Vẫn chuyển phần
          </button>
        </div>
      </div>
    </div>
  );
}
