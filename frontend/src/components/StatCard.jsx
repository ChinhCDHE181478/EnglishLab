import '../styles/home.css';

const StatCard = ({ icon: Icon, label, value, accent }) => {
  return (
    <div className="stat-card">
      <div className={`stat-card-icon ${accent || ''}`}>
        {Icon && <Icon size={20} />}
      </div>
      <div className="stat-card-info">
        <span className="stat-card-label">{label}</span>
        <span className="stat-card-value">{value}</span>
      </div>
    </div>
  );
};

export default StatCard;
