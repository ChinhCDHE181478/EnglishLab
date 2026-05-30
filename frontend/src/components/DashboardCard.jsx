import '../styles/home.css';

const DashboardCard = ({ title, icon: Icon, children, className = '' }) => {
  return (
    <section className={`dashboard-card ${className}`}>
      {title && (
        <div className="dashboard-card-header">
          {Icon && <Icon size={18} className="dashboard-card-icon" />}
          <h3 className="dashboard-card-title">{title}</h3>
        </div>
      )}
      <div className="dashboard-card-body">
        {children}
      </div>
    </section>
  );
};

export default DashboardCard;
