export const EMPTY_PAGE = Object.freeze({
  content: [],
  totalElements: 0,
  totalPages: 0,
  number: 0,
  size: 0,
  first: true,
  last: true,
});

export function normalizePage(payload) {
  const data = payload?.data ?? payload;

  if (Array.isArray(data?.content)) {
    return {
      ...EMPTY_PAGE,
      ...data,
      content: data.content,
      totalElements: Number(data.totalElements ?? data.content.length),
      totalPages: Number(data.totalPages ?? 0),
      number: Number(data.number ?? 0),
      size: Number(data.size ?? data.content.length),
    };
  }

  if (Array.isArray(data?.items)) {
    return {
      ...EMPTY_PAGE,
      content: data.items,
      totalElements: Number(data.totalElements ?? data.total ?? data.items.length),
      totalPages: Number(data.totalPages ?? 0),
      number: Number(data.number ?? data.page ?? 0),
      size: Number(data.size ?? data.items.length),
    };
  }

  if (Array.isArray(data?.courses)) {
    return {
      ...EMPTY_PAGE,
      content: data.courses,
      totalElements: Number(data.totalElements ?? data.total ?? data.courses.length),
      totalPages: Number(data.totalPages ?? (data.courses.length ? 1 : 0)),
      number: Number(data.number ?? data.page ?? 0),
      size: Number(data.size ?? data.courses.length),
    };
  }

  if (Array.isArray(data)) {
    return {
      ...EMPTY_PAGE,
      content: data,
      totalElements: data.length,
      totalPages: data.length ? 1 : 0,
      size: data.length,
    };
  }

  return { ...EMPTY_PAGE };
}

export const pageParams = (page, size, params = {}) => ({
  ...params,
  page: Math.max(0, Number(page) - 1),
  size,
});
