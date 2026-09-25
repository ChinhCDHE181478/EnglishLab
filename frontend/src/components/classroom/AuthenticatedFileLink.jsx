import { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { Loader2, X } from 'lucide-react';
import {
  downloadFileBlob,
  fetchProtectedFileBlob,
  isImageAttachment,
  isProtectedAttachmentUrl,
} from '../../utils/protectedFile';

/**
 * A flexible file link component.
 *
 * Protected images are fetched with the current access token and shown in a preview.
 * Protected documents are downloaded instead of navigating to an authenticated API URL.
 *
 * - `forceDownload={true}`: renders a <button> that always tries to download the file.
 *   - Protected URLs (internal backend / R2 via proxy) → fetched through authenticated backend.
 *   - External URLs → fetched directly as blob (CORS permitting), fallback to window.open.
 *   Use this for homework attachments, classroom materials, any file you want saved to disk.
 */
export default function AuthenticatedFileLink({
  children,
  className = '',
  containerClassName = 'inline-flex',
  fileName = '',
  forceDownload = false,
  title,
  url,
  ...rest
}) {
  const [downloading, setDownloading] = useState(false);
  const [error, setError] = useState('');
  const [previewUrl, setPreviewUrl] = useState('');

  useEffect(() => {
    if (!previewUrl) return undefined;
    const handleKeyDown = (event) => {
      if (event.key === 'Escape') setPreviewUrl('');
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      URL.revokeObjectURL(previewUrl);
    };
  }, [previewUrl]);

  if (!url) return null;

  const protectedUrl = isProtectedAttachmentUrl(url);

  if (!forceDownload && !protectedUrl) {
    return (
      <a className={className} href={url} rel="noreferrer" target="_blank" title={title} {...rest}>
        {children}
      </a>
    );
  }

  const closePreview = () => setPreviewUrl('');

  const handleFileAccess = async () => {
    if (downloading) return;
    setDownloading(true);
    setError('');
    try {
      if (protectedUrl) {
        const blob = await fetchProtectedFileBlob(url);
        if (isImageAttachment(url, fileName, blob.type)) {
          setPreviewUrl(URL.createObjectURL(blob));
        } else {
          downloadFileBlob(blob, url, fileName);
        }
      } else {
        const response = await fetch(url);
        if (!response.ok) throw new Error('fetch failed');
        const blob = await response.blob();
        downloadFileBlob(blob, url, fileName);
      }
    } catch {
      if (protectedUrl) {
        setError('Không thể mở tệp hoặc bạn không có quyền truy cập.');
      } else {
        window.open(url, '_blank', 'noopener,noreferrer');
      }
    } finally {
      setDownloading(false);
    }
  };

  return (
    <span className={`${containerClassName} flex-col items-start gap-1`}>
      <button
        className={className}
        disabled={downloading}
        onClick={handleFileAccess}
        title={title}
        type="button"
        {...rest}
      >
        {downloading ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : null}
        {children}
      </button>
      {error ? <span className="text-[10px] font-bold text-red-600" role="alert">{error}</span> : null}
      {previewUrl ? createPortal(
        <div
          aria-label="Xem ảnh đính kèm"
          aria-modal="true"
          className="fixed inset-0 z-[100] flex items-center justify-center bg-slate-950/80 p-4 backdrop-blur-sm"
          onClick={closePreview}
          role="dialog"
        >
          <button
            aria-label="Đóng ảnh"
            className="absolute right-5 top-5 rounded-full bg-white/95 p-2 text-slate-700 shadow-lg transition hover:bg-white"
            onClick={closePreview}
            type="button"
          >
            <X className="h-5 w-5" />
          </button>
          <img
            alt={fileName || 'Ảnh đính kèm'}
            className="max-h-[90vh] max-w-[95vw] rounded-2xl bg-white object-contain shadow-2xl"
            onClick={(event) => event.stopPropagation()}
            src={previewUrl}
          />
        </div>,
        document.body,
      ) : null}
    </span>
  );
}
