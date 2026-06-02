const CourseSectionHeading = ({ eyebrow, title, center = false, className = '' }) => (
  <div className={`${center ? 'text-center' : ''} ${className}`}>
    {eyebrow ? (
      <span className="mb-2 block text-[12px] font-semibold uppercase leading-none tracking-[0.1em] text-[#4b0009]">
        {eyebrow}
      </span>
    ) : null}
    <h2 className="font-headline-lg text-[32px] font-bold leading-[1.2] text-[#4b0009]">{title}</h2>
  </div>
);

export default CourseSectionHeading;
