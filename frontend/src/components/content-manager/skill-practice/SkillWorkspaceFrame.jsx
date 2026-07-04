export default function SkillWorkspaceFrame({ children, description, highlights, title }) {
  return (
    <div className="space-y-4">
      <section className="rounded-xl border border-[#dcc0bf]/30 bg-[#eff4ff]/50 p-4">
        <h3 className="font-['Manrope'] text-lg font-extrabold text-[#0b1c30]">{title}</h3>
        <p className="mt-2 text-sm leading-6 text-[#564241]">{description}</p>
        <div className="mt-4 grid gap-2 sm:grid-cols-2 xl:grid-cols-3">
          {highlights.map((item) => (
            <span
              className="rounded-lg border border-[#dcc0bf]/35 bg-white px-3 py-2 text-xs font-bold uppercase tracking-[0.1em] text-[#4b0009]"
              key={item}
            >
              {item}
            </span>
          ))}
        </div>
      </section>
      {children}
    </div>
  );
}
