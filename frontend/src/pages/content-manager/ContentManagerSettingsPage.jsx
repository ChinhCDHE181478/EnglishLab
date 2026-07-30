import { Link } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { Package, RefreshCw } from 'lucide-react';
import packageApi from '../../api/packageApi';
import { ContentManagerLoadingState, Panel, StatusBadge } from '../../components/content-manager/ContentManagerUi';

export default function ContentManagerSettingsPage() {
  const [types, setTypes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadTypes = async () => {
    setLoading(true);
    setError('');
    try {
      setTypes(await packageApi.getPackageTypes());
    } catch (err) {
      setError(err?.response?.data?.message || 'Không tải được cấu hình loại gói.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadTypes();
  }, []);

  if (loading && !types.length) {
    return <ContentManagerLoadingState message="Đang tải cài đặt nội dung..." />;
  }

  return (
    <div className="space-y-6">
      <Panel className="p-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h2 className="font-['Manrope'] text-xl font-extrabold text-[#4b0009]">Cài đặt gói học</h2>
            <p className="mt-2 max-w-2xl text-sm leading-6 text-[#8b706e]">
              Loại gói được cấu hình sẵn. Sản phẩm thương mại được tạo và liên kết tại trang Quản lý gói.
            </p>
          </div>
          <div className="flex gap-3">
            <button
              className="inline-flex items-center gap-2 rounded-2xl border border-[#dfbfbd]/70 bg-white px-4 py-3 text-sm font-bold text-[#730014]"
              onClick={loadTypes}
              type="button"
            >
              <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
              Làm mới
            </button>
            <Link
              className="inline-flex items-center gap-2 rounded-2xl bg-[#4b0009] px-4 py-3 text-sm font-bold text-white"
              to="/content-manager/packages"
            >
              <Package className="h-4 w-4" />
              Quản lý gói / Bundle
            </Link>
          </div>
        </div>
      </Panel>

      {error ? (
        <div className="rounded-2xl border border-[#f3b4b0] bg-[#fff5f5] px-4 py-3 text-sm font-semibold text-[#8a1c1c]">
          {error}
        </div>
      ) : null}

      <Panel className="overflow-hidden p-0">
        <table className="min-w-full text-left text-sm">
          <thead className="bg-[#fff2f3] text-xs uppercase tracking-wide text-[#730014]">
            <tr>
              <th className="px-4 py-3">Mã</th>
              <th className="px-4 py-3">Tên</th>
              <th className="px-4 py-3">Mô tả</th>
              <th className="px-4 py-3">Trạng thái</th>
            </tr>
          </thead>
          <tbody>
            {types.map((type) => (
              <tr key={type.id || type.code} className="border-t border-[#f0e2e1]">
                <td className="px-4 py-3 font-bold text-[#4b0009]">{type.code}</td>
                <td className="px-4 py-3 font-semibold text-[#1a1c1c]">{type.name}</td>
                <td className="px-4 py-3 text-[#8b706e]">{type.description || '—'}</td>
                <td className="px-4 py-3">
                  <StatusBadge status={type.active === false ? 'ARCHIVED' : 'PUBLISHED'} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Panel>
    </div>
  );
}
