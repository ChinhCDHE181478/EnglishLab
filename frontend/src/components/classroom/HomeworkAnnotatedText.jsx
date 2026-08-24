import React from 'react';
import { MessageSquareText, Trash2 } from 'lucide-react';
import { buildAnnotatedTextSegments, normalizeHomeworkAnnotations } from '../../utils/homeworkTextAnnotations';

export default function HomeworkAnnotatedText({
  text = '',
  annotations = [],
  onAnnotationClick,
  onRemoveAnnotation,
  containerRef,
  className = '',
  canvasClassName = '',
  showDetails = true,
  editable = false,
}) {
  const normalized = normalizeHomeworkAnnotations(annotations, text);
  const segments = buildAnnotatedTextSegments(text, normalized);

  return (
    <div className={`space-y-4 ${className}`}>
      {/* Writing Paper Canvas */}
      <div
        ref={containerRef}
        data-annotation-canvas="true"
        className={`relative min-h-[220px] rounded-2xl border border-[#dfbfbd]/40 bg-white p-6 shadow-sm text-sm leading-loose text-slate-800 selection:bg-rose-100 selection:text-[#730014] ${canvasClassName}`}
      >
        {segments.map((segment, index) => {
          if (!segment.annotation) {
            return <span key={`plain-${index}`}>{segment.text}</span>;
          }

          const noteText = segment.annotation.note || segment.annotation.replacementText || '';

          return (
            <span
              key={segment.annotation.id}
              onClick={() => onAnnotationClick?.(segment.annotation)}
              className="group relative inline-flex items-center flex-wrap gap-1 rounded px-1.5 py-0.5 font-semibold transition cursor-pointer bg-rose-100/90 text-[#730014] border-b-2 border-[#730014] hover:bg-rose-200/90"
              title="Bấm để xem hoặc chỉnh sửa nhận xét"
            >
              <span>{segment.text}</span>

              {noteText ? (
                <span
                  aria-hidden="true"
                  className="ml-1 inline-flex h-4 w-4 items-center justify-center rounded-full bg-[#730014] text-white"
                  title={noteText}
                >
                  <MessageSquareText className="h-2.5 w-2.5" />
                </span>
              ) : null}
            </span>
          );
        })}
      </div>

      {/* Summary List of Annotations */}
      {showDetails && normalized.length > 0 ? (
        <div className="space-y-2.5 pt-1">
          <div className="flex items-center justify-between">
            <span className="text-xs font-extrabold uppercase tracking-widest text-[#730014]">
              Danh sách nhận xét ({normalized.length})
            </span>
          </div>

          <div className="grid gap-2.5 sm:grid-cols-2">
            {normalized.map((item, idx) => {
              const noteText = item.note || item.replacementText || '';
              return (
                <div
                  key={item.id}
                  onClick={() => onAnnotationClick?.(item)}
                  className="group relative flex items-start justify-between gap-3 rounded-xl border border-rose-200/80 bg-rose-50/50 p-3.5 transition cursor-pointer hover:bg-rose-50 hover:border-rose-300"
                >
                  <div className="flex gap-3 min-w-0">
                    <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-[#730014] text-xs font-bold text-white">
                      #{idx + 1}
                    </span>

                    <div className="min-w-0 text-xs">
                      <span className="font-extrabold uppercase tracking-wider text-[#730014] block">
                        Nhận xét #{idx + 1}
                      </span>
                      <p className="mt-0.5 text-slate-500 font-medium italic truncate">
                        "{item.selectedText}"
                      </p>

                      {noteText ? (
                        <p className="mt-1 text-slate-800 font-bold leading-relaxed flex items-center gap-1">
                          <MessageSquareText className="h-3.5 w-3.5 text-[#730014] shrink-0" />
                          <span>{noteText}</span>
                        </p>
                      ) : null}
                    </div>
                  </div>

                  {editable && onRemoveAnnotation ? (
                    <button
                      aria-label="Xóa nhận xét này"
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        onRemoveAnnotation(item.id);
                      }}
                      className="opacity-0 group-hover:opacity-100 transition p-1.5 text-rose-600 hover:bg-white rounded-lg"
                      title="Xóa nhận xét này"
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </button>
                  ) : null}
                </div>
              );
            })}
          </div>
        </div>
      ) : null}
    </div>
  );
}
