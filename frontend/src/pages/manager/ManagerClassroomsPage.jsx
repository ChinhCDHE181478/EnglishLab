import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import classroomApi from '../../api/classroomApi';
import Header from '../../components/ai-learning/Header';
import CourseFooter from '../../components/course/CourseFooter';
import CourseGlobalStyles from '../../components/course/CourseGlobalStyles';
import { ClassroomEmptyState, ClassroomErrorState, ClassroomLoadingState } from '../../components/classroom/ClassroomUi';
import BrandedSelect from '../../components/ui/BrandedSelect';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';
import { formatClassroomDate, formatClassroomPrice, formatDeliveryMode, formatOfferingStatus, formatRegistrationStatus } from '../../utils/classroomHelpers';

export default function ManagerClassroomsPage() {
  const [classrooms, setClassrooms] = useState([]);
  const [selectedId, setSelectedId] = useState('');
  const [selectedClassroom, setSelectedClassroom] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionMessage, setActionMessage] = useState('');

  const loadClassrooms = async () => {
    setLoading(true);
    setError('');
    try {
      const data = await classroomApi.getManagerClassrooms();
      setClassrooms(data);
      if (!selectedId && data[0]?.id) {
        setSelectedId(String(data[0].id));
      }
    } catch (err) {
      setClassrooms([]);
      setError(getClassroomErrorMessage(err, 'Không thể tải danh sách lớp.'));
    } finally {
      setLoading(false);
    }
  };

  const loadSelectedClassroom = async (id) => {
    if (!id) {
      setSelectedClassroom(null);
      return;
    }
    try {
      const data = await classroomApi.getManagerClassroom(id);
      setSelectedClassroom(data);
    } catch (err) {
      setSelectedClassroom(null);
      setActionMessage(getClassroomErrorMessage(err, 'Không thể tải chi tiết lớp.'));
    }
  };

  useEffect(() => {
    loadClassrooms();
  }, []);

  useEffect(() => {
    if (selectedId) loadSelectedClassroom(selectedId);
  }, [selectedId]);

  const handlePublish = async () => {
    setActionMessage('');
    try {
      await classroomApi.publishManagerClassroom(selectedId);
      setActionMessage('Đã công bố lớp học.');
      await loadSelectedClassroom(selectedId);
      await loadClassrooms();
    } catch (err) {
      setActionMessage(getClassroomErrorMessage(err, 'Không thể công bố lớp.'));
    }
  };

  return (
    <div className="course-page flex min-h-[100dvh] flex-col bg-[#f9f9f9] text-[#1a1c1c]">
      <CourseGlobalStyles />
      <Header />
      <main className="mx-auto flex w-full max-w-[1320px] flex-1 flex-col px-4 pb-[80px] pt-8 md:px-10">
        <section className="rounded-[32px] border border-[#dfbfbd]/30 bg-white p-8 shadow-sm">
          <p className="text-[12px] font-extrabold uppercase tracking-[0.18em] text-[#730014]">Quản lý vận hành</p>
          <h1 className="mt-3 font-['Manrope'] text-4xl font-extrabold text-[#2b2828]">Tổng quan lớp học</h1>
          <p className="mt-4 max-w-3xl text-base leading-8 text-[#584140]">
            Xem trạng thái lớp, sĩ số và đăng ký. Các nghiệp vụ xác nhận đăng ký, học phí và xếp lớp do Training Manager xử lý tại{' '}
            <Link className="font-extrabold text-[#730014] underline" to="/training-manager/classroom-registrations">Quản lý đào tạo</Link>.
          </p>
        </section>

        {actionMessage ? <p className="mt-4 text-sm font-semibold text-[#730014]">{actionMessage}</p> : null}

        <section className="mt-8 flex flex-1 flex-col">
          {loading ? <ClassroomLoadingState message="Đang tải danh sách lớp..." /> : null}
          {!loading && error ? <ClassroomErrorState message={error} onRetry={loadClassrooms} /> : null}
          {!loading && !error && !classrooms.length ? (
            <ClassroomEmptyState description="Chưa có lớp học nào trong hệ thống." title="Chưa có lớp" />
          ) : null}
          {!loading && !error && classrooms.length ? (
            <div className="grid gap-6 lg:grid-cols-[320px_1fr]">
              <aside className="rounded-[28px] border border-[#dfbfbd]/25 bg-white p-5 shadow-sm">
                <label className="mb-2 block text-sm font-semibold text-[#584140]">Chọn lớp</label>
                <BrandedSelect
                  onChange={(event) => setSelectedId(event.target.value)}
                  options={classrooms.map((item) => ({ label: item.title, value: String(item.id) }))}
                  value={selectedId}
                />
              </aside>

              <div className="space-y-6">
                {selectedClassroom ? (
                  <section className="rounded-[28px] border border-[#dfbfbd]/25 bg-white p-6 shadow-sm">
                    <div className="flex flex-wrap gap-2">
                      <span className="rounded-full bg-[#fff1f3] px-3 py-1 text-xs font-extrabold text-[#730014]">
                        {formatDeliveryMode(selectedClassroom.deliveryMode, selectedClassroom.deliveryModeLabel)}
                      </span>
                      <span className="rounded-full bg-[#fcf8f8] px-3 py-1 text-xs font-extrabold text-[#584140]">
                        {formatOfferingStatus(selectedClassroom.classroomStatus)}
                      </span>
                    </div>
                    <h2 className="mt-4 font-['Manrope'] text-3xl font-extrabold text-[#2b2828]">{selectedClassroom.title}</h2>
                    <p className="mt-2 text-sm text-[#584140]">Khai giảng: {formatClassroomDate(selectedClassroom.startDate)}</p>
                    <p className="text-sm text-[#584140]">Sĩ số: {selectedClassroom.enrolledCount ?? 0}/{selectedClassroom.maxCapacity ?? '—'}</p>
                    <p className="text-sm text-[#584140]">Chờ xếp lớp: {selectedClassroom.waitlistCount ?? 0}</p>
                    <div className="mt-6">
                      <button className="rounded-2xl bg-[#4b0009] px-5 py-3 text-sm font-extrabold text-white transition hover:bg-[#730014]" onClick={handlePublish} type="button">
                        Công bố lớp (override)
                      </button>
                    </div>
                  </section>
                ) : null}

                {selectedClassroom?.enrollments?.length ? (
                  <section className="rounded-[28px] border border-[#dfbfbd]/25 bg-white p-6 shadow-sm">
                    <h3 className="font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">Đăng ký (chỉ xem)</h3>
                    <div className="mt-4 space-y-3">
                      {selectedClassroom.enrollments.map((enrollment) => (
                        <article key={enrollment.id || enrollment.studentId} className="rounded-2xl border border-[#f0e4e2] px-4 py-3 text-sm text-[#584140]">
                          <p className="font-extrabold text-[#2b2828]">
                            {enrollment.studentName || enrollment.studentEmail || `Học viên #${enrollment.studentId}`}
                          </p>
                          <p className="mt-1">
                            {formatRegistrationStatus(enrollment.registrationStatus, enrollment.registrationStatusLabel)}
                            {' · '}
                            {formatClassroomPrice(enrollment.tuitionAmountPaid ?? 0)} / {formatClassroomPrice(enrollment.tuitionAmountDue)}
                          </p>
                        </article>
                      ))}
                    </div>
                  </section>
                ) : null}
              </div>
            </div>
          ) : null}
        </section>
      </main>
      <CourseFooter />
    </div>
  );
}
