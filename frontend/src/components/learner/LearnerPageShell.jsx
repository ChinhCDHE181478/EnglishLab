import Header from '../ai-learning/Header';
import CourseFooter from '../course/CourseFooter';
import CourseGlobalStyles from '../course/CourseGlobalStyles';

const LearnerPageShell = ({ title, description, children, actions = null, eyebrow = '' }) => (
  <div className="course-page flex min-h-screen flex-col bg-[#f9f9f9] text-[#1a1c1c]">
    <CourseGlobalStyles />
    <Header />
    <main className="mx-auto flex w-full max-w-[1320px] flex-1 flex-col px-4 pb-[80px] pt-8 md:px-10">
      <section className="mb-8 rounded-[32px] border border-[#dfbfbd]/30 bg-[linear-gradient(135deg,_#fffdfd,_#fff3f4)] p-8 shadow-[0_18px_50px_rgba(75,0,9,0.08)]">
        <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
          <div className="max-w-3xl">
            {eyebrow ? <p className="text-[12px] font-extrabold uppercase tracking-[0.18em] text-[#730014]">{eyebrow}</p> : null}
            <h1 className="mt-3 font-['Manrope'] text-4xl font-extrabold tracking-tight text-[#2b2828] md:text-5xl">{title}</h1>
            <p className="mt-4 text-base leading-8 text-[#584140]">{description}</p>
          </div>
          {actions ? <div className="flex flex-wrap gap-3">{actions}</div> : null}
        </div>
      </section>
      {children}
    </main>
    <CourseFooter />
  </div>
);

export default LearnerPageShell;
