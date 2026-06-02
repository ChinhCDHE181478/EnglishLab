import React from 'react';

const tones = {
  yellow: {
    avatar: 'bg-yellow-100 text-yellow-700',
    bubble: 'border-yellow-200 bg-yellow-50',
    title: 'text-yellow-800',
  },
  red: {
    avatar: 'bg-red-100 text-red-700',
    bubble: 'border-red-200 bg-red-50',
    title: 'text-red-800',
  },
  blue: {
    avatar: 'bg-blue-100 text-blue-700',
    bubble: 'border-blue-200 bg-blue-50',
    title: 'text-blue-800',
  },
};

const FeedbackBubble = ({ delay, icon: Icon, tone, title, children }) => {
  const styles = tones[tone];

  return (
    <div
      className="animate-float-up flex gap-4 opacity-0"
      style={{ animationFillMode: 'forwards', animationDelay: `${delay}ms` }}
    >
      <div
        className={`mt-1 flex h-8 w-8 shrink-0 items-center justify-center rounded-full shadow-sm ${styles.avatar}`}
      >
        <Icon size={16} strokeWidth={2.2} />
      </div>
      <div
        className={`w-full rounded-lg rounded-tl-none border p-3 transition-shadow hover:shadow-md ${styles.bubble}`}
      >
        <p className={`mb-1 text-sm font-semibold ${styles.title}`}>{title}</p>
        <p className="text-sm text-[#605d5c]">{children}</p>
      </div>
    </div>
  );
};

export default FeedbackBubble;
