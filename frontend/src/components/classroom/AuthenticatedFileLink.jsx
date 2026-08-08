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

  if (!isProtectedAttachmentUrl(url)) {
    return (
      <a className={className} href={url} rel="noreferrer" target="_blank" title={title} {...rest}>
        {children}
      </a>
    );
  }

  const handleDownload = async () => {
    if (downloading) return;
    setDownloading(true);
    setError('');
    try {
      await downloadProtectedFile(url, fileName);
    } catch {
      setError('Không thể tải tệp hoặc bạn không có quyền truy cập.');
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
