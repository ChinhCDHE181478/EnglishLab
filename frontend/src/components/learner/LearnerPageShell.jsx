import { motion } from 'framer-motion';
import Header from '../ai-learning/Header';
import CourseFooter from '../course/CourseFooter';
import CourseGlobalStyles from '../course/CourseGlobalStyles';
import { PAGE_BODY_CLASS, PAGE_HEADER_CLASS, PAGE_MAIN_STACK_CLASS, PAGE_SHELL_CLASS } from '../../utils/pageLayout';

const pageVariants = {
  hidden: { opacity: 0, y: 14 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.35, ease: 'easeOut' } },
};

const headerVariants = {
  hidden: { opacity: 0, y: -8 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.3, ease: 'easeOut' } },
};

const LearnerPageShell = ({ title, description, children, actions = null, eyebrow = '', hideHeader = false }) => (
  <div className={PAGE_SHELL_CLASS}>
    <CourseGlobalStyles />
    <div className={PAGE_HEADER_CLASS}>
      <Header />
    </div>
    <div className={PAGE_BODY_CLASS}>
      <main className={PAGE_MAIN_STACK_CLASS}>
      {!hideHeader && (
        <motion.section
          className="mb-7"
          initial="hidden"
          animate="visible"
          variants={headerVariants}
        >
          <div className="rounded-[28px] border border-gray-200/80 bg-white p-6 md:p-8 shadow-[0_10px_35px_rgba(0,0,0,0.015)] flex flex-col gap-5 md:flex-row md:items-center md:justify-between transition duration-300 hover:shadow-[0_15px_45px_rgba(75,0,9,0.035)]">
            <div className="space-y-1.5">
              {eyebrow ? (
                <span className="inline-flex items-center rounded-full bg-[#fff0f1] px-2.5 py-0.5 text-[9px] font-extrabold uppercase tracking-widest text-[#730014] border border-[#dfbfbd]/35 mb-1.5">
                  {eyebrow}
                </span>
              ) : null}
              <div className="flex items-center gap-3">
                <span className="h-6 w-1 shrink-0 rounded-full bg-[#8a0018]" />
                <h1 className="font-['Manrope'] text-xl font-extrabold tracking-tight text-[#1a1c1c] md:text-2xl leading-snug">
                  {title}
                </h1>
              </div>
              {description ? (
                <p className="text-xs leading-relaxed text-[#584140] pl-4">{description}</p>
              ) : null}
            </div>
            {actions ? (
              <div className="flex flex-wrap gap-2.5 shrink-0 pt-2 md:pt-0">
                {actions}
              </div>
            ) : null}
          </div>
        </motion.section>
      )}
      <motion.div
        className="flex flex-1 flex-col"
        initial="hidden"
        animate="visible"
        variants={pageVariants}
      >
        {children}
      </motion.div>
    </main>
    </div>
    <CourseFooter />
  </div>
);

export default LearnerPageShell;
