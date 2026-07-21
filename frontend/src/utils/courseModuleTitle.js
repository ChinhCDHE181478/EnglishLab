const MODULE_ORDINAL_PREFIX = /^\s*(?:module|m[oô]\s*[-–—]?\s*đun)\s*\d+\s*(?::|[-–—])?\s*/iu;

export function stripModuleOrdinal(title) {
  return String(title || '').replace(MODULE_ORDINAL_PREFIX, '').trim();
}

export function formatModuleTitle(title, index) {
  const ordinal = Number(index) + 1;
  const cleanTitle = stripModuleOrdinal(title);
  return cleanTitle ? `Module ${ordinal}: ${cleanTitle}` : `Module ${ordinal}`;
}
