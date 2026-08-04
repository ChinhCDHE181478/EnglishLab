import { useState } from 'react';
import { Video } from 'lucide-react';
import classroomApi from '../../api/classroomApi';
import { getClassroomErrorMessage } from '../../utils/classroomErrorMessages';

/**
 * Gọi API join trước khi mở Google Meet để ghi nhận thời gian tham gia.
 */
export default function VirtualJoinButton({
  classroomId,
  sessionId,
  url,
  label = 'Vào lớp trực tuyến',
  className = '',
  disabled = false,
  onBlocked,
  onJoined,
}) {
  const [joining, setJoining] = useState(false);

  const handleJoin = async () => {
    if (joining || disabled) return;
    if (!classroomId || !sessionId) {
      onBlocked?.('Thiếu thông tin buổi học để tham gia.');
      return;
    }

    const popup = window.open('about:blank', '_blank');
    setJoining(true);
    onBlocked?.('');
    try {
      const updatedSession = await classroomApi.joinVirtualSession(classroomId, sessionId);
      onJoined?.(updatedSession);
      const meetingUrl = updatedSession?.larkMeetingUrl || url;
      if (!meetingUrl) {
        popup?.close();
        onBlocked?.('Chưa thể lấy liên kết phòng học.');
        return;
      }
      if (popup) {
        popup.opener = null;
        popup.location.href = meetingUrl;
      } else {
        const opened = window.open(meetingUrl, '_blank', 'noopener,noreferrer');
        if (!opened) {
          onBlocked?.('Trình duyệt đã chặn cửa sổ mới. Hãy cho phép popup hoặc mở liên kết thủ công.');
        }
      }
    } catch (error) {
      popup?.close();
      onBlocked?.(getClassroomErrorMessage(error, 'Không thể tham gia phòng học.'));
    } finally {
      setJoining(false);
    }
  };

  if (!url && !sessionId) return null;

  return (
    <div className="flex flex-wrap items-center gap-3">
      <button
        className={`inline-flex items-center gap-2 rounded-2xl bg-[#4b0009] px-5 py-3 text-sm font-extrabold text-white transition hover:bg-[#730014] disabled:opacity-60 ${className}`}
        disabled={joining || disabled}
        onClick={handleJoin}
        type="button"
      >
        <Video className="h-4 w-4" />
        {joining ? 'Đang vào lớp...' : label}
      </button>
      {url ? (
        <a className="text-sm font-semibold text-[#730014] underline" href={url} rel="noreferrer" target="_blank">
          Mở liên kết thủ công
        </a>
      ) : null}
    </div>
  );
}
