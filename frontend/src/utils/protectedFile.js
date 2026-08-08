import axiosClient from '../api/axiosClient';

const PROTECTED_ATTACHMENT_PATH = '/api/classroom-homework/attachments/';

export const isProtectedAttachmentUrl = (url = '') => String(url).includes(PROTECTED_ATTACHMENT_PATH);

export const fetchProtectedFileBlob = async (url) => {
  const response = await axiosClient.get(url, { responseType: 'blob' });
  return response.data;
};

export const downloadProtectedFile = async (url, fileName = '') => {
  const blob = await fetchProtectedFileBlob(url);
  const objectUrl = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = objectUrl;
  anchor.download = fileName || String(url).split('/').pop()?.split('?')[0] || 'tep-dinh-kem';
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  window.setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000);
};
