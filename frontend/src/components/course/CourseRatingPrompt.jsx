import { useEffect, useState } from 'react';
import { Star, X } from 'lucide-react';
import courseApi from '../../api/courseApi';

const EMPTY_RATING = {
  myRating: 0,
  myComment: '',
};

const CourseRatingPrompt = ({ courseId, onRatingSaved }) => {
  const [ratingInfo, setRatingInfo] = useState(EMPTY_RATING);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [selectedRating, setSelectedRating] = useState(0);
  const [comment, setComment] = useState('');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!courseId) return undefined;

    let active = true;
    const loadRating = async () => {
      try {
        const response = await courseApi.getMyCourseRating(courseId);
        if (!active) return;
        setRatingInfo(response || EMPTY_RATING);
      } catch {
        if (active) setRatingInfo(EMPTY_RATING);
      }
    };

    loadRating();
    return () => {
      active = false;
    };
  }, [courseId]);

  const openDialog = () => {
    setSelectedRating(Number(ratingInfo?.myRating || 0));
    setComment(ratingInfo?.myComment || '');
    setError('');
    setDialogOpen(true);
  };

  const closeDialog = () => {
    if (!saving) setDialogOpen(false);
  };

  const saveRating = async () => {
    if (!selectedRating) {
      setError('Vui lòng chọn số sao trước khi gửi.');
      return;
    }

    setSaving(true);
    setError('');
    try {
      const response = await courseApi.saveCourseRating(courseId, {
        rating: selectedRating,
        comment,
      });
      setRatingInfo(response || EMPTY_RATING);
      setDialogOpen(false);
      onRatingSaved?.(response);
    } catch (requestError) {
      setError(requestError?.response?.data?.message || 'Không thể lưu đánh giá. Vui lòng thử lại.');
    } finally {
      setSaving(false);
    }
  };

  const currentRating = Number(ratingInfo?.myRating || 0);

  return (
    <>
      <div className="flex flex-col gap-3 border-t border-[#eadedf] pt-4 sm:flex-row sm:items-center sm:justify-between">
        <span className="text-base font-extrabold text-[#1a1c1c]">Đánh giá khóa học này</span>
        <button
          className="inline-flex items-center gap-3 self-start rounded-lg px-2 py-1 text-sm font-extrabold text-[#730014] transition hover:bg-[#fff0f1] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#8a0018] focus-visible:ring-offset-2 sm:self-auto"
          onClick={openDialog}
          type="button"
        >
          {currentRating ? 'Sửa đánh giá' : 'Viết đánh giá'}
          <span className="flex gap-0.5 text-[#e11d48]" aria-label={currentRating ? `${currentRating} trên 5 sao` : 'Chưa đánh giá'}>
            {[1, 2, 3, 4, 5].map((star) => (
              <Star className="h-5 w-5" fill={star <= currentRating ? 'currentColor' : 'none'} key={star} />
            ))}
          </span>
        </button>
      </div>

      {dialogOpen ? (
        <div
          aria-labelledby="course-rating-title"
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) closeDialog();
          }}
          role="dialog"
        >
          <div className="w-full max-w-lg rounded-2xl border border-[#e5d7d9] bg-white p-6 shadow-xl">
            <div className="flex items-start justify-between gap-4">
              <div>
                <h3 className="font-['Manrope'] text-2xl font-extrabold text-[#2b2828]" id="course-rating-title">Đánh giá khóa học</h3>
                <p className="mt-1 text-xs text-[#8b706e]">Bạn có thể cập nhật đánh giá sau khi gửi.</p>
              </div>
              <button aria-label="Đóng" className="rounded-lg p-1 text-[#4b5563] hover:bg-slate-100" disabled={saving} onClick={closeDialog} type="button">
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="mt-6 flex gap-2" role="radiogroup" aria-label="Số sao đánh giá">
              {[1, 2, 3, 4, 5].map((star) => (
                <button
                  aria-checked={selectedRating === star}
                  aria-label={`${star} sao`}
                  className={`rounded-md p-1 transition hover:scale-110 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#8a0018] ${star <= selectedRating ? 'text-[#e11d48]' : 'text-[#d1d5db]'}`}
                  key={star}
                  onClick={() => setSelectedRating(star)}
                  role="radio"
                  type="button"
                >
                  <Star className="h-8 w-8" fill={star <= selectedRating ? 'currentColor' : 'none'} />
                </button>
              ))}
            </div>

            <label className="mt-5 block text-sm font-bold text-[#1a1c1c]" htmlFor="course-rating-comment">Nhận xét (không bắt buộc)</label>
            <textarea
              className="mt-2 min-h-28 w-full rounded-xl border border-[#d1d5db] p-3 text-sm outline-none focus:border-[#730014]"
              id="course-rating-comment"
              maxLength={2000}
              onChange={(event) => setComment(event.target.value)}
              placeholder="Chia sẻ trải nghiệm học của bạn"
              value={comment}
            />
            {error ? <p className="mt-3 text-sm font-semibold text-[#93000a]" role="alert">{error}</p> : null}
            <div className="mt-6 flex justify-end gap-3">
              <button className="rounded-xl border border-[#d1d5db] px-4 py-2 text-sm font-bold" disabled={saving} onClick={closeDialog} type="button">Hủy</button>
              <button className="rounded-xl bg-[#730014] px-4 py-2 text-sm font-bold text-white disabled:opacity-50" disabled={saving} onClick={saveRating} type="button">
                {saving ? 'Đang lưu...' : 'Gửi đánh giá'}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </>
  );
};

export default CourseRatingPrompt;
