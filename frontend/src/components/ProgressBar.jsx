import '../styles/home.css';

const ProgressBar = ({ value = 0, height = 8, showLabel = false }) => {
  const clampedValue = Math.min(100, Math.max(0, value));

  return (
    <div className="progress-bar-wrapper">
      <div
        className="progress-bar-track"
        style={{ height: `${height}px` }}
      >
        <div
          className="progress-bar-fill"
          style={{ width: `${clampedValue}%` }}
        />
      </div>
      {showLabel && (
        <span className="progress-bar-label">{clampedValue}%</span>
      )}
    </div>
  );
};

export default ProgressBar;
