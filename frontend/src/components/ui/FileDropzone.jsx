import { useRef, useState } from 'react';
import {
  AlertCircle,
  CheckCircle2,
  FileArchive,
  FileCode,
  FileSpreadsheet,
  FileText,
  Film,
  Image as ImageIcon,
  Loader2,
  Music,
  Trash2,
  UploadCloud,
} from 'lucide-react';

const formatBytes = (bytes) => {
  if (!bytes || bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(1))} ${sizes[i]}`;
};

const getFileMeta = (fileName = '', fileType = '') => {
  const ext = fileName.split('.').pop()?.toLowerCase() || fileType.split('/').pop()?.toLowerCase() || '';
  if (['pdf'].includes(ext)) return { label: 'PDF', bg: 'bg-rose-100 text-rose-700 border-rose-200', Icon: FileText };
  if (['doc', 'docx', 'txt', 'rtf'].includes(ext)) return { label: 'DOC', bg: 'bg-blue-100 text-blue-700 border-blue-200', Icon: FileText };
  if (['xls', 'xlsx', 'csv'].includes(ext)) return { label: 'XLS', bg: 'bg-emerald-100 text-emerald-700 border-emerald-200', Icon: FileSpreadsheet };
  if (['ppt', 'pptx'].includes(ext)) return { label: 'PPT', bg: 'bg-amber-100 text-amber-700 border-amber-200', Icon: FileText };
  if (['zip', 'rar', '7z', 'tar', 'gz'].includes(ext)) return { label: 'ZIP', bg: 'bg-purple-100 text-purple-700 border-purple-200', Icon: FileArchive };
  if (['jpg', 'jpeg', 'png', 'gif', 'webp', 'svg'].includes(ext)) return { label: 'IMG', bg: 'bg-sky-100 text-sky-700 border-sky-200', Icon: ImageIcon };
  if (['mp4', 'mov', 'avi', 'mkv', 'webm'].includes(ext)) return { label: 'VIDEO', bg: 'bg-violet-100 text-violet-700 border-violet-200', Icon: Film };
  if (['mp3', 'wav', 'ogg', 'm4a'].includes(ext)) return { label: 'AUDIO', bg: 'bg-teal-100 text-teal-700 border-teal-200', Icon: Music };
  return { label: ext.toUpperCase() || 'FILE', bg: 'bg-slate-100 text-slate-700 border-slate-200', Icon: FileCode };
};

export default function FileDropzone({
  onFileSelect,
  uploading = false,
  accept = '.pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.txt,.zip,.rar,.jpg,.jpeg,.png',
  maxSizeMB = 50,
  fileUrl = '',
  fileName = '',
  fileSize = null,
  fileType = '',
  onClear,
  label = 'Tải tệp tài liệu / học liệu lên',
  hint = 'Hỗ trợ PDF, DOCX, XLSX, PPTX, ZIP, MP3, MP4 và hình ảnh',
  className = '',
}) {
  const [isDragging, setIsDragging] = useState(false);
  const [dragError, setDragError] = useState('');
  const fileInputRef = useRef(null);

  const handleDragOver = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (!isDragging) setIsDragging(true);
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
  };

  const processFile = (file) => {
    if (!file) return;
    setDragError('');
    if (file.size > maxSizeMB * 1024 * 1024) {
      setDragError(`Tệp quá lớn. Dung lượng tối đa là ${maxSizeMB}MB.`);
      return;
    }
    onFileSelect?.(file);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
    const files = e.dataTransfer?.files;
    if (files && files.length > 0) {
      processFile(files[0]);
    }
  };

  const handleInputChange = (e) => {
    const files = e.target.files;
    if (files && files.length > 0) {
      processFile(files[0]);
      e.target.value = '';
    }
  };

  const hasFile = Boolean(fileUrl || fileName);
  const displayTitle = fileName || (fileUrl ? fileUrl.split('/').pop() : '');
  const { label: extLabel, bg: badgeStyle, Icon: FileTypeIcon } = getFileMeta(displayTitle, fileType);

  return (
    <div className={`space-y-2 ${className}`}>
      {label ? (
        <label className="block text-xs font-bold uppercase tracking-[0.14em] text-[#8b706e]">
          {label}
        </label>
      ) : null}

      {/* Hidden File Input */}
      <input
        accept={accept}
        className="hidden"
        disabled={uploading}
        onChange={handleInputChange}
        ref={fileInputRef}
        type="file"
      />

      {/* Uploading Progress Box */}
      {uploading ? (
        <div className="relative overflow-hidden rounded-2xl border border-[#dfbfbd] bg-[#fffafb] p-5 text-center shadow-sm">
          <div className="flex items-center justify-center gap-3">
            <Loader2 className="h-6 w-6 animate-spin text-[#730014]" />
            <span className="text-sm font-bold text-[#0b1c30]">Đang tải tệp lên hệ thống...</span>
          </div>
          <div className="mt-3 h-1.5 w-full overflow-hidden rounded-full bg-[#f2e6e7]">
            <div className="h-full w-2/3 animate-pulse rounded-full bg-[#730014]" />
          </div>
        </div>
      ) : hasFile ? (
        /* Selected File Card */
        <div className="group relative flex items-center justify-between gap-4 rounded-2xl border border-[#dcc0bf]/60 bg-white p-4 shadow-sm transition hover:border-[#730014]">
          <div className="flex min-w-0 items-center gap-3.5">
            <div className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-xl border ${badgeStyle}`}>
              <FileTypeIcon className="h-5 w-5" />
            </div>
            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-2">
                <span className={`rounded-md border px-1.5 py-0.5 text-[10px] font-extrabold uppercase ${badgeStyle}`}>
                  {extLabel}
                </span>
                <p className="truncate text-sm font-bold text-[#0b1c30]" title={displayTitle}>
                  {displayTitle}
                </p>
              </div>
              <div className="mt-1 flex items-center gap-3 text-xs text-slate-500">
                {fileSize ? <span>{formatBytes(fileSize)}</span> : null}
                <span className="inline-flex items-center gap-1 font-semibold text-emerald-700">
                  <CheckCircle2 className="h-3.5 w-3.5" /> Sẵn sàng
                </span>
              </div>
            </div>
          </div>

          <div className="flex items-center gap-2 shrink-0">
            <button
              className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs font-bold text-slate-700 transition hover:bg-slate-100"
              onClick={() => fileInputRef.current?.click()}
              type="button"
            >
              Đổi tệp
            </button>
            {onClear ? (
              <button
                aria-label="Xóa tệp đã chọn"
                className="inline-flex h-8 w-8 items-center justify-center rounded-xl border border-rose-200 text-rose-700 transition hover:bg-rose-50"
                onClick={onClear}
                title="Xóa tệp"
                type="button"
              >
                <Trash2 className="h-4 w-4" />
              </button>
            ) : null}
          </div>
        </div>
      ) : (
        /* Empty Drag & Drop Area */
        <div
          className={`relative cursor-pointer overflow-hidden rounded-2xl border-2 border-dashed p-6 text-center transition-all duration-200 ${
            isDragging
              ? 'scale-[1.01] border-[#730014] bg-[#fff0f2] shadow-lg ring-4 ring-[#730014]/10'
              : 'border-[#dfbfbd]/80 bg-[#fffdfd] hover:border-[#730014] hover:bg-[#fff9fa]'
          }`}
          onClick={() => fileInputRef.current?.click()}
          onDragLeave={handleDragLeave}
          onDragOver={handleDragOver}
          onDrop={handleDrop}
          role="button"
          tabIndex={0}
        >
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl border border-[#f0d5d7] bg-white shadow-sm transition group-hover:scale-110">
            <UploadCloud className={`h-6 w-6 transition ${isDragging ? 'text-[#730014] animate-bounce' : 'text-[#8b706e]'}`} />
          </div>

          <div className="mt-3 space-y-1">
            <p className="text-sm font-bold text-[#0b1c30]">
              <span className="text-[#730014] underline underline-offset-2">Nhấp để chọn tệp</span> hoặc kéo & thả tệp vào đây
            </p>
            {hint ? <p className="text-xs text-slate-500">{hint}</p> : null}
          </div>

          {dragError ? (
            <p className="mt-2.5 inline-flex items-center gap-1.5 rounded-lg bg-rose-50 px-3 py-1 text-xs font-bold text-rose-700">
              <AlertCircle className="h-3.5 w-3.5" /> {dragError}
            </p>
          ) : null}
        </div>
      )}
    </div>
  );
}
