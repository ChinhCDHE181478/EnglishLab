import { useEffect, useMemo, useState } from 'react';
import { ArrowLeft } from 'lucide-react';
import { Link } from 'react-router-dom';
import mockTestApi from '../api/mockTestApi';
import placementTestApi from '../api/placementTestApi';
import LearnerPageShell from '../components/learner/LearnerPageShell';
import BrandLoadingState from '../components/ui/BrandLoadingState';
import useMockTestSession from '../hooks/useMockTestSession';
import { splitMockTests } from '../utils/mockTestLibrary';

const SKILL_LABEL = {
  LISTENING: 'Listening',
  READING: 'Reading',
  WRITING: 'Writing',
  SPEAKING: 'Speaking',
};

export default function MockPracticeTestsPage() {
  const [tests, setTests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [hasPlacement, setHasPlacement] = useState(false);
  const [error, setError] = useState('');
  const session = useMockTestSession();

  useEffect(() => {
    let active = true;
    const load = async () => {
      setLoading(true);
      setError('');
      try {
        const [data, placement] = await Promise.all([
          mockTestApi.listMockTests(),
          placementTestApi.getCurrent().catch(() => null),
        ]);
        if (!active) return;
        setTests(Array.isArray(data) ? data : []);
        setHasPlacement(Boolean(placement?.latestAttempt));
      } catch (requestError) {
        if (active) setError(requestError?.response?.data?.message || 'Không tải được đề luyện.');
      } finally {
        if (active) setLoading(false);
      }
    };
    load();
    return () => { active = false; };
  }, []);

  const practiceTests = useMemo(() => splitMockTests(tests).practiceTests, [tests]);

  if (session.examView) return session.examView;

  return (
    <LearnerPageShell
      title="Đề luyện sau đánh giá đầu vào"
      description="Các đề không gắn tháng năm."
      actions={(
        <Link className="inline-flex items-center gap-2 text-sm font-extrabold text-[#730014]" to="/mock-tests">
          <ArrowLeft className="h-4 w-4" />
          Thư viện đề thi
        </Link>
      )}
    >
      <div className="flex flex-1 flex-col space-y-4">
        {error || session.error ? (
          <div className="rounded-2xl border border-rose-200 bg-rose-50 p-4 text-xs font-bold text-rose-700">
            {error || session.error}
          </div>
        ) : null}

        {loading ? (
          <BrandLoadingState className="flex-1 py-16" message="Đang tải đề luyện..." />
        ) : !hasPlacement ? (
          <div className="flex flex-1 flex-col items-center justify-center rounded-2xl border border-dashed border-[#ead9db] bg-[#fffaf9] px-6 py-16 text-center">
            <p className="text-sm font-bold text-[#564241]">Hoàn thành bài đánh giá đầu vào để mở các đề này.</p>
            <Link className="mt-4 rounded-full bg-[#730014] px-5 py-2 text-sm font-extrabold text-white" to="/placement-test">
              Làm bài đánh giá
            </Link>
          </div>
        ) : practiceTests.length ? (
          <div className="space-y-3">
            {practiceTests.map((item) => {
              const saved = session.completedScoresMap[item.id];
              return (
                <div className="flex items-center justify-between gap-4 rounded-2xl border border-[#eadcdc] bg-white px-4 py-4" key={item.id}>
                  <div className="min-w-0">
                    <p className="text-[11px] font-extrabold uppercase tracking-[0.14em] text-[#8b706e]">
                      {SKILL_LABEL[item.skill] || item.skill}
                    </p>
                    <p className="mt-1 truncate text-sm font-bold text-[#0b1c30]">{item.title}</p>
                  </div>
                  <button
                    className="shrink-0 rounded-full bg-[#0f172a] px-5 py-2 text-xs font-extrabold text-white"
                    onClick={() => session.startTest(item)}
                    type="button"
                  >
                    {saved ? 'Làm lại' : 'Start'}
                  </button>
                </div>
              );
            })}
          </div>
        ) : (
          <div className="flex flex-1 items-center justify-center rounded-2xl border border-dashed border-[#ead9db] bg-[#fffaf9] p-12 text-sm font-bold text-[#564241]">
            Chưa có đề luyện.
          </div>
        )}
      </div>
    </LearnerPageShell>
  );
}
