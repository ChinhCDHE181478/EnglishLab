import BrandedSelect from '../ui/BrandedSelect';
import {
  CEFR_LEVEL_OPTIONS,
  IELTS_BAND_OPTIONS,
  TOEIC_SCORE_PRESETS,
} from '../../utils/englishProgramProfile';
import { FIELD_CLASS } from '../../utils/formStyles';

const LABEL_CLASS = 'mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]';

export function IeltsBandSelect({
  label,
  value,
  onChange,
  allowEmpty = false,
  emptyLabel = 'Chưa xác định',
}) {
  const options = allowEmpty
    ? [{ label: emptyLabel, value: '' }, ...IELTS_BAND_OPTIONS]
    : IELTS_BAND_OPTIONS;

  return (
    <label className="block">
      <span className={LABEL_CLASS}>{label}</span>
      <BrandedSelect
        onChange={(event) => onChange(event.target.value)}
        options={options}
        placeholder="Chọn band IELTS"
        searchable
        value={value}
      />
    </label>
  );
}

export function ToeicScoreField({
  label,
  value,
  onChange,
  helperText = 'Nhập điểm từ 10 đến 990, theo bước 5.',
}) {
  return (
    <fieldset className="min-w-0">
      <legend className={LABEL_CLASS}>{label}</legend>
      <input
        aria-label={label}
        className={FIELD_CLASS}
        inputMode="numeric"
        max="990"
        min="10"
        onChange={(event) => onChange(event.target.value)}
        placeholder="Ví dụ: 650"
        step="5"
        type="number"
        value={value}
      />
      <div className="mt-2 flex flex-wrap gap-1.5" aria-label={`Mốc chọn nhanh cho ${label}`}>
        {TOEIC_SCORE_PRESETS.map((score) => (
          <button
            aria-pressed={String(value) === String(score)}
            className={`rounded-full border px-2.5 py-1 text-xs font-bold transition ${
              String(value) === String(score)
                ? 'border-[#730014] bg-[#730014] text-white'
                : 'border-[#dfbfbd] bg-white text-[#584140] hover:border-[#730014] hover:text-[#730014]'
            }`}
            key={score}
            onClick={() => onChange(String(score))}
            type="button"
          >
            {score}
          </button>
        ))}
      </div>
      <p className="mt-2 text-xs leading-5 text-[#806765]">{helperText}</p>
    </fieldset>
  );
}

export function EnglishEntryLevelField({ examCategory, value, onChange }) {
  if (examCategory === 'IELTS') {
    return (
      <IeltsBandSelect
        label="Band IELTS đầu vào"
        onChange={onChange}
        value={value}
      />
    );
  }

  if (examCategory === 'TOEIC') {
    return (
      <ToeicScoreField
        helperText="Điểm TOEIC hiện tại tối thiểu phù hợp với chương trình."
        label="Điểm TOEIC đầu vào"
        onChange={onChange}
        value={value}
      />
    );
  }

  return (
    <label className="block">
      <span className={LABEL_CLASS}>Trình độ CEFR đầu vào</span>
      <BrandedSelect
        onChange={(event) => onChange(event.target.value)}
        options={CEFR_LEVEL_OPTIONS}
        placeholder="Chọn trình độ CEFR"
        value={value}
      />
    </label>
  );
}
