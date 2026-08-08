import { useEffect, useState } from 'react';
import { Loader2, Volume2 } from 'lucide-react';
import { fetchProtectedFileBlob, isProtectedAttachmentUrl } from '../../utils/protectedFile';

export default function AuthenticatedAudioPlayer({ url }) {
  const [source, setSource] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    let objectUrl = '';

    const loadAudio = async () => {
      if (!url) {
        setSource('');
        return;
      }
      if (!isProtectedAttachmentUrl(url)) {
        setSource(url);
        return;
      }

      setLoading(true);
      setError('');
      try {
        const blob = await fetchProtectedFileBlob(url);
        if (!active) return;
        objectUrl = URL.createObjectURL(blob);
        setSource(objectUrl);
      } catch {
        if (active) setError('Không thể tải bản ghi âm hoặc bạn không có quyền truy cập.');
      } finally {
        if (active) setLoading(false);
      }
    };

    loadAudio();
    return () => {
      active = false;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [url]);

  if (!url) return null;
  if (loading) {
    return <p className="inline-flex items-center gap-2 text-xs font-bold text-[#8b706e]"><Loader2 className="h-4 w-4 animate-spin" /> Đang tải bản ghi âm...</p>;
  }
  if (error) return <p className="text-xs font-bold text-rose-700" role="alert">{error}</p>;

  return (
    <div className="rounded-xl border border-sky-100 bg-sky-50/60 p-4">
      <p className="mb-3 inline-flex items-center gap-2 text-xs font-extrabold uppercase tracking-[0.12em] text-sky-800">
        <Volume2 className="h-4 w-4" /> Bản ghi âm của học viên
      </p>
      <audio className="w-full" controls preload="metadata" src={source}>
        Trình duyệt không hỗ trợ phát tệp âm thanh này.
      </audio>
    </div>
  );
}
