import commerceApi from '../api/commerceApi';
import { hasAccessToken } from '../utils/auth';

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

const normalizeApiItem = (item = {}) => ({
  id: item.id,
  slug: item.slug,
  title: item.title,
  thumbnailUrl: item.thumbnailUrl,
  category: item.category,
  categoryName: item.categoryName,
  duration: item.duration,
  totalLessons: item.totalLessons,
  targetBand: item.targetBand,
  targetOutcome: item.targetOutcome,
  shortDescription: item.shortDescription || '',
  price: Number(item.price ?? item.salePrice ?? 0),
  salePrice: Number(item.salePrice ?? item.price ?? 0),
  originalPrice: Number(item.originalPrice ?? item.salePrice ?? item.price ?? 0),
  discountPercent: Number(item.discountPercent ?? 0),
  registered: Boolean(item.registered),
  status: item.status ?? 'AVAILABLE',
});

const normalizeStoredCourse = (course = {}) => normalizeApiItem(course);

const dedupeById = (items) => {
  const seen = new Set();
  return items.filter((item) => {
    const key = String(item?.id ?? '');
    if (!key || seen.has(key)) return false;
    seen.add(key);
    return true;
  });
};

const notifyCommerceUpdated = () => {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent(COMMERCE_EVENT));
  }
};

const useServerCommerce = () => hasAccessToken();

export const commerceStorageKeys = storageKeys;
export const commerceEventName = COMMERCE_EVENT;

export const readCart = () => readCollection(storageKeys.cart);
export const readWishlist = () => readCollection(storageKeys.wishlist);

export const isCourseInCart = (courseId) => readCart().some((item) => String(item.id) === String(courseId));
export const isCourseInWishlist = (courseId) => readWishlist().some((item) => String(item.id) === String(courseId));

export const fetchCart = async () => {
  if (!useServerCommerce()) return readCart();
  try {
    const items = (await commerceApi.getCart()).map(normalizeApiItem);
    writeCollection(storageKeys.cart, items);
    return items;
  } catch {
    return readCart();
  }
};

export const fetchWishlist = async () => {
  if (!useServerCommerce()) return readWishlist();
  try {
    const items = (await commerceApi.getWishlist()).map(normalizeApiItem);
    writeCollection(storageKeys.wishlist, items);
    return items;
  } catch {
    return readWishlist();
  }
};

export const addCourseToCart = async (course) => {
  const normalized = normalizeStoredCourse(course);
  if (normalized.registered) {
    return { ok: false, reason: 'registered' };
  }
  if (useServerCommerce()) {
    try {
      await commerceApi.addToCart(normalized.id);
      await fetchCart();
      notifyCommerceUpdated();
      return { ok: true };
    } catch (error) {
      return { ok: false, reason: error?.response?.data?.message || 'cart_error' };
    }
  }
  const items = readCart();
  if (items.some((item) => String(item.id) === String(normalized.id))) {
    return { ok: false, reason: 'exists' };
  }
  writeCollection(storageKeys.cart, dedupeById([normalized, ...items]));
  return { ok: true };
};

export const removeCourseFromCart = async (courseId) => {
  if (useServerCommerce()) {
    try {
      await commerceApi.removeFromCart(courseId);
    } catch {
      // fall through to local cleanup
    }
  }
  const nextItems = readCart().filter((item) => String(item.id) !== String(courseId));
  writeCollection(storageKeys.cart, nextItems);
  notifyCommerceUpdated();
  return nextItems;
};

export const addCourseToWishlist = async (course) => {
  const normalized = normalizeStoredCourse(course);
  if (useServerCommerce()) {
    try {
      await commerceApi.addToWishlist(normalized.id);
      await fetchWishlist();
      notifyCommerceUpdated();
      return { ok: true };
    } catch (error) {
      return { ok: false, reason: error?.response?.data?.message || 'wishlist_error' };
    }
  }
  const items = readWishlist();
  if (items.some((item) => String(item.id) === String(normalized.id))) {
    return { ok: false, reason: 'exists' };
  }
  writeCollection(storageKeys.wishlist, dedupeById([normalized, ...items]));
  return { ok: true };
};

export const removeCourseFromWishlist = async (courseId) => {
  if (useServerCommerce()) {
    try {
      await commerceApi.removeFromWishlist(courseId);
    } catch {
      // fall through
    }
  }
  const nextItems = readWishlist().filter((item) => String(item.id) !== String(courseId));
  writeCollection(storageKeys.wishlist, nextItems);
  notifyCommerceUpdated();
  return nextItems;
};

export const moveWishlistCourseToCart = async (course) => {
  if (useServerCommerce()) {
    try {
      await commerceApi.moveWishlistToCart(course.id);
      await Promise.all([fetchCart(), fetchWishlist()]);
      notifyCommerceUpdated();
      return { ok: true };
    } catch (error) {
      return { ok: false, reason: error?.response?.data?.message || 'move_error' };
    }
  }
  const addResult = await addCourseToCart(course);
  if (addResult.ok) {
    await removeCourseFromWishlist(course.id);
  }
  return addResult;
};

export const syncLocalCartToServer = async () => {
  if (!useServerCommerce()) return readCart();
  const localIds = readCart().map((item) => item.id).filter(Boolean);
  if (!localIds.length) return fetchCart();
  try {
    const items = (await commerceApi.syncCart(localIds)).map(normalizeApiItem);
    writeCollection(storageKeys.cart, items);
    notifyCommerceUpdated();
    return items;
  } catch {
    return readCart();
  }
};
