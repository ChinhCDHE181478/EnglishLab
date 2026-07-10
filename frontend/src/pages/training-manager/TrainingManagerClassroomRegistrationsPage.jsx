import { useSearchParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import TrainingManagerRegistrationPanel from '../../components/training-manager/TrainingManagerRegistrationPanel';

export default function TrainingManagerClassroomRegistrationsPage() {
  const [searchParams] = useSearchParams();
  const initialTab = searchParams.get('tab') || 'NEEDS_ACTION';
  const classroomOfferingId = searchParams.get('classroomId') ? Number(searchParams.get('classroomId')) : null;
  const initialEnrollmentId = searchParams.get('enrollmentId') || '';

  return (
    <motion.main
      className="space-y-6"
      initial={{ opacity: 0, y: 14 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.32, ease: 'easeOut' }}
    >
      <TrainingManagerRegistrationPanel
        classroomOfferingId={classroomOfferingId}
        initialEnrollmentId={initialEnrollmentId}
        initialTab={initialTab}
      />
    </motion.main>
  );
}
