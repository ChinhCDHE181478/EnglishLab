import axiosClient from '../api/axiosClient';

const PROTECTED_ATTACHMENT_PATH = '/api/classroom-homework/attachments/';
const LOCAL_FILES_PATH = '/local-files/';

export const isProtectedAttachmentUrl = (url = '') => {
  const str = String(url);
  return str.includes(PROTECTED_ATTACHMENT_PATH) || 
         str.includes(LOCAL_FILES_PATH) || 
         str.includes('/classroom-attachments/');
};

export const isLocalFileUrl = (url = '') => String(url).includes(LOCAL_FILES_PATH);

export const fetchProtectedFileBlob = async (url) => {
  let apiUrl = url;
  if (url.includes(LOCAL_FILES_PATH)) {
    const objectKey = url.replace(LOCAL_FILES_PATH, '');
    apiUrl = PROTECTED_ATTACHMENT_PATH + 'local-files/' + objectKey;
  } else if (!url.includes(PROTECTED_ATTACHMENT_PATH) && url.includes('/classroom-attachments/')) {
    const parts = url.split('/classroom-attachments/');
    const fileName = parts[1].split('?')[0];
    apiUrl = PROTECTED_ATTACHMENT_PATH + fileName;
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
