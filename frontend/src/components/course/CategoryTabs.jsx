import { COURSE_CATEGORIES } from './courseConstants';

const CategoryTabs = ({ activeCategory, onChange }) => (
  <section className="mb-12 overflow-x-auto whitespace-nowrap pb-4 scroll-smooth">
    <div className="flex justify-start gap-3 md:justify-center">
      {COURSE_CATEGORIES.map((category) => (
        <button
          key={category.label}
          className={`category-chip rounded-full border border-[#dfbfbd]/30 px-6 py-2 text-[14px] font-semibold leading-none tracking-[0.02em] transition-all ${activeCategory === category.value ? 'active' : 'bg-white hover:bg-[#eeeeed]'}`}
          type="button"
          onClick={() => onChange(category.value)}
        >
          {category.label}
        </button>
      ))}
    </div>
  </section>
);

export default CategoryTabs;
