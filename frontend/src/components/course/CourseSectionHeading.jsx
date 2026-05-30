const CourseSectionHeading = ({ eyebrow, title, centered = false, className = '' }) => (
  <div className={`${centered ? 'text-center' : ''} ${className}`}>
    {eyebrow && (
      <span className="mb-2 block font-['Inter'] text-[12px] font-semibold uppercase leading-none tracking-[0.1em] text-[#4b0009]">
        {eyebrow}
      </span>
    )}
    <h2 className="font-['Manrope'] text-[32px] font-bold leading-[1.2] text-[#1a1c1c]">
      {title}
    </h2>
  </div>
);

export default CourseSectionHeading;
