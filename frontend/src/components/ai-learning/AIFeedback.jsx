import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Target, Zap, CheckCircle2, AlertCircle } from 'lucide-react';

const feedbackVariants = {
  hidden: { opacity: 0, x: 20 },
  visible: { opacity: 1, x: 0, transition: { duration: 0.4 } },
  exit: { opacity: 0, x: -20, transition: { duration: 0.3 } }
};

const AIFeedback = ({ activeTab, feedback, loading }) => {
  // Điểm số mô phỏng cho từng tab
  const bandScores = {
    listening: 7.5,
    reading: 8.0,
    writing: 6.5,
    speaking: 7.0
  };

  return (
    <div className="flex h-[400px] flex-col rounded-xl border border-[#730014]/10 bg-[#730014]/[0.02] p-6">
      <div className="flex items-center justify-between mb-6 border-b border-[#730014]/10 pb-4">
        <h3 className="font-bold text-[#1a1c1c] text-lg flex items-center gap-2">
          <Zap size={20} className="text-[#730014]" /> AI Assessment
        </h3>
        <div className="flex items-center gap-2 bg-white px-3 py-1 rounded-full shadow-sm border border-[#dfbfbd]">
          <Target size={16} className="text-[#730014]" />
          <span className="font-bold text-[#1a1c1c]">Band {activeTab === 'writing' && feedback?.estimatedScore != null ? feedback.estimatedScore : bandScores[activeTab]}</span>
        </div>
      </div>

      <div className="relative flex-1 overflow-y-auto pr-2 scrollbar-thin scrollbar-thumb-gray-200">
        <AnimatePresence mode="wait">
          {activeTab === 'writing' && (
            <motion.div key="writing" variants={feedbackVariants} initial="hidden" animate="visible" exit="exit" className="space-y-4">
              {loading ? <p className="py-16 text-center text-sm font-semibold text-[#730014]">AI đang đọc và phân tích bài viết...</p> : null}
              {!loading && !feedback ? <p className="py-12 text-center text-sm leading-6 text-gray-500">Nhập bài viết và chọn “Nhận phản hồi AI” để xem điểm ước tính cùng góp ý chi tiết.</p> : null}
              {!loading && feedback ? <>
                <div className="rounded-lg border border-gray-100 bg-white p-4 shadow-sm">
                  <h4 className="mb-2 flex items-center gap-2 text-sm font-semibold text-gray-800"><Zap size={16} className="text-[#730014]"/> Nhận xét tổng quan</h4>
                  <p className="text-sm leading-6 text-gray-600">{feedback.overallFeedback}</p>
                </div>
                {(feedback.strengths || []).map((item) => <div className="rounded-lg border border-gray-100 bg-white p-4 shadow-sm" key={item}><h4 className="mb-2 flex items-center gap-2 text-sm font-semibold text-gray-800"><CheckCircle2 size={16} className="text-green-500"/> Điểm mạnh</h4><p className="text-sm text-gray-600">{item}</p></div>)}
                {(feedback.improvements || []).map((item) => <div className="rounded-lg border border-gray-100 bg-white p-4 shadow-sm" key={item}><h4 className="mb-2 flex items-center gap-2 text-sm font-semibold text-gray-800"><AlertCircle size={16} className="text-amber-500"/> Cần cải thiện</h4><p className="text-sm text-gray-600">{item}</p></div>)}
              </> : null}
            </motion.div>
          )}

          {activeTab === 'speaking' && (
            <motion.div key="speaking" variants={feedbackVariants} initial="hidden" animate="visible" exit="exit" className="space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <div className="bg-white p-3 rounded-lg shadow-sm border border-gray-100 text-center">
                  <p className="text-xs text-gray-500 uppercase font-semibold">Fluency</p>
                  <p className="text-lg font-bold text-[#730014]">7.0</p>
                </div>
                <div className="bg-white p-3 rounded-lg shadow-sm border border-gray-100 text-center">
                  <p className="text-xs text-gray-500 uppercase font-semibold">Pronunciation</p>
                  <p className="text-lg font-bold text-[#730014]">7.5</p>
                </div>
              </div>
              <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-100">
                <p className="text-sm text-gray-600"><strong>AI Feedback:</strong> Clear pronunciation, but there were brief hesitations around the 0:45 mark. Your intonation is natural.</p>
              </div>
            </motion.div>
          )}

          {(activeTab === 'reading' || activeTab === 'listening') && (
            <motion.div key={activeTab} variants={feedbackVariants} initial="hidden" animate="visible" exit="exit" className="space-y-4">
              <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-100 flex items-center justify-between">
                <div>
                  <p className="text-xs text-gray-500 uppercase font-semibold">Correct Answers</p>
                  <p className="text-xl font-bold text-green-600">32/40</p>
                </div>
                <div className="text-right">
                  <p className="text-xs text-gray-500 uppercase font-semibold">Time Spent</p>
                  <p className="text-xl font-bold text-gray-700">45:12</p>
                </div>
              </div>
              <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-100">
                <h4 className="font-semibold text-sm text-gray-800 mb-2">Weakness detected</h4>
                <p className="text-sm text-gray-600">You consistently struggle with <strong>Matching Headings</strong>. Recommend reviewing strategies for identifying topic sentences.</p>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
};

export default AIFeedback;
