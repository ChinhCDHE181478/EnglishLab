import { describe, expect, it, vi } from 'vitest';
import { persistOptimisticReorder, reorderItems } from './courseReorder';

describe('course content reorder', () => {
  it('normalizes display order immediately after moving an item', () => {
    expect(reorderItems([
      { id: 11, displayOrder: 1 },
      { id: 12, displayOrder: 2 },
      { id: 19, displayOrder: 9 },
    ], 2, 1)).toEqual([
      { id: 11, displayOrder: 1 },
      { id: 19, displayOrder: 2 },
      { id: 12, displayOrder: 3 },
    ]);
  });

  it('rolls back the previous normalized order when persistence fails', async () => {
    const onOptimistic = vi.fn();
    const onRollback = vi.fn();
    const error = new Error('Backend unavailable');

    await expect(persistOptimisticReorder({
      items: [{ id: 1, displayOrder: 1 }, { id: 2, displayOrder: 2 }],
      fromIndex: 1,
      toIndex: 0,
      onOptimistic,
      onRollback,
      persist: vi.fn().mockRejectedValue(error),
    })).rejects.toThrow('Backend unavailable');

    expect(onOptimistic).toHaveBeenCalledWith([
      { id: 2, displayOrder: 1 },
      { id: 1, displayOrder: 2 },
    ]);
    expect(onRollback).toHaveBeenCalledWith([
      { id: 1, displayOrder: 1 },
      { id: 2, displayOrder: 2 },
    ]);
  });
});
