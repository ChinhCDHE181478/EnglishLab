import React from 'react';
import { motion } from 'framer-motion';
import { Headphones, BookOpen, PenLine, Mic } from 'lucide-react';

const tabs = [
  { id: 'listening', label: 'Listening', icon: Headphones },
  { id: 'reading', label: 'Reading', icon: BookOpen },
  { id: 'writing', label: 'Writing', icon: PenLine },
  { id: 'speaking', label: 'Speaking', icon: Mic },
];

const SkillTabs = ({ activeTab, setActiveTab }) => {
  return (
    <div className="mx-auto flex w-fit max-w-full overflow-x-auto rounded-full border border-[#dfbfbd]/50 bg-white p-1.5 shadow-sm scrollbar-hide">
      {tabs.map((tab) => {
        const Icon = tab.icon;
        const isActive = activeTab === tab.id;

        return (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={`relative flex items-center gap-2 whitespace-nowrap rounded-full px-5 py-2.5 text-sm font-semibold transition-colors md:px-6 md:text-base ${
              isActive ? 'text-[#730014]' : 'text-[#584140] hover:text-[#730014]'
            }`}
          >
            {isActive && (
              <motion.div
                layoutId="active-tab-indicator"
                className="absolute inset-0 rounded-full bg-[#730014]/10"
                transition={{ type: 'spring', bounce: 0.2, duration: 0.6 }}
              />
            )}
            <Icon size={18} className="relative z-10" />
            <span className="relative z-10">{tab.label}</span>
          </button>
        );
      })}
    </div>
  );
};

export default SkillTabs;