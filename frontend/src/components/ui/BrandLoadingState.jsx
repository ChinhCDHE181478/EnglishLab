const BrandLoadingState = ({
  message = 'Đang tải dữ liệu...',
  className = '',
  compact = false,
}) => {
  return (
    <div className={`w-full space-y-6 ${className}`}>
      {/* Sleek loading state banner */}
      <div className="flex items-center gap-3 rounded-2xl border border-[#dfbfbd]/35 bg-[#fff5f5]/30 p-4 animate-pulse">
        <div className="relative flex h-5 w-5 shrink-0 items-center justify-center">
          <div className="absolute h-4 w-4 animate-spin rounded-full border-2 border-[#dfbfbd]/45 border-t-[#730014]" />
        </div>
        <span className="text-[10px] font-extrabold text-[#730014] uppercase tracking-widest">{message}</span>
      </div>

      {/* Shimmer skeleton cards block layout */}
      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3 w-full animate-pulse">
        {Array.from({ length: compact ? 2 : 3 }).map((_, idx) => (
          <div
            key={idx}
            className="flex flex-col rounded-[24px] border border-gray-200/80 bg-white p-6 shadow-[0_10px_35px_rgba(0,0,0,0.01)] min-h-[190px] space-y-4"
          >
            <div className="h-3 w-12 rounded-lg bg-gray-200/80" />
            <div className="h-5 w-3/4 rounded-lg bg-gray-200/80" />
            <div className="h-3 w-1/2 rounded-lg bg-gray-150/80" />
            <div className="space-y-2 pt-4 mt-auto border-t border-gray-50">
              <div className="h-2 w-full rounded-md bg-gray-100/70" />
              <div className="h-2 w-4/5 rounded-md bg-gray-100/70" />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default BrandLoadingState;
