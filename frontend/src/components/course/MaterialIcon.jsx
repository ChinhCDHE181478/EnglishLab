const MaterialIcon = ({ name, className = '', ...props }) => (
  <span className={`material-symbols-outlined ${className}`} aria-hidden="true" {...props}>
    {name}
  </span>
);

export default MaterialIcon;
