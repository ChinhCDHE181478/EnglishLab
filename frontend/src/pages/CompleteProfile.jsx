import React, { useMemo, useState } from 'react';
import { BookOpen, Mail, Phone, Save, Target, User } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { getCurrentUser, updateCurrentUser } from '../api/authApi';
import Footer from '../components/ai-learning/Footer';
import Header from '../components/ai-learning/Header';
import BrandedSelect from '../components/ui/BrandedSelect';
import { getStoredUser } from '../utils/auth';

const targetOptions = ['IELTS', 'TOEIC', 'Giao tiếp', 'Du học', 'Khác'];

const buildSummaryCards = (user) => [
  {
    label: 'Tài khoản',
    value: user?.email || 'Chưa có email',
    hint: 'Email đăng nhập',
  },
  {
    label: 'Mục tiêu hiện tại',
    value: user?.targetExam || 'Đang cập nhật',
    hint: user?.targetScore || 'Chưa đặt điểm mục tiêu',
  },
  {
    label: 'Liên hệ',
    value: user?.phoneNumber || 'Chưa cập nhật',
    hint: 'Số điện thoại học viên',
  },
];

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
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);
  const summaryCards = useMemo(() => buildSummaryCards(storedUser), [storedUser]);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setFormData((current) => ({ ...current, [name]: value }));
    setError('');
    setSuccess('');
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (!formData.fullName.trim() || !formData.phoneNumber.trim() || !formData.targetExam.trim()) {
      setError('Vui lòng nhập họ tên, số điện thoại và mục tiêu học.');
      return;
    }

    setLoading(true);
    setError('');
    setSuccess('');

    try {
      await updateCurrentUser(formData);
      const response = await getCurrentUser();
      localStorage.setItem('user', JSON.stringify(response.data));
      window.dispatchEvent(new Event('englishlab:user-updated'));
      setSuccess('Hồ sơ đã được cập nhật thành công.');
      navigate('/profile', { replace: true });
    } catch (err) {
      setError(err.response?.data?.message || 'Không thể lưu thông tin. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#f8f5f4] font-['Inter'] text-[#1A1C1C]">
      <Header />
      <main className="mx-auto w-full max-w-[1200px] px-4 py-8 md:px-6 md:py-10">
        <div className="overflow-hidden rounded-[32px] border border-[#e6d5d1] bg-white shadow-[0_24px_60px_rgba(92,24,33,0.08)]">
          <div className="border-b border-[#f0e4e1] px-6 py-6 md:px-10 md:py-8">
            <p className="text-[11px] font-bold uppercase tracking-[0.18em] text-[#9c7f7b]">Hồ sơ học viên</p>
            <h1 className="mt-2 font-['Manrope'] text-3xl font-extrabold tracking-tight text-[#2b2828] md:text-4xl">
              Cài đặt tài khoản
            </h1>
            <p className="mt-3 max-w-3xl text-sm leading-7 text-[#6a5553] md:text-base">
              Giao diện này chỉ giữ lại những thông tin mà hệ thống hiện đang có thật và đang sử dụng thật, để hồ sơ gọn hơn và đúng với EnglishLab.
            </p>

            <div className="mt-5 flex flex-wrap gap-3">
              {['Thông tin cá nhân', 'Mục tiêu học', 'Liên hệ'].map((item) => (
                <span
                  key={item}
                  className="rounded-full border border-[#ead8d4] bg-[#fff7f5] px-4 py-2 text-sm font-bold text-[#730014]"
                >
                  {item}
                </span>
              ))}
            </div>
          </div>

          <div className="grid gap-6 px-6 py-6 md:px-10 md:py-8 xl:grid-cols-[1.15fr_0.85fr]">
            <section className="space-y-6">
              {error ? (
                <div className="rounded-2xl border border-[#BA1A1A] bg-[#FFDAD6] px-4 py-3 text-sm text-[#93000A]">
                  {error}
                </div>
              ) : null}

              {success ? (
                <div className="rounded-2xl border border-[#c7e7d2] bg-[#edf8f1] px-4 py-3 text-sm text-[#185c37]">
                  {success}
                </div>
              ) : null}

              <form className="space-y-6" onSubmit={handleSubmit}>
                <div className="rounded-[28px] border border-[#ead8d4] bg-[#fffdfc] p-5 md:p-6">
                  <h2 className="font-['Manrope'] text-xl font-extrabold text-[#2b2828]">Thông tin cá nhân</h2>
                  <p className="mt-2 text-sm leading-6 text-[#7a6461]">
                    Cập nhật các trường cơ bản để hệ thống hiển thị đúng tài khoản của bạn.
                  </p>

                  <div className="mt-5 grid gap-4 md:grid-cols-2">
                    <div className="md:col-span-2">
                      <label className="mb-2 block text-xs font-bold uppercase tracking-[0.12em] text-[#7a6461]" htmlFor="fullName">
                        Họ và tên
                      </label>
                      <div className="relative">
                        <User className="absolute left-3 top-1/2 -translate-y-1/2 text-[#8a6f6b]" size={18} />
                        <input
                          className="w-full rounded-2xl border border-[#e7d7d3] bg-white py-3 pl-10 pr-4 text-base outline-none transition focus:border-[#730014] focus:ring-1 focus:ring-[#730014]"
                          id="fullName"
                          name="fullName"
                          onChange={handleChange}
                          placeholder="Nguyễn Văn A"
                          value={formData.fullName}
                        />
                      </div>
                    </div>

                    <div>
                      <label className="mb-2 block text-xs font-bold uppercase tracking-[0.12em] text-[#7a6461]" htmlFor="phoneNumber">
                        Số điện thoại
                      </label>
                      <div className="relative">
                        <Phone className="absolute left-3 top-1/2 -translate-y-1/2 text-[#8a6f6b]" size={18} />
                        <input
                          className="w-full rounded-2xl border border-[#e7d7d3] bg-white py-3 pl-10 pr-4 text-base outline-none transition focus:border-[#730014] focus:ring-1 focus:ring-[#730014]"
                          id="phoneNumber"
                          name="phoneNumber"
                          onChange={handleChange}
                          placeholder="09xx xxx xxx"
                          value={formData.phoneNumber}
                        />
                      </div>
                    </div>

                    <div>
                      <label className="mb-2 block text-xs font-bold uppercase tracking-[0.12em] text-[#7a6461]" htmlFor="email">
                        Email đăng nhập
                      </label>
                      <div className="relative">
                        <Mail className="absolute left-3 top-1/2 -translate-y-1/2 text-[#8a6f6b]" size={18} />
                        <input
                          className="w-full rounded-2xl border border-[#e7d7d3] bg-[#faf7f6] py-3 pl-10 pr-4 text-base text-[#6a5553] outline-none"
                          disabled
                          id="email"
                          value={storedUser?.email || ''}
                        />
                      </div>
                    </div>
                  </div>
                </div>

                <div className="rounded-[28px] border border-[#ead8d4] bg-[#fffdfc] p-5 md:p-6">
                  <h2 className="font-['Manrope'] text-xl font-extrabold text-[#2b2828]">Mục tiêu học</h2>
                  <p className="mt-2 text-sm leading-6 text-[#7a6461]">
                    Dữ liệu ở đây được dùng để gợi ý khóa học và cá nhân hóa trải nghiệm học tập.
                  </p>

                  <div className="mt-5 grid gap-4 md:grid-cols-2">
                    <div>
                      <label className="mb-2 block text-xs font-bold uppercase tracking-[0.12em] text-[#7a6461]" htmlFor="targetExam">
                        Mục tiêu học
                      </label>
                      <div className="relative">
                        <BookOpen className="pointer-events-none absolute left-3 top-1/2 z-10 -translate-y-1/2 text-[#8a6f6b]" size={18} />
                        <BrandedSelect
                          buttonClassName="rounded-2xl border-[#e7d7d3] bg-white pl-10 pr-4"
                          id="targetExam"
                          name="targetExam"
                          onChange={handleChange}
                          options={targetOptions}
                          value={formData.targetExam}
                        />
                      </div>
                    </div>

                    <div>
                      <label className="mb-2 block text-xs font-bold uppercase tracking-[0.12em] text-[#7a6461]" htmlFor="targetScore">
                        Điểm mục tiêu
                      </label>
                      <div className="relative">
                        <Target className="absolute left-3 top-1/2 -translate-y-1/2 text-[#8a6f6b]" size={18} />
                        <input
                          className="w-full rounded-2xl border border-[#e7d7d3] bg-white py-3 pl-10 pr-4 text-base outline-none transition focus:border-[#730014] focus:ring-1 focus:ring-[#730014]"
                          id="targetScore"
                          name="targetScore"
                          onChange={handleChange}
                          placeholder="Ví dụ: IELTS 7.0 hoặc TOEIC 850"
                          value={formData.targetScore}
                        />
                      </div>
                    </div>

                    <div className="md:col-span-2">
                      <label className="mb-2 block text-xs font-bold uppercase tracking-[0.12em] text-[#7a6461]" htmlFor="studyGoal">
                        Ghi chú mục tiêu
                      </label>
                      <textarea
                        className="min-h-32 w-full resize-none rounded-2xl border border-[#e7d7d3] bg-white px-4 py-3 text-base outline-none transition focus:border-[#730014] focus:ring-1 focus:ring-[#730014]"
                        id="studyGoal"
                        name="studyGoal"
                        onChange={handleChange}
                        placeholder="Bạn muốn đạt mục tiêu trong bao lâu, đang cần cải thiện kỹ năng nào, hoặc mong muốn lộ trình ra sao..."
                        value={formData.studyGoal}
                      />
                    </div>
                  </div>
                </div>

                <button
                  className="inline-flex w-full items-center justify-center gap-2 rounded-2xl bg-[#730014] px-5 py-3.5 text-sm font-extrabold text-white shadow-sm transition-all hover:-translate-y-0.5 hover:bg-[#9E001F] disabled:cursor-not-allowed disabled:opacity-70"
                  disabled={loading}
                  type="submit"
                >
                  <Save size={18} />
                  {loading ? 'Đang lưu hồ sơ...' : 'Lưu thay đổi'}
                </button>
              </form>
            </section>

            <aside className="space-y-5">
              <div className="rounded-[28px] border border-[#ead8d4] bg-[linear-gradient(135deg,_#fff8f7,_#fff1ef)] p-5 md:p-6">
                <p className="text-[11px] font-bold uppercase tracking-[0.18em] text-[#9c7f7b]">Tóm tắt tài khoản</p>
                <h2 className="mt-2 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">
                  Những gì EnglishLab đang dùng
                </h2>
                <p className="mt-3 text-sm leading-7 text-[#6a5553]">
                  Màn hình này được rút gọn theo dữ liệu thật trong hệ thống thay vì nhồi thêm các mục chưa có backend hoặc chưa dùng đến.
                </p>
              </div>

              <div className="grid gap-4">
                {summaryCards.map((item) => (
                  <div key={item.label} className="rounded-[24px] border border-[#ead8d4] bg-white p-5 shadow-[0_10px_24px_rgba(92,24,33,0.05)]">
                    <p className="text-[11px] font-bold uppercase tracking-[0.16em] text-[#9c7f7b]">{item.label}</p>
                    <p className="mt-3 font-['Manrope'] text-xl font-extrabold text-[#2b2828]">{item.value}</p>
                    <p className="mt-2 text-sm leading-6 text-[#7a6461]">{item.hint}</p>
                  </div>
                ))}
              </div>

              <div className="rounded-[24px] border border-[#f1ddd8] bg-[#fff8f6] p-5">
                <h3 className="font-['Manrope'] text-lg font-extrabold text-[#730014]">Đã bỏ các mục không tồn tại</h3>
                <p className="mt-2 text-sm leading-7 text-[#6a5553]">
                  Ảnh đại diện, tài khoản liên kết, xác thực hai lớp, thiết bị đăng nhập, đổi mật khẩu tại chỗ và xóa tài khoản chưa có flow hoàn chỉnh trong repo hiện tại nên đã được ẩn khỏi giao diện hồ sơ.
                </p>
              </div>
            </aside>
          </div>
        </div>
      </main>
      <Footer />
    </div>
  );
};

export default CompleteProfile;
