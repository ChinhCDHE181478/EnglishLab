const storageKeys = {
  cart: 'englishlab_cart',
  wishlist: 'englishlab_wishlist',
};

const COMMERCE_EVENT = 'englishlab:commerce-updated';

const safeParse = (value, fallback) => {
  try {
    return JSON.parse(value ?? '');
  } catch {
    return fallback;
  }
};

const readCollection = (key) => {
  if (typeof window === 'undefined') return [];
  const parsed = safeParse(window.localStorage.getItem(key), []);
  return Array.isArray(parsed) ? parsed : [];
};

const writeCollection = (key, value) => {
  if (typeof window === 'undefined') return;
  window.localStorage.setItem(key, JSON.stringify(value));
  window.dispatchEvent(new CustomEvent(COMMERCE_EVENT, { detail: { key } }));
};

const normalizeStoredCourse = (course = {}) => ({
  id: course.id,
  slug: course.slug,
  title: course.title,
  thumbnailUrl: course.thumbnailUrl,
  category: course.category,
  categoryName: course.categoryName,
  duration: course.duration,
  totalLessons: course.totalLessons,
  targetBand: course.targetBand,
  targetOutcome: course.targetOutcome,
  shortDescription: course.shortDescription || course.description || '',
  price: Number(course.price ?? 0),
  salePrice: Number(course.salePrice ?? course.price ?? 0),
  originalPrice: Number(course.originalPrice ?? course.salePrice ?? course.price ?? 0),
  discountPercent: Number(course.discountPercent ?? 0),
  registered: Boolean(course.registered),
  status: course.status ?? 'AVAILABLE',
});

const dedupeById = (items) => {
  const seen = new Set();
  return items.filter((item) => {
    const key = String(item?.id ?? '');
    if (!key || seen.has(key)) return false;
    seen.add(key);
    return true;
  });
};

export const commerceStorageKeys = storageKeys;
export const commerceEventName = COMMERCE_EVENT;

export const readCart = () => readCollection(storageKeys.cart);
export const readWishlist = () => readCollection(storageKeys.wishlist);

export const isCourseInCart = (courseId) => readCart().some((item) => String(item.id) === String(courseId));
export const isCourseInWishlist = (courseId) => readWishlist().some((item) => String(item.id) === String(courseId));

export const addCourseToCart = (course) => {
  const items = readCart();
  const normalized = normalizeStoredCourse(course);
  if (normalized.registered) {
    return { ok: false, reason: 'registered' };
  }
  if (items.some((item) => String(item.id) === String(normalized.id))) {
    return { ok: false, reason: 'exists' };
  }
  writeCollection(storageKeys.cart, dedupeById([normalized, ...items]));
  return { ok: true };
};

export const removeCourseFromCart = (courseId) => {
  const nextItems = readCart().filter((item) => String(item.id) !== String(courseId));
  writeCollection(storageKeys.cart, nextItems);
  return nextItems;
};

export const addCourseToWishlist = (course) => {
  const items = readWishlist();
  const normalized = normalizeStoredCourse(course);
  if (items.some((item) => String(item.id) === String(normalized.id))) {
    return { ok: false, reason: 'exists' };
  }
  writeCollection(storageKeys.wishlist, dedupeById([normalized, ...items]));
  return { ok: true };
};

export const removeCourseFromWishlist = (courseId) => {
  const nextItems = readWishlist().filter((item) => String(item.id) !== String(courseId));
  writeCollection(storageKeys.wishlist, nextItems);
  return nextItems;
};

export const moveWishlistCourseToCart = (course) => {
  const addResult = addCourseToCart(course);
  if (addResult.ok) {
    removeCourseFromWishlist(course.id);
  }
  return addResult;
};
