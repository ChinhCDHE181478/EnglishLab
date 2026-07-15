import { useCallback, useEffect, useState } from 'react';
import { CheckCircle2, Clipboard, Printer, RotateCcw } from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import courseApi from '../api/courseApi';
import Header from '../components/ai-learning/Header';
import CertificatePreview from '../components/course/CertificatePreview';
import CourseFooter from '../components/course/CourseFooter';
import CourseGlobalStyles from '../components/course/CourseGlobalStyles';

const CertificateVerifyPage = () => {
  const { code = '' } = useParams();
  const [certificate, setCertificate] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [copyMessage, setCopyMessage] = useState('');

  const loadCertificate = useCallback(async () => {
    setLoading(true);
    setError('');
    setCertificate(null);
    try {
      const result = await courseApi.verifyCourseCertificate(code);
      if (!result?.verified || !result?.eligible) {
        setError('Chứng nhận này chưa đủ điều kiện xác thực.');
        return;
      }
      setCertificate(result);
    } catch (err) {
      setError(err?.response?.data?.message || 'Mã xác thực không hợp lệ hoặc chứng nhận không tồn tại.');
    } finally {
      setLoading(false);
    }
  }, [code]);

  useEffect(() => {
    loadCertificate();
  }, [loadCertificate]);

  const handleCopy = async () => {
    setCopyMessage('');
    try {
      await navigator.clipboard.writeText(certificate?.verificationCode || code);
      setCopyMessage('Đã sao chép mã xác thực.');
    } catch {
      setCopyMessage('Trình duyệt không hỗ trợ sao chép tự động.');
    }
  };

  return (
    <div id="top" className="course-page min-h-screen bg-[#f9f9f9] text-[#1a1c1c]">
      <CourseGlobalStyles />
      <div className="khong-in"><Header /></div>
      <main className="mx-auto max-w-[1200px] px-4 pb-20 pt-8 md:px-10 print:max-w-none print:p-0">
        {loading ? (
          <section className="rounded-3xl border border-[#ead9db] bg-white px-6 py-20 text-center shadow-sm">
            <p className="text-sm font-semibold text-[#584140]">Đang xác thực chứng nhận...</p>
          </section>
        ) : error ? (
          <section className="rounded-3xl border border-[#f0c9ce] bg-white px-6 py-16 text-center shadow-sm">
            <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-[#fff0f1] text-[#8a0018]"><RotateCcw className="h-6 w-6" /></div>
            <h1 className="mt-5 font-['Manrope'] text-3xl font-extrabold text-[#4b0009]">Không thể xác thực chứng nhận</h1>
            <p className="mx-auto mt-3 max-w-xl text-sm leading-7 text-[#584140]">{error}</p>
            <div className="mt-6 flex flex-wrap justify-center gap-3">
              <button className="rounded-2xl bg-[#4b0009] px-5 py-3 text-sm font-extrabold text-white" onClick={loadCertificate} type="button">Thử lại</button>
              <Link className="rounded-2xl border border-[#dfbfbd] bg-white px-5 py-3 text-sm font-extrabold text-[#730014]" to="/courses">Xem khóa học</Link>
            </div>
          </section>
        ) : certificate ? (
          <>
            <section className="khong-in mb-6 rounded-3xl border border-[#d7eadf] bg-[#f5fff8] px-5 py-4 shadow-sm">
              <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                <div className="flex items-start gap-3">
                  <CheckCircle2 className="mt-0.5 h-6 w-6 shrink-0 text-emerald-700" />
                  <div>
                    <h1 className="font-['Manrope'] text-xl font-extrabold text-emerald-800">Chứng nhận hợp lệ</h1>
                    <p className="mt-1 text-sm text-emerald-700 font-medium">
                      Chứng nhận cấp cho học viên <strong className="font-bold text-emerald-950 underline decoration-[#b9ddc6] underline-offset-4">{certificate.learnerName || 'Học viên EnglishLab'}</strong> đã được xác thực trực tiếp từ hệ thống EnglishLab.
                    </p>
                  </div>
                </div>
                <div className="flex flex-wrap gap-2">
                  <button className="inline-flex items-center gap-2 rounded-2xl bg-[#4b0009] px-4 py-3 text-sm font-extrabold text-white" onClick={() => window.print()} type="button"><Printer className="h-4 w-4" />In / Tải PDF</button>
                  <button className="inline-flex items-center gap-2 rounded-2xl border border-[#b9ddc6] bg-white px-4 py-3 text-sm font-extrabold text-emerald-800" onClick={handleCopy} type="button"><Clipboard className="h-4 w-4" />Sao chép mã xác thực</button>
                  <Link className="rounded-2xl border border-[#b9ddc6] bg-white px-4 py-3 text-sm font-extrabold text-emerald-800" to={`/courses/${certificate.courseId}`}>Quay lại khóa học</Link>
                </div>
              </div>
              {copyMessage ? <p className="mt-3 text-right text-xs font-semibold text-emerald-700">{copyMessage}</p> : null}
            </section>
            <CertificatePreview certificate={certificate} verificationUrl={`/certificates/${encodeURIComponent(certificate.verificationCode)}`} />
          </>
        ) : null}
      </main>
      <div className="khong-in"><CourseFooter /></div>
    </div>
  );
};

export default CertificateVerifyPage;
