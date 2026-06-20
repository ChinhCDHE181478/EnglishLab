import {
  BarChart3,
  BookOpen,
  Brain,
  FileAudio,
  FileCheck2,
  FileQuestion,
  FileSpreadsheet,
  FileStack,
  FolderKanban,
  BadgePercent,
  Headphones,
  LayoutDashboard,
  NotebookPen,
  Settings,
} from 'lucide-react';

export const contentManagerNav = [
  {
    title: 'Workspace',
    items: [
      { label: 'Dashboard', href: '/content-manager/dashboard', icon: LayoutDashboard },
      { label: 'Online Courses', href: '/content-manager/courses', icon: BookOpen },
      { label: 'Discount Codes', href: '/content-manager/discount-codes', icon: BadgePercent },
      { label: 'Course Categories', href: '/content-manager/categories', icon: FolderKanban },
      { label: 'Syllabus', href: '/content-manager/syllabus', icon: FileSpreadsheet },
      { label: 'Learning Materials', href: '/content-manager/materials', icon: FileStack },
    ],
  },
  {
    title: 'Practice & Exams',
    items: [
      { label: 'Flashcards', href: '/content-manager/flashcards', icon: Brain },
      { label: 'Listening Practice', href: '/content-manager/listening', icon: Headphones },
      { label: 'Writing Practice', href: '/content-manager/writing', icon: NotebookPen },
      { label: 'Mock Exam Bank', href: '/content-manager/mock-exams', icon: FileQuestion },
    ],
  },
  {
    title: 'Admin',
    items: [
      { label: 'Publication Queue', href: '/content-manager/publication', icon: FileCheck2 },
      { label: 'Content Analytics', href: '/content-manager/analytics', icon: BarChart3 },
      { label: 'Settings', href: '/content-manager/settings', icon: Settings },
    ],
  },
];

export const contentManagerPageMeta = {
  '/content-manager/dashboard': {
    title: 'Content Management Workspace',
    subtitle: 'Create, organize, and publish English learning content for IELTS/TOEIC learners.',
    searchPlaceholder: 'Search courses, lessons, or materials...',
  },
  '/content-manager/courses': {
    title: 'Online Course Management',
    subtitle: 'Search, filter, publish, archive, and maintain all online course inventory.',
    searchPlaceholder: 'Search by title or slug...',
  },
  '/content-manager/courses/new': {
    title: 'Create Online Course',
    subtitle: 'Define metadata, learning structure, and publication-ready settings.',
    searchPlaceholder: 'Search field or setting...',
  },
  '/content-manager/courses/:slugOrId/edit': {
    title: 'Edit Online Course',
    subtitle: 'Update metadata, outcomes, modules, and publishing readiness.',
    searchPlaceholder: 'Search field or setting...',
  },
  '/content-manager/courses/:slugOrId/builder': {
    title: 'Course Builder',
    subtitle: 'Organize modules, lessons, and linked assets in the authoring workspace.',
    searchPlaceholder: 'Search module, lesson, or asset...',
  },
  '/content-manager/discount-codes': {
    title: 'Discount Code Management',
    subtitle: 'Create limited-use coupon codes and track reserved and successful usage.',
    searchPlaceholder: 'Search discount code...',
  },
  '/content-manager/materials': {
    title: 'Learning Material Management',
    subtitle: 'Manage uploaded videos, PDFs, audio files, and reusable lesson resources.',
    searchPlaceholder: 'Search material, provider, or lesson...',
  },
  '/content-manager/flashcards': {
    title: 'Flashcard Content Management',
    subtitle: 'Build vocabulary sets with examples, IPA, and course-linked practice.',
    searchPlaceholder: 'Search flashcard set or topic...',
  },
  '/content-manager/listening': {
    title: 'Listening Practice Management',
    subtitle: 'Maintain listening exercises, transcripts, and answer explanations.',
    searchPlaceholder: 'Search listening exercise...',
  },
  '/content-manager/writing': {
    title: 'Writing Practice Management',
    subtitle: 'Manage writing prompts, sample answers, rubrics, and learner previews.',
    searchPlaceholder: 'Search prompt title or task type...',
  },
  '/content-manager/syllabus': {
    title: 'Syllabus Management',
    subtitle: 'Design weekly learning plans, outcomes, materials, and checkpoints.',
    searchPlaceholder: 'Search syllabus title or outcome...',
  },
  '/content-manager/mock-exams': {
    title: 'Mock Exam Question Bank',
    subtitle: 'Create and maintain IELTS/TOEIC mock exam questions with explanations.',
    searchPlaceholder: 'Search question or topic...',
  },
  '/content-manager/publication': {
    title: 'Publication / Approval Control',
    subtitle: 'Review content visibility across draft, published, and archived states.',
    searchPlaceholder: 'Search content type or owner...',
  },
  '/content-manager/analytics': {
    title: 'Content Analytics',
    subtitle: 'Track publishing throughput, lesson density, and content health signals.',
    searchPlaceholder: 'Search metrics...',
  },
  '/content-manager/settings': {
    title: 'Content Manager Settings',
    subtitle: 'Configure publishing defaults, naming rules, and workspace preferences.',
    searchPlaceholder: 'Search setting...',
  },
  '/content-manager/categories': {
    title: 'Course Categories',
    subtitle: 'Review category taxonomy and how courses are grouped across the platform.',
    searchPlaceholder: 'Search category...',
  },
};

export const staticPageContent = {
  materials: {
    filters: ['Type: All', 'Provider: All', 'Upload status: All'],
    columns: ['File name', 'Type', 'Linked lesson', 'File size', 'Provider', 'Upload status', 'Created date'],
    rows: [
      ['task-2-cohesion-breakdown.mp4', 'Video', 'IELTS Masterclass / Lesson 12', '284 MB', 'Bunny Stream', 'Processed', 'Jun 1, 2026'],
      ['toeic-part-3-transcript.pdf', 'PDF', 'TOEIC Business / Lesson 4', '2.4 MB', 'Cloudflare R2', 'Ready', 'May 31, 2026'],
      ['academic-wordlist-audio.mp3', 'Audio', 'AWL Pack / Flashcards', '18 MB', 'api.video', 'Encoding', 'May 30, 2026'],
    ],
    sideTitle: 'Upload Workflow',
    sideBlocks: [
      'Upload modal supports video, PDF, audio, image, document, and external link.',
      'Replace-file flow should preserve existing lesson links when possible.',
      'Processing statuses should remain visible until media is ready.',
    ],
  },
  flashcards: {
    filters: ['Category: All', 'Level: All', 'Status: All'],
    columns: ['Set title', 'Topic', 'Level', 'Cards', 'Linked lesson', 'Status', 'Last updated'],
    rows: [
      ['IELTS Environment Lexicon', 'Environment', 'Advanced', '48', 'Writing Task 2 / Module 4', 'Published', 'Jun 1, 2026'],
      ['TOEIC Office Phrases', 'Business', 'Intermediate', '36', 'TOEIC Business / Module 2', 'Draft', 'May 30, 2026'],
    ],
    sideTitle: 'Editor Scope',
    sideBlocks: [
      'Card fields include term, IPA, meaning in Vietnamese, example, translation, collocations, and note.',
      'Bulk CSV import and student preview belong on this page.',
    ],
  },
  listening: {
    filters: ['Difficulty: All', 'Skill focus: All', 'Status: All'],
    columns: ['Title', 'Skill focus', 'Difficulty', 'Questions', 'Linked lesson', 'Status'],
    rows: [
      ['Section 2 Campus Tour', 'Listening for detail', 'Intermediate', '10', 'IELTS Listening / Lesson 7', 'Published'],
      ['TOEIC Part 4 Briefing', 'Inference', 'Advanced', '6', 'TOEIC Accelerator / Lesson 11', 'Pending review'],
    ],
    sideTitle: 'Editor Scope',
    sideBlocks: [
      'Transcript editor, answer key, explanation area, and media picker should live in the right workspace rail.',
    ],
  },
  writing: {
    filters: ['Task type: All', 'Status: All'],
    columns: ['Title', 'Task type', 'Time limit', 'Linked lesson', 'Status'],
    rows: [
      ['Urban Traffic Solutions', 'IELTS Task 2', '40 min', 'Writing Sprint / Module 5', 'Published'],
      ['Line Graph Energy Usage', 'IELTS Task 1', '20 min', 'Visual Logic / Module 2', 'Draft'],
    ],
    sideTitle: 'Editor Scope',
    sideBlocks: [
      'Prompt text, suggested outline, rubric, sample answer, and useful vocabulary should be editable in one flow.',
    ],
  },
  syllabus: {
    filters: ['Target level: All', 'Outcome: All'],
    columns: ['Syllabus title', 'Target level', 'Target outcome', 'Duration', 'Linked courses', 'Assessment plan'],
    rows: [
      ['IELTS Intensive 12 Weeks', 'Intermediate', 'Band 6.5+', '12 weeks', '4 courses', 'Weekly mocks'],
      ['TOEIC Score Boost 8 Weeks', 'Intermediate', '850+', '8 weeks', '2 courses', 'Bi-weekly review'],
    ],
    sideTitle: 'Weekly Planning',
    sideBlocks: [
      'Week-by-week structure, learning outcomes, materials, and checkpoints need a detail drawer or secondary panel.',
    ],
  },
  mockExams: {
    filters: ['Exam type: All', 'Skill: All', 'Difficulty: All', 'Status: All'],
    columns: ['Question title', 'Exam type', 'Skill', 'Difficulty', 'Question type', 'Status', 'Last updated'],
    rows: [
      ['Reading Passage Matching Set 14', 'IELTS', 'Reading', 'Advanced', 'Matching headings', 'Published', 'Jun 1, 2026'],
      ['Part 3 Conversation Inference', 'TOEIC', 'Listening', 'Intermediate', 'Multiple choice', 'Draft', 'May 31, 2026'],
    ],
    sideTitle: 'Question Editor',
    sideBlocks: [
      'Question stem, passage/audio, options, correct answer, explanation, difficulty, and linked mock test should all be editable here.',
      'Bulk import is a first-class action on this screen.',
    ],
  },
  publication: {
    filters: ['Content type: All', 'Status: All', 'Owner: All'],
    columns: ['Title', 'Content type', 'Status', 'Owner', 'Last updated', 'Actions'],
    rows: [
      ['IELTS Masterclass 7.5+', 'Course', 'Pending review', 'Alex Thorne', '10 min ago', 'Preview / Publish'],
      ['Band Descriptor Explainer', 'Lesson', 'Draft', 'Linh Dao', '45 min ago', 'Preview / Request revision'],
      ['Office Phrases Pack', 'Flashcard', 'Published', 'Huy Pham', '2h ago', 'Unpublish / Archive'],
    ],
    sideTitle: 'Approval Flow',
    sideBlocks: [
      'Preview, publish, unpublish, archive, and request revision should all require visible confirmation states.',
    ],
  },
  analytics: {
    filters: ['Range: 30 days', 'Type: All'],
    columns: ['Metric', 'Current value', 'Trend', 'Note'],
    rows: [
      ['New course drafts', '5', '+2', 'Mostly IELTS premium tracks'],
      ['Lessons published', '26', '+8', 'Builder throughput improved this week'],
      ['Assets missing thumbnail', '3', '-1', 'Still blocking publication'],
    ],
    sideTitle: 'Future Integration',
    sideBlocks: [
      'This page is ready to connect to backend metrics once analytics endpoints are introduced.',
    ],
  },
  settings: {
    filters: ['Workspace preferences'],
    columns: ['Setting', 'Current value', 'Purpose'],
    rows: [
      ['Default visibility', 'Enrollment only', 'Applied when new courses are created'],
      ['Thumbnail requirement', 'Enabled', 'Blocks publication when missing'],
      ['Draft autosave', 'Every 60 seconds', 'Reduces accidental loss'],
    ],
    sideTitle: 'Settings Scope',
    sideBlocks: [
      'Keep settings scoped to content operations, not the global platform admin.',
    ],
  },
  categories: {
    filters: ['Active categories'],
    columns: ['Code', 'Label', 'Used by courses'],
    rows: [
      ['IELTS', 'IELTS', '12'],
      ['TOEIC', 'TOEIC', '6'],
      ['COMMUNICATION', 'Communication', '3'],
      ['FOUNDATION', 'Foundation', '3'],
    ],
    sideTitle: 'Taxonomy Notes',
    sideBlocks: [
      'Categories are backend-driven enums today; this page documents usage and can become editable later if the model changes.',
    ],
  },
};
