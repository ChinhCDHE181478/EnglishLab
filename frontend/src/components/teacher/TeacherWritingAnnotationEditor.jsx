import React, { useEffect, useRef, useState } from 'react';
import {
  MessageSquareText,
  RefreshCw,
  Save,
  Trash2,
  X,
} from 'lucide-react';
import HomeworkAnnotatedText from '../classroom/HomeworkAnnotatedText';
import {
  overlapsExistingAnnotation,
  selectionToTextRange,
} from '../../utils/homeworkTextAnnotations';

const createId = () =>
  globalThis.crypto?.randomUUID?.() ||
  `annotation-${Date.now()}-${Math.random().toString(16).slice(2)}`;

export default function TeacherWritingAnnotationEditor({ text, annotations = [], onChange }) {
  const textRef = useRef(null);
  const [selection, setSelection] = useState(null);
  const [popPosition, setPopPosition] = useState(null);
  const [editing, setEditing] = useState(null);
  const [noteDraft, setNoteDraft] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    setSelection(null);
    setPopPosition(null);
    setEditing(null);
    setNoteDraft('');
    setError('');
  }, [text]);

  const captureSelection = () => {
    const selectedRange = selectionToTextRange(textRef.current);
    if (!selectedRange) return;

    if (annotations.length >= 100) {
      setError('Mỗi bài làm chỉ được có tối đa 100 nhận xét theo đoạn.');
      return;
    }
    if (selectedRange.selectedText.length > 2000) {
      setError('Mỗi đoạn được chọn không được vượt quá 2.000 ký tự.');
      return;
    }
    if (overlapsExistingAnnotation(selectedRange, annotations)) {
      setError('Đoạn này đang chồng lên một nhận xét khác. Hãy chọn một đoạn riêng biệt.');
      return;
    }

    // Compute floating popover position relative to textRef container
    const windowSel = window.getSelection();
    if (windowSel && windowSel.rangeCount > 0 && textRef.current) {
      const range = windowSel.getRangeAt(0);
      const rect = range.getBoundingClientRect();
      const containerRect = textRef.current.getBoundingClientRect();
      const top = Math.max(10, rect.top - containerRect.top - 55);
      const left = Math.min(
        Math.max(10, rect.left - containerRect.left + rect.width / 2 - 70),
        Math.max(10, containerRect.width - 160)
      );
      setPopPosition({ top, left });
    }

    setError('');
    setSelection(selectedRange);
    setEditing(null);
    setNoteDraft('');
  };

  const openNew = () => {
    if (!selection) return;
    setEditing({ ...selection, id: createId(), isNew: true });
    setNoteDraft('');
    setError('');
  };

  const openExisting = (annotation) => {
    setSelection(null);
    setPopPosition(null);
    setEditing({ ...annotation, isNew: false });
    setNoteDraft(annotation.note || annotation.replacementText || '');
    setError('');
  };

  const save = async () => {
    const value = noteDraft.trim();

    if (!value) {
      setError('Vui lòng nhập nội dung nhận xét hoặc sửa lỗi.');
      return;
    }
    if (value.length > 2000) {
      setError('Nội dung nhận xét không được vượt quá 2.000 ký tự.');
      return;
    }

    const saved = {
      id: editing.id,
      type: 'NOTE',
      startOffset: editing.startOffset,
      endOffset: editing.endOffset,
      selectedText: editing.selectedText,
      replacementText: '',
      note: value,
    };

    const nextAnnotations = editing.isNew
      ? [...annotations, saved]
      : annotations.map((item) => (item.id === editing.id ? saved : item));
    setSaving(true);
    setError('');
    try {
      await onChange(nextAnnotations);
      setSelection(null);
      setPopPosition(null);
      setEditing(null);
      setNoteDraft('');
      window.getSelection()?.removeAllRanges();
    } catch (saveError) {
      setError(saveError?.message || 'Không thể lưu nhận xét. Vui lòng thử lại.');
    } finally {
      setSaving(false);
    }
  };

  const removeAnnotation = async (id) => {
    setSaving(true);
    setError('');
    try {
      await onChange(annotations.filter((item) => item.id !== id));
      if (editing?.id === id) {
        setEditing(null);
        setNoteDraft('');
      }
    } catch (saveError) {
      setError(saveError?.message || 'Không thể xóa nhận xét. Vui lòng thử lại.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="relative space-y-4" onMouseUp={captureSelection}>
      {/* Top Banner */}
      <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-[#dfbfbd]/35 bg-[#fffafb] px-4 py-2.5">
        <div className="flex items-center gap-2">
          <MessageSquareText className="h-4 w-4 text-[#730014]" />
          <p className="text-xs font-bold text-[#1a1c1c]">
            Bôi đen văn bản để tạo nhận xét &amp; sửa lỗi cho học viên
          </p>
        </div>

        <div className="flex items-center gap-2 text-xs font-bold">
          <span className="text-[#730014] bg-[#730014]/10 px-2.5 py-0.5 rounded-md border border-[#730014]/20">
            {annotations.length} nhận xét
          </span>
        </div>
      </div>

      {/* SINGLE Floating Selection Button Toolbar: Red Accent with "Nhận xét" */}
      {selection && !editing && popPosition ? (
        <div
          style={{ top: `${popPosition.top}px`, left: `${popPosition.left}px` }}
          className="absolute z-30 flex items-center gap-1.5 rounded-2xl border border-rose-200 bg-white p-1.5 shadow-xl ring-4 ring-[#730014]/5 transition-all animate-in fade-in zoom-in-95"
        >
          <button
            type="button"
            onClick={openNew}
            className="inline-flex items-center gap-1.5 rounded-xl bg-[#730014] px-3.5 py-2 text-xs font-extrabold text-white shadow-xs transition hover:bg-[#8a0018] active:scale-95"
          >
            <MessageSquareText className="h-4 w-4" />
            Nhận xét
          </button>

          <button
            type="button"
            onClick={() => {
              setSelection(null);
              setPopPosition(null);
              window.getSelection()?.removeAllRanges();
            }}
            aria-label="Bỏ chọn"
            className="inline-flex h-8 w-8 items-center justify-center rounded-xl border border-gray-200 bg-white text-gray-500 hover:bg-gray-50 active:scale-95"
            title="Bỏ chọn"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
      ) : null}

      {/* Writing Paper Canvas */}
      <HomeworkAnnotatedText
        annotations={annotations}
        containerRef={textRef}
        onAnnotationClick={openExisting}
        onRemoveAnnotation={removeAnnotation}
        showDetails={true}
        editable={true}
        text={text}
      />

      {/* Single Textarea Comment Card */}
      {editing ? (
        <div className="rounded-2xl border border-[#730014]/30 bg-[#fffafb] p-5 shadow-sm space-y-4">
          <div className="flex items-start justify-between gap-3 border-b border-gray-200/60 pb-3">
            <div>
              <span className="inline-block rounded-md bg-[#730014] px-2.5 py-0.5 text-[10px] font-extrabold uppercase tracking-wider text-white">
                Nhận xét đoạn văn
              </span>
              <p className="mt-2 text-xs italic font-medium text-slate-700 line-clamp-2">
                "&nbsp;{editing.selectedText}&nbsp;"
              </p>
            </div>

            <button
              type="button"
              onClick={() => setEditing(null)}
              aria-label="Đóng"
              className="rounded-lg p-1 text-slate-500 hover:bg-white transition"
            >
              <X className="h-4 w-4" />
            </button>
          </div>

          <div className="space-y-1.5">
            <label className="block text-xs font-bold text-slate-700">
              Nhập nội dung nhận xét hoặc sửa lỗi:
            </label>
            <textarea
              autoFocus
              className="min-h-[95px] w-full rounded-xl border border-gray-300 bg-white p-3.5 text-sm leading-relaxed text-slate-800 outline-none transition focus:border-[#730014] focus:ring-2 focus:ring-[#730014]/10"
              maxLength={2000}
              onChange={(e) => setNoteDraft(e.target.value)}
              placeholder="Nhập câu sửa lại hoặc ghi chú giải thích lỗi cho học viên..."
              value={noteDraft}
            />
          </div>

          <div className="flex items-center justify-between pt-1">
            <span className="text-[11px] text-slate-500">
              {noteDraft.length} / 2.000 ký tự
            </span>
            <div className="flex gap-2">
              {!editing.isNew ? (
                <button
                  type="button"
                  onClick={() => removeAnnotation(editing.id)}
                  disabled={saving}
                  className="inline-flex items-center gap-1.5 rounded-xl border border-rose-200 bg-white px-3.5 py-2 text-xs font-bold text-rose-700 hover:bg-rose-50 transition active:scale-95 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  <Trash2 className="h-3.5 w-3.5" /> Xóa
                </button>
              ) : null}
              <button
                type="button"
                onClick={save}
                disabled={saving}
                className="inline-flex items-center gap-1.5 rounded-xl bg-[#730014] px-4 py-2 text-xs font-bold text-white shadow-xs transition hover:bg-[#8a0018] active:scale-95 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {saving ? <RefreshCw className="h-3.5 w-3.5 animate-spin" /> : <Save className="h-3.5 w-3.5" />}
                {saving ? 'Đang lưu...' : 'Lưu nhận xét'}
              </button>
            </div>
          </div>
        </div>
      ) : null}

      {error ? (
        <div className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-xs font-bold text-rose-700">
          {error}
        </div>
      ) : null}
    </div>
  );
}
