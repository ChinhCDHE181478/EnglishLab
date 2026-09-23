import { useState } from 'react';
import { Loader2 } from 'lucide-react';
import { downloadProtectedFile, isProtectedAttachmentUrl } from '../../utils/protectedFile';

export default function AuthenticatedFileLink({
  children,
  className = '',
  containerClassName = 'inline-flex',
  fileName = '',
  title,
  url,
  ...rest
}) {
  const [downloading, setDownloading] = useState(false);
  const [error, setError] = useState('');

  if (!url) return null;

  // Always render a button to intercept clicks and force downloads where possible.

  const handleDownload = async () => {
    if (downloading) return;
    setDownloading(true);
    setError('');
    try {
      if (isProtectedAttachmentUrl(url)) {
        await downloadProtectedFile(url, fileName);
      } else {
        // Attempt to fetch public URL to force download
        const response = await fetch(url);
        if (!response.ok) throw new Error('Network response was not ok');
        const blob = await response.blob();
        const objectUrl = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = objectUrl;
        anchor.download = fileName || String(url).split('/').pop()?.split('?')[0] || 'tep-dinh-kem';
        document.body.appendChild(anchor);
        anchor.click();
        anchor.remove();
        window.setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000);
      }
    } catch {
      if (!isProtectedAttachmentUrl(url)) {
        // Fallback for public URLs if fetch fails (e.g., due to CORS)
        window.open(url, '_blank');
      } else {
        setError('Không thể tải tệp hoặc bạn không có quyền truy cập.');
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
        onClick={handleDownload}
        title={title}
        type="button"
        {...rest}
      >
        {downloading ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : null}
        {children}
      </button>
      {error ? <span className="text-[10px] font-bold text-red-600" role="alert">{error}</span> : null}
    </span>
  );
}
