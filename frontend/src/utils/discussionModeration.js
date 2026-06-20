export const quyTacThaoLuan = [
  'Tôn trọng người học khác, không dùng lời lẽ khiếm nhã hoặc xúc phạm cá nhân.',
  'Không đăng nội dung gây chia rẽ, kích động thù ghét, công kích vùng miền, giới tính hoặc niềm tin.',
  'Không kêu gọi phá hoại, chống đối, quấy rối hoặc cổ vũ hành vi gây hại cho cộng đồng học tập.',
  'Tập trung vào câu hỏi học tập, chia sẻ kinh nghiệm và góp ý mang tính xây dựng.',
];

const cumTuCam = [
  'thằng ngu',
  'con ngu',
  'đồ ngu',
  'óc chó',
  'câm mồm',
  'biến đi',
  'đồ điên',
  'đồ khùng',
  'phá hoại',
  'lật đổ',
  'kích động thù ghét',
  'đánh nhau',
  'tẩy chay bọn',
  'ghét bọn',
  'giết',
  'đập phá',
];

const chuanHoa = (value) => String(value || '')
  .normalize('NFD')
  .replace(/[\u0300-\u036f]/g, '')
  .toLowerCase()
  .replace(/[^a-z0-9\s]/g, ' ')
  .replace(/\s+/g, ' ')
  .trim();

export const kiemTraNoiDungThaoLuan = (value) => {
  const text = chuanHoa(value);
  if (!text) {
    return { hopLe: false, thongDiep: 'Vui lòng nhập nội dung câu hỏi.' };
  }

  const viPham = cumTuCam.find((phrase) => text.includes(chuanHoa(phrase)));
  if (viPham) {
    return {
      hopLe: false,
      thongDiep: 'Nội dung chứa từ ngữ không phù hợp, gây chia rẽ, xúc phạm hoặc cổ vũ hành vi chống đối. Vui lòng chỉnh sửa trước khi đăng.',
    };
  }

  return { hopLe: true, thongDiep: '' };
};
