import { useEffect, useMemo, useState } from 'react';
import { BookOpen, Layers3 } from 'lucide-react';
import WorkspaceFlashcards from '../course-workspace/WorkspaceFlashcards';
import BrandedSelect from '../ui/BrandedSelect';

const toFlashcardCourse = (curriculum, unit) => ({
  title: `${curriculum?.title || 'Chương trình học'} · ${unit?.title || 'Flashcards'}`,
  modules: unit ? [{
    id: `classroom-unit-${unit.id}`,
    title: unit.title,
    lessons: [{
      id: `classroom-unit-${unit.id}-flashcards`,
      title: `Flashcards · ${unit.title}`,
      flashcardSets: (unit.flashcards || []).map((reference) => ({
        id: reference.resourceId,
        title: reference.title,
        description: reference.note || reference.subtitle,
        cardsJson: reference.contentJson || '[]',
      })),
    }],
  }] : [],
});

export default function ClassroomFlashcardsPanel({ curriculum, initialUnitId = null }) {
  const flashcardUnits = useMemo(
    () => (curriculum?.units || []).filter((unit) => (unit.flashcards || []).length > 0),
    [curriculum?.units],
  );
  const [activeUnitId, setActiveUnitId] = useState(() => String(initialUnitId || flashcardUnits[0]?.id || ''));

  useEffect(() => {
    const requestedUnit = flashcardUnits.find((unit) => String(unit.id) === String(initialUnitId));
    const currentUnitExists = flashcardUnits.some((unit) => String(unit.id) === String(activeUnitId));
    if (requestedUnit) {
      setActiveUnitId(String(requestedUnit.id));
    } else if (!currentUnitExists) {
      setActiveUnitId(String(flashcardUnits[0]?.id || ''));
    }
  }, [activeUnitId, flashcardUnits, initialUnitId]);

  const activeUnit = flashcardUnits.find((unit) => String(unit.id) === String(activeUnitId)) || flashcardUnits[0];
  const course = useMemo(() => toFlashcardCourse(curriculum, activeUnit), [activeUnit, curriculum]);

  if (!flashcardUnits.length) {
    return (
      <div className="flex min-h-[360px] flex-col items-center justify-center border border-dashed border-[#dfbfbd] bg-white px-6 text-center">
        <BookOpen className="h-12 w-12 text-[#8c716f]" />
        <h3 className="mt-4 font-['Manrope'] text-xl font-extrabold text-[#2b2828]">Chưa có flashcard trong giáo trình</h3>
      </div>
    );
  }

  return (
    <section className="space-y-6">
      <div className="flex flex-col gap-4 border-b border-[#eadfe0] pb-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p className="inline-flex items-center gap-2 text-xs font-extrabold uppercase tracking-[0.14em] text-[#8a0018]">
            <Layers3 className="h-4 w-4" />
            Flashcard theo unit
          </p>
          <h2 className="mt-2 font-['Manrope'] text-2xl font-extrabold text-[#1a1c1c]">{activeUnit?.title}</h2>
        </div>
        <div className="w-full lg:hidden">
          <BrandedSelect
            onChange={(event) => setActiveUnitId(event.target.value)}
            options={flashcardUnits.map((unit) => ({ label: unit.title, value: String(unit.id) }))}
            value={String(activeUnit?.id || '')}
          />
        </div>
      </div>

      <div className="hidden gap-2 overflow-x-auto pb-2 lg:flex" role="tablist" aria-label="Chọn unit flashcard">
        {flashcardUnits.map((unit) => {
          const active = String(unit.id) === String(activeUnit?.id);
          return (
            <button
              aria-selected={active}
              className={`shrink-0 border-b-2 px-4 py-3 text-sm font-extrabold transition ${active ? 'border-[#8a0018] text-[#8a0018]' : 'border-transparent text-[#6a5553] hover:border-[#dfbfbd] hover:text-[#8a0018]'}`}
              key={unit.id}
              onClick={() => setActiveUnitId(String(unit.id))}
              role="tab"
              type="button"
            >
              Unit {unit.displayOrder}
            </button>
          );
        })}
      </div>

      <WorkspaceFlashcards course={course} key={activeUnit?.id} />
    </section>
  );
}
