import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Play } from 'lucide-react';

const contentVariants = {
  hidden: { opacity: 0, x: -20 },
  visible: { opacity: 1, x: 0, transition: { duration: 0.4 } },
  exit: { opacity: 0, x: 20, transition: { duration: 0.3 } }
};

const EssayDraft = ({ activeTab, essayText, onEssayChange, onSubmit, loading, error }) => {
  return (
    <div className="flex h-[400px] flex-col rounded-xl bg-gray-50/50 p-6">
      <h3 className="mb-4 font-bold text-[#1a1c1c] text-lg border-b border-gray-200 pb-3">
        Bài làm của học viên
      </h3>
      
      <div className="relative flex-1 overflow-y-auto pr-2 scrollbar-thin scrollbar-thumb-gray-200">
        <AnimatePresence mode="wait">
          {activeTab === 'writing' && (
            <motion.div key="writing" variants={contentVariants} initial="hidden" animate="visible" exit="exit" className="flex h-full flex-col gap-3">
              <textarea
                className="min-h-0 flex-1 resize-none rounded-xl border border-[#dfbfbd] bg-white p-4 text-sm leading-6 text-gray-700 outline-none transition focus:border-[#730014] focus:ring-2 focus:ring-[#730014]/10"
                maxLength={3000}
                onChange={(event) => onEssayChange(event.target.value)}
                placeholder="Dán hoặc nhập bài Writing của bạn tại đây (80–3000 ký tự)..."
                value={essayText}
              />
              <div className="flex items-center justify-between gap-3 text-xs text-gray-500">
                <span>{essayText.length}/3000 ký tự</span>
                <button className="rounded-full bg-[#730014] px-5 py-2.5 font-bold text-white transition hover:bg-[#4b0009] disabled:cursor-not-allowed disabled:opacity-60" disabled={loading} onClick={onSubmit} type="button">
                  {loading ? 'Đang phân tích...' : 'Nhận phản hồi AI'}
                </button>
              </div>
              {error ? <p className="text-sm font-medium text-red-600">{error}</p> : null}
            </motion.div>
          )}

          {activeTab === 'speaking' && (
            <motion.div key="speaking" variants={contentVariants} initial="hidden" animate="visible" exit="exit" className="flex h-full flex-col items-center justify-center space-y-6">
              <div className="flex items-center gap-4 bg-white p-4 rounded-full shadow-sm border border-gray-100 w-full max-w-sm">
                <button className="bg-[#730014] text-white p-3 rounded-full hover:bg-[#5a0010] transition">
                  <Play size={20} className="ml-1" />
                </button>
                <div className="flex-1 flex gap-1 items-center h-8">
                   {/* Waveform mô phỏng */}
                  {[...Array(15)].map((_, i) => (
                    <motion.div key={i} animate={{ height: [10, Math.random() * 20 + 10, 10] }} transition={{ repeat: Infinity, duration: 1.5, delay: i * 0.1 }} className="w-1.5 bg-[#dfbfbd] rounded-full" />
                  ))}
                </div>
                <span className="text-sm font-medium text-gray-500">01:24</span>
              </div>
              <p className="text-center text-gray-600 italic">"Well, I think technology has fundamentally changed how we communicate..."</p>
            </motion.div>
          )}

          {activeTab === 'reading' && (
            <motion.div key="reading" variants={contentVariants} initial="hidden" animate="visible" exit="exit" className="text-gray-600 leading-relaxed space-y-4">
              <p><strong>Question 14:</strong> The development of artificial intelligence...</p>
              <div className="bg-white p-3 rounded-lg border border-gray-200">
                <p>Your answer: <span className="text-red-500 font-semibold line-through">False</span></p>
                <p>Correct answer: <span className="text-green-600 font-semibold">Not Given</span></p>
              </div>
              <p className="text-sm bg-blue-50 p-3 rounded text-blue-800">
                * Location: Paragraph C, Line 4 - The text mentions the concept but does not confirm or deny the statement.
              </p>
            </motion.div>
          )}

          {activeTab === 'listening' && (
            <motion.div key="listening" variants={contentVariants} initial="hidden" animate="visible" exit="exit" className="space-y-6">
               <div className="bg-white p-4 rounded-xl border border-gray-100 flex items-center justify-between shadow-sm">
                 <span className="font-medium text-gray-700">Section 2: Audio track</span>
                 <button className="flex items-center gap-2 text-[#730014] font-semibold text-sm">
                   <Play size={16} /> Play Audio
                 </button>
               </div>
               <div className="space-y-3">
                 <p>11. The main purpose of the museum is to <span className="border-b-2 border-green-500 text-green-600 font-medium px-2">educate children</span>.</p>
                 <p>12. The exhibition will run until <span className="border-b-2 border-red-500 text-red-500 font-medium px-2">November</span> (Answer: December).</p>
               </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
};

export default EssayDraft;
