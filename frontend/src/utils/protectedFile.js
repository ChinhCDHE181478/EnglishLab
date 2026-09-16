import axiosClient from '../api/axiosClient';

const PROTECTED_ATTACHMENT_PATH = '/api/classroom-homework/attachments/';
const LOCAL_FILES_PATH = '/local-files/';

export const isProtectedAttachmentUrl = (url = '') => String(url).includes(PROTECTED_ATTACHMENT_PATH);

export const isLocalFileUrl = (url = '') => String(url).includes(LOCAL_FILES_PATH);

export const fetchProtectedFileBlob = async (url) => {
  // Convert local file URL to authenticated API endpoint
  let apiUrl = url;
  if (url.includes(LOCAL_FILES_PATH)) {
    // /local-files/classroom-attachments/homework-uuid.pdf -> /api/classroom-homework/attachments/local-files/classroom-attachments/homework-uuid.pdf
    const objectKey = url.replace(LOCAL_FILES_PATH, '');
    apiUrl = PROTECTED_ATTACHMENT_PATH + 'local-files/' + objectKey;
  }
  const response = await axiosClient.get(apiUrl, { responseType: 'blob' });
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
