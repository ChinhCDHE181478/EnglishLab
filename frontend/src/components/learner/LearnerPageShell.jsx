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

const LearnerPageShell = ({ title, description, children, actions = null, eyebrow = '' }) => (
  <div className={PAGE_SHELL_CLASS}>
    <CourseGlobalStyles />
    <div className={PAGE_HEADER_CLASS}>
      <Header />
    </div>
    <div className={PAGE_BODY_CLASS}>
      <main className={PAGE_MAIN_STACK_CLASS}>
      <motion.section
        className="mb-7 border-b border-[#ebebeb] pb-6"
        initial="hidden"
        animate="visible"
        variants={headerVariants}
      >
        <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div className="max-w-3xl">
            {eyebrow ? <p className="mb-2 text-[11px] font-semibold uppercase tracking-widest text-[#8a0018]">{eyebrow}</p> : null}
            <div className="flex items-center gap-3">
              <span className="h-7 w-1 shrink-0 rounded-full bg-[#8a0018]" />
              <h1 className="font-['Manrope'] text-2xl font-extrabold tracking-tight text-[#1a1c1c] md:text-3xl">{title}</h1>
            </div>
            <p className="mt-2 pl-4 text-sm leading-6 text-[#6a5553]">{description}</p>
          </div>
          {actions ? <div className="flex flex-wrap gap-3">{actions}</div> : null}
        </div>
      </motion.section>
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
