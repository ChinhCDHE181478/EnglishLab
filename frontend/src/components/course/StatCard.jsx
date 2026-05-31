import MaterialIcon from './MaterialIcon';

const StatCard = ({ icon, value, label }) => (
  <div className="group flex w-40 cursor-default flex-col items-center rounded-2xl border border-white/20 bg-white/10 p-6 text-center backdrop-blur-xl transition-all hover:bg-white/20">
    <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-[#730014]/50 transition-transform group-hover:scale-110">
      <MaterialIcon name={icon} className="text-white" />
    </div>
    <p className="mb-1 max-w-full truncate text-2xl font-bold text-white">{value}</p>
    <p className="text-[10px] font-semibold uppercase tracking-widest text-white/60">{label}</p>
  </div>
);

export default StatCard;
