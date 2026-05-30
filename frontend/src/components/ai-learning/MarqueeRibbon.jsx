import React from 'react';

const items = [
  'AI Writing Scoring',
  'Online Mock Tests',
  'Personalized Roadmaps',
  'Expert Native Mentors',
  '24/7 Learning Platform',
  'Interactive TOEIC Mastery',
];

const MarqueeGroup = () => (
  <div className="animate-marquee flex w-1/2 items-center justify-around gap-8 px-4 text-xs font-semibold uppercase tracking-widest opacity-90">
    {items.map((item) => (
      <React.Fragment key={item}>
        <span>{item}</span>
        <span className="h-1.5 w-1.5 flex-shrink-0 rounded-full bg-white/50" />
      </React.Fragment>
    ))}
  </div>
);

const MarqueeRibbon = () => (
  <div className="overflow-hidden border-y border-[#4b0009] bg-[#730014] py-4 text-white">
    <div className="flex w-[200%] whitespace-nowrap">
      <MarqueeGroup />
      <MarqueeGroup />
    </div>
  </div>
);

export default MarqueeRibbon;
