import React, { useState } from 'react';
import { Brain, PenLine, TrendingUp, Mic, Headphones, BookOpen } from 'lucide-react';
import { motion } from 'framer-motion';
import AIFeedback from './AIFeedback';
import EssayDraft from './EssayDraft';
import SkillTabs from './SkillTabs';
import { Link } from 'react-router-dom';
import { courseApi } from '../../api/courseApi';

const AILearningSection = () => {
  const [activeTab, setActiveTab] = useState('writing');
  const [essayText, setEssayText] = useState('');
  const [feedback, setFeedback] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleWritingFeedback = async () => {
    const text = essayText.trim();
    if (text.length < 80) {
      setError('Bài viết cần có ít nhất 80 ký tự để AI có đủ dữ liệu phân tích.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      const result = await courseApi.getWritingFeedback({ essayText: text, targetExam: 'IELTS' });
      setFeedback(result);
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Chưa thể nhận phản hồi lúc này. Vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  // Animation variants
  const fadeUp = {
    hidden: { opacity: 0, y: 30 },
    visible: { opacity: 1, y: 0, transition: { duration: 0.6, ease: 'easeOut' } }
  };

  return (
    <section className="mx-auto max-w-7xl overflow-hidden px-4 py-20 md:px-10">
      <motion.div 
        className="mb-16 text-center"
        initial="hidden"
        whileInView="visible"
        viewport={{ once: true }}
        variants={fadeUp}
      >
        <span className="mb-4 inline-block rounded bg-[#730014]/10 px-3 py-1.5 text-xs font-semibold uppercase tracking-wider text-[#730014]">
          Công nghệ tiên phong
        </span>
        <h2 className="mb-4 font-['Manrope'] text-3xl font-bold text-[#1a1c1c] md:text-4xl">
          Trải nghiệm sức mạnh của AI trong học tập
        </h2>
        <p className="mx-auto mb-8 max-w-2xl text-lg leading-8 text-[#584140]">
          Chấm điểm & phân tích chi tiết 4 kỹ năng tức thì với độ chính xác
          cao, nhận feedback chi tiết như được kèm 1-1 bởi giám khảo bản xứ.
        </p>

        <SkillTabs activeTab={activeTab} setActiveTab={setActiveTab} />
      </motion.div>

      <motion.div 
        className="grid grid-cols-1 items-stretch gap-8 rounded-2xl border border-[#dfbfbd]/30 bg-white p-6 shadow-sm md:p-12 lg:grid-cols-2"
        initial="hidden"
        whileInView="visible"
        viewport={{ once: true }}
        variants={fadeUp}
      >
        <EssayDraft activeTab={activeTab} essayText={essayText} onEssayChange={setEssayText} onSubmit={handleWritingFeedback} loading={loading} error={error} />
        <AIFeedback activeTab={activeTab} feedback={feedback} loading={loading} />
      </motion.div>

      <div className="mt-6 text-center">
        <Link className="inline-flex rounded-full border border-[#8a0018] px-6 py-3 text-sm font-extrabold text-[#730014] transition hover:bg-[#730014] hover:text-white" to="/ai/writing-feedback">
          Mở công cụ phản hồi Writing đầy đủ
        </Link>
      </div>

      <motion.div 
        className="mx-auto mt-10 flex max-w-3xl flex-wrap items-center justify-center gap-3 text-sm text-[#584140]"
        initial={{ opacity: 0 }}
        whileInView={{ opacity: 1 }}
        transition={{ delay: 0.4, duration: 0.6 }}
        viewport={{ once: true }}
      >
        <span className="inline-flex items-center gap-2 rounded-full border border-[#730014]/10 bg-white px-4 py-2 hover:bg-[#730014]/5 transition-colors cursor-default">
          <PenLine size={16} className="text-[#730014]" /> Grammar
        </span>
        <span className="inline-flex items-center gap-2 rounded-full border border-[#730014]/10 bg-white px-4 py-2 hover:bg-[#730014]/5 transition-colors cursor-default">
          <TrendingUp size={16} className="text-[#730014]" /> Vocabulary
        </span>
        <span className="inline-flex items-center gap-2 rounded-full border border-[#730014]/10 bg-white px-4 py-2 hover:bg-[#730014]/5 transition-colors cursor-default">
          <Mic size={16} className="text-[#730014]" /> Pronunciation
        </span>
        <span className="inline-flex items-center gap-2 rounded-full border border-[#730014]/10 bg-white px-4 py-2 hover:bg-[#730014]/5 transition-colors cursor-default">
          <Brain size={16} className="text-[#730014]" /> Band score insight
        </span>
      </motion.div>
    </section>
  );
};

export default AILearningSection;
