import { useEffect, useMemo, useRef, useState } from 'react';
import courseApi from '../../api/courseApi';
import BrandedSelect from '../ui/BrandedSelect';
import ListeningExamMode from './ListeningExamMode';
import ReadingExamMode from './ReadingExamMode';
import WritingExamMode from './WritingExamMode';

const statusLabels = {
  PASSED: 'Hoàn thành',
  NEEDS_IMPROVEMENT: 'Cần cải thiện',
  AI_EVALUATED: 'Đã chấm xong',
  SUBMITTED: 'Đã nộp',
};

const skillLabel = (skill) => ({
  WRITING: 'Viết',
  SPEAKING: 'Nói',
  LISTENING: 'Nghe',
  READING: 'Đọc',
  VOCABULARY: 'Từ vựng',
  GRAMMAR: 'Ngữ pháp',
  MIXED: 'Tổng hợp',
}[skill] || skill || 'Bài kiểm tra');

const riskLabel = (value) => ({
  LOW: 'Thấp',
  MEDIUM: 'Trung bình',
  HIGH: 'Cao',
}[String(value || '').toUpperCase()] || value || 'Chưa có');

const normalizeComparisonText = (value) => String(value || '')
  .toLowerCase()
  .replace(/\s+/g, ' ')
  .trim();

const tryParseFeedback = (feedback) => {
  if (!feedback) return null;
  try {
    return typeof feedback === 'string' ? JSON.parse(feedback) : feedback;
  } catch {
    return null;
  }
};

const toArray = (value) => (Array.isArray(value) ? value.filter(Boolean) : []);
const toObject = (value) => (value && typeof value === 'object' && !Array.isArray(value) ? value : null);
const fallbackText = (value) => value || 'Chưa có';
const buildSubmissionComparison = (currentSubmission, previousSubmission) => {
  if (!currentSubmission || !previousSubmission) return null;

  const currentFeedback = tryParseFeedback(currentSubmission.aiFeedbackJson);
  const previousFeedback = tryParseFeedback(previousSubmission.aiFeedbackJson);
  const currentCriteria = toArray(currentFeedback?.criteria);
  const previousCriteriaMap = new Map(
    toArray(previousFeedback?.criteria).map((criterion) => [normalizeComparisonText(criterion?.name), criterion])
  );
  const improvedCriteria = currentCriteria
    .map((criterion) => {
      const previousCriterion = previousCriteriaMap.get(normalizeComparisonText(criterion?.name));
      const currentScore = Number(criterion?.score);
      const previousScore = Number(previousCriterion?.score);
      if (!Number.isFinite(currentScore) || !Number.isFinite(previousScore) || currentScore <= previousScore) return null;
      return {
        name: criterion?.name,
        delta: currentScore - previousScore,
        currentScore,
        previousScore,
      };
    })
    .filter(Boolean)
    .sort((left, right) => right.delta - left.delta);
  const regressedCriteria = currentCriteria
    .map((criterion) => {
      const previousCriterion = previousCriteriaMap.get(normalizeComparisonText(criterion?.name));
      const currentScore = Number(criterion?.score);
      const previousScore = Number(previousCriterion?.score);
      if (!Number.isFinite(currentScore) || !Number.isFinite(previousScore) || currentScore >= previousScore) return null;
      return {
        name: criterion?.name,
        delta: previousScore - currentScore,
        currentScore,
        previousScore,
      };
    })
    .filter(Boolean)
    .sort((left, right) => right.delta - left.delta);

  const currentStrengths = toArray(currentFeedback?.strengths);
  const previousStrengths = toArray(previousFeedback?.strengths);
  const previousStrengthSet = new Set(previousStrengths.map(normalizeComparisonText));
  const currentStrengthSet = new Set(currentStrengths.map(normalizeComparisonText));
  const newStrengths = currentStrengths.filter((item) => !previousStrengthSet.has(normalizeComparisonText(item)));
  const lostStrengths = previousStrengths.filter((item) => !currentStrengthSet.has(normalizeComparisonText(item)));

  const currentScore = Number(currentSubmission?.aiScore ?? currentFeedback?.estimatedScore);
  const previousScore = Number(previousSubmission?.aiScore ?? previousFeedback?.estimatedScore);

  return {
    scoreDelta: Number.isFinite(currentScore) && Number.isFinite(previousScore)
      ? Number((currentScore - previousScore).toFixed(2))
      : null,
    improvedCriteria,
    regressedCriteria,
    newStrengths,
    lostStrengths,
  };
};

const formatSpeakingPartLabel = (partKey) => ({
  part_1: 'Part 1',
  part_2: 'Part 2',
  part_3: 'Part 3',
}[partKey] || partKey || 'Part');
const criterionTranslations = {
  'Meaning Accuracy': 'Độ chính xác về nghĩa',
  'Collocation': 'Cụm từ đi kèm tự nhiên',
  'Sentence Quality': 'Chất lượng câu',
  'Topic Relevance': 'Mức độ bám chủ đề',
  'Task Achievement': 'Mức độ đáp ứng yêu cầu đề bài',
  'Coherence and Cohesion': 'Độ mạch lạc và liên kết',
  'Lexical Resource': 'Vốn từ vựng',
  'Grammatical Range and Accuracy': 'Ngữ pháp và độ chính xác',
  'Pronunciation': 'Phát âm',
  'Fluency': 'Độ trôi chảy',
};
const criterionDescriptionTranslations = {
  'Uses the target word with the correct meaning in context.': 'Dùng từ vựng mục tiêu đúng nghĩa trong ngữ cảnh.',
  'Combines target vocabulary with natural collocations.': 'Kết hợp từ vựng mục tiêu với cụm từ đi kèm tự nhiên.',
  'Produces grammatically clear and meaningful sentences.': 'Viết câu rõ nghĩa, tự nhiên và đúng ngữ pháp.',
  'Connects vocabulary to the IELTS topic of the module.': 'Bám đúng chủ đề của module khi sử dụng từ vựng.',
};
const topicTranslations = {
  'Family and Relationships': 'Gia đình và các mối quan hệ',
  'Climate Change': 'Biến đổi khí hậu',
  'Health and Well-being': 'Sức khỏe và đời sống',
  'Life and Personal Development': 'Cuộc sống và phát triển bản thân',
  'Education': 'Giáo dục',
  'Language and Communication': 'Ngôn ngữ và giao tiếp',
  'Body and Mind': 'Thể chất và tinh thần',
};

const translateTopic = (value) => {
  const text = String(value || '').trim();
  if (!text) return '';

  let translated = text;
  Object.entries(topicTranslations).forEach(([english, vietnamese]) => {
    translated = translated.replaceAll(english, vietnamese);
  });
  return translated;
};

const formatModuleTitle = (value) => {
  const text = String(value || '').trim();
  if (!text) return 'module hiện tại';
  return text
    .replace(/^Module Orientation:\s*/i, '')
    .replace(/^Vocabulary Deep Dive:\s*/i, '')
    .replace(/^Practice Assignment:\s*/i, '')
    .replace(/^Video Practice:\s*/i, '')
    .trim();
};

const formatRubricName = (name, skill) => {
  const normalizedName = String(name || '').trim();
  if (!normalizedName) {
    return skill === 'VOCABULARY' ? 'Bộ tiêu chí chấm từ vựng của module' : 'Bộ tiêu chí chấm của bài kiểm tra';
  }

  if (/IELTS Vocabulary Usage AI Rubric/i.test(normalizedName)) {
    return 'Bộ tiêu chí chấm đầu ra từ vựng của module';
  }

  return 'Bộ tiêu chí chấm của bài kiểm tra';
};

const formatAssessmentTitle = (assessment) => {
  const rawTitle = String(assessment?.title || '').trim();
  if (!rawTitle) return 'Final Module Assessment';
  if (/Vocabulary Output Check/i.test(rawTitle)) {
    return 'Vocabulary Output Check';
  }
  return rawTitle
    .replace(/\bAI\b/gi, '')
    .replace(/\s{2,}/g, ' ')
    .trim();
};

const formatAssessmentDescription = (assessment) => {
  const text = String(assessment?.description || assessment?.instructions || '').trim();
  if (assessment?.skill === 'VOCABULARY') {
    return 'Write 5-7 sentences using the target vocabulary from this module. Your response will be checked for meaning accuracy, natural collocations, sentence quality, and topic relevance.';
  }
  if (!text) {
    return 'Complete the task below and submit your response for feedback.';
  }
  return text
    .replace(/\bAI\b/gi, '')
    .replace(/\s{2,}/g, ' ')
    .trim();
};

const modeLabel = (mode) => ({
  EXPLAIN_ONLY: 'Phân tích học tập',
  RUBRIC_FEEDBACK: 'Chấm theo rubric',
  ESTIMATED_BAND: 'Ước lượng band',
  NONE: 'Không chấm AI',
}[mode] || 'Đánh giá AI');

const supportsNumericScoring = (assessment) => (
  assessment?.aiEvaluationMode === 'ESTIMATED_BAND' || assessment?.aiEvaluationMode === 'RUBRIC_FEEDBACK'
);

const isObjectiveSkill = (skill) => skill === 'LISTENING' || skill === 'READING';
const isExamSkill = (skill) => ['LISTENING', 'READING', 'WRITING', 'SPEAKING'].includes(String(skill || '').toUpperCase());
const waveformBars = [28, 44, 36, 58, 32, 52, 40, 62, 34, 48, 30, 54];
const SPEAKING_PROMPT_VIDEO_MAP = {
  jan_2025_test_1: {
    part_1: [
      'https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%201/Part%201-%20Q1-Where%20are%20you%20from.mp4',
      'https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%201/Part%201-%20Q2-Where%20do%20you%20live%20now.mp4',
      'https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%201/Part%201-%20Q3-How%20long%20have%20you%20lived%20there.mp4',
      'https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%201/Part%201-%20Q4-Who%20do%20you%20live%20with.mp4',
      'https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%201/Part%201-%20Q5-Do%20you%20plan%20to%20live%20there%20for%20a%20long%20time.mp4',
      'https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%201/Part%201-%20Q6-Do%20you%20like%20watching%20films.mp4',
      'https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%201/Part%201-%20Q7-What%20kinds%20of%20movies%20do%20you%20like%20best.mp4',
      'https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%201/Part%201-%20Q8-How%20often%20do%20you%20watch%20films.mp4',
      'http://link.intergreat.com/7o2T1',
      'https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%201/Part%201-%20Q10-Would%20you%20like%20to%20be%20in%20a%20movie.mp4',
      'https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%201/Part%201-%20Q11-How%20often%20do%20you%20drink%20water.mp4',
      'https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Test%202023/1/Test%201/Part%201%20-%20Q12%20-%20What%20kinds%20of%20water%20do%20you%20like%20to%20drink.mp4',
      'http://link.intergreat.com/5gEnA',
    ],
    part_2: ['http://link.intergreat.com/RSi4I'],
    part_3: [
      'https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%201/Part%203-%20Q1-Do%20you%20think%20people%20should%20only%20focus%20on%20work.mp4',
      'http://link.intergreat.com/M93tA',
      'http://link.intergreat.com/LwsAe',
      'http://link.intergreat.com/6SSIw',
    ],
  },
  jan_2025_test_2: {
    part_1: [
      'https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%202/Part%201-%20Q1-Are%20you%20a%20student%20or%20do%20you%20work%20now.mp4',
      'https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%202/Part%201-%20Q2-Why%20did%20you%20choose%20this%20coursejob.mp4',
      'https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%202/Part%201-%20Q3-Talk%20about%20your%20daily%20routine.mp4',
      'http://link.intergreat.com/D8I5B',
      'http://link.intergreat.com/iP0zN',
      'https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%202/Part%201-%20Q6-Who%20does%20most%20of%20the%20shopping%20in%20your%20household.mp4',
      'https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%202/Part%201-%20Q7-What%20type%20of%20shopping%20do%20you%20like%20%28Why%29.mp4',
      'http://link.intergreat.com/bYnoU',
      'http://link.intergreat.com/lOMN3',
      'https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%202/Part%201-%20Q10-Let%E2%80%99s%20talk%20about%20films..mp4',
      'https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%202/Part%201-%20Q11-What%20type%20of%20films%20do%20you%20like%20best%20%28Why%29.mp4',
      'http://link.intergreat.com/3FbDt',
    ],
    part_2: ['https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%202/Part%202-Describe%20an%20important%20event%20in%20your%20life..mp4'],
    part_3: [
      'https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%202/Part%203-%20Q1-What%20days%20are%20important%20in%20your%20country.mp4',
      'https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%202/Part%203-%20Q2-Why%20it%20is%20important%20to%20have%20national%20celebrations.mp4',
      'http://link.intergreat.com/Q2kcd',
      'http://link.intergreat.com/2W3Bd',
      'http://link.intergreat.com/PAQjN',
      'http://link.intergreat.com/lATrc',
    ],
  },
};
const SPEAKING_PART_VIDEO_FALLBACKS = {
  jan_2025_test_1: {
    part_1: 'https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%201/Part%201-%20Q1-Where%20are%20you%20from.mp4',
    part_2: 'https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%201/Part%201-%20Q1-Where%20are%20you%20from.mp4',
    part_3: 'https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%201/Part%203-%20Q1-Do%20you%20think%20people%20should%20only%20focus%20on%20work.mp4',
  },
  jan_2025_test_2: {
    part_1: 'https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%202/Part%201-%20Q1-Are%20you%20a%20student%20or%20do%20you%20work%20now.mp4',
    part_2: 'https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%202/Part%202-Describe%20an%20important%20event%20in%20your%20life..mp4',
    part_3: 'https://ieltsonlinetests.oss-ap-southeast-1.aliyuncs.com/IOT%20Videos/IELTS%20Mock%20Tests%202020/1/Test%202/Part%203-%20Q1-What%20days%20are%20important%20in%20your%20country.mp4',
  },
};

const SPEAKING_MOCK_VARIANTS = [
  {
    key: 'jan_2025_test_1',
    label: 'Mock Test 1',
    sourceLabel: 'Theo bộ đề January Practice Test 1',
    parts: [
      {
        key: 'part_1',
        label: 'Part 1',
        caption: 'Introduction and Interview',
        description: 'Trả lời ngắn, tự nhiên và đi thẳng vào ý. Mỗi câu nên có answer + explain + example.',
        prepSeconds: 0,
        answerSeconds: 300,
        prompts: [
          'Where are you from?',
          'Where do you live now?',
          'How long have you lived there?',
          'Who do you live with?',
          'Do you plan to live there for a long time?',
          'Do you like watching films?',
          'What kinds of movies do you like best?',
          'How often do you watch films?',
          'Do you like to watch movies alone or with your friends?',
          'Would you like to be in a movie?',
          'How often do you drink water?',
          'What kinds of water do you like to drink?',
          'Do you drink bottled water or water from water machines?',
        ],
      },
      {
        key: 'part_2',
        label: 'Part 2',
        caption: 'Cue Card',
        description: 'Bạn có 1 phút chuẩn bị và khoảng 2 phút nói liên tục.',
        prepSeconds: 60,
        answerSeconds: 120,
        cueCardTitle: 'Describe an activity you would do when you are alone in your free time.',
        cueCardBullets: [
          'What you do',
          'How often you do it',
          'Why you like to do this activity',
          'How you feel when you do it',
        ],
      },
      {
        key: 'part_3',
        label: 'Part 3',
        caption: 'Topic Discussion',
        description: 'Mở rộng, so sánh, nêu nguyên nhân - hệ quả và ý kiến cá nhân rõ ràng hơn Part 1.',
        prepSeconds: 0,
        answerSeconds: 300,
        prompts: [
          'Do you think people should only focus on work?',
          'Do you ever think how much time we will spend at work in a week?',
          'Should parents plan children’s leisure time and activities?',
          'Do you think the activities of the younger generation are different from those of the older generation?',
        ],
      },
    ],
  },
  {
    key: 'jan_2025_test_2',
    label: 'Mock Test 2',
    sourceLabel: 'Theo bộ đề January Practice Test 2',
    parts: [
      {
        key: 'part_1',
        label: 'Part 1',
        caption: 'Introduction and Interview',
        description: 'Giữ câu trả lời linh hoạt, tránh học thuộc từng câu.',
        prepSeconds: 0,
        answerSeconds: 300,
        prompts: [
          'Are you a student or do you work now?',
          'Why did you choose this course or job?',
          'Talk about your daily routine.',
          'Is there anything about your course or job you would like to change?',
          'I’d like to move on and ask you some questions about shopping.',
          'Who does most of the shopping in your household?',
          'What type of shopping do you like? Why?',
          'Is shopping a popular activity in your country? Why or why not?',
          'What type of shops do teenagers like best in your country?',
          'Let’s talk about films. How often do you go to the cinema?',
          'What type of films do you like best? Why?',
          'What type of films don’t you like? Why not?',
        ],
      },
      {
        key: 'part_2',
        label: 'Part 2',
        caption: 'Cue Card',
        description: 'Dành 1 phút ghi ý chính rồi nói liền mạch trong khoảng 2 phút.',
        prepSeconds: 60,
        answerSeconds: 120,
        cueCardTitle: 'Describe an important event in your life.',
        cueCardBullets: [
          'When it happened',
          'Who you were with',
          'What happened',
          'Why you feel it was important',
        ],
      },
      {
        key: 'part_3',
        label: 'Part 3',
        caption: 'Topic Discussion',
        description: 'Tập trung vào góc nhìn xã hội, xu hướng, lợi ích và sự thay đổi theo thời gian.',
        prepSeconds: 0,
        answerSeconds: 300,
        prompts: [
          'What days are important in your country?',
          'Why is it important to have national celebrations?',
          'How are national celebrations now different from those in the past?',
          'Do you think any new national celebrations will appear in the future?',
          'Are there any celebrations from other countries that people celebrate in your country?',
          'What are the benefits of having events that many people around the world celebrate on the same day?',
        ],
      },
    ],
  },
];

const SPEAKING_TOPIC_BANK = {
  key: 'part_1_topic_bank',
  label: 'Part 1 Topic Bank',
  sourceLabel: 'Theo module 100 IELTS Speaking Questions',
  topics: [
    {
      title: 'Hometown and Living Place',
      prompts: [
        'Where are you from?',
        'What do you like most about your hometown?',
        'Has your hometown changed much in recent years?',
        'Would you like to live there in the future?',
      ],
    },
    {
      title: 'Work or Study',
      prompts: [
        'Do you work or are you a student?',
        'Why did you choose this course or job?',
        'What is the most interesting part of your daily routine?',
        'Is there anything you would like to change about your work or study?',
      ],
    },
    {
      title: 'Shopping',
      prompts: [
        'Do you enjoy shopping?',
        'Who usually does the shopping in your family?',
        'What kinds of shops do young people like?',
        'Is online shopping more popular than before?',
      ],
    },
    {
      title: 'Films and Entertainment',
      prompts: [
        'How often do you watch films?',
        'What kinds of films do you enjoy most?',
        'Do you prefer watching films alone or with other people?',
        'Would you ever like to be in a film?',
      ],
    },
  ],
};

const formatSeconds = (totalSeconds) => {
  const safeValue = Math.max(0, Number(totalSeconds) || 0);
  const minutes = String(Math.floor(safeValue / 60)).padStart(2, '0');
  const seconds = String(safeValue % 60).padStart(2, '0');
  return `${minutes}:${seconds}`;
};

const parseAssessmentUiConfig = (assessment) => {
  const rawConfig = String(assessment?.uiConfigJson || '').trim();
  if (!rawConfig) return null;
  try {
    return JSON.parse(rawConfig);
  } catch {
    return null;
  }
};

const resolveSpeakingExperience = (assessment, moduleTitle, selectedVariantKey) => {
  if (assessment?.skill !== 'SPEAKING') return null;

  const uiConfig = parseAssessmentUiConfig(assessment);
  if (uiConfig?.type === 'speaking_mock_test') {
    const variants = Array.isArray(uiConfig.variants) ? uiConfig.variants : [];
    const activeVariant = variants.find((variant) => variant.key === selectedVariantKey) || variants[0] || null;
    return {
      kind: 'mock_test',
      flow: uiConfig.flow || [],
      briefing: uiConfig.briefing || null,
      variants,
      activeVariant,
    };
  }
  if (uiConfig?.type === 'speaking_topic_bank') {
    return {
      kind: 'topic_bank',
      flow: uiConfig.flow || [],
      briefing: uiConfig.briefing || null,
      sourceLabel: uiConfig.topicBank?.sourceLabel || '',
      topics: Array.isArray(uiConfig.topicBank?.topics) ? uiConfig.topicBank.topics : [],
    };
  }

  const rawContext = `${assessment?.moduleTitle || ''} ${moduleTitle || ''} ${assessment?.title || ''}`.toLowerCase();

  if (rawContext.includes('100 ielts speaking questions')) {
    return {
      kind: 'topic_bank',
      ...SPEAKING_TOPIC_BANK,
    };
  }

  if (rawContext.includes('speaking practice test with answers')) {
    const activeVariant = SPEAKING_MOCK_VARIANTS.find((variant) => variant.key === selectedVariantKey) || SPEAKING_MOCK_VARIANTS[0];
    return {
      kind: 'mock_test',
      variants: SPEAKING_MOCK_VARIANTS,
      activeVariant,
      briefing: {
        title: 'IELTS Speaking Mock Test',
        summary: 'Complete the device check, then follow Part 1, Part 2, and Part 3 as a speaking mock test.',
      },
      flow: ['mic_check', 'briefing', 'mock_test', 'recording', 'submit'],
    };
  }

  return null;
};

const IELTS_QUESTION_COUNT = 40;

const objectiveSections = {
  LISTENING: [
    { key: 'section_1', label: 'Section 1', from: 1, to: 10 },
    { key: 'section_2', label: 'Section 2', from: 11, to: 20 },
    { key: 'section_3', label: 'Section 3', from: 21, to: 30 },
    { key: 'section_4', label: 'Section 4', from: 31, to: 40 },
  ],
  READING: [
    { key: 'passage_1', label: 'Passage 1', from: 1, to: 13 },
    { key: 'passage_2', label: 'Passage 2', from: 14, to: 26 },
    { key: 'passage_3', label: 'Passage 3', from: 27, to: 40 },
  ],
};

const createObjectiveEntry = (index = 0) => ({
  id: `objective-${index + 1}`,
  questionNumber: String(index + 1),
  answer: '',
});

const createObjectiveDraft = (skill) => ({
  responses: Array.from({ length: IELTS_QUESTION_COUNT }, (_, index) => createObjectiveEntry(index)),
  overallNotes: '',
  sectionNotes: (objectiveSections[skill] || []).reduce((accumulator, section) => ({
    ...accumulator,
    [section.key]: '',
  }), {}),
});

const parseObjectiveDraft = (rawValue, skill) => {
  const fallback = createObjectiveDraft(skill);
  const rawText = String(rawValue || '').trim();
  if (!rawText) return fallback;

  try {
    const parsed = JSON.parse(rawText);
    const responses = Array.isArray(parsed?.responses) && parsed.responses.length
      ? parsed.responses.map((item, index) => ({
        id: item.id || createObjectiveEntry(index).id,
        questionNumber: String(item.questionNumber || index + 1),
        answer: String(item.answer || ''),
      }))
      : fallback.responses;
    const responseMap = new Map(responses.map((item) => [String(item.questionNumber), item]));
    const normalizedResponses = fallback.responses.map((item) => responseMap.get(item.questionNumber) || item);
    return {
      responses: normalizedResponses,
      overallNotes: String(parsed?.overallNotes || ''),
      sectionNotes: {
        ...fallback.sectionNotes,
        ...(parsed?.sectionNotes && typeof parsed.sectionNotes === 'object' ? parsed.sectionNotes : {}),
      },
    };
  } catch {
    return {
      ...fallback,
      overallNotes: rawText,
    };
  }
};

const serializeObjectiveDraft = (skill, draft) => JSON.stringify({
  skill,
  responseFormat: 'ielts_answer_sheet',
  responses: (draft?.responses || [])
    .map((item) => ({
      questionNumber: String(item.questionNumber || '').trim(),
      answer: String(item.answer || '').trim(),
    })),
  sectionNotes: draft?.sectionNotes || {},
  overallNotes: String(draft?.overallNotes || '').trim(),
}, null, 2);

const hasObjectiveContent = (draft) => (
  (draft?.responses || []).some((item) => String(item.answer || '').trim())
  || Object.values(draft?.sectionNotes || {}).some((value) => String(value || '').trim())
  || String(draft?.overallNotes || '').trim()
);

const assessmentInputCopy = (skill) => ({
  LISTENING: {
    label: 'IELTS Listening Answer Sheet',
    helper: 'Nhập đáp án cho đủ 40 câu theo kiểu answer sheet của IELTS. Bạn có thể ghi chú lỗi theo từng section ở bên dưới.',
    placeholder: 'Ghi chú chung: hay bỏ lỡ signposting ở Section 3, nhầm số điện thoại ở Section 1...',
    emptyError: 'Hãy nhập đáp án hoặc ghi chú lỗi nghe trước khi gửi chấm.',
    payloadField: 'objectiveAnswersJson',
    buttonText: 'Gửi phân tích Listening',
  },
  READING: {
    label: 'IELTS Reading Answer Sheet',
    helper: 'Nhập đáp án cho đủ 40 câu theo kiểu answer sheet của IELTS. Bạn có thể ghi chú lỗi theo từng passage ở bên dưới.',
    placeholder: 'Ghi chú chung: hay nhầm FALSE với NOT GIVEN, tìm keyword chậm ở Passage 3...',
    emptyError: 'Hãy nhập đáp án hoặc ghi chú lỗi đọc trước khi gửi chấm.',
    payloadField: 'objectiveAnswersJson',
    buttonText: 'Gửi phân tích Reading',
  },
  SPEAKING: {
    label: 'IELTS Speaking Recording',
    helper: 'Ghi âm câu trả lời Speaking để làm bài theo trải nghiệm gần với phòng thi.',
    placeholder: 'Bài Speaking sẽ được chấm dựa trên bản ghi âm bạn nộp.',
    audioLabel: 'Bản ghi hoặc đường dẫn âm thanh',
    audioPlaceholder: 'https://.../speaking-answer.webm',
    emptyError: 'Hãy ghi âm hoặc dán đường dẫn âm thanh trước khi gửi chấm.',
    payloadField: 'submittedText',
    buttonText: 'Gửi chấm Speaking',
  },
  WRITING: {
    label: 'Bài viết của học viên',
    helper: 'Viết/dán đoạn văn hoặc essay để AI góp ý theo rubric Writing được gắn với bài kiểm tra.',
    placeholder: 'Dán bài IELTS Writing Task 1/Task 2 hoặc đoạn văn của bạn vào đây...',
    emptyError: 'Hãy nhập bài viết trước khi gửi chấm.',
    payloadField: 'submittedText',
    buttonText: 'Gửi chấm Writing',
  },
  VOCABULARY: {
    label: 'Câu sử dụng từ vựng mục tiêu',
    helper: 'Viết 5-7 câu dùng từ vựng mục tiêu của module để kiểm tra nghĩa, collocation và độ tự nhiên.',
    placeholder: 'Viết 5-7 câu dùng từ vựng mục tiêu của module này...',
    emptyError: 'Hãy nhập câu sử dụng từ vựng mục tiêu trước khi gửi chấm.',
    payloadField: 'submittedText',
    buttonText: 'Gửi chấm từ vựng',
  },
  MIXED: {
    label: 'Reflection, đáp án hoặc nhật ký lỗi',
    helper: 'Tóm tắt đáp án, lỗi sai, dạng câu hỏi khó và mục tiêu ôn tập tiếp theo để AI tạo kế hoạch review.',
    placeholder: 'Ví dụ: Listening sai map labeling, Reading sai True/False/Not Given, Writing thiếu ví dụ...',
    emptyError: 'Hãy nhập reflection, đáp án hoặc nhật ký lỗi trước khi gửi phân tích.',
    payloadField: 'submittedText',
    buttonText: 'Gửi phân tích tổng hợp',
  },
}[skill] || {
  label: 'Bài làm của học viên',
  helper: 'Nhập nội dung bài làm hoặc ghi chú để nhận phản hồi.',
  placeholder: 'Nhập câu trả lời, ghi chú hoặc bài làm vào đây...',
  emptyError: 'Hãy nhập nội dung bài làm trước khi gửi chấm.',
  payloadField: 'submittedText',
  buttonText: 'Nộp bài để chấm',
});

const formatCriterionName = (name) => {
  const normalizedName = String(name || '').trim();
  if (!normalizedName) return 'Tiêu chí';
  const translated = criterionTranslations[normalizedName];
  return translated || translateTopic(normalizedName);
};

const formatCriterionDescription = (description) => {
  const normalizedDescription = String(description || '').trim();
  if (!normalizedDescription) return 'Tiêu chí chấm của bài kiểm tra này.';
  return criterionDescriptionTranslations[normalizedDescription] || translateTopic(normalizedDescription);
};

const buildFriendlyError = (error) => {
  const message = error?.response?.data?.message || error?.message || 'Không thể gửi bài để chấm.';
  if (/AI is disabled/i.test(message)) {
    return 'Tính năng chấm bài hiện đang tạm thời chưa khả dụng. Hãy thử lại sau.';
  }
  if (/API key is missing|GEMINI_API_KEY|OPENAI_API_KEY/i.test(message)) {
    return 'Backend chưa có API key của nhà cung cấp AI. Hãy cấu hình key rồi thử lại.';
  }
  return message;
};

const speakingAudioRecoveryKey = (assessmentId) => (
  assessmentId ? `englishlab:speaking-audio:${assessmentId}` : ''
);

const readRecoveredSpeakingAudioUrl = (assessmentId) => {
  const key = speakingAudioRecoveryKey(assessmentId);
  if (!key || typeof window === 'undefined') return '';
  try {
    return window.localStorage.getItem(key) || '';
  } catch {
    return '';
  }
};

const persistRecoveredSpeakingAudioUrl = (assessmentId, url) => {
  const key = speakingAudioRecoveryKey(assessmentId);
  if (!key || typeof window === 'undefined') return;
  try {
    if (url) {
      window.localStorage.setItem(key, url);
    } else {
      window.localStorage.removeItem(key);
    }
  } catch {
    // localStorage may be blocked in private browsing or strict browser settings.
  }
};

export default function AiAssessmentPanel({ assessments = [], moduleTitle, isLocked = false, onMoveStep, onSubmitAssessment }) {
  const [selectedId, setSelectedId] = useState(null);
  const [answer, setAnswer] = useState('');
  const [objectiveDraft, setObjectiveDraft] = useState(() => createObjectiveDraft('LISTENING'));
  const [audioUrl, setAudioUrl] = useState('');
  const [audioPreviewUrl, setAudioPreviewUrl] = useState('');
  const [isRecording, setIsRecording] = useState(false);
  const [uploadingAudio, setUploadingAudio] = useState(false);
  const [recordingError, setRecordingError] = useState('');
  const [selectedSpeakingMockKey, setSelectedSpeakingMockKey] = useState('');
  const [activeSpeakingPartKey, setActiveSpeakingPartKey] = useState('part_1');
  const [speakingStage, setSpeakingStage] = useState('mic_check');
  const [micPermissionState, setMicPermissionState] = useState('idle');
  const [micTesting, setMicTesting] = useState(false);
  const [micLevel, setMicLevel] = useState(0);
  const [micCheckPreviewUrl, setMicCheckPreviewUrl] = useState('');
  const [micCheckCountdown, setMicCheckCountdown] = useState(5);
  const [recordingLevel, setRecordingLevel] = useState(0);
  const [recordingPeakLevel, setRecordingPeakLevel] = useState(0);
  const [completedRecordingDurationSeconds, setCompletedRecordingDurationSeconds] = useState(0);
  const [recordingHasVoiceSignal, setRecordingHasVoiceSignal] = useState(false);
  const [micCheckPassed, setMicCheckPassed] = useState(false);
  const [headphoneCheckPlayed, setHeadphoneCheckPlayed] = useState(false);
  const [availableInputDevices, setAvailableInputDevices] = useState([]);
  const [availableOutputDevices, setAvailableOutputDevices] = useState([]);
  const [selectedInputDeviceId, setSelectedInputDeviceId] = useState('');
  const [selectedOutputDeviceId, setSelectedOutputDeviceId] = useState('');
  const [speakingQuestionIndex, setSpeakingQuestionIndex] = useState(0);
  const [recordingDurationSeconds, setRecordingDurationSeconds] = useState(0);
  const [speakingTimer, setSpeakingTimer] = useState({
    partKey: null,
    phase: null,
    remainingSeconds: 0,
    running: false,
    finished: false,
  });
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [creatingNewAttempt, setCreatingNewAttempt] = useState(false);
  const [pendingSpeakingSubmit, setPendingSpeakingSubmit] = useState(false);
  const [examModeOpen, setExamModeOpen] = useState(false);
  const [examWarning, setExamWarning] = useState(null);
  const [examViolations, setExamViolations] = useState([]);
  const [examExitConfirmOpen, setExamExitConfirmOpen] = useState(false);
  const mediaRecorderRef = useRef(null);
  const mediaStreamRef = useRef(null);
  const audioChunksRef = useRef([]);
  const micCheckStreamRef = useRef(null);
  const micCheckAudioContextRef = useRef(null);
  const micCheckAnalyserRef = useRef(null);
  const micCheckFrameRef = useRef(null);
  const micCheckRecorderRef = useRef(null);
  const micCheckChunksRef = useRef([]);
  const micCheckTimeoutRef = useRef(null);
  const micCheckIntervalRef = useRef(null);
  const recordingAudioContextRef = useRef(null);
  const recordingAnalyserRef = useRef(null);
  const recordingFrameRef = useRef(null);
  const recordingDurationRef = useRef(0);
  const headphoneAudioContextRef = useRef(null);
  const headphoneTestAudioRef = useRef(null);
  const speakingVideoRef = useRef(null);
  const examViolationLockRef = useRef(false);
  const examIntentionalExitRef = useRef(false);

  const restoreAssessmentAttemptState = () => {
    const latestSubmission = selected?.latestSubmission;
    const objectiveSeed = latestSubmission?.objectiveAnswersJson || '';
    const recoveredAudioUrl = selected?.skill === 'SPEAKING'
      ? readRecoveredSpeakingAudioUrl(selected?.id)
      : '';

    stopRecordingMeter();
    stopMediaStream();
    stopMicCheck();
    if (audioPreviewUrl) {
      URL.revokeObjectURL(audioPreviewUrl);
    }
    if (mediaRecorderRef.current?.state && mediaRecorderRef.current.state !== 'inactive') {
      mediaRecorderRef.current.stop();
    }

    setAnswer(latestSubmission?.submittedText || '');
    setObjectiveDraft(parseObjectiveDraft(objectiveSeed, selected?.skill || 'LISTENING'));
    setAudioUrl(latestSubmission?.submittedAudioUrl || recoveredAudioUrl || '');
    setAudioPreviewUrl('');
    setIsRecording(false);
    setUploadingAudio(false);
    setRecordingError('');
    setSelectedSpeakingMockKey(speakingExperience?.activeVariant?.key || '');
    setActiveSpeakingPartKey(speakingExperience?.activeVariant?.parts?.[0]?.key || 'part_1');
    setSpeakingStage('mic_check');
    setMicPermissionState('idle');
    setMicTesting(false);
    setMicLevel(0);
    if (micCheckPreviewUrl) {
      URL.revokeObjectURL(micCheckPreviewUrl);
    }
    setMicCheckPreviewUrl('');
    setMicCheckCountdown(5);
    setRecordingLevel(0);
    setRecordingPeakLevel(0);
    setCompletedRecordingDurationSeconds(0);
    setRecordingHasVoiceSignal(false);
    setMicCheckPassed(false);
    setHeadphoneCheckPlayed(false);
    setSpeakingQuestionIndex(0);
    setRecordingDurationSeconds(0);
    recordingDurationRef.current = 0;
    setSpeakingTimer({
      partKey: null,
      phase: null,
      remainingSeconds: 0,
      running: false,
      finished: false,
    });
    setResult(latestSubmission || null);
    setError('');
    setCreatingNewAttempt(false);
    setPendingSpeakingSubmit(false);
  };

  const requestExamFullscreen = async () => {
    if (!document?.documentElement?.requestFullscreen) return false;
    try {
      await document.documentElement.requestFullscreen();
      return true;
    } catch {
      return false;
    }
  };

  const refreshMediaDevices = async () => {
    if (!navigator.mediaDevices?.enumerateDevices) return;
    try {
      const devices = await navigator.mediaDevices.enumerateDevices();
      const inputs = devices.filter((device) => device.kind === 'audioinput');
      const outputs = devices.filter((device) => device.kind === 'audiooutput');
      setAvailableInputDevices(inputs);
      setAvailableOutputDevices(outputs);
      setSelectedInputDeviceId((current) => current || inputs[0]?.deviceId || '');
      setSelectedOutputDeviceId((current) => current || outputs[0]?.deviceId || '');
    } catch {
      // Ignore enumerate-device failures; browser permissions may still be pending.
    }
  };

  const stopRecordingMeter = () => {
    if (recordingFrameRef.current) {
      window.cancelAnimationFrame(recordingFrameRef.current);
      recordingFrameRef.current = null;
    }
    recordingAnalyserRef.current = null;
    if (recordingAudioContextRef.current && recordingAudioContextRef.current.state !== 'closed') {
      recordingAudioContextRef.current.close().catch(() => {});
    }
    recordingAudioContextRef.current = null;
    setRecordingLevel(0);
  };

  const buildSpeakingSubmissionText = () => {
    const parts = (activeSpeakingVariant?.parts || []).map((part) => {
      const prompts = Array.isArray(part.prompts) && part.prompts.length
        ? part.prompts.map((prompt, index) => `${index + 1}. ${typeof prompt === 'string' ? prompt : prompt?.text || ''}`).join('\n')
        : (part.cueCardBullets || []).map((bullet, index) => `${index + 1}. ${bullet}`).join('\n');
      const cueCardBlock = part.cueCardTitle
        ? `Cue card: ${part.cueCardTitle}\n${prompts}`
        : prompts;
      return `${formatSpeakingPartLabel(part.key)} - ${part.caption || part.label}\n${cueCardBlock}`.trim();
    }).join('\n\n');

    return [
      `Speaking mock test: ${activeSpeakingVariant?.label || 'Unknown variant'}`,
      `Recording duration seconds: ${completedRecordingDurationSeconds || recordingDurationSeconds || 0}`,
      `Voice signal detected: ${recordingHasVoiceSignal ? 'yes' : 'no'}`,
      '',
      'Part prompts shown to the learner:',
      parts || 'No part metadata available.',
    ].join('\n').trim();
  };

  const resetSpeakingAttemptState = () => {
    stopRecordingMeter();
    stopMediaStream();
    stopMicCheck();
    if (audioPreviewUrl) {
      URL.revokeObjectURL(audioPreviewUrl);
    }
    if (mediaRecorderRef.current?.state && mediaRecorderRef.current.state !== 'inactive') {
      mediaRecorderRef.current.stop();
    }
    setAnswer('');
    setAudioUrl('');
    persistRecoveredSpeakingAudioUrl(selected?.id, '');
    setAudioPreviewUrl('');
    setIsRecording(false);
    setUploadingAudio(false);
    setRecordingError('');
    setRecordingDurationSeconds(0);
    recordingDurationRef.current = 0;
    setCompletedRecordingDurationSeconds(0);
    setRecordingLevel(0);
    setRecordingPeakLevel(0);
    setRecordingHasVoiceSignal(false);
    setSelectedSpeakingMockKey(speakingExperience?.activeVariant?.key || '');
    setActiveSpeakingPartKey(speakingExperience?.activeVariant?.parts?.[0]?.key || 'part_1');
    setSpeakingStage('mic_check');
    setMicPermissionState('idle');
    setMicTesting(false);
    setMicLevel(0);
    if (micCheckPreviewUrl) {
      URL.revokeObjectURL(micCheckPreviewUrl);
    }
    setMicCheckPreviewUrl('');
    setMicCheckCountdown(5);
    setMicCheckPassed(false);
    setHeadphoneCheckPlayed(false);
    setSpeakingQuestionIndex(0);
    setSpeakingTimer({
      partKey: null,
      phase: null,
      remainingSeconds: 0,
      running: false,
      finished: false,
    });
    setResult(null);
    setError('');
    setCreatingNewAttempt(true);
    setPendingSpeakingSubmit(false);
  };

  const handleRetakeAttempt = () => {
    setObjectiveDraft(createObjectiveDraft(selected?.skill || 'LISTENING'));
    resetSpeakingAttemptState();
  };

  const handleOpenExamMode = () => {
    if (isLocked || submitting || isLockedAfterResult) return;
    setError('');
    setExamWarning(null);
    setExamViolations([]);
    setExamExitConfirmOpen(false);
    examIntentionalExitRef.current = false;
    setExamModeOpen(true);
  };

  const handleCloseExamMode = async () => {
    if (submitting) return;
    examIntentionalExitRef.current = true;
    setExamExitConfirmOpen(false);
    setExamWarning(null);
    restoreAssessmentAttemptState();
    if (document.fullscreenElement) {
      try {
        await document.exitFullscreen?.();
      } catch {
        // Ignore fullscreen exit failures; the guard is already disabled.
      }
    }
    setExamModeOpen(false);
  };

  const orderedAssessments = useMemo(() => (
    [...assessments].sort((left, right) => {
      const leftOrder = Number(left.displayOrder ?? Number.MAX_SAFE_INTEGER);
      const rightOrder = Number(right.displayOrder ?? Number.MAX_SAFE_INTEGER);
      if (leftOrder !== rightOrder) return leftOrder - rightOrder;
      return String(left.title || '').localeCompare(String(right.title || ''));
    })
  ), [assessments]);

  const selected = orderedAssessments.find((item) => String(item.id) === String(selectedId)) || orderedAssessments[0];
  const speakingExperience = resolveSpeakingExperience(selected, moduleTitle, selectedSpeakingMockKey);
  const inputCopy = assessmentInputCopy(selected?.skill);
  const assessmentUiConfig = parseAssessmentUiConfig(selected);
  const isReadingExamMode = selected?.skill === 'READING' && assessmentUiConfig?.type === 'ielts_reading_exam';
  const isListeningExamMode = selected?.skill === 'LISTENING' && assessmentUiConfig?.type === 'ielts_listening_exam';
  const isWritingExamMode = selected?.skill === 'WRITING' && assessmentUiConfig?.type === 'ielts_writing_exam';
  const isDedicatedExamMode = isReadingExamMode || isListeningExamMode || isWritingExamMode;
  const feedback = tryParseFeedback(result?.aiFeedbackJson);
  const previousSubmission = selected?.previousSubmission || null;
  const submissionComparison = buildSubmissionComparison(result, previousSubmission);
  const criteria = toArray(feedback?.criteria);
  const strengths = toArray(feedback?.strengths);
  const weaknesses = toArray(feedback?.weaknesses);
  const suggestions = toArray(feedback?.suggestions);
  const recommendedReview = toArray(feedback?.recommendedReview);
  const correctedExamples = toArray(feedback?.correctedExamples);
  const partFeedback = toArray(feedback?.partFeedback);
  const originalityAnalysis = toObject(feedback?.originalityAnalysis);
  const sourceSignals = toArray(feedback?.sourceSignals);
  const plagiarismRisk = riskLabel(feedback?.plagiarismRisk || feedback?.plagiarism?.riskLevel || originalityAnalysis?.plagiarismRisk);
  const aiUsageRisk = riskLabel(feedback?.aiUsageRisk || feedback?.aiUsage?.riskLevel || originalityAnalysis?.aiUsageRisk);
  const numericScore = result?.aiScore ?? feedback?.estimatedScore ?? null;
  const showNumericScore = numericScore != null;
  const shouldShowExamScoreBadges = isExamSkill(selected?.skill);
  const scoreDisplay = numericScore ?? 'Chưa có';
  const bandDisplay = feedback?.estimatedBand || (numericScore != null ? String(numericScore) : 'Chưa có');
  const usesFixedScoring = isObjectiveSkill(selected?.skill);
  const isLockedAfterResult = Boolean(result) && !creatingNewAttempt;
  const isSubmissionLocked = isLocked || isLockedAfterResult;
  const activeSpeakingVariant = speakingExperience?.kind === 'mock_test' ? speakingExperience.activeVariant : null;
  const activeSpeakingPart = activeSpeakingVariant?.parts?.find((part) => part.key === activeSpeakingPartKey) || activeSpeakingVariant?.parts?.[0] || null;
  const activeSpeakingQuestions = Array.isArray(activeSpeakingPart?.prompts) ? activeSpeakingPart.prompts : [];
  const currentSpeakingQuestion = activeSpeakingQuestions[speakingQuestionIndex] || null;
  const isSpeakingMockFlow = selected?.skill === 'SPEAKING' && speakingExperience?.kind === 'mock_test';
  const isLastSpeakingQuestionInPart = speakingQuestionIndex >= activeSpeakingQuestions.length - 1;
  const activeSpeakingPartIndex = activeSpeakingVariant?.parts?.findIndex((part) => part.key === activeSpeakingPartKey) ?? -1;
  const isLastSpeakingPart = activeSpeakingPartIndex >= 0 && activeSpeakingPartIndex === (activeSpeakingVariant?.parts?.length ?? 0) - 1;
  const isFinalSpeakingPrompt = isSpeakingMockFlow && isLastSpeakingPart && isLastSpeakingQuestionInPart;
  const showSpeakingResultOnly = isSpeakingMockFlow && isLockedAfterResult;
  const vocabularySentences = answer
    .split(/[.!?]+/)
    .map((sentence) => sentence.trim())
    .filter((sentence) => sentence.split(/\s+/).filter(Boolean).length >= 3);
  const vocabularySentenceCount = vocabularySentences.length;
  const vocabularyWordCount = answer.trim() ? answer.trim().split(/\s+/).filter(Boolean).length : 0;
  const vocabularyReadinessPercent = Math.min(100, Math.round((vocabularySentenceCount / 5) * 100));
  const vocabularyChecks = [
    {
      label: '5-7 câu',
      done: vocabularySentenceCount >= 5 && vocabularySentenceCount <= 7,
      hint: `${vocabularySentenceCount}/7 câu`,
    },
    {
      label: 'Đủ ngữ cảnh',
      done: vocabularyWordCount >= 45,
      hint: `${vocabularyWordCount} từ`,
    },
    {
      label: 'Tự nhiên',
      done: /because|although|while|when|if|so|therefore|however|nhưng|vì|khi|nếu/i.test(answer),
      hint: 'Có liên kết ý',
    },
  ];
  const activeFallbackPromptVideoUrl = SPEAKING_PROMPT_VIDEO_MAP[selectedSpeakingMockKey]?.[activeSpeakingPartKey]?.[speakingQuestionIndex] || '';
  const activeSpeakingVideoUrl = currentSpeakingQuestion?.videoUrl
    || activeFallbackPromptVideoUrl
    || activeSpeakingPart?.videoUrl
    || SPEAKING_PART_VIDEO_FALLBACKS[selectedSpeakingMockKey]?.[activeSpeakingPartKey]
    || '';
  const speakingMeterBars = Array.from({ length: 40 }, (_, index) => {
    if (!isRecording) return 4;
    const center = 19.5;
    const distance = Math.abs(index - center);
    const curve = Math.max(0, 1 - (distance / center));
    const base = 6 + (curve * 12);
    return Math.max(6, Math.round(base + (recordingLevel * 0.18 * curve)));
  });
  const hasRecordedAudio = Boolean(audioPreviewUrl || audioUrl.trim());
  const hasMeaningfulSpeakingEvidence = selected?.skill === 'SPEAKING'
    ? (
      hasRecordedAudio && completedRecordingDurationSeconds >= 5 && recordingHasVoiceSignal
    )
    : false;
  const hasMultipleAssessments = orderedAssessments.length > 1;
  const initialExamObjectiveAnswers = useMemo(() => (
    (objectiveDraft.responses || []).reduce((accumulator, entry) => ({
      ...accumulator,
      [String(entry.questionNumber || entry.id || '')]: entry.answer || '',
    }), {})
  ), [objectiveDraft.responses]);
  const isFullscreenExamMode = examModeOpen && !isDedicatedExamMode;
  const showStartExamCard = !examModeOpen && !isLockedAfterResult;
  const startExamButtonLabel = selected?.skill === 'SPEAKING' ? 'Bắt đầu kiểm tra' : 'Vào chế độ làm bài';

  useEffect(() => {
    refreshMediaDevices();
    if (!navigator.mediaDevices?.addEventListener) return undefined;
    const handleDeviceChange = () => {
      refreshMediaDevices();
    };
    navigator.mediaDevices.addEventListener('devicechange', handleDeviceChange);
    return () => navigator.mediaDevices.removeEventListener('devicechange', handleDeviceChange);
  }, []);

  useEffect(() => {
    const latestSubmission = selected?.latestSubmission;
    const objectiveSeed = latestSubmission?.objectiveAnswersJson || '';
    const recoveredAudioUrl = selected?.skill === 'SPEAKING'
      ? readRecoveredSpeakingAudioUrl(selected?.id)
      : '';
    setAnswer(latestSubmission?.submittedText || '');
    setObjectiveDraft(parseObjectiveDraft(objectiveSeed, selected?.skill || 'LISTENING'));
    setAudioUrl(latestSubmission?.submittedAudioUrl || recoveredAudioUrl || '');
    setAudioPreviewUrl('');
    setUploadingAudio(false);
    setRecordingError('');
    setSelectedSpeakingMockKey('');
    setActiveSpeakingPartKey('part_1');
    setSpeakingStage('mic_check');
    setMicPermissionState('idle');
    setMicTesting(false);
    setMicLevel(0);
    if (micCheckPreviewUrl) {
      URL.revokeObjectURL(micCheckPreviewUrl);
    }
    setMicCheckPreviewUrl('');
    setMicCheckCountdown(5);
    setRecordingLevel(0);
    setRecordingPeakLevel(0);
    setCompletedRecordingDurationSeconds(0);
    setRecordingHasVoiceSignal(false);
    setMicCheckPassed(false);
    setHeadphoneCheckPlayed(false);
    setSpeakingQuestionIndex(0);
    setRecordingDurationSeconds(0);
    recordingDurationRef.current = 0;
    setSpeakingTimer({
      partKey: null,
      phase: null,
      remainingSeconds: 0,
      running: false,
      finished: false,
    });
    setResult(latestSubmission || null);
    setError('');
    setCreatingNewAttempt(false);
    setPendingSpeakingSubmit(false);
    setExamModeOpen(false);
    setExamWarning(null);
    setExamViolations([]);
    setExamExitConfirmOpen(false);
    examIntentionalExitRef.current = false;
  }, [selected?.id]);

  useEffect(() => {
    if (speakingExperience?.kind !== 'mock_test') return;
    const firstPartKey = speakingExperience.activeVariant?.parts?.[0]?.key || 'part_1';
    const activeVariantKey = speakingExperience.activeVariant?.key || '';
    if (activeVariantKey && activeVariantKey !== selectedSpeakingMockKey) {
      setSelectedSpeakingMockKey(activeVariantKey);
    }
    setActiveSpeakingPartKey(firstPartKey);
    setSpeakingQuestionIndex(0);
    setSpeakingTimer({
      partKey: null,
      phase: null,
      remainingSeconds: 0,
      running: false,
      finished: false,
    });
  }, [speakingExperience?.kind, speakingExperience?.activeVariant?.key]);

  useEffect(() => {
    setSpeakingQuestionIndex(0);
  }, [activeSpeakingPartKey]);

  useEffect(() => {
    if (!examModeOpen) return undefined;

    const originalOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    return () => {
      document.body.style.overflow = originalOverflow;
    };
  }, [examModeOpen]);

  useEffect(() => {
    if (!isFullscreenExamMode) return undefined;

    examViolationLockRef.current = false;
    examIntentionalExitRef.current = false;
    const pushExamState = () => {
      window.history.pushState({ englishlabExamMode: true }, '', window.location.href);
    };
    const recordViolation = (reason) => {
      if (examViolationLockRef.current) return;
      examViolationLockRef.current = true;
      const violation = {
        reason,
        at: new Date().toISOString(),
      };
      setExamViolations((current) => [...current, violation]);
      setExamWarning(violation);
      window.setTimeout(() => {
        examViolationLockRef.current = false;
      }, 300);
    };

    pushExamState();
    const handlePopState = () => {
      pushExamState();
      recordViolation('Bạn không thể quay lại trang khác trong khi đang thi.');
    };
    const handleVisibilityChange = () => {
      if (document.hidden) {
        recordViolation('Hệ thống ghi nhận bạn đã rời tab hoặc thu nhỏ cửa sổ trong lúc làm bài.');
      }
    };
    const handleWindowBlur = () => {
      recordViolation('Hệ thống ghi nhận cửa sổ làm bài đã mất focus.');
    };
    const handleBeforeUnload = (event) => {
      event.preventDefault();
      event.returnValue = '';
    };
    const handleKeyDown = (event) => {
      const loweredKey = String(event.key || '').toLowerCase();
      const isBlockedShortcut = event.key === 'F5'
        || event.key === 'Escape'
        || (event.altKey && loweredKey === 'arrowleft')
        || (event.altKey && loweredKey === 'arrowright')
        || ((event.ctrlKey || event.metaKey) && ['r', 'w', 't', 'n', 'l', 'c', 'v', 'x', 'a', 'p', 's', 'u'].includes(loweredKey));
      if (!isBlockedShortcut) return;
      event.preventDefault();
      event.stopPropagation();
      recordViolation(
        event.key === 'Escape'
          ? 'Bạn không thể dùng phím Esc để thoát toàn màn hình trong khi đang thi.'
          : 'Một thao tác điều hướng hoặc sao chép ngoài bài thi vừa bị chặn.'
      );
    };
    const handleFullScreenChange = () => {
      if (!document.fullscreenElement) {
        if (examIntentionalExitRef.current) return;
        recordViolation('Bạn không thể thoát chế độ toàn màn hình trong khi đang thi.');
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    document.addEventListener('fullscreenchange', handleFullScreenChange);
    window.addEventListener('blur', handleWindowBlur);
    window.addEventListener('beforeunload', handleBeforeUnload);
    window.addEventListener('popstate', handlePopState);
    window.addEventListener('keydown', handleKeyDown, true);
    requestExamFullscreen();

    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
      document.removeEventListener('fullscreenchange', handleFullScreenChange);
      window.removeEventListener('blur', handleWindowBlur);
      window.removeEventListener('beforeunload', handleBeforeUnload);
      window.removeEventListener('popstate', handlePopState);
      window.removeEventListener('keydown', handleKeyDown, true);
      if (document.fullscreenElement && !examIntentionalExitRef.current) {
        document.exitFullscreen?.().catch(() => {});
      }
    };
  }, [isFullscreenExamMode]);

  useEffect(() => {
    if (!isSpeakingMockFlow) return;
    if (speakingStage !== 'test' && speakingStage !== 'recording') return;
    if (!activeSpeakingPart?.key) return;
    if (speakingTimer.partKey === activeSpeakingPart.key && (speakingTimer.running || speakingTimer.remainingSeconds > 0)) {
      return;
    }
    startSpeakingTimer(activeSpeakingPart);
  }, [isSpeakingMockFlow, speakingStage, activeSpeakingPart?.key]);

  useEffect(() => {
    if (!isRecording) {
      return undefined;
    }
    const intervalId = window.setInterval(() => {
      setRecordingDurationSeconds((current) => {
        const nextValue = current + 1;
        recordingDurationRef.current = nextValue;
        return nextValue;
      });
    }, 1000);
    return () => window.clearInterval(intervalId);
  }, [isRecording]);

  useEffect(() => {
    if (!pendingSpeakingSubmit || selected?.skill !== 'SPEAKING') {
      return;
    }
    if (isRecording || uploadingAudio || submitting) {
      return;
    }
    if (!hasMeaningfulSpeakingEvidence) {
      setPendingSpeakingSubmit(false);
      setError('Bài nói này chưa có đủ nội dung để chấm. Hệ thống chưa ghi nhận phần nói rõ ràng hoặc thời lượng nói quá ngắn.');
      return;
    }
    setPendingSpeakingSubmit(false);
    handleSubmit();
  }, [pendingSpeakingSubmit, selected?.skill, isRecording, uploadingAudio, submitting, hasMeaningfulSpeakingEvidence]);

  useEffect(() => {
    if (!speakingTimer.running || speakingTimer.remainingSeconds <= 0) {
      return undefined;
    }

    const intervalId = window.setInterval(() => {
      setSpeakingTimer((current) => {
        if (!current.running) return current;
        if (current.remainingSeconds > 1) {
          return {
            ...current,
            remainingSeconds: current.remainingSeconds - 1,
          };
        }

        const timerPart = activeSpeakingVariant?.parts?.find((part) => part.key === current.partKey);
        if (!timerPart) {
          return {
            ...current,
            remainingSeconds: 0,
            running: false,
            finished: true,
          };
        }

        if (current.phase === 'prep' && timerPart.answerSeconds > 0) {
          return {
            ...current,
            phase: 'answer',
            remainingSeconds: timerPart.answerSeconds,
            running: true,
            finished: false,
          };
        }

        return {
          ...current,
          remainingSeconds: 0,
          running: false,
          finished: true,
        };
      });
    }, 1000);

    return () => window.clearInterval(intervalId);
  }, [speakingTimer.running, speakingTimer.remainingSeconds, activeSpeakingVariant]);

  useEffect(() => () => {
    if (audioPreviewUrl) {
      URL.revokeObjectURL(audioPreviewUrl);
    }
    if (micCheckPreviewUrl) {
      URL.revokeObjectURL(micCheckPreviewUrl);
    }
    if (mediaRecorderRef.current?.state !== 'inactive') {
      mediaRecorderRef.current?.stop();
    }
    mediaStreamRef.current?.getTracks?.().forEach((track) => track.stop());
    stopMicCheck();
    stopRecordingMeter();
  }, [audioPreviewUrl, micCheckPreviewUrl]);

  if (!assessments.length) {
    return (
      <section className="rounded-[28px] border border-[#dfbfbd]/20 bg-white p-6 shadow-sm">
        <p className="text-[11px] font-bold uppercase tracking-[0.14em] text-[#8c716f]">Bài kiểm tra</p>
        <h3 className="mt-2 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">Chưa có bài kiểm tra nào được gắn vào module này</h3>
        <p className="mt-3 text-sm leading-7 text-[#584140]">
          Content Manager cần gắn một bài đánh giá theo rubric thì học viên mới có thể nộp bài và nhận góp ý.
        </p>
      </section>
    );
  }

  const handleSubmit = async () => {
    if (isLocked) {
      setError('Hãy hoàn thành các bài học trong module này trước khi làm bài kiểm tra.');
      return;
    }

    if (!selected?.id) {
      setError(inputCopy.emptyError);
      return;
    }

    if (isObjectiveSkill(selected.skill) && !hasObjectiveContent(objectiveDraft)) {
      setError(inputCopy.emptyError);
      return;
    }

    const hasRecordedAudio = Boolean(audioPreviewUrl || audioUrl.trim());
    if (selected.skill === 'SPEAKING' && uploadingAudio) {
      setError('Bản ghi đang được xử lý. Hãy đợi vài giây rồi gửi chấm.');
      return;
    }
    if (selected.skill === 'SPEAKING' && speakingStage !== 'recording') {
      setError('Hãy hoàn thành bước kiểm tra micro, xem đề Speaking rồi chuyển sang bước ghi âm trước khi gửi chấm.');
      return;
    }
    if (selected.skill === 'SPEAKING' && hasRecordedAudio && !audioUrl.trim()) {
      setError('Bản ghi đã có nhưng vẫn đang hoàn tất. Hãy đợi một chút rồi gửi chấm.');
      return;
    }
    if (selected.skill === 'SPEAKING' && !hasMeaningfulSpeakingEvidence) {
      setError('Bài nói này chưa có đủ nội dung để chấm. Nếu bạn chưa trả lời hoặc gần như không nói gì, hệ thống sẽ không chấm điểm.');
      return;
    }
    if (!isObjectiveSkill(selected.skill) && !(selected.skill === 'SPEAKING'
      ? hasRecordedAudio
      : answer.trim())) {
      setError(inputCopy.emptyError);
      return;
    }

    setSubmitting(true);
    setError('');
    setResult(null);

    try {
      const payload = inputCopy.payloadField === 'objectiveAnswersJson'
        ? { objectiveAnswersJson: serializeObjectiveDraft(selected.skill, objectiveDraft) }
        : { submittedText: selected.skill === 'SPEAKING' ? buildSpeakingSubmissionText() : answer.trim() };
      if (selected.skill === 'SPEAKING' && audioUrl.trim()) {
        payload.submittedAudioUrl = audioUrl.trim();
      }
      const response = await onSubmitAssessment(selected.id, payload);
      setResult(response);
      setCreatingNewAttempt(false);
      setExamModeOpen(false);
    } catch (submissionError) {
      setError(buildFriendlyError(submissionError));
    } finally {
      setSubmitting(false);
    }
  };

  const handleExamModeSubmit = async (payload) => {
    if (isLocked) {
      setError('Hãy hoàn thành các bài học trong module này trước khi làm bài kiểm tra.');
      return;
    }
    if (!selected?.id || !payload) {
      setError(inputCopy.emptyError);
      return;
    }

    setSubmitting(true);
    setError('');
    setResult(null);
    try {
      const response = await onSubmitAssessment(selected.id, payload);
      setResult(response);
      setCreatingNewAttempt(false);
      setExamModeOpen(false);
    } catch (submissionError) {
      setError(buildFriendlyError(submissionError));
    } finally {
      setSubmitting(false);
    }
  };

  const handleObjectiveResponseChange = (entryId, value) => {
    setObjectiveDraft((current) => ({
      ...current,
      responses: current.responses.map((item) => (item.id === entryId
        ? {
          ...item,
          answer: value,
        }
        : item)),
    }));
  };

  const startSpeakingTimer = (part) => {
    if (!part) return;
    setActiveSpeakingPartKey(part.key);
    setSpeakingTimer({
      partKey: part.key,
      phase: part.prepSeconds > 0 ? 'prep' : 'answer',
      remainingSeconds: part.prepSeconds > 0 ? part.prepSeconds : part.answerSeconds,
      running: true,
      finished: false,
    });
  };

  const toggleSpeakingTimer = () => {
    setSpeakingTimer((current) => ({
      ...current,
      running: !current.running,
    }));
  };

  const resetSpeakingTimer = (part) => {
    if (!part) return;
    setSpeakingTimer({
      partKey: part.key,
      phase: part.prepSeconds > 0 ? 'prep' : 'answer',
      remainingSeconds: part.prepSeconds > 0 ? part.prepSeconds : part.answerSeconds,
      running: false,
      finished: false,
    });
  };

  const stopMediaStream = () => {
    mediaStreamRef.current?.getTracks?.().forEach((track) => track.stop());
    mediaStreamRef.current = null;
  };

  const stopMicCheck = () => {
    if (micCheckFrameRef.current) {
      window.cancelAnimationFrame(micCheckFrameRef.current);
      micCheckFrameRef.current = null;
    }
    if (micCheckIntervalRef.current) {
      window.clearInterval(micCheckIntervalRef.current);
      micCheckIntervalRef.current = null;
    }
    if (micCheckTimeoutRef.current) {
      window.clearTimeout(micCheckTimeoutRef.current);
      micCheckTimeoutRef.current = null;
    }
    if (micCheckRecorderRef.current?.state && micCheckRecorderRef.current.state !== 'inactive') {
      micCheckRecorderRef.current.stop();
    }
    micCheckRecorderRef.current = null;
    micCheckStreamRef.current?.getTracks?.().forEach((track) => track.stop());
    micCheckStreamRef.current = null;
    micCheckAnalyserRef.current = null;
    if (micCheckAudioContextRef.current && micCheckAudioContextRef.current.state !== 'closed') {
      micCheckAudioContextRef.current.close().catch(() => {});
    }
    micCheckAudioContextRef.current = null;
    micCheckChunksRef.current = [];
    setMicTesting(false);
    setMicLevel(0);
    setMicCheckCountdown(5);
  };

  const handleStartMicCheck = async () => {
    if (micTesting) {
      stopMicCheck();
      return;
    }
    stopMicCheck();
    if (micCheckPreviewUrl) {
      URL.revokeObjectURL(micCheckPreviewUrl);
      setMicCheckPreviewUrl('');
    }
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: selectedInputDeviceId ? { deviceId: { exact: selectedInputDeviceId } } : true,
      });
      const AudioContextCtor = window.AudioContext || window.webkitAudioContext;
      if (!AudioContextCtor || typeof MediaRecorder === 'undefined') {
        stream.getTracks?.().forEach((track) => track.stop());
        setMicPermissionState('unsupported');
        return;
      }
      const audioContext = new AudioContextCtor();
      if (audioContext.state === 'suspended') {
        await audioContext.resume();
      }
      const analyser = audioContext.createAnalyser();
      analyser.fftSize = 256;
      const source = audioContext.createMediaStreamSource(stream);
      source.connect(analyser);
      const recorder = new MediaRecorder(stream);
      micCheckStreamRef.current = stream;
      micCheckAudioContextRef.current = audioContext;
      micCheckAnalyserRef.current = analyser;
      micCheckRecorderRef.current = recorder;
      micCheckChunksRef.current = [];
      setMicPermissionState('granted');
      setMicCheckPassed(false);
      setMicTesting(true);
      setMicCheckCountdown(5);

      recorder.ondataavailable = (event) => {
        if (event.data?.size) {
          micCheckChunksRef.current.push(event.data);
        }
      };

      recorder.onstop = () => {
        const blob = new Blob(micCheckChunksRef.current, { type: recorder.mimeType || 'audio/webm' });
        micCheckChunksRef.current = [];
        if (micCheckPreviewUrl) {
          URL.revokeObjectURL(micCheckPreviewUrl);
        }
        if (blob.size > 0) {
          setMicCheckPreviewUrl(URL.createObjectURL(blob));
          setMicCheckPassed(true);
        } else {
          setMicCheckPreviewUrl('');
        }
      };

      await refreshMediaDevices();
      const dataArray = new Uint8Array(analyser.fftSize);
      const tick = () => {
        const activeAnalyser = micCheckAnalyserRef.current;
        if (!activeAnalyser) return;
        activeAnalyser.getByteTimeDomainData(dataArray);
        const rms = Math.sqrt(dataArray.reduce((sum, value) => {
          const normalized = (value - 128) / 128;
          return sum + (normalized * normalized);
        }, 0) / Math.max(dataArray.length, 1));
        setMicLevel(Math.min(100, Math.round(rms * 260)));
        micCheckFrameRef.current = window.requestAnimationFrame(tick);
      };
      micCheckFrameRef.current = window.requestAnimationFrame(tick);
      recorder.start();
      micCheckIntervalRef.current = window.setInterval(() => {
        setMicCheckCountdown((current) => (current > 1 ? current - 1 : 0));
      }, 1000);
      micCheckTimeoutRef.current = window.setTimeout(() => {
        stopMicCheck();
      }, 5000);
    } catch {
      setMicPermissionState('denied');
      setMicCheckPassed(false);
      stopMicCheck();
    }
  };

  const handleConfirmMicCheck = () => {
    setMicCheckPassed(true);
    stopMicCheck();
    setSpeakingStage('briefing');
  };

  const handlePlayHeadphoneCheck = async () => {
    try {
      const AudioContextCtor = window.AudioContext || window.webkitAudioContext;
      if (!AudioContextCtor) return;
      if (!headphoneAudioContextRef.current || headphoneAudioContextRef.current.state === 'closed') {
        headphoneAudioContextRef.current = new AudioContextCtor();
      }
      const audioContext = headphoneAudioContextRef.current;
      if (audioContext.state === 'suspended') {
        await audioContext.resume();
      }
      const oscillator = audioContext.createOscillator();
      const gainNode = audioContext.createGain();
      const destination = audioContext.createMediaStreamDestination();
      const audioElement = headphoneTestAudioRef.current;
      if (audioElement && 'setSinkId' in HTMLMediaElement.prototype && selectedOutputDeviceId) {
        try {
          await audioElement.setSinkId(selectedOutputDeviceId);
        } catch {
          // Some browsers do not allow changing the output device.
        }
      }
      oscillator.type = 'sine';
      oscillator.frequency.setValueAtTime(523.25, audioContext.currentTime);
      gainNode.gain.setValueAtTime(0.0001, audioContext.currentTime);
      gainNode.gain.exponentialRampToValueAtTime(0.18, audioContext.currentTime + 0.1);
      gainNode.gain.exponentialRampToValueAtTime(0.0001, audioContext.currentTime + 2.4);
      oscillator.connect(gainNode);
      gainNode.connect(audioContext.destination);
      gainNode.connect(destination);
      if (audioElement) {
        audioElement.srcObject = destination.stream;
        await audioElement.play().catch(() => {});
      }
      oscillator.start();
      oscillator.stop(audioContext.currentTime + 2.5);
      setHeadphoneCheckPlayed(true);
    } catch {
      setRecordingError('Không thể phát âm thanh kiểm tra tai nghe trên trình duyệt hiện tại.');
    }
  };

  const handleNextSpeakingQuestion = () => {
    if (speakingQuestionIndex < activeSpeakingQuestions.length - 1) {
      setSpeakingQuestionIndex((current) => current + 1);
      return;
    }
    const currentPartIndex = activeSpeakingVariant?.parts?.findIndex((part) => part.key === activeSpeakingPartKey) ?? -1;
    const nextPart = currentPartIndex >= 0 ? activeSpeakingVariant?.parts?.[currentPartIndex + 1] : null;
    if (nextPart) {
      setActiveSpeakingPartKey(nextPart.key);
      setSpeakingQuestionIndex(0);
      resetSpeakingTimer(nextPart);
    }
  };

  const handleAdvanceSpeakingFlow = () => {
    if (isFinalSpeakingPrompt) {
      setError('');
      if (isRecording) {
        setPendingSpeakingSubmit(true);
        handleStopRecording();
        return;
      }
      if (uploadingAudio) {
        setPendingSpeakingSubmit(true);
        return;
      }
      handleSubmit();
      return;
    }
    handleNextSpeakingQuestion();
  };

  const handleSpeakingVideoEnded = () => {
    if (!isSpeakingMockFlow || isRecording || uploadingAudio || isSubmissionLocked) return;
    handleStartRecording();
  };

  const handleStartRecording = async () => {
    if (isSubmissionLocked || submitting || isRecording) return;

    try {
      setSpeakingStage('recording');
      setRecordingError('');
      setRecordingDurationSeconds(0);
      recordingDurationRef.current = 0;
    setCompletedRecordingDurationSeconds(0);
    setRecordingPeakLevel(0);
    setRecordingHasVoiceSignal(false);
    stopRecordingMeter();
    setAudioUrl('');
    persistRecoveredSpeakingAudioUrl(selected?.id, '');
    const stream = await navigator.mediaDevices.getUserMedia({
        audio: selectedInputDeviceId ? { deviceId: { exact: selectedInputDeviceId } } : true,
      });
      const mediaRecorder = new MediaRecorder(stream);
      audioChunksRef.current = [];
      mediaStreamRef.current = stream;
      mediaRecorderRef.current = mediaRecorder;
      const AudioContextCtor = window.AudioContext || window.webkitAudioContext;
      if (AudioContextCtor) {
        const audioContext = new AudioContextCtor();
        if (audioContext.state === 'suspended') {
          await audioContext.resume();
        }
        const analyser = audioContext.createAnalyser();
        analyser.fftSize = 256;
        const source = audioContext.createMediaStreamSource(stream);
        source.connect(analyser);
        recordingAudioContextRef.current = audioContext;
        recordingAnalyserRef.current = analyser;
        const dataArray = new Uint8Array(analyser.fftSize);
        const tick = () => {
          const activeAnalyser = recordingAnalyserRef.current;
          if (!activeAnalyser) return;
          activeAnalyser.getByteTimeDomainData(dataArray);
          const rms = Math.sqrt(dataArray.reduce((sum, value) => {
            const normalized = (value - 128) / 128;
            return sum + (normalized * normalized);
          }, 0) / Math.max(dataArray.length, 1));
          const level = Math.min(100, Math.round(rms * 260));
          setRecordingLevel(level);
          setRecordingPeakLevel((current) => Math.max(current, level));
          if (level >= 12) {
            setRecordingHasVoiceSignal(true);
          }
          recordingFrameRef.current = window.requestAnimationFrame(tick);
        };
        recordingFrameRef.current = window.requestAnimationFrame(tick);
      }

      mediaRecorder.ondataavailable = (event) => {
        if (event.data?.size) {
          audioChunksRef.current.push(event.data);
        }
      };

      mediaRecorder.onstop = () => {
        stopRecordingMeter();
        stopMediaStream();
        setCompletedRecordingDurationSeconds(recordingDurationRef.current);
        const blob = new Blob(audioChunksRef.current, { type: mediaRecorder.mimeType || 'audio/webm' });
        if (audioPreviewUrl) {
          URL.revokeObjectURL(audioPreviewUrl);
        }
        if (blob.size > 0) {
          setAudioPreviewUrl(URL.createObjectURL(blob));
        }
        if (blob.size > 0) {
          const mimeType = mediaRecorder.mimeType || 'audio/webm';
          const extension = mimeType.includes('mpeg') ? 'mp3' : mimeType.includes('mp4') ? 'm4a' : mimeType.includes('ogg') ? 'ogg' : mimeType.includes('wav') ? 'wav' : 'webm';
          const file = new File([blob], `speaking-answer.${extension}`, { type: mimeType });
          setUploadingAudio(true);
          courseApi.uploadAssessmentAudio(file)
            .then((uploadResponse) => {
              const uploadedUrl = uploadResponse?.url || '';
              setAudioUrl(uploadedUrl);
              persistRecoveredSpeakingAudioUrl(selected?.id, uploadedUrl);
              setError('');
            })
            .catch(() => {
              setRecordingError('Bạn đã ghi âm xong nhưng hệ thống chưa lưu được bản ghi. Hãy thử ghi lại hoặc dán đường dẫn âm thanh khác.');
            })
            .finally(() => {
              setUploadingAudio(false);
            });
        }
      };

      mediaRecorder.start();
      setIsRecording(true);
    } catch {
      setRecordingError('Không thể truy cập microphone. Hãy kiểm tra quyền dùng mic của trình duyệt.');
      stopMediaStream();
    }
  };

  const handleStopRecording = () => {
    if (!isRecording) return;
    stopRecordingMeter();
    setCompletedRecordingDurationSeconds(recordingDurationRef.current);
    if (mediaRecorderRef.current?.state && mediaRecorderRef.current.state !== 'inactive') {
      mediaRecorderRef.current.stop();
    } else {
      stopMediaStream();
    }
    setIsRecording(false);
  };

  const handleDiscardRecording = () => {
    if (audioPreviewUrl) {
      URL.revokeObjectURL(audioPreviewUrl);
    }
    setAudioPreviewUrl('');
    setAudioUrl('');
    persistRecoveredSpeakingAudioUrl(selected?.id, '');
    setRecordingDurationSeconds(0);
    recordingDurationRef.current = 0;
    setCompletedRecordingDurationSeconds(0);
    setRecordingLevel(0);
    setRecordingPeakLevel(0);
    setRecordingHasVoiceSignal(false);
  };

  return (
    <>
    <section
      className={isFullscreenExamMode ? 'fixed inset-0 z-50 overflow-y-auto bg-[#f8f4f1]' : 'rounded-[28px] border border-[#dfbfbd]/20 bg-white p-6 shadow-sm'}
      onContextMenu={isFullscreenExamMode ? (event) => event.preventDefault() : undefined}
      onCopy={isFullscreenExamMode ? (event) => event.preventDefault() : undefined}
      onCut={isFullscreenExamMode ? (event) => event.preventDefault() : undefined}
      onPaste={isFullscreenExamMode ? (event) => event.preventDefault() : undefined}
    >
      <div className={isFullscreenExamMode ? 'mx-auto min-h-screen max-w-6xl px-4 py-6 md:px-8 md:py-8' : ''}>
      <audio ref={headphoneTestAudioRef} className="hidden" />
      {!isFullscreenExamMode ? (
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="flex items-start gap-4">
          <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-[#fff0f1] text-[#8a0018]">
            <span className="material-symbols-outlined text-[24px]">assignment_turned_in</span>
          </div>
          <div>
            <p className="text-[11px] font-bold uppercase tracking-[0.14em] text-[#8c716f]">Bài kiểm tra</p>
            <h3 className="mt-2 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">Bài kiểm tra chấm tự động</h3>
            <p className="mt-2 max-w-3xl text-sm leading-7 text-[#584140]">
              Bài kiểm tra này thuộc {formatModuleTitle(moduleTitle)}. Hãy hoàn thành bài làm rồi xem nhận xét bên dưới.
            </p>
            {isLocked ? (
              <p className="mt-2 max-w-3xl text-sm font-semibold leading-7 text-[#8a0018]">
                Bài kiểm tra này sẽ mở sau khi bạn hoàn thành toàn bộ bài học trong module.
              </p>
            ) : null}
          </div>
        </div>
      </div>
      ) : (
        <div className="mb-6 flex flex-wrap items-center justify-between gap-3 rounded-[28px] border border-[#dfbfbd]/25 bg-white px-5 py-4 shadow-sm">
          <div>
            <p className="text-[11px] font-bold uppercase tracking-[0.14em] text-[#8c716f]">Chế độ làm bài</p>
            <h3 className="mt-1 font-['Manrope'] text-xl font-extrabold text-[#2b2828]">{formatAssessmentTitle(selected)}</h3>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <div className="rounded-full bg-[#8a0018] px-4 py-2 text-xs font-bold text-white shadow-[0_10px_24px_rgba(75,0,9,0.16)] transition hover:brightness-95">
              Vi phạm đã ghi nhận: {examViolations.length}
            </div>
            <button
              className="rounded-full border border-[#8a0018] bg-white px-4 py-2 text-xs font-bold text-[#8a0018] transition hover:border-[#650012] hover:bg-[#fff0f1] hover:text-[#650012]"
              onClick={() => setExamExitConfirmOpen(true)}
              type="button"
            >
              Thoát bài thi
            </button>
          </div>
        </div>
      )}

      {!isFullscreenExamMode ? (
      <div className="mt-5 grid gap-3">
        {orderedAssessments.map((assessment) => (
          <button
            key={assessment.id}
            className={`rounded-3xl border p-5 text-left transition ${hasMultipleAssessments ? 'hover:border-[#8a0018]/30 hover:bg-[#fff7f7]' : 'cursor-default'} ${String(assessment.id) === String(selected?.id) ? 'border-[#8a0018] bg-[#fff0f1]' : 'border-[#dfbfbd]/30 bg-[#fffdfc]'}`}
            type="button"
            disabled={!hasMultipleAssessments}
            onClick={() => {
              setSelectedId(assessment.id);
              setResult(null);
              setError('');
            }}
          >
            <div className="flex items-center justify-between gap-3">
              <p className="max-w-3xl font-extrabold text-[#2b2828]">{formatAssessmentTitle(assessment)}</p>
              <span className="rounded-full bg-white px-3 py-1 text-[11px] font-bold text-[#8a0018]">{skillLabel(assessment.skill)}</span>
            </div>
            {assessment.moduleTitle ? (
              <p className="mt-2 text-[11px] font-bold uppercase tracking-[0.12em] text-[#8c716f]">
                Thuộc module: {formatModuleTitle(assessment.moduleTitle)}
              </p>
            ) : null}
            <p className="mt-2 text-xs leading-5 text-[#584140]">{formatAssessmentDescription(assessment)}</p>
            <div className="mt-2 flex flex-wrap items-center gap-2 text-[11px] font-bold uppercase tracking-[0.12em] text-[#8c716f]">
              <span>Chế độ: {modeLabel(assessment.aiEvaluationMode)}</span>
              <span>•</span>
              <span>Tiêu chí: {formatRubricName(assessment.rubric?.name, assessment.skill)}</span>
            </div>
          </button>
        ))}
      </div>
      ) : null}

      {selected ? (
        <div className="mt-5 rounded-3xl border border-[#dfbfbd]/25 bg-[#fffdfc] p-5">
          {!isSpeakingMockFlow && !isFullscreenExamMode ? (
            <div className="rounded-2xl border border-[#dfbfbd]/25 bg-white p-4">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <p className="text-sm font-extrabold text-[#2b2828]">Tiêu chí chấm</p>
              <span className="rounded-full bg-[#fff0f1] px-3 py-1 text-[11px] font-bold text-[#8a0018]">
                {formatRubricName(selected.rubric?.name, selected.skill)}
              </span>
            </div>
            {(selected.rubric?.criteria || []).length ? (
              <div className="mt-3 grid gap-3 md:grid-cols-2 xl:grid-cols-4">
                {(selected.rubric?.criteria || []).map((criterion) => (
                  <div key={criterion.id || criterion.name} className="rounded-2xl bg-[#faf7f7] p-4">
                    <p className="text-sm font-bold text-[#4b0009]">{formatCriterionName(criterion.name)} · {criterion.weight}%</p>
                    <p className="mt-2 text-xs leading-5 text-[#584140]">{formatCriterionDescription(criterion.description)}</p>
                  </div>
                ))}
              </div>
            ) : (
              <div className="mt-3 rounded-2xl bg-[#faf7f7] p-4 text-sm leading-6 text-[#584140]">
                Bài này hiện ưu tiên phân tích lỗi, cách làm và hướng ôn tập tiếp theo. Chưa có rubric chi tiết hoặc thang điểm số đủ tin cậy để chấm tự động như Writing/Speaking.
              </div>
            )}
            </div>
          ) : null}

          {showStartExamCard && !isDedicatedExamMode && !result ? (
            <div className="mt-5 rounded-[28px] border border-[#dfbfbd]/25 bg-[linear-gradient(135deg,#fff8f8,#ffffff)] p-6">
              <div className="flex flex-wrap items-center justify-between gap-4">
                <div>
                  <p className="text-[11px] font-black uppercase tracking-[0.18em] text-[#8a0018]">Exam workspace</p>
                  <h4 className="mt-2 font-['Manrope'] text-2xl font-black text-[#2b2828]">{formatAssessmentTitle(selected)}</h4>
                  <p className="mt-3 max-w-2xl text-sm leading-7 text-[#584140]">
                    Khi bắt đầu, bài kiểm tra sẽ chuyển sang chế độ toàn màn hình để người học chỉ tập trung vào phần làm bài. Sau khi nộp thành công, hệ thống sẽ quay lại màn hình tổng quan khóa học.
                  </p>
                </div>
                <button
                  className="rounded-2xl bg-[#8a0018] px-6 py-4 text-sm font-black text-white shadow-[0_16px_34px_rgba(138,0,24,0.22)] transition hover:bg-[#650012] disabled:opacity-60"
                  disabled={isLocked || submitting}
                  onClick={handleOpenExamMode}
                  type="button"
                >
                  {startExamButtonLabel}
                </button>
              </div>
            </div>
          ) : null}

          {!showSpeakingResultOnly && (isFullscreenExamMode || (isDedicatedExamMode && !isLockedAfterResult)) ? (
          <div className="mt-5">
            {!isSpeakingMockFlow ? (
              <label className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-[#8c716f]">{inputCopy.label}</label>
            ) : null}
            {isReadingExamMode ? (
              <div className="rounded-[28px] border border-[#dfbfbd]/30 bg-[linear-gradient(135deg,#fff7f7,#ffffff)] p-6">
                <div className="grid gap-5 lg:grid-cols-[1fr_auto] lg:items-center">
                  <div>
                    <p className="text-[11px] font-black uppercase tracking-[0.18em] text-[#8a0018]">IELTS Reading simulation</p>
                    <h4 className="mt-2 font-['Manrope'] text-2xl font-black text-[#341c1d]">
                      {assessmentUiConfig?.title || 'Reading exam mode'}
                    </h4>
                    <p className="mt-3 max-w-2xl text-sm leading-7 text-[#584140]">
                      Bài Reading sẽ mở trong màn hình thi riêng với passage bên trái, câu hỏi bên phải,
                      đồng hồ đếm giờ và thanh theo dõi câu hỏi ở phía dưới.
                    </p>
                    <div className="mt-4 flex flex-wrap gap-2 text-[11px] font-bold uppercase tracking-[0.12em] text-[#8c716f]">
                      <span>40 questions</span>
                      <span>•</span>
                      <span>{assessmentUiConfig?.durationMinutes || selected.timeLimitMinutes || 60} minutes</span>
                      <span>•</span>
                      <span>Anti copy/paste + focus warning</span>
                    </div>
                  </div>
                  {isLockedAfterResult ? (
                    <span className="inline-flex rounded-2xl bg-[#ebe3e2] px-6 py-4 text-sm font-black text-[#7a6766]">
                      Đã có kết quả
                    </span>
                  ) : (
                    <button
                      className="rounded-2xl bg-[linear-gradient(135deg,#8a0018,#650012)] px-6 py-4 text-sm font-black text-white shadow-[0_16px_34px_rgba(138,0,24,0.22)] transition hover:brightness-105 disabled:opacity-60"
                      disabled={isLocked || submitting}
                      onClick={() => setExamModeOpen(true)}
                      type="button"
                    >
                      Vào phòng thi Reading
                    </button>
                  )}
                </div>
              </div>
            ) : isListeningExamMode ? (
              <div className="rounded-[28px] border border-[#dfbfbd]/30 bg-[linear-gradient(135deg,#fff7f7,#ffffff)] p-6">
                <div className="grid gap-5 lg:grid-cols-[1fr_auto] lg:items-center">
                  <div>
                    <p className="text-[11px] font-black uppercase tracking-[0.18em] text-[#8a0018]">IELTS Listening simulation</p>
                    <h4 className="mt-2 font-['Manrope'] text-2xl font-black text-[#341c1d]">
                      {assessmentUiConfig?.title || 'Listening exam mode'}
                    </h4>
                    <p className="mt-3 max-w-2xl text-sm leading-7 text-[#584140]">
                      Bài Listening sẽ mở trong màn hình thi riêng với audio ở header, câu hỏi theo từng part
                      và thanh tiến độ câu hỏi ở phía dưới.
                    </p>
                    <div className="mt-4 flex flex-wrap gap-2 text-[11px] font-bold uppercase tracking-[0.12em] text-[#8c716f]">
                      <span>40 questions</span>
                      <span>•</span>
                      <span>{assessmentUiConfig?.durationMinutes || selected.timeLimitMinutes || 40} minutes</span>
                      <span>•</span>
                      <span>Audio + anti copy/paste + focus warning</span>
                    </div>
                  </div>
                  {isLockedAfterResult ? (
                    <span className="inline-flex rounded-2xl bg-[#ebe3e2] px-6 py-4 text-sm font-black text-[#7a6766]">
                      Đã có kết quả
                    </span>
                  ) : (
                    <button
                      className="rounded-2xl bg-[linear-gradient(135deg,#8a0018,#650012)] px-6 py-4 text-sm font-black text-white shadow-[0_16px_34px_rgba(138,0,24,0.22)] transition hover:brightness-105 disabled:opacity-60"
                      disabled={isLocked || submitting}
                      onClick={() => setExamModeOpen(true)}
                      type="button"
                    >
                      Vào phòng thi Listening
                    </button>
                  )}
                </div>
              </div>
            ) : isWritingExamMode ? (
              <div className="rounded-[28px] border border-[#dfbfbd]/30 bg-[linear-gradient(135deg,#fff7f7,#ffffff)] p-6">
                <div className="grid gap-5 lg:grid-cols-[1fr_auto] lg:items-center">
                  <div>
                    <p className="text-[11px] font-black uppercase tracking-[0.18em] text-[#8a0018]">IELTS Writing simulation</p>
                    <h4 className="mt-2 font-['Manrope'] text-2xl font-black text-[#341c1d]">
                      {assessmentUiConfig?.title || 'Writing exam mode'}
                    </h4>
                    <p className="mt-3 max-w-2xl text-sm leading-7 text-[#584140]">
                      Bài Writing sẽ mở trong màn hình thi riêng với đề bên trái, khung viết bên phải
                      và chuyển nhanh giữa Task 1, Task 2 ở thanh dưới.
                    </p>
                    <div className="mt-4 flex flex-wrap gap-2 text-[11px] font-bold uppercase tracking-[0.12em] text-[#8c716f]">
                      <span>{(assessmentUiConfig?.tasks || []).length || 2} tasks</span>
                      <span>•</span>
                      <span>{assessmentUiConfig?.durationMinutes || selected.timeLimitMinutes || 60} minutes</span>
                      <span>•</span>
                      <span>Split workspace + anti cheat</span>
                    </div>
                  </div>
                  {isLockedAfterResult ? (
                    <span className="inline-flex rounded-2xl bg-[#ebe3e2] px-6 py-4 text-sm font-black text-[#7a6766]">
                      Đã có kết quả
                    </span>
                  ) : (
                    <button
                      className="rounded-2xl bg-[linear-gradient(135deg,#8a0018,#650012)] px-6 py-4 text-sm font-black text-white shadow-[0_16px_34px_rgba(138,0,24,0.22)] transition hover:brightness-105 disabled:opacity-60"
                      disabled={isLocked || submitting}
                      onClick={() => setExamModeOpen(true)}
                      type="button"
                    >
                      Vào phòng thi Writing
                    </button>
                  )}
                </div>
              </div>
            ) : isObjectiveSkill(selected.skill) && !isDedicatedExamMode ? (
              <div className="space-y-4">
                {(objectiveSections[selected.skill] || []).map((section) => (
                  <div key={section.key} className="rounded-2xl border border-[#dfbfbd]/30 bg-white p-4">
                    <div className="flex flex-wrap items-center justify-between gap-3">
                      <p className="text-sm font-extrabold text-[#2b2828]">{section.label}</p>
                      <span className="rounded-full bg-[#fff0f1] px-3 py-1 text-[11px] font-bold text-[#8a0018]">
                        Câu {section.from}-{section.to}
                      </span>
                    </div>
                    <div className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
                      {objectiveDraft.responses
                        .filter((entry) => Number(entry.questionNumber) >= section.from && Number(entry.questionNumber) <= section.to)
                        .map((entry) => (
                          <label key={entry.id} className="rounded-2xl border border-[#dfbfbd]/25 bg-[#fffdfc] p-3">
                            <span className="text-[11px] font-bold uppercase tracking-[0.12em] text-[#8c716f]">Q{entry.questionNumber}</span>
                            <input
                              className={`mt-2 w-full rounded-xl border px-3 py-2 text-sm outline-none transition ${isSubmissionLocked ? 'border-[#ebe3e2] bg-[#f7f3f2] text-[#7a6766]' : 'border-[#dfbfbd]/60 bg-white focus:border-[#8a0018]'}`}
                              value={entry.answer}
                              onChange={(event) => handleObjectiveResponseChange(entry.id, event.target.value)}
                              readOnly={isSubmissionLocked || submitting}
                              placeholder={selected.skill === 'LISTENING' ? 'A / word' : 'T / F / NG'}
                            />
                          </label>
                        ))}
                    </div>
                    <textarea
                      className={`mt-4 min-h-[96px] w-full rounded-2xl border px-4 py-3 text-sm leading-6 outline-none transition ${isSubmissionLocked ? 'border-[#ebe3e2] bg-[#f7f3f2] text-[#7a6766]' : 'border-[#dfbfbd]/60 bg-white focus:border-[#8a0018]'}`}
                      value={objectiveDraft.sectionNotes?.[section.key] || ''}
                      onChange={(event) => setObjectiveDraft((current) => ({
                        ...current,
                        sectionNotes: {
                          ...current.sectionNotes,
                          [section.key]: event.target.value,
                        },
                      }))}
                      readOnly={isSubmissionLocked || submitting}
                      placeholder={selected.skill === 'LISTENING'
                        ? `Ghi chú lỗi cho ${section.label}: keyword bỏ lỡ, distractor, signposting, timestamp...`
                        : `Ghi chú lỗi cho ${section.label}: evidence trong passage, keyword, bẫy đề, lý do sai...`}
                    />
                  </div>
                ))}
                <div>
                  <label className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-[#8c716f]">
                    Nhận xét tổng quát
                  </label>
                  <textarea
                    className={`min-h-[120px] w-full rounded-2xl border px-4 py-3 text-sm leading-7 outline-none transition ${isSubmissionLocked ? 'border-[#ebe3e2] bg-[#f7f3f2] text-[#7a6766]' : 'border-[#dfbfbd]/60 bg-white focus:border-[#8a0018]'}`}
                    value={objectiveDraft.overallNotes}
                    onChange={(event) => setObjectiveDraft((current) => ({ ...current, overallNotes: event.target.value }))}
                    readOnly={isSubmissionLocked || submitting}
                    placeholder={inputCopy.placeholder}
                  />
                </div>
              </div>
            ) : (
              <div className="space-y-4">
                {selected.skill === 'SPEAKING' ? (
                  <>
                    <div className="rounded-2xl border border-[#dfbfbd]/30 bg-white p-4">
                      <div className="flex flex-wrap items-center gap-2">
                        {[
                          ['mic_check', '1. Kiểm tra micro'],
                          ['briefing', '2. Hướng dẫn'],
                          ['test', '3. Xem đề'],
                          ['recording', '4. Ghi âm'],
                        ].map(([stageKey, stageLabel]) => (
                          <span
                            key={stageKey}
                            className={`rounded-full px-3 py-2 text-xs font-bold ${speakingStage === stageKey ? 'bg-[#8a0018] text-white' : 'bg-[#faf7f7] text-[#8c716f]'}`}
                          >
                            {stageLabel}
                          </span>
                        ))}
                      </div>
                    </div>

                    {speakingStage === 'mic_check' ? (
                      <div className="rounded-[30px] border border-[#dfbfbd]/25 bg-white p-6">
                        <h4 className="text-center font-['Manrope'] text-2xl font-extrabold text-[#21446d]">IELTS Speaking Test</h4>
                        <div className="mt-8 space-y-8">
                          <div className="grid gap-5 md:grid-cols-[60px_1fr]">
                            <div className="flex items-start justify-center">
                              <div className="flex h-12 w-12 items-center justify-center rounded-full border border-[#8a0018]/25 text-[#8a0018]">
                                <span className="material-symbols-outlined text-[24px]">headphones</span>
                              </div>
                            </div>
                            <div>
                              <p className="text-2xl font-extrabold text-[#21446d]"><span className="mr-2 text-[#21446d]/55">1.</span>Headphone check</p>
                              <p className="mt-3 text-sm leading-7 text-[#584140]">
                                Hãy thử phát âm thanh mẫu để chắc rằng tai nghe hoặc loa của bạn nghe rõ trước khi bắt đầu bài thi.
                              </p>
                              <div className="mt-5 flex flex-wrap items-center gap-4 rounded-[24px] border border-[#dfbfbd]/40 bg-[#fffdfc] px-5 py-5">
                                <button
                                  className="flex h-14 w-14 items-center justify-center rounded-full bg-[linear-gradient(135deg,#8a0018,#650012)] text-white shadow-[0_10px_24px_rgba(75,0,9,0.24)] transition hover:-translate-y-0.5 hover:brightness-105"
                                  type="button"
                                  onClick={handlePlayHeadphoneCheck}
                                >
                                  <span className="material-symbols-outlined">play_arrow</span>
                                </button>
                                <div className="min-w-[220px] flex-1">
                                  <div className="h-2 rounded-full bg-[#f3d7dd]">
                                    <div
                                      className="h-full rounded-full bg-[linear-gradient(90deg,#8a0018,#b4233f)] transition-all"
                                      style={{ width: headphoneCheckPlayed ? '100%' : '0%' }}
                                    />
                                  </div>
                                </div>
                                <span className="text-sm font-semibold text-[#7a6766]">{headphoneCheckPlayed ? '00:08' : '00:00'}</span>
                                <div className="min-w-[220px]">
                                  <BrandedSelect
                                    buttonClassName="min-w-[220px] border-[#dfbfbd]/50 py-3 text-sm font-medium text-[#584140] shadow-none"
                                    onChange={(event) => setSelectedOutputDeviceId(event.target.value)}
                                    options={availableOutputDevices.length
                                      ? availableOutputDevices.map((device, index) => ({
                                        label: device.label || `Loa / tai nghe ${index + 1}`,
                                        value: device.deviceId,
                                      }))
                                      : [{ label: 'Thiết bị mặc định', value: '' }]}
                                    value={selectedOutputDeviceId}
                                  />
                                </div>
                              </div>
                            </div>
                          </div>

                          <div className="grid gap-5 md:grid-cols-[60px_1fr]">
                            <div className="flex items-start justify-center">
                              <div className="flex h-12 w-12 items-center justify-center rounded-full border border-[#8a0018]/25 text-[#8a0018]">
                                <span className="material-symbols-outlined text-[24px]">mic</span>
                              </div>
                            </div>
                            <div>
                              <p className="text-2xl font-extrabold text-[#21446d]"><span className="mr-2 text-[#21446d]/55">2.</span>Microphone check</p>
                              <p className="mt-3 text-sm leading-7 text-[#584140]">
                                Hãy bấm kiểm tra micro và đọc to câu sau để xem tiếng thu vào có ổn định hay không.
                              </p>
                              <p className="mt-5 text-center text-base font-semibold leading-8 text-[#8c716f]">
                                Please read out loud:
                                <br />
                                “I love English. My English is great and I practice it everyday!”
                              </p>
                              <div className="mt-5 flex flex-wrap items-center gap-4 rounded-[24px] border border-[#dfbfbd]/40 bg-[#fffdfc] px-5 py-5">
                                <button
                                  className={`flex h-14 items-center gap-2 rounded-full px-5 text-sm font-extrabold transition ${micTesting ? 'bg-[#fff0f1] text-[#8a0018] hover:bg-[#ffe5e8]' : 'bg-[linear-gradient(135deg,#8a0018,#650012)] text-white shadow-[0_10px_24px_rgba(75,0,9,0.24)] hover:-translate-y-0.5 hover:brightness-105'}`}
                                  type="button"
                                  onClick={handleStartMicCheck}
                                >
                                  <span className="material-symbols-outlined">{micTesting ? 'radio_button_checked' : 'mic'}</span>
                                  {micTesting ? `Đang ghi thử ${micCheckCountdown}s` : 'Bắt đầu test mic'}
                                </button>
                                <div className="min-w-[220px] flex-1">
                                  <div className="h-2 rounded-full bg-[#f3d7dd]">
                                    <div
                                      className="h-full rounded-full bg-[linear-gradient(90deg,#8a0018,#b4233f)] transition-all"
                                      style={{ width: `${micLevel}%` }}
                                    />
                                  </div>
                                </div>
                                <span className="text-sm font-semibold text-[#7a6766]">{micLevel}%</span>
                                <div className="min-w-[220px]">
                                  <BrandedSelect
                                    buttonClassName="min-w-[220px] border-[#dfbfbd]/50 py-3 text-sm font-medium text-[#584140] shadow-none"
                                    onChange={(event) => setSelectedInputDeviceId(event.target.value)}
                                    options={availableInputDevices.length
                                      ? availableInputDevices.map((device, index) => ({
                                        label: device.label || `Micro ${index + 1}`,
                                        value: device.deviceId,
                                      }))
                                      : [{ label: 'Micro mặc định', value: '' }]}
                                    value={selectedInputDeviceId}
                                  />
                                </div>
                              </div>
                              {micCheckPreviewUrl ? (
                                <div className="mt-4 rounded-[24px] border border-[#dfbfbd]/40 bg-[#fff7f7] p-4">
                                  <p className="text-sm font-bold text-[#4b0009]">Bản ghi thử 5 giây đã sẵn sàng. Bạn nghe lại trực tiếp ở đây để kiểm tra mic.</p>
                                  <audio className="mt-3 w-full" controls src={micCheckPreviewUrl} />
                                </div>
                              ) : null}
                              <p className="mt-3 text-sm leading-7 text-[#7a6766]">
                                {micPermissionState === 'idle' && 'Bấm Bắt đầu test mic để cấp quyền micro, ghi thử 5 giây và nghe lại ngay trên thiết bị của bạn.'}
                                {micPermissionState === 'granted' && (micCheckPassed
                                  ? 'Micro đã ghi thử xong. Nếu nghe lại rõ và thanh tín hiệu nhảy ổn, bạn có thể tiếp tục.'
                                  : 'Micro đang được ghi thử trong 5 giây. Hãy đọc câu mẫu thật rõ để kiểm tra chất lượng thu âm.')}
                                {micPermissionState === 'denied' && 'Trình duyệt chưa được cấp quyền dùng micro. Hãy bật quyền micro rồi thử lại.'}
                                {micPermissionState === 'unsupported' && 'Trình duyệt hiện không hỗ trợ bước kiểm tra này. Hãy thử trên Chrome hoặc Edge.'}
                              </p>
                            </div>
                          </div>

                          <div className="grid gap-5 md:grid-cols-[60px_1fr]">
                            <div className="flex items-start justify-center">
                              <div className={`flex h-12 w-12 items-center justify-center rounded-full border ${headphoneCheckPlayed && micCheckPassed ? 'border-[#8a0018]/25 bg-[#fff0f1] text-[#8a0018]' : 'border-[#f2d6dc] text-[#e3b0bb]'}`}>
                                <span className="material-symbols-outlined text-[24px]">{headphoneCheckPlayed && micCheckPassed ? 'check_circle' : 'hourglass_empty'}</span>
                              </div>
                            </div>
                            <div>
                              <p className={`text-2xl font-extrabold ${headphoneCheckPlayed && micCheckPassed ? 'text-[#8a0018]' : 'text-[#a9b4c2]'}`}>
                                <span className={`mr-2 ${headphoneCheckPlayed && micCheckPassed ? 'text-[#8a0018]/60' : 'text-[#a9b4c2]/70'}`}>3.</span>
                                Waiting room
                              </p>
                              <p className={`mt-3 text-sm leading-7 ${headphoneCheckPlayed && micCheckPassed ? 'text-[#584140]' : 'text-[#b2a6a7]'}`}>
                                Khi thiết bị đã sẵn sàng, bạn có thể vào phòng thi mô phỏng để bắt đầu từng phần của bài Speaking.
                              </p>
                            </div>
                          </div>
                        </div>

                        <div className="mt-8 flex justify-end">
                          <button
                            className="rounded-full bg-[linear-gradient(135deg,#8a0018,#650012)] px-6 py-3 text-sm font-extrabold text-white shadow-[0_10px_24px_rgba(75,0,9,0.24)] transition hover:-translate-y-0.5 hover:brightness-105 disabled:opacity-50"
                            type="button"
                            onClick={handleConfirmMicCheck}
                            disabled={!headphoneCheckPlayed || micPermissionState !== 'granted' || !micCheckPassed}
                          >
                            Continue to test
                          </button>
                        </div>
                      </div>
                    ) : null}

                    {speakingStage === 'briefing' ? (
                      <div className="rounded-[28px] border border-[#dfbfbd]/30 bg-white p-5">
                        <p className="text-xs font-bold uppercase tracking-[0.16em] text-[#8c716f]">Bước 2</p>
                        <h4 className="mt-2 text-xl font-extrabold text-[#2b2828]">
                          {speakingExperience?.briefing?.title || 'Hướng dẫn làm bài Speaking'}
                        </h4>
                        <p className="mt-3 max-w-3xl text-sm leading-7 text-[#584140]">
                          {speakingExperience?.briefing?.summary || 'Đọc nhanh hướng dẫn rồi chuyển sang phần đề trước khi bắt đầu ghi âm.'}
                        </p>
                        <div className="mt-5 grid gap-3 md:grid-cols-3">
                          <div className="rounded-2xl bg-[#faf7f7] p-4 text-sm leading-6 text-[#584140]">
                            Kiểm tra kỹ micro và nơi ngồi để tiếng nói đủ rõ, ít tạp âm.
                          </div>
                          <div className="rounded-2xl bg-[#faf7f7] p-4 text-sm leading-6 text-[#584140]">
                            Với Part 2, nên dùng 1 phút chuẩn bị để chốt ý chính chứ không viết thành bài hoàn chỉnh.
                          </div>
                          <div className="rounded-2xl bg-[#faf7f7] p-4 text-sm leading-6 text-[#584140]">
                            Hãy nói tự nhiên như đang thi thật. Phần chấm ưu tiên bản ghi âm, không phải một đoạn text soạn sẵn.
                          </div>
                        </div>
                        <div className="mt-5 flex flex-wrap gap-3">
                          <button
                            className="rounded-2xl bg-[#8a0018] px-5 py-3 text-sm font-extrabold text-white transition hover:bg-[#650012]"
                            type="button"
                            onClick={() => setSpeakingStage('test')}
                          >
                            Bắt đầu kiểm tra
                          </button>
                          <button
                            className="rounded-2xl border border-[#8a0018]/20 px-5 py-3 text-sm font-bold text-[#8a0018] transition hover:bg-[#fff0f1]"
                            type="button"
                            onClick={() => setSpeakingStage('mic_check')}
                          >
                            Kiểm tra micro lại
                          </button>
                        </div>
                      </div>
                    ) : null}

                    {(speakingStage === 'test' || speakingStage === 'recording') && speakingExperience?.kind === 'mock_test' ? (
                      <div className="overflow-hidden rounded-[30px] border border-[#dfbfbd]/25 bg-white">
                        <div className="flex items-center justify-between border-b border-[#f0e6e6] px-6 py-4">
                          <div className="flex flex-wrap gap-2">
                            {(speakingExperience.variants || []).map((variant) => (
                              <button
                                key={variant.key}
                                className={`rounded-full px-4 py-2 text-sm font-bold transition ${variant.key === activeSpeakingVariant?.key ? 'bg-[#8a0018] text-white' : 'bg-[#faf7f7] text-[#7a6766]'}`}
                                type="button"
                                onClick={() => setSelectedSpeakingMockKey(variant.key)}
                              >
                                {variant.label}
                              </button>
                            ))}
                          </div>
                          <p className="font-['Manrope'] text-3xl font-extrabold text-[#8a0018]">
                            {formatSeconds(
                              speakingTimer.partKey === activeSpeakingPart?.key && speakingTimer.remainingSeconds > 0
                                ? speakingTimer.remainingSeconds
                                : activeSpeakingPart?.answerSeconds || 0
                            )}
                            <span className="ml-1 text-sm font-medium text-[#2b2828]">minutes remaining</span>
                          </p>
                        </div>

                        <div className="px-6 py-8 text-center">
                          <p className="text-3xl font-extrabold text-[#21446d]">
                            {activeSpeakingPart?.label?.toUpperCase()}
                            <span className="font-medium text-[#2b2828]">: {activeSpeakingPart?.caption}</span>
                          </p>

                          <div className="mx-auto mt-8 max-w-[430px]">
                            {activeSpeakingVideoUrl ? (
                              <video
                                ref={speakingVideoRef}
                                key={activeSpeakingVideoUrl}
                                className="h-[250px] w-full rounded-[10px] object-cover"
                                autoPlay
                                controls={false}
                                controlsList="nodownload noplaybackrate noremoteplayback nofullscreen"
                                disablePictureInPicture
                                onEnded={handleSpeakingVideoEnded}
                                onContextMenu={(event) => event.preventDefault()}
                                playsInline
                                preload="auto"
                                src={activeSpeakingVideoUrl}
                              />
                            ) : (
                              <div className="flex h-[250px] w-full items-center justify-center rounded-[10px] bg-[linear-gradient(135deg,#eef2f8,#fbfcfe)]">
                                <div className="text-center">
                                  <div className="mx-auto flex h-24 w-24 items-center justify-center rounded-full bg-white text-[#cf6f83] shadow-[0_14px_40px_rgba(207,111,131,0.18)]">
                                    <span className="material-symbols-outlined text-[42px]">person</span>
                                  </div>
                                  <p className="mt-5 text-sm font-semibold text-[#7a6766]">Examiner video sẽ hiển thị ở đây khi đề có kèm video.</p>
                                </div>
                              </div>
                            )}
                          </div>

                          <div className="mx-auto mt-6 max-w-3xl">
                            {activeSpeakingPart?.cueCardTitle ? (
                              <div className="rounded-[24px] border border-[#efd9de] bg-[#fffdfc] p-5 text-left">
                                <p className="text-xs font-bold uppercase tracking-[0.16em] text-[#8c716f]">Cue card</p>
                                <h5 className="mt-2 text-xl font-extrabold text-[#2b2828]">{activeSpeakingPart.cueCardTitle}</h5>
                                <div className="mt-4 grid gap-3 md:grid-cols-2">
                                  {(activeSpeakingPart.cueCardBullets || []).map((bullet) => (
                                    <div key={bullet} className="rounded-2xl bg-[#faf7f7] px-4 py-3 text-sm font-semibold text-[#4b0009]">
                                      {bullet}
                                    </div>
                                  ))}
                                </div>
                              </div>
                            ) : null}
                          </div>

                          <div className="mx-auto mt-8 max-w-[560px]">
                            <div className="relative flex items-center justify-center">
                              <div className="absolute left-0 right-0 flex items-center justify-between gap-[2px] px-3">
                                <div className="flex h-10 items-center gap-[2px]">
                                  {speakingMeterBars.map((height, index) => (
                                    <span
                                      key={`left-${index}`}
                                      className={`w-[2px] rounded-full transition-all duration-150 ${isRecording ? 'bg-[#8a0018]' : 'bg-[#dfbfbd]'}`}
                                      style={{ height }}
                                    />
                                  ))}
                                </div>
                                <div className="w-20 shrink-0" />
                                <div className="flex h-10 items-center gap-[2px]">
                                  {[...speakingMeterBars].reverse().map((height, index) => (
                                    <span
                                      key={`right-${index}`}
                                      className={`w-[2px] rounded-full transition-all duration-150 ${isRecording ? 'bg-[#8a0018]' : 'bg-[#dfbfbd]'}`}
                                      style={{ height }}
                                    />
                                  ))}
                                </div>
                              </div>
                            <div
                              className={`relative z-10 flex h-16 w-16 items-center justify-center rounded-full border-4 border-white mx-auto shadow-[0_12px_30px_rgba(75,0,9,0.16)] ${isRecording ? 'bg-[#8a0018] text-white' : 'bg-white text-[#8a0018]'}`}
                              aria-hidden="true"
                            >
                              <span className="material-symbols-outlined text-[34px]">{isRecording ? 'mic' : 'mic'}</span>
                            </div>
                            </div>
                            <p className="mt-4 font-['Manrope'] text-2xl font-extrabold text-[#8a0018]">{formatSeconds(recordingDurationSeconds)}</p>
                          </div>

                          <div className="mt-6 flex flex-wrap justify-center gap-3">
                            <button
                              className="rounded-full bg-[linear-gradient(135deg,#8a0018,#650012)] px-6 py-3 text-base font-extrabold text-white shadow-[0_10px_24px_rgba(75,0,9,0.18)] transition hover:brightness-95 disabled:opacity-60"
                              disabled={submitting || pendingSpeakingSubmit}
                              type="button"
                              onClick={handleAdvanceSpeakingFlow}
                            >
                              {isFinalSpeakingPrompt
                                ? (submitting || pendingSpeakingSubmit ? 'Đang gửi...' : 'Nộp bài')
                                : speakingQuestionIndex < activeSpeakingQuestions.length - 1 ? 'Next question' : 'Next part'}
                            </button>
                          </div>
                        </div>

                        <div className="grid gap-4 border-t border-[#f0e6e6] px-6 py-4 md:grid-cols-3">
                          {(activeSpeakingVariant?.parts || []).map((part) => (
                            <button
                              key={part.key}
                              className={`rounded-[18px] border px-5 py-4 text-center text-xl font-extrabold transition ${part.key === activeSpeakingPartKey ? 'border-[#8a0018] text-[#21446d]' : 'border-[#dfe8e0] text-[#21446d]'}`}
                              type="button"
                              onClick={() => {
                                setActiveSpeakingPartKey(part.key);
                                setSpeakingQuestionIndex(0);
                              }}
                            >
                              {part.label}
                            </button>
                          ))}
                        </div>
                      </div>
                    ) : (speakingStage === 'test' || speakingStage === 'recording') && speakingExperience?.kind === 'topic_bank' ? (
                      <div className="rounded-2xl border border-[#dfbfbd]/30 bg-white p-4">
                        <div className="flex flex-wrap items-start justify-between gap-3">
                          <div>
                            <p className="text-sm font-extrabold text-[#2b2828]">Part 1 Speaking Topic Bank</p>
                            <p className="mt-2 text-sm leading-6 text-[#584140]">
                              Module này thiên về luyện phản xạ theo chủ đề. Chọn một topic rồi ghi âm liên tiếp 3-4 câu hỏi như lúc warm-up trong IELTS Speaking.
                            </p>
                          </div>
                          <span className="rounded-full bg-[#fff0f1] px-3 py-1 text-[11px] font-bold text-[#8a0018]">
                            {speakingExperience.sourceLabel}
                          </span>
                        </div>
                        <div className="mt-4 grid gap-3 lg:grid-cols-2">
                          {(speakingExperience.topics || []).map((topic) => (
                            <div key={topic.title} className="rounded-[24px] border border-[#dfbfbd]/25 bg-[linear-gradient(145deg,#fffdfc,#fff6f6)] p-4">
                              <p className="text-sm font-extrabold text-[#2b2828]">{topic.title}</p>
                              <div className="mt-3 space-y-2">
                                {topic.prompts.map((prompt, index) => (
                                  <div key={`${topic.title}-${index}`} className="rounded-2xl bg-white px-4 py-3 text-sm leading-6 text-[#584140]">
                                    <span className="mr-2 font-bold text-[#8a0018]">{index + 1}.</span>
                                    {prompt}
                                  </div>
                                ))}
                              </div>
                            </div>
                          ))}
                        </div>
                        <div className="mt-5 flex flex-wrap gap-3">
                          <button
                            className="rounded-2xl bg-[#8a0018] px-5 py-3 text-sm font-extrabold text-white transition hover:bg-[#650012]"
                            type="button"
                            onClick={() => setSpeakingStage('recording')}
                          >
                            Bắt đầu ghi âm trả lời
                          </button>
                          <button
                            className="rounded-2xl border border-[#8a0018]/20 px-5 py-3 text-sm font-bold text-[#8a0018] transition hover:bg-[#fff0f1]"
                            type="button"
                            onClick={() => setSpeakingStage('briefing')}
                          >
                            Quay lại hướng dẫn
                          </button>
                        </div>
                      </div>
                    ) : null}

                    {speakingStage === 'recording' && !isSpeakingMockFlow ? (
                    <div className="rounded-2xl border border-[#dfbfbd]/30 bg-white p-4">
                      <div className="flex flex-wrap items-center gap-3">
                        {!isRecording ? (
                          <button
                            className="rounded-2xl bg-[#8a0018] px-4 py-2 text-sm font-extrabold text-white transition hover:bg-[#650012] disabled:opacity-50"
                            type="button"
                            onClick={handleStartRecording}
                            disabled={isSubmissionLocked || submitting || uploadingAudio}
                          >
                            Bắt đầu ghi âm
                          </button>
                        ) : (
                          <button
                            className="rounded-2xl bg-[#2b2828] px-4 py-2 text-sm font-extrabold text-white transition hover:bg-[#8a0018]"
                            type="button"
                            onClick={handleStopRecording}
                          >
                            Dừng ghi âm
                          </button>
                        )}
                        {audioPreviewUrl ? (
                          <button
                            className="rounded-2xl border border-[#8a0018]/20 px-4 py-2 text-sm font-bold text-[#8a0018] transition hover:bg-[#fff0f1]"
                            type="button"
                            onClick={handleDiscardRecording}
                            disabled={isSubmissionLocked || submitting || uploadingAudio}
                          >
                            Xóa bản ghi
                          </button>
                        ) : null}
                        <span className={`text-sm font-semibold ${isRecording ? 'text-[#8a0018]' : 'text-[#7a6766]'}`}>
                          {isRecording ? 'Đang ghi âm để làm bài IELTS Speaking.' : uploadingAudio ? 'Đang lưu bản ghi của bạn...' : 'Bản ghi âm là phần chính của bài Speaking.'}
                        </span>
                      </div>

                      <div className="mt-4 overflow-hidden rounded-[28px] border border-[#dfbfbd]/30 bg-[radial-gradient(circle_at_top,_rgba(138,0,24,0.12),_transparent_55%),linear-gradient(135deg,#fff7f7,#fffdfc)] p-5">
                        <div className="flex h-24 items-end justify-center gap-2">
                          {waveformBars.map((height, index) => (
                            <span
                              key={`${height}-${index}`}
                              className={`w-2 rounded-full transition-all duration-300 ${isRecording ? 'animate-pulse bg-[#8a0018] shadow-[0_0_16px_rgba(138,0,24,0.25)]' : uploadingAudio ? 'animate-bounce bg-[#c85a6d]' : audioPreviewUrl ? 'bg-[#8a0018]/75' : 'bg-[#dfbfbd]'}`}
                              style={{
                                height,
                                animationDelay: `${index * 90}ms`,
                                opacity: isRecording || uploadingAudio || audioPreviewUrl ? 1 : 0.55,
                                transform: isRecording ? `scaleY(${1 + ((index % 4) * 0.08)})` : 'scaleY(1)',
                              }}
                            />
                          ))}
                        </div>
                        <div className="mt-4 flex flex-wrap items-center justify-between gap-3 text-sm">
                          <span className="font-bold text-[#4b0009]">
                            {isRecording ? 'Mic đang mở, hãy nói tự nhiên như lúc thi Speaking.' : uploadingAudio ? 'Đang xử lý bản ghi, bạn chờ trong giây lát nhé.' : audioPreviewUrl ? 'Bản ghi đã sẵn sàng, bạn có thể nghe lại trước khi nộp.' : 'Nhấn bắt đầu ghi âm để tạo bài nói.'}
                          </span>
                          <span className="rounded-full bg-white/80 px-3 py-1 font-semibold text-[#8c716f]">
                            {audioUrl ? 'Bản ghi đã sẵn sàng' : audioPreviewUrl ? 'Đã ghi âm' : 'Chưa ghi âm'}
                          </span>
                        </div>
                      </div>

                      {audioPreviewUrl ? (
                        <audio className="mt-4 w-full" controls src={audioPreviewUrl} />
                      ) : null}

                      {recordingError ? (
                        <p className="mt-3 text-sm font-semibold text-[#93000a]">{recordingError}</p>
                      ) : null}

                      {audioUrl ? (
                        <p className="mt-3 text-sm leading-6 text-[#584140]">
                          Bản ghi đã sẵn sàng để gửi chấm. Hệ thống đã lưu tại:
                          {' '}
                          <span className="font-semibold text-[#8a0018]">{audioUrl}</span>
                        </p>
                      ) : null}
                    </div>
                    ) : null}

                  </>
                ) : selected.skill === 'VOCABULARY' ? (
                  <div className="overflow-hidden rounded-[30px] border border-[#dfbfbd]/35 bg-[radial-gradient(circle_at_top_left,_rgba(138,0,24,0.12),_transparent_34%),linear-gradient(135deg,#fff9f8,#ffffff)] shadow-[0_18px_50px_rgba(75,0,9,0.06)]">
                    <div className="grid gap-5 border-b border-[#f0e2e2] p-5 lg:grid-cols-[1fr_320px]">
                      <div>
                        <div className="flex flex-wrap items-center gap-2">
                          <span className="rounded-full bg-[#8a0018] px-3 py-1 text-[11px] font-black uppercase tracking-[0.14em] text-white">
                            Vocabulary output
                          </span>
                          <span className="rounded-full bg-white px-3 py-1 text-[11px] font-bold text-[#8a0018] ring-1 ring-[#dfbfbd]/50">
                            AI checks meaning + collocation
                          </span>
                        </div>
                        <h4 className="mt-4 font-['Manrope'] text-2xl font-black text-[#2b2828]">
                          Viết câu để chứng minh bạn thật sự dùng được từ
                        </h4>
                        <p className="mt-3 max-w-2xl text-sm leading-7 text-[#584140]">
                          Hãy viết 5-7 câu ngắn, mỗi câu nên có ngữ cảnh rõ ràng. Bài chấm sẽ tập trung vào nghĩa, collocation,
                          độ tự nhiên và mức độ bám chủ đề của module.
                        </p>
                      </div>

                      <div className="rounded-[24px] border border-[#dfbfbd]/40 bg-white/85 p-4">
                        <div className="flex items-center justify-between gap-3">
                          <p className="text-xs font-black uppercase tracking-[0.14em] text-[#8c716f]">Độ sẵn sàng</p>
                          <span className="font-['Manrope'] text-2xl font-black text-[#8a0018]">{vocabularyReadinessPercent}%</span>
                        </div>
                        <div className="mt-3 h-2 overflow-hidden rounded-full bg-[#f1dce0]">
                          <div
                            className="h-full rounded-full bg-[linear-gradient(90deg,#8a0018,#c62845)] transition-all duration-300"
                            style={{ width: `${vocabularyReadinessPercent}%` }}
                          />
                        </div>
                        <div className="mt-4 grid gap-2">
                          {vocabularyChecks.map((check) => (
                            <div key={check.label} className="flex items-center justify-between gap-3 rounded-2xl bg-[#fff7f7] px-3 py-2">
                              <span className={`text-sm font-bold ${check.done ? 'text-[#4b0009]' : 'text-[#8c716f]'}`}>
                                {check.done ? '✓' : '•'} {check.label}
                              </span>
                              <span className="text-xs font-semibold text-[#8c716f]">{check.hint}</span>
                            </div>
                          ))}
                        </div>
                      </div>
                    </div>

                    <div className="grid gap-5 p-5 lg:grid-cols-[1fr_300px]">
                      <div>
                        <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
                          <label className="text-xs font-black uppercase tracking-[0.14em] text-[#8c716f]">
                            Câu trả lời của bạn
                          </label>
                          <div className="flex flex-wrap gap-2">
                            <span className="rounded-full bg-[#fff0f1] px-3 py-1 text-xs font-bold text-[#8a0018]">
                              {vocabularySentenceCount} câu
                            </span>
                            <span className="rounded-full bg-[#fff0f1] px-3 py-1 text-xs font-bold text-[#8a0018]">
                              {vocabularyWordCount} từ
                            </span>
                          </div>
                        </div>
                        <textarea
                          className={`min-h-[280px] w-full resize-y rounded-[26px] border px-5 py-4 text-base leading-8 shadow-inner outline-none transition ${isSubmissionLocked ? 'border-[#ebe3e2] bg-[#f7f3f2] text-[#7a6766]' : 'border-[#8a0018]/70 bg-white focus:border-[#8a0018] focus:shadow-[0_0_0_4px_rgba(138,0,24,0.08)]'}`}
                          value={answer}
                          onChange={(event) => setAnswer(event.target.value)}
                          readOnly={isSubmissionLocked || submitting}
                          placeholder="Ví dụ: The new policy has a significant impact on students because it encourages them to manage their time more effectively..."
                        />
                        <p className="mt-3 text-sm leading-6 text-[#7a6766]">
                          Mẹo nhỏ: đừng chỉ liệt kê từ vựng. Hãy đặt từ vào tình huống cụ thể để AI kiểm tra được bạn dùng đúng nghĩa hay chưa.
                        </p>
                      </div>

                      <aside className="space-y-3">
                        <div className="rounded-[24px] border border-[#dfbfbd]/35 bg-white p-4">
                          <p className="text-xs font-black uppercase tracking-[0.14em] text-[#8c716f]">Checklist trước khi nộp</p>
                          <div className="mt-4 space-y-3 text-sm leading-6 text-[#584140]">
                            <p><span className="font-black text-[#8a0018]">1.</span> Mỗi câu có ít nhất một từ/cụm từ mục tiêu.</p>
                            <p><span className="font-black text-[#8a0018]">2.</span> Có collocation tự nhiên, không dịch từng chữ từ tiếng Việt.</p>
                            <p><span className="font-black text-[#8a0018]">3.</span> Câu đủ ngữ cảnh để người đọc hiểu tình huống.</p>
                            <p><span className="font-black text-[#8a0018]">4.</span> Ưu tiên câu rõ ràng hơn câu quá dài và rối.</p>
                          </div>
                        </div>

                        <div className="rounded-[24px] border border-[#f0d7a6] bg-[#fffaf0] p-4">
                          <p className="text-xs font-black uppercase tracking-[0.14em] text-[#9b6400]">Chế độ thi</p>
                          <p className="mt-3 text-sm leading-6 font-semibold text-[#7a4e00]">
                            Hệ thống vẫn ghi nhận rời tab, thoát toàn màn hình, quay lại trang khác, copy/paste và phím tắt điều hướng.
                          </p>
                        </div>
                      </aside>
                    </div>
                  </div>
                ) : (
                  <textarea
                    className={`min-h-[190px] w-full rounded-2xl border px-4 py-3 text-sm leading-7 outline-none transition ${isSubmissionLocked ? 'border-[#ebe3e2] bg-[#f7f3f2] text-[#7a6766]' : 'border-[#dfbfbd]/60 bg-white focus:border-[#8a0018]'}`}
                    value={answer}
                    onChange={(event) => setAnswer(event.target.value)}
                    readOnly={isSubmissionLocked || submitting}
                    placeholder={inputCopy.placeholder}
                  />
                )}
              </div>
            )}
            {!isSpeakingMockFlow && !isDedicatedExamMode && selected.skill !== 'VOCABULARY' ? (
              <p className="mt-2 text-sm leading-6 text-[#7a6766]">{inputCopy.helper}</p>
            ) : null}
            {selected.skill === 'SPEAKING' && speakingStage === 'recording' && !isSpeakingMockFlow ? (
              <div className="mt-4 space-y-4 rounded-2xl border border-[#dfbfbd]/30 bg-white p-4">
                <div>
                  <label className="mb-2 block text-xs font-bold uppercase tracking-[0.14em] text-[#8c716f]">{inputCopy.audioLabel}</label>
                  <input
                    className={`w-full rounded-2xl border px-4 py-3 text-sm outline-none transition ${isSubmissionLocked ? 'border-[#ebe3e2] bg-[#f7f3f2] text-[#7a6766]' : 'border-[#dfbfbd]/60 bg-white focus:border-[#8a0018]'}`}
                    value={audioUrl}
                    onChange={(event) => setAudioUrl(event.target.value)}
                    readOnly={isSubmissionLocked || submitting}
                    placeholder={inputCopy.audioPlaceholder}
                    type="url"
                  />
                  <p className="mt-2 text-sm leading-6 text-[#7a6766]">
                    Đây là đường dẫn tới bản ghi đã lưu hoặc một tệp âm thanh công khai khác. Nếu bạn vừa ghi âm xong, hệ thống sẽ tự điền phần này sau khi xử lý xong.
                  </p>
                </div>
              </div>
            ) : null}
            {isLocked ? (
              <p className="mt-2 text-sm text-[#7a6766]">Bạn cần hoàn thành hết các bài học trong module rồi mới có thể mở phần nộp bài này.</p>
            ) : null}
            {isLockedAfterResult ? (
              <p className="mt-2 text-sm text-[#7a6766]">Bài làm này đã có kết quả. Muốn chỉnh sửa và gửi lại, hãy bấm làm lại bài.</p>
            ) : null}
            {error ? <p className="mt-2 text-sm font-semibold text-[#93000a]">{error}</p> : null}

            {!isSpeakingMockFlow && !isDedicatedExamMode ? (
              <div className="mt-3 flex flex-wrap items-center gap-3">
                {!isLockedAfterResult ? (
                  <button
                    className="rounded-2xl bg-[#8a0018] px-5 py-3 text-sm font-extrabold text-white transition hover:bg-[#650012] disabled:opacity-60"
                    disabled={submitting || isLocked || (selected.skill === 'SPEAKING' && speakingStage !== 'recording')}
                    onClick={handleSubmit}
                    type="button"
                  >
                    {submitting ? 'Đang gửi...' : inputCopy.buttonText}
                  </button>
                ) : (
                  <button
                    className="rounded-2xl bg-[#ebe3e2] px-5 py-3 text-sm font-extrabold text-[#7a6766]"
                    disabled
                    type="button"
                  >
                    Đã có kết quả
                  </button>
                )}

                {result && !isLocked ? (
                  <button
                    className="rounded-2xl border border-[#8a0018]/20 px-5 py-3 text-sm font-bold text-[#8a0018] transition hover:bg-[#fff0f1]"
                    onClick={handleRetakeAttempt}
                    type="button"
                  >
                    Làm lại bài
                  </button>
                ) : null}
              </div>
            ) : null}
          </div>
          ) : null}

          {showSpeakingResultOnly && result && !isLocked ? (
            <div className="mt-4 flex flex-wrap items-center gap-3">
              <button
                className="rounded-2xl bg-[#ebe3e2] px-5 py-3 text-sm font-extrabold text-[#7a6766]"
                disabled
                type="button"
              >
                Đã có kết quả
              </button>
              <button
                className="rounded-2xl border border-[#8a0018]/20 px-5 py-3 text-sm font-bold text-[#8a0018] transition hover:bg-[#fff0f1]"
                onClick={() => {
                  handleRetakeAttempt();
                  setExamModeOpen(true);
                }}
                type="button"
              >
                Làm lại bài
              </button>
            </div>
          ) : null}

          {result && !isLocked && !isFullscreenExamMode && !showSpeakingResultOnly ? (
            <div className="mt-4 flex flex-wrap items-center gap-3">
              <button
                className="rounded-2xl bg-[#ebe3e2] px-5 py-3 text-sm font-extrabold text-[#7a6766]"
                disabled
                type="button"
              >
                Đã có kết quả
              </button>
              <button
                className="rounded-2xl border border-[#8a0018]/20 px-5 py-3 text-sm font-bold text-[#8a0018] transition hover:bg-[#fff0f1]"
                onClick={() => {
                  handleRetakeAttempt();
                  setExamModeOpen(true);
                }}
                type="button"
              >
                Làm lại bài
              </button>
            </div>
          ) : null}

          {result && submissionComparison ? (
            <div className="mt-5 rounded-2xl border border-[#d7c2b7] bg-[#fffaf6] p-5">
              <div className="flex flex-wrap items-center gap-3">
                <span className="rounded-full bg-[linear-gradient(135deg,#b4233f,#8a0018)] px-4 py-2 text-sm font-extrabold text-white shadow-[0_10px_24px_rgba(138,0,24,0.14)]">So với lần làm trước</span>
                {submissionComparison.scoreDelta != null ? (
                  <span className={`rounded-full px-4 py-2 text-sm font-bold ${submissionComparison.scoreDelta >= 0 ? 'bg-[#edf8f0] text-[#1d6b3a]' : 'bg-[#fff0f1] text-[#8a0018]'}`}>
                    {submissionComparison.scoreDelta >= 0 ? '+' : ''}{submissionComparison.scoreDelta} điểm
                  </span>
                ) : null}
              </div>

              {submissionComparison.improvedCriteria.length ? (
                <div className="mt-4">
                  <p className="text-sm font-extrabold text-[#2b2828]">Điểm tiến bộ</p>
                  <ul className="mt-2 space-y-1 text-sm leading-6 text-[#584140]">
                    {submissionComparison.improvedCriteria.map((criterion) => (
                      <li key={`improved-${criterion.name}`}>• {formatCriterionName(criterion.name)} tăng từ {criterion.previousScore} lên {criterion.currentScore}</li>
                    ))}
                    {submissionComparison.newStrengths.slice(0, 3).map((item) => (
                      <li key={`new-strength-${item}`}>• Điểm mạnh mới: {item}</li>
                    ))}
                  </ul>
                </div>
              ) : null}

              {submissionComparison.regressedCriteria.length || submissionComparison.lostStrengths.length ? (
                <div className="mt-4">
                  <p className="text-sm font-extrabold text-[#2b2828]">Điểm còn thiếu so với bài trước</p>
                  <ul className="mt-2 space-y-1 text-sm leading-6 text-[#584140]">
                    {submissionComparison.regressedCriteria.map((criterion) => (
                      <li key={`regressed-${criterion.name}`}>• {formatCriterionName(criterion.name)} giảm từ {criterion.previousScore} xuống {criterion.currentScore}</li>
                    ))}
                    {submissionComparison.lostStrengths.slice(0, 3).map((item) => (
                      <li key={`lost-strength-${item}`}>• Điểm tốt của bài trước cần giữ lại: {item}</li>
                    ))}
                  </ul>
                </div>
              ) : null}
            </div>
          ) : null}

          {result ? (
            <div className="mt-5 rounded-2xl border border-[#8a0018]/15 bg-[#fff7f7] p-5">
              <div className="flex flex-wrap items-center gap-3">
                {shouldShowExamScoreBadges ? (
                  <>
                    <span className="rounded-full bg-[linear-gradient(135deg,#b4233f,#8a0018)] px-4 py-2 text-sm font-extrabold text-white shadow-[0_10px_24px_rgba(138,0,24,0.14)]">
                      {usesFixedScoring ? 'Điểm' : 'Điểm ước lượng'}: {scoreDisplay}
                    </span>
                    <span className="rounded-full bg-white px-4 py-2 text-sm font-bold text-[#8a0018]">
                      {usesFixedScoring ? 'Band' : 'Band ước lượng'}: {bandDisplay}
                    </span>
                  </>
                ) : showNumericScore ? (
                  <span className="rounded-full bg-[linear-gradient(135deg,#b4233f,#8a0018)] px-4 py-2 text-sm font-extrabold text-white shadow-[0_10px_24px_rgba(138,0,24,0.14)]">
                    Điểm ước lượng: {scoreDisplay}
                  </span>
                ) : (
                  <span className="rounded-full bg-[linear-gradient(135deg,#b4233f,#8a0018)] px-4 py-2 text-sm font-extrabold text-white shadow-[0_10px_24px_rgba(138,0,24,0.14)]">
                    Đã phân tích bài làm
                  </span>
                )}
                <span className="rounded-full bg-white px-4 py-2 text-sm font-bold text-[#8a0018]">{statusLabels[result.status] || result.status}</span>
              </div>

              {partFeedback.length ? (
                <div className="mt-4">
                  <p className="text-sm font-extrabold text-[#2b2828]">Nhận xét theo từng phần thi</p>
                  <div className="mt-3 space-y-3">
                    {partFeedback.map((part, index) => (
                      <div key={`${part.partKey || part.partLabel || index}`} className="rounded-xl bg-white p-4">
                        <p className="text-sm font-extrabold text-[#4b0009]">
                          {part.partLabel || formatSpeakingPartLabel(part.partKey) || `Part ${index + 1}`}
                        </p>
                        {part.summary ? (
                          <p className="mt-2 text-sm leading-6 text-[#584140]">{part.summary}</p>
                        ) : null}
                        <div className="mt-3 grid gap-3 lg:grid-cols-3">
                          {toArray(part.strengths).length ? (
                            <div className="rounded-lg bg-[#fff8f8] p-3">
                              <p className="text-xs font-bold uppercase tracking-[0.12em] text-[#8c716f]">Điểm ổn</p>
                              <ul className="mt-1 space-y-1 text-sm leading-6 text-[#584140]">
                                {toArray(part.strengths).map((item) => <li key={`${part.partKey}-s-${item}`}>• {item}</li>)}
                              </ul>
                            </div>
                          ) : null}
                          {toArray(part.weaknesses).length ? (
                            <div className="rounded-lg bg-[#fff8f8] p-3">
                              <p className="text-xs font-bold uppercase tracking-[0.12em] text-[#8c716f]">Vấn đề chính</p>
                              <ul className="mt-1 space-y-1 text-sm leading-6 text-[#584140]">
                                {toArray(part.weaknesses).map((item) => <li key={`${part.partKey}-w-${item}`}>• {item}</li>)}
                              </ul>
                            </div>
                          ) : null}
                          {toArray(part.suggestions).length ? (
                            <div className="rounded-lg bg-[#fff8f8] p-3">
                              <p className="text-xs font-bold uppercase tracking-[0.12em] text-[#8c716f]">Gợi ý sửa</p>
                              <ul className="mt-1 space-y-1 text-sm leading-6 text-[#584140]">
                                {toArray(part.suggestions).map((item) => <li key={`${part.partKey}-g-${item}`}>• {item}</li>)}
                              </ul>
                            </div>
                          ) : null}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              ) : null}

              {criteria.length ? (
                <div className="mt-4">
                  <p className="text-sm font-extrabold text-[#2b2828]">Nhận xét theo từng tiêu chí</p>
                  <div className="mt-2 space-y-2">
                    {criteria.map((criterion) => (
                      <div key={`${criterion.name}-${criterion.score}`} className="rounded-xl bg-white p-3">
                        <p className="text-sm font-bold text-[#4b0009]">{formatCriterionName(criterion.name)} · {criterion.score ?? 'Chưa có'}</p>
                        <p className="mt-1 text-sm leading-6 text-[#584140]">{criterion.feedback}</p>
                      </div>
                    ))}
                  </div>
                </div>
              ) : null}

              {strengths.length ? (
                <div className="mt-4">
                  <p className="text-sm font-extrabold text-[#2b2828]">Điểm mạnh</p>
                  <ul className="mt-2 space-y-1 text-sm leading-6 text-[#584140]">
                    {strengths.map((item) => <li key={item}>• {item}</li>)}
                  </ul>
                </div>
              ) : null}

              {weaknesses.length ? (
                <div className="mt-4">
                  <p className="text-sm font-extrabold text-[#2b2828]">Điểm cần cải thiện</p>
                  <ul className="mt-2 space-y-1 text-sm leading-6 text-[#584140]">
                    {weaknesses.map((item) => <li key={item}>• {item}</li>)}
                  </ul>
                </div>
              ) : null}

              {suggestions.length ? (
                <div className="mt-4">
                  <p className="text-sm font-extrabold text-[#2b2828]">Gợi ý hành động tiếp theo</p>
                  <ul className="mt-2 space-y-1 text-sm leading-6 text-[#584140]">
                    {suggestions.map((item) => <li key={item}>• {item}</li>)}
                  </ul>
                </div>
              ) : null}

              {recommendedReview.length ? (
                <div className="mt-4">
                  <p className="text-sm font-extrabold text-[#2b2828]">Nên ôn lại</p>
                  <ul className="mt-2 space-y-1 text-sm leading-6 text-[#584140]">
                    {recommendedReview.map((item) => <li key={item}>• {item}</li>)}
                  </ul>
                </div>
              ) : null}

              <div className="mt-4">
                <p className="text-sm font-extrabold text-[#2b2828]">Dấu hiệu về độ nguyên bản</p>
                <div className="mt-2 grid gap-3 md:grid-cols-2">
                  <div className="rounded-xl bg-white p-3">
                    <p className="text-xs font-bold uppercase tracking-[0.12em] text-[#8c716f]">Mức rủi ro đạo văn</p>
                    <p className="mt-1 text-sm font-extrabold text-[#4b0009]">{plagiarismRisk}</p>
                  </div>
                  <div className="rounded-xl bg-white p-3">
                    <p className="text-xs font-bold uppercase tracking-[0.12em] text-[#8c716f]">Mức rủi ro dùng AI</p>
                    <p className="mt-1 text-sm font-extrabold text-[#4b0009]">{aiUsageRisk}</p>
                  </div>
                </div>
                {originalityAnalysis?.summary ? (
                  <p className="mt-3 text-sm leading-6 text-[#584140]">{originalityAnalysis.summary}</p>
                ) : null}
                {sourceSignals.length ? (
                  <ul className="mt-3 space-y-1 text-sm leading-6 text-[#584140]">
                    {sourceSignals.map((item) => <li key={item}>• {item}</li>)}
                  </ul>
                ) : null}
              </div>

              {correctedExamples.length ? (
                <div className="mt-4">
                  <p className="text-sm font-extrabold text-[#2b2828]">Ví dụ sửa lỗi</p>
                  <div className="mt-2 space-y-2">
                    {correctedExamples.map((example, index) => (
                      <div key={`${example.original}-${index}`} className="rounded-xl bg-white p-3 text-sm leading-6 text-[#584140]">
                        <p><span className="font-bold text-[#4b0009]">Câu gốc:</span> {fallbackText(example.original)}</p>
                        <p><span className="font-bold text-[#4b0009]">Câu đã sửa:</span> {fallbackText(example.corrected)}</p>
                        <p><span className="font-bold text-[#4b0009]">Giải thích:</span> {fallbackText(example.explanation)}</p>
                      </div>
                    ))}
                  </div>
                </div>
              ) : null}
            </div>
          ) : null}

          {isFullscreenExamMode ? (
            <div className="mt-4 rounded-2xl border border-[#ffe2b7] bg-[#fff8ea] px-4 py-3 text-sm font-semibold text-[#8a5d00]">
              Chế độ thi đang bật. Hệ thống sẽ ghi nhận rời tab, thoát toàn màn hình, quay lại trang khác, copy/paste và các phím tắt điều hướng.
            </div>
          ) : null}

          {!isFullscreenExamMode ? (
          <div className="mt-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex flex-wrap gap-3">
              <button
                className="cursor-pointer rounded-2xl border border-[#8a0018]/20 px-4 py-2 text-sm font-bold text-[#8a0018] transition hover:bg-[#fff0f1]"
                type="button"
                onClick={() => onMoveStep?.(-1)}
              >
                Bài trước
              </button>
              <button
                className="cursor-pointer rounded-2xl bg-[#2b2828] px-4 py-2 text-sm font-bold text-white transition hover:bg-[#8a0018]"
                type="button"
                onClick={() => onMoveStep?.(1)}
              >
                Bài tiếp theo
              </button>
            </div>
          </div>
          ) : null}
        </div>
      ) : null}
      {examWarning && isFullscreenExamMode ? (
        <div className="fixed inset-0 z-[60] flex items-center justify-center bg-[#1c120f]/45 px-4">
          <div className="max-w-md rounded-[28px] bg-white p-6 shadow-2xl">
            <p className="text-[11px] font-black uppercase tracking-[0.18em] text-[#b26a00]">Cảnh báo bài thi</p>
            <h3 className="mt-2 font-['Manrope'] text-2xl font-black text-[#2b2828]">Hệ thống đã ghi nhận vi phạm</h3>
            <p className="mt-3 text-sm leading-7 text-[#584140]">{examWarning.reason}</p>
            <div className="mt-5 flex flex-col gap-3">
              <button
                className="w-full rounded-2xl bg-[#8a0018] px-5 py-3 text-sm font-black text-white"
                onClick={async () => {
                  await requestExamFullscreen();
                  setExamWarning(null);
                }}
                type="button"
              >
                Quay lại toàn màn hình và tiếp tục làm bài
              </button>
              <button
                className="w-full rounded-2xl border border-[#dfbfbd]/50 px-5 py-3 text-sm font-bold text-[#584140] transition hover:bg-[#faf7f7]"
                onClick={() => {
                  setExamWarning(null);
                  setExamExitConfirmOpen(true);
                }}
                type="button"
              >
                Thoát bài thi
              </button>
            </div>
          </div>
        </div>
      ) : null}
      {examExitConfirmOpen && isFullscreenExamMode ? (
        <div className="fixed inset-0 z-[61] flex items-center justify-center bg-[#1c120f]/55 px-4">
          <div className="max-w-md rounded-[28px] bg-white p-6 shadow-2xl">
            <p className="text-[11px] font-black uppercase tracking-[0.18em] text-[#8c716f]">Xác nhận thoát</p>
            <h3 className="mt-2 font-['Manrope'] text-2xl font-black text-[#2b2828]">Thoát khỏi bài thi này?</h3>
            <p className="mt-3 text-sm leading-7 text-[#584140]">
              Nếu bạn thoát bây giờ, lần làm bài hiện tại sẽ không được nộp và hệ thống sẽ quay lại màn hình tổng quan của khóa học.
            </p>
            <div className="mt-5 flex flex-col gap-3 sm:flex-row">
              <button
                className="flex-1 rounded-2xl border border-[#dfbfbd]/50 px-5 py-3 text-sm font-bold text-[#584140] transition hover:bg-[#faf7f7]"
                onClick={() => setExamExitConfirmOpen(false)}
                type="button"
              >
                Ở lại làm bài
              </button>
              <button
                className="flex-1 rounded-2xl bg-[#8a0018] px-5 py-3 text-sm font-black text-white"
                onClick={handleCloseExamMode}
                type="button"
              >
                Thoát bài thi
              </button>
            </div>
          </div>
        </div>
      ) : null}
      </div>
    </section>
    {examModeOpen && isReadingExamMode ? (
      <ReadingExamMode
        assessment={selected}
        config={assessmentUiConfig}
        initialAnswers={initialExamObjectiveAnswers}
        isLocked={isSubmissionLocked}
        onClose={() => setExamModeOpen(false)}
        onSubmit={handleExamModeSubmit}
        submitting={submitting}
      />
    ) : null}
    {examModeOpen && isListeningExamMode ? (
      <ListeningExamMode
        assessment={selected}
        config={assessmentUiConfig}
        initialAnswers={initialExamObjectiveAnswers}
        isLocked={isSubmissionLocked}
        onClose={() => setExamModeOpen(false)}
        onSubmit={handleExamModeSubmit}
        submitting={submitting}
      />
    ) : null}
    {examModeOpen && isWritingExamMode ? (
      <WritingExamMode
        assessment={selected}
        config={assessmentUiConfig}
        initialSubmissionText={answer}
        isLocked={isSubmissionLocked}
        onClose={() => setExamModeOpen(false)}
        onSubmit={handleExamModeSubmit}
        submitting={submitting}
      />
    ) : null}
    </>
  );
}

