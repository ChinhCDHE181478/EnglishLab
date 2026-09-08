export const formatCriteriaSetName = (value) => String(value || '')
  .replace(/\s+AI rubrics?\b/gi, ' - Bộ tiêu chí AI')
  .replace(/\brubrics?\b/gi, 'Bộ tiêu chí');
