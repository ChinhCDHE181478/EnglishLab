export function normalizeDisplayOrder(items = []) {
  return items.map((item, index) => ({ ...item, displayOrder: index + 1 }));
}

export function reorderItems(items, fromIndex, toIndex) {
  if (fromIndex === toIndex || fromIndex < 0 || toIndex < 0 || fromIndex >= items.length || toIndex >= items.length) {
    return normalizeDisplayOrder(items);
  }
  const next = [...items];
  const [moved] = next.splice(fromIndex, 1);
  next.splice(toIndex, 0, moved);
  return normalizeDisplayOrder(next);
}

export async function persistOptimisticReorder({
  items,
  fromIndex,
  toIndex,
  onOptimistic,
  onRollback,
  persist,
}) {
  const previous = normalizeDisplayOrder(items);
  const optimistic = reorderItems(items, fromIndex, toIndex);
  onOptimistic(optimistic);
  try {
    const saved = await persist(optimistic);
    return normalizeDisplayOrder(Array.isArray(saved) ? saved : optimistic);
  } catch (error) {
    onRollback(previous);
    throw error;
  }
}
