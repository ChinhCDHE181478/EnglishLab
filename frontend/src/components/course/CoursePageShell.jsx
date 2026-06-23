import Header from '../ai-learning/Header';
import CourseFooter from './CourseFooter';
import CourseGlobalStyles from './CourseGlobalStyles';
import {
  PAGE_BODY_CLASS,
  PAGE_CONTAINER_CLASS,
  PAGE_HEADER_CLASS,
  PAGE_SHELL_CLASS,
} from '../../utils/pageLayout';

const CoursePageShell = ({ children, bottomBar = null, mainClassName = '' }) => (
  <>
    <CourseGlobalStyles />
    <div className={PAGE_SHELL_CLASS}>
      <div className={PAGE_HEADER_CLASS}>
        <Header />
      </div>
      <div className={PAGE_BODY_CLASS}>
        <div className={`${PAGE_CONTAINER_CLASS} flex flex-1 flex-col min-h-0 py-8`}>
          <main className={['flex flex-1 flex-col min-h-0 space-y-8', mainClassName].filter(Boolean).join(' ')}>
            {children}
          </main>
          {bottomBar}
        </div>
      </div>
      <CourseFooter />
    </div>
  </>
);

export default CoursePageShell;
