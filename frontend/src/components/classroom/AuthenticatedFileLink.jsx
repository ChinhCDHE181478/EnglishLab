import { useState } from 'react';
import { Loader2 } from 'lucide-react';
import { downloadProtectedFile, isProtectedAttachmentUrl } from '../../utils/protectedFile';

/**
 * A flexible file link component.
 *
 * - `forceDownload={false}` (default): renders a plain <a> link that opens in a new tab.
 *   Use this for navigation links, YouTube, Google Drive, etc.
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

  if (!url) return null;

  // ── Link mode (default) ─────────────────────────────────────────────────────
  if (!forceDownload) {
    return (
      <a className={className} href={url} rel="noreferrer" target="_blank" title={title} {...rest}>
        {children}
      </a>
    );
  }

  // ── Download mode ───────────────────────────────────────────────────────────
  const handleDownload = async () => {
    if (downloading) return;
    setDownloading(true);
    setError('');
    try {
      if (isProtectedAttachmentUrl(url)) {
        // Internal / R2 via backend proxy — uses authenticated axiosClient
        await downloadProtectedFile(url, fileName);
      } else {
        // External public URL — try direct fetch to force blob download
        const response = await fetch(url);
        if (!response.ok) throw new Error('fetch failed');
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
      // Fallback: open in new tab so the user can still access the file
      window.open(url, '_blank');
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
