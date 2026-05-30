import { categoryTabs } from './courseData';

const CategoryTabs = () => (
  <section className="course-scrollbar mb-12 overflow-x-auto whitespace-nowrap pb-4 scroll-smooth">
    <div className="flex justify-start gap-3 md:justify-center">
      {categoryTabs.map((tab, index) => (
        <button
          key={tab}
          className={`rounded-full border border-[#dfbfbd]/30 px-6 py-2 font-['Inter'] text-sm font-semibold leading-none tracking-[0.02em] transition-all ${
            index === 0 ? 'bg-[#730014] text-white' : 'bg-white text-[#1a1c1c] hover:bg-[#eeeeed]'
          }`}
        >
          {tab}
        </button>
      ))}
    </div>
  </section>
);

export default CategoryTabs;
