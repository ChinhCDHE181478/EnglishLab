import { Link } from 'react-router-dom';

const BackendFeatureNotice = ({
  title,
  description,
  primaryLabel = 'Xem khóa học',
  primaryTo = '/courses',
}) => (
  <section className="rounded-[32px] border border-dashed border-[#dfbfbd] bg-white px-6 py-16 text-center shadow-[0_18px_45px_rgba(75,0,9,0.04)]">
    <h2 className="font-['Manrope'] text-4xl font-extrabold text-[#2b2828]">{title}</h2>
    <p className="mx-auto mt-4 max-w-2xl text-sm leading-8 text-[#584140]">{description}</p>
    <div className="mt-6 flex justify-center">
      <Link
        className="rounded-2xl bg-[#4b0009] px-6 py-4 text-sm font-extrabold text-white transition hover:-translate-y-0.5 hover:bg-[#730014]"
        to={primaryTo}
      >
        {primaryLabel}
      </Link>
    </div>
  </section>
);

export default BackendFeatureNotice;
