import { useEffect, useState } from 'react';
import { ExternalLink, FileText } from 'lucide-react';
import axiosClient from '../../api/axiosClient';
import AuthenticatedFileLink from './AuthenticatedFileLink';
import { isProtectedAttachmentUrl } from '../../utils/protectedFile';

const isImageUrl = (url = '', fileName = '') => {
  const source = `${fileName || ''} ${url || ''}`.toLowerCase();
  return /\.(jpe?g|png|gif|webp|bmp)(\?|#|$)/i.test(source);
};

const isPdfUrl = (url = '', fileName = '') => {
  const source = `${fileName || ''} ${url || ''}`.toLowerCase();
  return /\.pdf(\?|#|$)/i.test(source);
};

/**
 * Renders tuition transfer proof media for learner/TM review.
 * Images show inline preview; PDFs/other files keep open-in-new-tab link.
 */
export default function TuitionProofMedia({
  url,
  fileName,
  alt = 'Minh chứng chuyển khoản',
  className = '',
  imageClassName = 'max-h-64 w-full rounded-xl border border-[#ecdedd] object-contain bg-[#faf7f7]',
}) {
  const image = isImageUrl(url, fileName);
  const pdf = isPdfUrl(url, fileName);
  const [previewUrl, setPreviewUrl] = useState('');
  const [previewError, setPreviewError] = useState('');

  useEffect(() => {
    if (!url || !image || !isProtectedAttachmentUrl(url)) {
      setPreviewUrl(url || '');
      setPreviewError('');
      return undefined;
    }

    let active = true;
    let objectUrl = '';
    const loadPreview = async () => {
      setPreviewError('');
      try {
        const response = await axiosClient.get(url, { responseType: 'blob' });
        if (!active) return;
        objectUrl = URL.createObjectURL(response.data);
        setPreviewUrl(objectUrl);
      } catch {
        if (active) {
          setPreviewUrl('');
          setPreviewError('Không thể tải ảnh minh chứng hoặc bạn không có quyền truy cập.');
        }
      }
    };
    loadPreview();
    return () => {
      active = false;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [image, url]);

  if (!url) return null;

  // Ảnh: chỉ hiện preview (bấm để xem/tải), không thêm link bên dưới.
  if (image) {
    return (
      <div className={className}>
        {previewUrl ? (
          <img
            alt={alt}
            className={imageClassName}
            loading="lazy"
            src={previewUrl}
          />
        ) : (
          <div className={`${imageClassName} flex min-h-32 items-center justify-center px-4 text-center text-xs font-bold text-red-600`}>
            {previewError || 'Đang tải ảnh minh chứng...'}
          </div>
        )}
        <AuthenticatedFileLink
          className="mt-2 inline-flex items-center gap-1 text-[11px] font-bold text-[#730014] hover:underline"
          fileName={fileName}
          title="Tải ảnh minh chứng"
          url={url}
        >
          <ExternalLink className="h-3 w-3" /> Tải ảnh minh chứng
        </AuthenticatedFileLink>
      </div>
    );
  }

  // PDF / tệp khác: hiện thẻ + link mở/tải.
  return (
    <div className={`space-y-2 ${className}`.trim()}>
      <div className="flex items-center gap-2 rounded-xl border border-[#ecdedd] bg-[#fffafb] px-3 py-2.5">
        <FileText className="h-4 w-4 flex-shrink-0 text-[#730014]" />
        <div className="min-w-0 flex-1">
          <p className="truncate text-[11px] font-bold text-[#584140]">
            {fileName || (pdf ? 'Tệp PDF minh chứng' : 'Tệp minh chứng')}
          </p>
          <p className="text-[10px] text-[#8b706e]">
            {pdf ? 'Mở PDF trong tab mới để xem chi tiết.' : 'Mở tệp trong tab mới để xem.'}
          </p>
        </div>
      </div>
      <AuthenticatedFileLink
        className="inline-flex items-center gap-1 text-[11px] font-bold text-[#730014] hover:underline"
        fileName={fileName}
        url={url}
      >
        <ExternalLink className="h-3 w-3" />
        {pdf ? 'Tải PDF minh chứng' : 'Tải minh chứng'}
      </AuthenticatedFileLink>
    </div>
  );
}

export function LocalFilePreview({ file, className = '' }) {
  const [objectUrl, setObjectUrl] = useState('');
  const isImage = Boolean(file && String(file.type || '').startsWith('image/'));
  const isPdf = Boolean(file && (file.type === 'application/pdf' || /\.pdf$/i.test(file.name || '')));

  useEffect(() => {
    if (!file || !String(file.type || '').startsWith('image/')) {
      setObjectUrl('');
      return undefined;
    }
    const nextUrl = URL.createObjectURL(file);
    setObjectUrl(nextUrl);
    return () => URL.revokeObjectURL(nextUrl);
  }, [file]);

  if (!file) return null;

  if (isImage && objectUrl) {
    return (
      <div className={`space-y-2 ${className}`.trim()}>
        <img
          alt={file.name || 'Xem trước minh chứng'}
          className="max-h-56 w-full rounded-xl border border-[#ecdedd] object-contain bg-[#faf7f7]"
          src={objectUrl}
        />
        <p className="truncate text-center text-[11px] font-bold text-[#584140]">{file.name}</p>
      </div>
    );
  }

  return (
    <div className={`flex items-center gap-2 rounded-xl border border-[#ecdedd] bg-white px-3 py-2.5 ${className}`.trim()}>
      <FileText className="h-4 w-4 flex-shrink-0 text-[#730014]" />
      <div className="min-w-0">
        <p className="truncate text-xs font-bold text-[#584140]">{file.name}</p>
        <p className="text-[10px] text-[#8b706e]">
          {isPdf ? 'PDF đã chọn — gửi để Nhân viên đào tạo xem.' : 'Tệp đã chọn.'}
        </p>
      </div>
    </div>
  );
}
