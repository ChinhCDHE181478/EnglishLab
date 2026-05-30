import React, { useState } from 'react';
import { BookOpen, Phone, Target, User } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { getCurrentUser, updateCurrentUser } from '../api/authApi';
import { getStoredUser } from '../utils/auth';

const targetOptions = ['IELTS', 'TOEIC', 'Giao tiếp', 'Du học', 'Khác'];

const CompleteProfile = () => {
  const navigate = useNavigate();
  const storedUser = getStoredUser();
  const [formData, setFormData] = useState({
    fullName: storedUser?.fullName || '',
    phoneNumber: storedUser?.phoneNumber || '',
    targetExam: storedUser?.targetExam || 'IELTS',
    targetScore: storedUser?.targetScore || '',
    studyGoal: storedUser?.studyGoal || '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setFormData((current) => ({ ...current, [name]: value }));
    setError('');
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (!formData.fullName.trim() || !formData.phoneNumber.trim() || !formData.targetExam.trim()) {
      setError('Vui lòng nhập họ tên, số điện thoại và mục tiêu học.');
      return;
    }

    setLoading(true);
    setError('');

    try {
      await updateCurrentUser(formData);
      const response = await getCurrentUser();
      localStorage.setItem('user', JSON.stringify(response.data));
      window.dispatchEvent(new Event('englishlab:user-updated'));
      navigate('/home', { replace: true });
    } catch (err) {
      setError(err.response?.data?.message || 'Không thể lưu thông tin. Vui lòng kiểm tra backend rồi thử lại.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#f4f3f3] px-4 py-10 font-['Inter'] text-[#1A1C1C]">
      <div className="mx-auto flex min-h-[calc(100vh-80px)] max-w-[760px] items-center">
        <div className="w-full rounded-3xl bg-white p-8 shadow-[0_20px_45px_rgba(0,0,0,0.12)] md:p-10">
          <div className="mb-8">
            <div className="mb-4 inline-flex h-12 w-12 items-center justify-center rounded-full bg-[#730014]/10 text-[#730014]">
              <User size={24} />
            </div>
            <h1 className="font-['Manrope'] text-3xl font-extrabold tracking-tight">
              Hoàn thiện hồ sơ học tập
            </h1>
            <p className="mt-2 text-[#584140]">
              EnglishLab cần vài thông tin để cá nhân hóa lộ trình và hiển thị đúng tài khoản của bạn.
            </p>
          </div>

          {error && (
            <div className="mb-6 rounded border border-[#BA1A1A] bg-[#FFDAD6] px-4 py-3 text-sm text-[#93000A]">
              {error}
            </div>
          )}

          <form className="grid gap-5 md:grid-cols-2" onSubmit={handleSubmit}>
            <div className="md:col-span-2">
              <label className="mb-2 block text-xs font-bold uppercase tracking-[0.1em]" htmlFor="fullName">
                Họ và tên
              </label>
              <div className="relative">
                <User className="absolute left-3 top-1/2 -translate-y-1/2 text-[#584140]/50" size={20} />
                <input
                  className="w-full rounded border border-[#E5E2E0] py-3 pl-10 pr-4 outline-none transition focus:border-[#730014] focus:ring-1 focus:ring-[#730014]"
                  id="fullName"
                  name="fullName"
                  onChange={handleChange}
                  placeholder="Nguyễn Văn A"
                  value={formData.fullName}
                />
              </div>
            </div>

            <div>
              <label className="mb-2 block text-xs font-bold uppercase tracking-[0.1em]" htmlFor="phoneNumber">
                Số điện thoại
              </label>
              <div className="relative">
                <Phone className="absolute left-3 top-1/2 -translate-y-1/2 text-[#584140]/50" size={20} />
                <input
                  className="w-full rounded border border-[#E5E2E0] py-3 pl-10 pr-4 outline-none transition focus:border-[#730014] focus:ring-1 focus:ring-[#730014]"
                  id="phoneNumber"
                  name="phoneNumber"
                  onChange={handleChange}
                  placeholder="09xx xxx xxx"
                  value={formData.phoneNumber}
                />
              </div>
            </div>

            <div>
              <label className="mb-2 block text-xs font-bold uppercase tracking-[0.1em]" htmlFor="targetExam">
                Mục tiêu học
              </label>
              <div className="relative">
                <BookOpen className="absolute left-3 top-1/2 -translate-y-1/2 text-[#584140]/50" size={20} />
                <select
                  className="w-full cursor-pointer appearance-none rounded border border-[#E5E2E0] bg-white py-3 pl-10 pr-4 outline-none transition focus:border-[#730014] focus:ring-1 focus:ring-[#730014]"
                  id="targetExam"
                  name="targetExam"
                  onChange={handleChange}
                  value={formData.targetExam}
                >
                  {targetOptions.map((option) => (
                    <option key={option} value={option}>
                      {option}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="md:col-span-2">
              <label className="mb-2 block text-xs font-bold uppercase tracking-[0.1em]" htmlFor="targetScore">
                Điểm mục tiêu
              </label>
              <div className="relative">
                <Target className="absolute left-3 top-1/2 -translate-y-1/2 text-[#584140]/50" size={20} />
                <input
                  className="w-full rounded border border-[#E5E2E0] py-3 pl-10 pr-4 outline-none transition focus:border-[#730014] focus:ring-1 focus:ring-[#730014]"
                  id="targetScore"
                  name="targetScore"
                  onChange={handleChange}
                  placeholder="Ví dụ: IELTS 7.0 hoặc TOEIC 850"
                  value={formData.targetScore}
                />
              </div>
            </div>

            <div className="md:col-span-2">
              <label className="mb-2 block text-xs font-bold uppercase tracking-[0.1em]" htmlFor="studyGoal">
                Ghi chú mục tiêu
              </label>
              <textarea
                className="min-h-28 w-full resize-none rounded border border-[#E5E2E0] px-4 py-3 outline-none transition focus:border-[#730014] focus:ring-1 focus:ring-[#730014]"
                id="studyGoal"
                name="studyGoal"
                onChange={handleChange}
                placeholder="Bạn muốn đạt mục tiêu trong bao lâu, đang yếu kỹ năng nào..."
                value={formData.studyGoal}
              />
            </div>

            <button
              className="md:col-span-2 flex w-full cursor-pointer justify-center rounded bg-[#730014] px-5 py-3 font-bold text-white shadow-sm transition-all hover:-translate-y-0.5 hover:bg-[#9E001F] disabled:cursor-not-allowed disabled:opacity-70"
              disabled={loading}
              type="submit"
            >
              {loading ? 'Đang lưu...' : 'Lưu và vào trang chủ'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};

export default CompleteProfile;
