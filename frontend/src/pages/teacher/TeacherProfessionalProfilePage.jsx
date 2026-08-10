import { useEffect, useState } from 'react';
import { BarChart3, BookOpenCheck, BriefcaseBusiness, CheckCircle2, FileBadge, Link2, LoaderCircle, ShieldCheck, Star, Unplug, Users, Video } from 'lucide-react';
import { useSearchParams } from 'react-router-dom';
import teacherProfessionalApi from '../../api/teacherProfessionalApi';
import LearnerPageShell from '../../components/learner/LearnerPageShell';

const formatDate = (value) => value ? new Intl.DateTimeFormat('vi-VN').format(new Date(`${value}T00:00:00`)) : 'Không thời hạn';

export default function TeacherProfessionalProfilePage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [profile, setProfile] = useState(null);
  const [feedback, setFeedback] = useState(null);
  const [meetConnection, setMeetConnection] = useState(null);
  const [meetBusy, setMeetBusy] = useState(false);
  const [meetMessage, setMeetMessage] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    const load = async () => {
      try {
        const [profileData, feedbackData, connectionData] = await Promise.all([
          teacherProfessionalApi.getMine(),
          teacherProfessionalApi.getMyFeedbackSummary(),
          teacherProfessionalApi.getGoogleMeetConnection(),
        ]);
        if (active) {
          setProfile(profileData);
          setFeedback(feedbackData);
          setMeetConnection(connectionData);
        }
      } catch (requestError) {
        if (active) setError(requestError?.response?.data?.message || 'Không thể tải hồ sơ chuyên môn.');
      } finally {
        if (active) setLoading(false);
      }
    };
    load();
    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    const result = searchParams.get('googleMeet');
    if (result === 'connected') {
      setMeetMessage('Đã kết nối Google Meet thành công.');
    } else if (result === 'error') {
      setMeetMessage(searchParams.get('message') || 'Không thể kết nối Google Meet.');
    }
    if (result) {
      const next = new URLSearchParams(searchParams);
      next.delete('googleMeet');
      next.delete('message');
      setSearchParams(next, { replace: true });
    }
  }, [searchParams, setSearchParams]);

  const handleConnectGoogleMeet = async () => {
    if (meetConnection?.connected) return;
    setMeetBusy(true);
    setMeetMessage('');
    try {
      const response = await teacherProfessionalApi.connectGoogleMeet();
      window.location.assign(response.authorizationUrl);
    } catch (requestError) {
      setMeetMessage(requestError?.response?.data?.message || 'Không thể bắt đầu kết nối Google Meet.');
      setMeetBusy(false);
    }
  };

  const handleDisconnectGoogleMeet = async () => {
    setMeetBusy(true);
    setMeetMessage('');
    try {
      await teacherProfessionalApi.disconnectGoogleMeet();
      setMeetConnection((current) => ({ ...current, connected: false, status: 'DISCONNECTED' }));
      setMeetMessage('Đã ngắt kết nối Google Meet.');
    } catch (requestError) {
      setMeetMessage(requestError?.response?.data?.message || 'Không thể ngắt kết nối Google Meet.');
    } finally {
      setMeetBusy(false);
    }
  };

  return (
    <LearnerPageShell
      eyebrow="Hồ sơ giảng dạy"
      title="Hồ sơ chuyên môn của tôi"
      description="Thông tin chuyên môn, minh chứng đã xác minh và đánh giá tổng hợp ẩn danh từ học viên."
    >
      {loading ? (
        <div className="flex min-h-[520px] flex-1 items-center justify-center rounded-[28px] border border-[#ead9db] bg-white text-sm font-semibold text-[#756361]">
          <LoaderCircle className="mr-2 h-5 w-5 animate-spin text-[#8a0018]" /> Đang tải hồ sơ...
        </div>
      ) : null}
      {!loading && error ? <div className="flex min-h-[520px] flex-1 items-center justify-center rounded-[28px] border border-rose-200 bg-rose-50 p-8 text-center font-semibold text-rose-700">{error}</div> : null}
      {!loading && profile ? (
        <div className="space-y-6">
          <section className="rounded-[28px] border border-sky-200 bg-gradient-to-br from-sky-50 via-white to-white p-6 shadow-sm md:p-8">
            <div className="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
              <div className="flex items-start gap-4">
                <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-sky-100 text-sky-700">
                  <Video className="h-6 w-6" />
                </span>
                <div>
                  <h2 className="font-['Manrope'] text-2xl font-black text-[#2b2828]">Google Meet giảng dạy</h2>
                  <p className="mt-1 max-w-2xl text-sm leading-6 text-[#756361]">
                    Kết nối tài khoản Google dùng để giảng dạy để bạn là người tổ chức các phòng học được phân công.
                  </p>
                  <p className="mt-3 text-sm font-bold text-sky-800">
                    {meetConnection?.integrationEnabled === false
                      ? 'Tích hợp Google Meet chưa được bật trên hệ thống.'
                      : meetConnection?.connected
                      ? `Đã kết nối${meetConnection.googleEmail ? `: ${meetConnection.googleEmail}` : ''}`
                      : meetConnection?.status === 'REAUTH_REQUIRED'
                        ? 'Quyền truy cập đã hết hạn, cần kết nối lại.'
                        : 'Chưa kết nối Google Meet.'}
                  </p>
                  {meetMessage ? <p className="mt-2 text-sm font-semibold text-[#730014]">{meetMessage}</p> : null}
                </div>
              </div>
              {meetConnection?.connected ? (
                <button className="inline-flex items-center justify-center gap-2 rounded-xl border border-sky-300 bg-white px-5 py-3 text-sm font-extrabold text-sky-800 hover:bg-sky-50 disabled:opacity-60" disabled={meetBusy} onClick={handleDisconnectGoogleMeet} type="button">
                  <Unplug className="h-4 w-4" /> Ngắt kết nối
                </button>
              ) : (
                <button className="inline-flex items-center justify-center gap-2 rounded-xl bg-sky-700 px-5 py-3 text-sm font-extrabold text-white hover:bg-sky-800 disabled:opacity-60" disabled={meetBusy || meetConnection?.integrationEnabled === false} onClick={handleConnectGoogleMeet} type="button">
                  {meetBusy ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <Link2 className="h-4 w-4" />}
                  {meetConnection?.status === 'REAUTH_REQUIRED' ? 'Kết nối lại Google' : 'Kết nối Google'}
                </button>
              )}
            </div>
          </section>

          <section className="rounded-[28px] border border-[#ead9db] bg-white p-6 shadow-sm md:p-8">
            <p className="text-xs font-extrabold uppercase tracking-[0.16em] text-[#8a0018]">Giáo viên EnglishLab</p>
            <h1 className="mt-2 font-['Manrope'] text-3xl font-black text-[#2b2828]">{profile.fullName}</h1>
            <p className="mt-1 text-sm text-[#756361]">{profile.email}{profile.phoneNumber ? ` · ${profile.phoneNumber}` : ''}</p>
            <p className="mt-4 text-lg font-extrabold text-[#4b0009]">{profile.headline || 'Chưa cập nhật tiêu đề chuyên môn'}</p>
            {profile.biography ? <p className="mt-3 max-w-4xl whitespace-pre-wrap text-sm leading-7 text-[#584140]">{profile.biography}</p> : null}
            <div className="mt-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
              <Metric icon={BookOpenCheck} label="Lớp phụ trách" value={profile.assignedClassrooms} />
              <Metric icon={BriefcaseBusiness} label="Buổi đã lên lịch" value={profile.totalSessions} />
              <Metric icon={CheckCircle2} label="Buổi hoàn thành" value={profile.completedSessions} />
              <Metric icon={Star} label="Điểm từ học viên" value={feedback?.overallScore == null ? '—' : Number(feedback.overallScore).toFixed(2)} />
            </div>
            <div className="mt-5 grid gap-4 md:grid-cols-2">
              <Info label="Chuyên môn" value={profile.specializations || 'Chưa cập nhật'} />
              <Info label="Học vị cao nhất" value={profile.highestQualification || 'Chưa cập nhật'} />
              <Info label="Ngôn ngữ giảng dạy" value={profile.teachingLanguages || 'Chưa cập nhật'} />
              <Info label="Kinh nghiệm" value={profile.yearsOfExperience == null ? 'Chưa cập nhật' : `${profile.yearsOfExperience} năm`} />
            </div>
          </section>

          <section className="rounded-[28px] border border-[#ead9db] bg-white p-6 shadow-sm md:p-8">
            <h2 className="font-['Manrope'] text-2xl font-black text-[#2b2828]">Minh chứng chuyên môn</h2>
            <p className="mt-1 text-sm text-[#756361]">Liên hệ bộ phận đào tạo nếu thông tin bằng cấp hoặc chứng chỉ cần cập nhật.</p>
            {!profile.credentials?.length ? <Empty text="Chưa có minh chứng chuyên môn." /> : (
              <div className="mt-5 grid gap-4 lg:grid-cols-2">
                {profile.credentials.map((item) => (
                  <article className="rounded-2xl border border-[#ead9db] p-5" key={item.id}>
                    <div className="flex items-start gap-3">
                      <FileBadge className="mt-0.5 h-5 w-5 shrink-0 text-[#8a0018]" />
                      <div>
                        <div className="flex flex-wrap items-center gap-2">
                          <h3 className="font-extrabold text-[#2b2828]">{item.title}</h3>
                          <span className={`rounded-full px-2.5 py-1 text-[10px] font-extrabold ${
                            item.verificationStatus === 'VERIFIED' ? 'bg-emerald-100 text-emerald-700'
                              : item.verificationStatus === 'REJECTED' ? 'bg-rose-100 text-rose-700'
                                : 'bg-amber-100 text-amber-700'
                          }`}>{item.verificationStatus === 'VERIFIED' ? 'Đã xác minh' : item.verificationStatus === 'REJECTED' ? 'Bị từ chối' : item.verificationStatus === 'EXPIRED' ? 'Đã hết hạn' : 'Chờ xác minh'}</span>
                        </div>
                        <p className="mt-2 text-sm text-[#584140]">{item.issuer}</p>
                        <p className="mt-1 text-xs text-[#8c716f]">Cấp {formatDate(item.issuedDate)} · Hết hạn {formatDate(item.expiryDate)}</p>
                        {item.verificationNote ? <p className="mt-3 rounded-xl bg-[#fff7f7] p-3 text-xs leading-5 text-[#756361]">{item.verificationNote}</p> : null}
                      </div>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </section>

          <section className="rounded-[28px] border border-[#ead9db] bg-white p-6 shadow-sm md:p-8">
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div>
                <h2 className="font-['Manrope'] text-2xl font-black text-[#2b2828]">Đánh giá tổng hợp từ học viên</h2>
                <p className="mt-1 text-sm text-[#756361]">Bạn không thể xem từng phiếu, bình luận cá nhân hoặc danh tính người đánh giá.</p>
              </div>
              <span className="inline-flex items-center gap-2 rounded-full bg-[#fff0f1] px-4 py-2 text-xs font-extrabold text-[#730014]"><ShieldCheck className="h-4 w-4" /> Bảo vệ danh tính học viên</span>
            </div>
            {feedback?.protectedByAnonymity ? (
              <div className="mt-5 rounded-2xl border border-amber-200 bg-amber-50 p-5">
                <p className="font-extrabold text-amber-900">Chưa đủ dữ liệu để hiển thị kết quả</p>
                <p className="mt-2 text-sm leading-6 text-amber-800">Hiện có {feedback.responseCount || 0}/{feedback.anonymityThreshold || 3} phản hồi. Kết quả chỉ mở khi đạt ngưỡng ẩn danh tối thiểu.</p>
              </div>
            ) : (
              <>
                <div className="mt-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                  <Metric icon={Star} label="Điểm tổng hợp" value={`${Number(feedback?.overallScore || 0).toFixed(2)}/5`} />
                  <Metric icon={Users} label="Số phản hồi" value={feedback?.responseCount || 0} />
                  <Metric icon={CheckCircle2} label="Sẵn sàng giới thiệu" value={`${Number(feedback?.recommendationPercent || 0).toFixed(2)}%`} />
                  <Metric icon={BarChart3} label="Tác phong" value={Number(feedback?.professionalismScore || 0).toFixed(2)} />
                </div>
                <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                  <Info label="Trình bày dễ hiểu" value={`${Number(feedback?.clarityScore || 0).toFixed(2)}/5`} />
                  <Info label="Tạo hứng thú" value={`${Number(feedback?.engagementScore || 0).toFixed(2)}/5`} />
                  <Info label="Hỗ trợ học viên" value={`${Number(feedback?.learnerSupportScore || 0).toFixed(2)}/5`} />
                  <Info label="Phản hồi bài học" value={`${Number(feedback?.feedbackTimelinessScore || 0).toFixed(2)}/5`} />
                </div>
              </>
            )}
          </section>
        </div>
      ) : null}
    </LearnerPageShell>
  );
}

function Metric({ icon: Icon, label, value }) {
  return <div className="rounded-2xl border border-[#ead9db] bg-[#fffafa] p-4"><Icon className="h-5 w-5 text-[#8a0018]" /><p className="mt-3 text-2xl font-black text-[#341c1d]">{value ?? 0}</p><p className="mt-1 text-xs font-bold text-[#756361]">{label}</p></div>;
}

function Info({ label, value }) {
  return <div className="rounded-2xl bg-[#f8f5f4] p-4"><p className="text-[10px] font-extrabold uppercase tracking-[0.12em] text-[#9b8582]">{label}</p><p className="mt-2 whitespace-pre-wrap text-sm font-semibold leading-6 text-[#584140]">{value}</p></div>;
}

function Empty({ text }) {
  return <div className="mt-5 rounded-2xl border border-dashed border-[#dfbfbd] bg-[#fffafa] p-10 text-center text-sm text-[#756361]">{text}</div>;
}
