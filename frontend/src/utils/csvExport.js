const escapeCsvCell = (value) => {
  const text = value == null ? '' : String(value);
  return `"${text.replace(/"/g, '""')}"`;
};

export const buildCsv = (headers, rows) => {
  const lines = [headers, ...rows].map((row) => row.map(escapeCsvCell).join(','));
  return `\uFEFF${lines.join('\r\n')}`;
};

export const downloadCsv = (filename, headers, rows) => {
  const blob = new Blob([buildCsv(headers, rows)], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename.endsWith('.csv') ? filename : `${filename}.csv`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
};

export const sanitizeCsvFilename = (value, fallback = 'export') => String(value || fallback)
  .trim()
  .replace(/[<>:"/\\|?*]+/g, '-')
  .replace(/\s+/g, '-')
  .slice(0, 100) || fallback;
