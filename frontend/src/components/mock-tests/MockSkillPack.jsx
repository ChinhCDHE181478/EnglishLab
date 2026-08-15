import { BookOpen, Headphones, Layers, Mic, PenLine, Play, RotateCcw } from 'lucide-react';
import { SKILL_ORDER, packProgress } from '../../utils/mockTestLibrary';

const SKILL_UI = {
  LISTENING: {
    label: 'Listening',
    icon: Headphones,
    tile: 'border-[#d7ecec] bg-[#f4fbfb]',
    iconWrap: 'bg-[#e6f6f6] text-[#0f6b6e]',
  },
  READING: {
    label: 'Reading',
    icon: BookOpen,
    tile: 'border-[#dce8dc] bg-[#f6faf6]',
    iconWrap: 'bg-[#e8f3e8] text-[#2e6b32]',
  },
  WRITING: {
    label: 'Writing',
    icon: PenLine,
    tile: 'border-[#eadcdc] bg-[#fffaf9]',
    iconWrap: 'bg-[#f7ecec] text-[#730014]',
  },
  SPEAKING: {
    label: 'Speaking',
    icon: Mic,
    tile: 'border-[#e4dcec] bg-[#faf7fc]',
    iconWrap: 'bg-[#f1e9f7] text-[#6b3d86]',
  },
};

export default function MockSkillPack({ pack, completedScoresMap, onStart, heading }) {
  const progress = packProgress(pack, completedScoresMap);
  return (
    <section className="space-y-5 rounded-[24px] border border-[#eadcdc] bg-white p-5 sm:p-7">
      <h3 className="font-['Manrope'] text-lg font-extrabold text-[#0b1c30]">{heading || pack.title}</h3>
      <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
        {SKILL_ORDER.map((skill) => {
          const ui = SKILL_UI[skill];
          const Icon = ui.icon;
          const testItem = pack.skills[skill];
          const saved = testItem ? completedScoresMap[testItem.id] : null;
          return (
            <div
              className={`flex min-h-[168px] flex-col items-center justify-between rounded-2xl border p-4 text-center ${ui.tile}`}
              key={skill}
            >
              <div className={`flex h-10 w-10 items-center justify-center rounded-full ${ui.iconWrap}`}>
                <Icon className="h-4 w-4" />
              </div>
              <p className="font-['Manrope'] text-sm font-extrabold text-[#0b1c30]">{ui.label}</p>
              {saved ? (
                <span className="text-xs font-bold text-emerald-800">
                  {saved.score != null ? saved.score : saved.correct != null ? `${saved.correct}/${saved.total}` : 'Đã làm'}
                </span>
              ) : (
                <span className="text-xs text-[#8b706e]">Chưa làm</span>
              )}
              {testItem ? (
                <button
                  className={saved
                    ? 'inline-flex w-full items-center justify-center gap-1.5 rounded-xl border border-[#eadcdc] bg-white py-2 text-xs font-bold text-[#564241]'
                    : 'inline-flex w-full items-center justify-center rounded-xl bg-[#730014] py-2 text-xs font-extrabold text-white hover:bg-[#4b0009]'}
                  onClick={() => onStart(testItem)}
                  type="button"
                >
                  {saved ? <><RotateCcw className="h-3 w-3" /> Làm lại</> : 'Làm bài'}
                </button>
              ) : (
                <span className="w-full rounded-xl bg-white/70 py-2 text-xs font-bold text-slate-400">Chưa có</span>
              )}
            </div>
          );
        })}
      </div>
      <div className="flex items-center justify-between gap-3 rounded-2xl border border-[#eadcdc] bg-[#fffaf9] px-4 py-3">
        <div className="flex min-w-0 flex-1 items-center gap-3">
          <span className="flex h-7 w-7 items-center justify-center rounded-md bg-[#f7ecec] text-[#730014]">
            <Layers className="h-3.5 w-3.5" />
          </span>
          <span className="text-sm font-extrabold text-[#0b1c30]">Cả 4 kỹ năng</span>
          <div className="hidden h-1.5 w-28 overflow-hidden rounded-full bg-[#eadcdc] sm:block">
            <div className="h-full bg-[#730014]" style={{ width: `${progress.percent}%` }} />
          </div>
          <span className="hidden text-[11px] font-bold text-[#8b706e] sm:inline">{progress.percent}%</span>
        </div>
        <button
          className="inline-flex items-center gap-1.5 rounded-xl bg-[#730014] px-5 py-2 text-xs font-extrabold text-white hover:bg-[#4b0009] disabled:opacity-40"
          disabled={!progress.nextTest}
          onClick={() => onStart(progress.nextTest)}
          type="button"
        >
          <Play className="h-3.5 w-3.5 fill-current" />
          Bắt đầu
        </button>
      </div>
    </section>
  );
}
