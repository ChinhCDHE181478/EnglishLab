import { useEffect, useMemo, useState } from 'react';
import courseApi from '../../api/courseApi';
import { FilterChip, Panel, SectionTitle, StatusBadge } from '../../components/content-manager/ContentManagerUi';
import { staticPageContent } from '../../components/content-manager/contentManagerConfig';

export default function ContentManagerStaticPage({ pageKey }) {
  const content = staticPageContent[pageKey];
  const [courses, setCourses] = useState([]);
  const [stats, setStats] = useState(null);

  useEffect(() => {
    let active = true;
    if (!['materials', 'categories', 'publication', 'analytics', 'syllabus'].includes(pageKey)) return undefined;

    Promise.all([
      courseApi.getManagedOnlineCourses({ page: 0, size: 200 }),
      courseApi.getManagedCourseStats().catch(() => null),
    ]).then(([page, statsData]) => {
      if (!active) return;
      setCourses(page.content || []);
      setStats(statsData);
    }).catch(() => {});

    return () => {
      active = false;
    };
  }, [pageKey]);

  const realContent = useMemo(() => buildRealContent(pageKey, content, courses, stats), [content, courses, pageKey, stats]);

  if (!realContent) {
    return (
      <Panel className="p-6 text-sm text-[#584140]">
        This page has not been configured yet.
      </Panel>
    );
  }

  return (
    <div className="grid gap-6 xl:grid-cols-[1.15fr_0.7fr]">
      <div className="space-y-6">
        <Panel className="p-5">
          <div className="flex flex-wrap items-center gap-3">
            {realContent.filters.map((item) => <FilterChip key={item} label={item} />)}
          </div>
        </Panel>

        <Panel className="overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-full text-left">
              <thead className="bg-[#fbf3f4] text-xs uppercase tracking-[0.18em] text-[#8e7371]">
                <tr>
                  {realContent.columns.map((heading) => (
                    <th key={heading} className="px-5 py-4 font-semibold">{heading}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-[#f0e3e4]">
                {realContent.rows.map((row, rowIndex) => (
                  <tr key={rowIndex}>
                    {row.map((cell, cellIndex) => (
                      <td key={cellIndex} className="px-5 py-4 text-sm text-[#1a1c1c]">
                        {/draft|published|archived|ready|processed|encoding|pending review/i.test(cell) ? <StatusBadge label={cell} /> : cell}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Panel>
      </div>

      <Panel className="p-6">
        <SectionTitle title={realContent.sideTitle} />
        <div className="mt-5 space-y-4">
          {realContent.sideBlocks.map((block) => (
            <div key={block} className="rounded-2xl border border-[#f0e3e4] bg-[#fcfbfb] p-4 text-sm text-[#584140]">
              {block}
            </div>
          ))}
        </div>
      </Panel>
    </div>
  );
}

function buildRealContent(pageKey, fallback, courses, stats) {
  if (!fallback) return null;
  if (!courses.length && !stats) return fallback;

  if (pageKey === 'categories') {
    const counts = courses.reduce((acc, course) => {
      const code = course.category || 'ONLINE';
      acc[code] = (acc[code] || 0) + 1;
      return acc;
    }, {});
    return {
      ...fallback,
      rows: Object.entries(counts).map(([code, count]) => [code, labelCategory(code), String(count)]),
      sideBlocks: [
        `${courses.length} courses are currently grouped by backend category values.`,
        'Category counts update from the real course inventory.',
      ],
    };
  }

  if (pageKey === 'materials') {
    const rows = courses.flatMap((course) =>
      (course.modules || []).flatMap((module) =>
        (module.lessons || []).flatMap((lesson) => {
          const items = [];
          if (lesson.videoUrl) items.push([lesson.title, 'Video', `${course.title} / ${module.title}`, '-', providerName(lesson.videoUrl), 'Ready', formatDate(course.updatedAt)]);
          if (lesson.materialUrl) items.push([lesson.title, 'Material', `${course.title} / ${module.title}`, '-', providerName(lesson.materialUrl), 'Ready', formatDate(course.updatedAt)]);
          return items;
        })
      )
    );
    return {
      ...fallback,
      rows: rows.length ? rows : [['No linked media yet', '-', '-', '-', '-', 'Draft', '-']],
      sideBlocks: [
        'Rows are generated from saved lesson video/material URLs.',
        'Upload video through the Bunny lesson endpoint, then it appears here after refresh.',
      ],
    };
  }

  if (pageKey === 'publication') {
    return {
      ...fallback,
      rows: courses.map((course) => [course.title, 'Course', course.status, course.category || '-', formatDate(course.updatedAt), 'Edit / Builder']),
      sideBlocks: [
        `${stats?.publishedCourses ?? 0} published courses are visible to learners.`,
        `${stats?.draftCourses ?? 0} draft courses need review before publication.`,
      ],
    };
  }

  if (pageKey === 'analytics') {
    return {
      ...fallback,
      rows: [
        ['Total courses', stats?.totalCourses ?? courses.length, '-', 'Backend inventory count'],
        ['Published courses', stats?.publishedCourses ?? 0, '-', 'Visible catalog count'],
        ['Total lessons', stats?.totalLessons ?? 0, '-', 'Saved lesson count'],
        ['Total enrollments', stats?.totalEnrollments ?? 0, '-', 'Student registrations'],
      ],
      sideBlocks: ['Metrics are now read from the content-manager stats endpoint.'],
    };
  }

  if (pageKey === 'syllabus') {
    return {
      ...fallback,
      rows: courses.map((course) => [course.title, course.level || '-', course.targetScore || '-', course.duration || '-', `${course.modules?.length || 0} modules`, course.status]),
      sideBlocks: ['Syllabus rows are derived from real course metadata and module counts.'],
    };
  }

  return fallback;
}

function labelCategory(code) {
  const labels = { IELTS: 'IELTS', TOEIC: 'TOEIC', COMMUNICATION: 'Communication', FOUNDATION: 'Foundation', ONLINE: 'Online' };
  return labels[code] || code;
}

function providerName(url) {
  if (/mediadelivery|b-cdn/i.test(url)) return 'Bunny Stream';
  if (/youtube|youtu\.be/i.test(url)) return 'YouTube';
  return 'External';
}

function formatDate(value) {
  if (!value) return '-';
  return new Date(value).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
}
