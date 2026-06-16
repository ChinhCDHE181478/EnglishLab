import BackendFeatureNotice from '../components/learner/BackendFeatureNotice';
import LearnerPageShell from '../components/learner/LearnerPageShell';

const NotificationsPage = () => (
  <LearnerPageShell
    title="Thông báo"
    description="Nơi này sẽ hiển thị các cập nhật học tập, ưu đãi và nhắc nhở dành cho bạn."
  >
    <BackendFeatureNotice
      title="Thông báo đang được hoàn thiện"
      description="EnglishLab đang hoàn thiện lại khu vực thông báo để các nhắc nhở học tập và cập nhật khóa học hiển thị đầy đủ, rõ ràng và đúng tài khoản hơn."
    />
  </LearnerPageShell>
);

export default NotificationsPage;
