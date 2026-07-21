import { AnimatePresence, motion } from 'framer-motion';
import { useEffect, useMemo, useState } from 'react';
import courseApi from '../../api/courseApi';
import BrandLoadingState from '../ui/BrandLoadingState';

const cleanInlineMarkdown = (text = '') => String(text).replace(/\*\*/g, '').replace(/^["']|["']$/g, '').trim();

const findField = (block, labels) => {
  const line = block
    .split('\n')
    .find((item) => labels.some((label) => new RegExp(`^\\*\\*${label}:\\*\\*`, 'i').test(item.trim())));
  return line ? cleanInlineMarkdown(line.replace(/^\*\*[^:]+:\*\*\s*/i, '')) : '';
};

const extractTermsFromLesson = (lesson, module, moduleIndex) => {
  const content = lesson?.contentText || '';
  if (!content.includes('### ')) return [];

  const matches = [...content.matchAll(/^###\s+\d+\.\s+(.+)$/gm)];
  return matches
    .map((match, index) => {
      const start = match.index + match[0].length;
      const end = matches[index + 1]?.index ?? content.length;
      const block = content.slice(start, end);
      const meaning = findField(block, ['Meaning']);
      if (!meaning) return null;

      const term = cleanInlineMarkdown(match[1]);
      return {
        termKey: `${lesson.id ?? `${moduleIndex}-${lesson.title}`}-${term.toLowerCase().replace(/[^a-z0-9]+/g, '-')}`,
        term,
        meaning,
        example: findField(block, ['IELTS example', 'Example']),
        commonError: findField(block, ['Common error to avoid', 'Common error']),
        moduleTitle: module?.title || `Module ${moduleIndex + 1}`,
        lessonTitle: lesson?.title || 'Vocabulary lesson',
        status: 'NEW',
        starred: false,
      };
    })
    .filter(Boolean);
};

const parseFlashcardSetCards = (set) => {
  try {
    const cards = JSON.parse(set?.cardsJson || '[]');
    return Array.isArray(cards) ? cards : [];
  } catch {
    return [];
  }
};

const extractTermsFromFlashcardSet = (set, lesson, module, moduleIndex) =>
  parseFlashcardSetCards(set)
    .map((card, index) => {
      const term = cleanInlineMarkdown(card?.term || card?.front || card?.question || card?.word || '');
      const meaning = cleanInlineMarkdown(card?.meaning || card?.back || card?.answer || card?.definition || '');
      if (!term || !meaning) return null;
      return {
        termKey: `flashcard-set-${set.id || set.title}-${index}-${term.toLowerCase().replace(/[^a-z0-9]+/g, '-')}`,
        term,
        meaning,
        example: cleanInlineMarkdown(card?.example || card?.sentence || ''),
        commonError: cleanInlineMarkdown(card?.commonError || card?.note || ''),
        moduleTitle: module?.title || `Module ${moduleIndex + 1}`,
        lessonTitle: lesson?.title || set?.title || 'Flashcard bank',
        status: 'NEW',
        starred: false,
      };
    })
    .filter(Boolean);

const extractBankFlashcardTerms = (course) =>
  (course?.modules || []).flatMap((module, moduleIndex) =>
    (module.lessons || []).flatMap((lesson) =>
      (lesson.flashcardSets || []).flatMap((set) => extractTermsFromFlashcardSet(set, lesson, module, moduleIndex))
    )
  );

export const extractVocabularyTerms = (course) => {
  const bankTerms = extractBankFlashcardTerms(course);
  if (bankTerms.length) return bankTerms;
  return (course?.modules || []).flatMap((module, moduleIndex) =>
    (module.lessons || []).flatMap((lesson) => extractTermsFromLesson(lesson, module, moduleIndex))
  );
};

const modeTabs = [
  { id: 'cards', label: 'Thẻ', icon: 'style' },
  { id: 'learn', label: 'Học', icon: 'school' },
  { id: 'match', label: 'Ghép', icon: 'extension' },
];
const LEARN_TURN_SIZE = 10;
const MATCH_TURN_SIZE = 6;

const shuffleArray = (items) =>
  [...items]
    .map((item) => ({ item, sort: Math.random() }))
    .sort((a, b) => a.sort - b.sort)
    .map(({ item }) => item);

const speak = (text) => {
  if (!window.speechSynthesis || !text) return;
  const utterance = new SpeechSynthesisUtterance(text);
  utterance.lang = 'en-US';
  window.speechSynthesis.cancel();
  window.speechSynthesis.speak(utterance);
};

const Toggle = ({ checked, onChange }) => (
  <label className={`relative h-6 w-11 cursor-pointer rounded-full transition ${checked ? 'bg-[#8a0018]' : 'bg-[#c7ccd8]'}`}>
    <input className="sr-only" type="checkbox" checked={checked} onChange={(event) => onChange(event.target.checked)} />
    <span className={`absolute top-0.5 h-5 w-5 rounded-full bg-white shadow transition ${checked ? 'left-5' : 'left-0.5'}`} />
  </label>
);

const WorkspaceFlashcards = ({
  course,
  termsOverride,
  emptyStateDescription = 'Khóa học này chưa gắn bộ flashcard nào từ kho.',
}) => {
  const fallbackTerms = useMemo(() => extractVocabularyTerms(course), [course]);
  const hasBankFlashcards = useMemo(() => extractBankFlashcardTerms(course).length > 0, [course]);
  const [terms, setTerms] = useState(() => (Array.isArray(termsOverride) ? termsOverride : fallbackTerms));
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [mode, setMode] = useState('cards');
  const [activeIndex, setActiveIndex] = useState(0);
  const [shuffleEnabled, setShuffleEnabled] = useState(false);
  const [shuffledOrder, setShuffledOrder] = useState([]);
  const [shuffleNotice, setShuffleNotice] = useState('');
  const [flipped, setFlipped] = useState(false);
  const [quizChoice, setQuizChoice] = useState('');
  const [matchPairs, setMatchPairs] = useState([]);
  const [selectedMatch, setSelectedMatch] = useState(null);
  const [matchedKeys, setMatchedKeys] = useState(() => new Set());
  const [matchFeedback, setMatchFeedback] = useState(null);
  const [matchRound, setMatchRound] = useState(0);
  const [matchTurnKeys, setMatchTurnKeys] = useState([]);
  const [matchIntroducedCount, setMatchIntroducedCount] = useState(0);
  const [matchTurnNumber, setMatchTurnNumber] = useState(1);
  const [matchAdvancing, setMatchAdvancing] = useState(false);
  const [trackingEnabled, setTrackingEnabled] = useState(false);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [fullscreenOpen, setFullscreenOpen] = useState(false);
  const [frontSide, setFrontSide] = useState('term');
  const [starredOnly, setStarredOnly] = useState(false);
  const [keyboardOpen, setKeyboardOpen] = useState(false);
  const [textToSpeech, setTextToSpeech] = useState(false);
  const [speakingKey, setSpeakingKey] = useState('');
  const [quizOptionKeys, setQuizOptionKeys] = useState([]);
  const [learnQueue, setLearnQueue] = useState([]);
  const [learnNextQueue, setLearnNextQueue] = useState([]);
  const [learnIntroducedCount, setLearnIntroducedCount] = useState(0);
  const [learnTurnNumber, setLearnTurnNumber] = useState(1);
  const [learnTurnAnsweredCount, setLearnTurnAnsweredCount] = useState(0);
  const [learnMasteryMap, setLearnMasteryMap] = useState({});
  const [learnCheckpoint, setLearnCheckpoint] = useState(null);
  const [learnRetryMode, setLearnRetryMode] = useState(false);
  const [learnRoundKeys, setLearnRoundKeys] = useState([]);

  const visibleTerms = useMemo(() => (starredOnly ? terms.filter((term) => term.starred) : terms), [starredOnly, terms]);
  const baseStudyTerms = useMemo(() => (visibleTerms.length ? visibleTerms : terms), [terms, visibleTerms]);
  const prioritizedBaseTerms = useMemo(() => (
    [...baseStudyTerms].sort((left, right) => {
      const rank = (term) => {
        if ((term.incorrectCount || 0) > 0 && term.lastResultCorrect === false) return 0;
        if (term.status === 'NEW') return 1;
        if (term.status === 'LEARNING') return 2;
        return 3;
      };
      const rankDiff = rank(left) - rank(right);
      if (rankDiff !== 0) return rankDiff;
      return String(left.term || '').localeCompare(String(right.term || ''));
    })
  ), [baseStudyTerms]);
  const studyTerms = useMemo(() => {
    if (!shuffleEnabled) return prioritizedBaseTerms;
    const byKey = new Map(prioritizedBaseTerms.map((term) => [term.termKey, term]));
    const ordered = shuffledOrder.map((key) => byKey.get(key)).filter(Boolean);
    const missing = prioritizedBaseTerms.filter((term) => !shuffledOrder.includes(term.termKey));
    return [...ordered, ...missing];
  }, [prioritizedBaseTerms, shuffleEnabled, shuffledOrder]);
  const termMap = useMemo(() => new Map(terms.map((term) => [term.termKey, term])), [terms]);
  const learnSeedKeys = useMemo(() => baseStudyTerms.map((term) => term.termKey), [baseStudyTerms]);
  const learnSeedSignature = useMemo(() => learnSeedKeys.join('|'), [learnSeedKeys]);
  const matchSeedKeys = useMemo(() => baseStudyTerms.map((term) => term.termKey), [baseStudyTerms]);
  const matchSeedSignature = useMemo(() => matchSeedKeys.join('|'), [matchSeedKeys]);
  const activeTerm = studyTerms[Math.min(activeIndex, studyTerms.length - 1)];
  const newTerms = terms.filter((term) => term.status === 'NEW');
  const learningTerms = terms.filter((term) => term.status === 'LEARNING');
  const masteredTerms = terms.filter((term) => term.status === 'MASTERED');
  const reviewedTerms = terms.filter((term) => (term.reviewCount || 0) > 0);
  const incorrectTerms = terms.filter((term) => (term.incorrectCount || 0) > 0 && term.lastResultCorrect === false);
  const reviewedPercent = terms.length ? Math.round((reviewedTerms.length / terms.length) * 100) : 0;
  const learnBaseCount = learnSeedKeys.length;
  const learnActiveTerm = termMap.get(learnQueue[0]) || null;
  const currentQuizTerm = mode === 'learn' ? learnActiveTerm : activeTerm;
  const learnProgressSegments = Math.max(1, Math.ceil(Math.max(learnBaseCount, 1) / LEARN_TURN_SIZE));
  const learnMasteredCount = learnSeedKeys.filter((key) => (learnMasteryMap[key]?.correctCount || 0) > 0).length;
  const learnNeedsReviewCount = learnSeedKeys.filter((key) => {
    const mastery = learnMasteryMap[key];
    return mastery?.seen && (mastery.correctCount || 0) === 0;
  }).length;
  const matchTurnTotal = Math.max(1, matchTurnKeys.length);
  const matchTotalTurns = Math.max(1, Math.ceil(Math.max(matchSeedKeys.length, 1) / MATCH_TURN_SIZE));
  const frontText = frontSide === 'term' ? activeTerm?.term : activeTerm?.meaning;
  const backTitle = frontSide === 'term' ? activeTerm?.meaning : activeTerm?.term;
  const backBody = frontSide === 'term' ? activeTerm?.example : activeTerm?.meaning;

  const quizOptions = useMemo(() => {
    if (!currentQuizTerm) return [];
    const byKey = new Map(terms.map((term) => [term.termKey, term]));
    const ordered = quizOptionKeys.map((key) => byKey.get(key)).filter(Boolean);
    if (ordered.length) return ordered;
    return [currentQuizTerm, ...terms.filter((term) => term.termKey !== currentQuizTerm.termKey).slice(0, 3)];
  }, [currentQuizTerm, quizOptionKeys, terms]);
  const hasAnsweredQuiz = Boolean(quizChoice);
  const answeredCorrectly = hasAnsweredQuiz && quizChoice === currentQuizTerm?.termKey;

  useEffect(() => {
    let mounted = true;
    const loadTerms = async () => {
      if (Array.isArray(termsOverride)) {
        setTerms(termsOverride);
        setLoading(false);
        setError('');
        return;
      }
      if (!course?.id) {
        setTerms(fallbackTerms);
        setLoading(false);
        return;
      }
      if (hasBankFlashcards) {
        setTerms(fallbackTerms);
        setLoading(false);
        return;
      }

      setLoading(!fallbackTerms.length);
      setError('');
      try {
        const apiTerms = await courseApi.getVocabularyTerms(course.id);
        if (mounted) setTerms(apiTerms.length ? apiTerms : fallbackTerms);
      } catch (err) {
        if (mounted) {
          setTerms(fallbackTerms);
          setError(err?.response?.data?.message || 'Chưa tải được tiến độ flashcards từ backend, đang dùng bộ flashcard của khóa học.');
        }
      } finally {
        if (mounted) setLoading(false);
      }
    };

    loadTerms();
    return () => {
      mounted = false;
    };
  }, [course?.id, fallbackTerms, hasBankFlashcards, termsOverride]);

  useEffect(() => {
    if (activeIndex >= studyTerms.length) setActiveIndex(0);
    setFlipped(false);
    setQuizChoice('');
  }, [activeIndex, studyTerms.length, mode]);

  useEffect(() => {
    if (!currentQuizTerm) {
      setQuizOptionKeys([]);
      return;
    }
    const nextOptions = shuffleArray([
      currentQuizTerm,
      ...shuffleArray(terms.filter((term) => term.termKey !== currentQuizTerm.termKey)).slice(0, 3),
    ]);
    setQuizOptionKeys(nextOptions.map((term) => term.termKey));
  }, [currentQuizTerm?.termKey]);

  useEffect(() => {
    if (mode !== 'learn') return;
    const firstRoundKeys = learnSeedKeys.slice(0, LEARN_TURN_SIZE);
    setLearnQueue(firstRoundKeys);
    setLearnNextQueue([]);
    setLearnIntroducedCount(Math.min(LEARN_TURN_SIZE, learnSeedKeys.length));
    setLearnTurnNumber(1);
    setLearnTurnAnsweredCount(0);
    setLearnMasteryMap(
      Object.fromEntries(learnSeedKeys.map((key) => [key, { correctCount: 0, wrongCount: 0, seen: false }]))
    );
    setLearnCheckpoint(null);
    setLearnRetryMode(false);
    setLearnRoundKeys(firstRoundKeys);
    setQuizChoice('');
  }, [course?.id, learnSeedSignature, mode]);

  useEffect(() => {
    if (!shuffleNotice) return undefined;
    const timer = window.setTimeout(() => setShuffleNotice(''), 5000);
    return () => window.clearTimeout(timer);
  }, [shuffleNotice]);

  useEffect(() => {
    if (!shuffleEnabled) return;
    setShuffledOrder((current) => {
      const keys = baseStudyTerms.map((term) => term.termKey);
      const kept = current.filter((key) => keys.includes(key));
      const missing = keys.filter((key) => !kept.includes(key));
      return [...kept, ...shuffleArray(missing)];
    });
  }, [baseStudyTerms, shuffleEnabled]);

  useEffect(() => {
    const locked = mode === 'learn' || mode === 'match' || fullscreenOpen;
    if (!locked) return undefined;

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, [fullscreenOpen, mode]);

  useEffect(() => {
    if (mode !== 'match') return;
    const firstTurnKeys = matchSeedKeys.slice(0, MATCH_TURN_SIZE);
    setMatchTurnKeys(firstTurnKeys);
    setMatchIntroducedCount(Math.min(MATCH_TURN_SIZE, matchSeedKeys.length));
    setMatchTurnNumber(1);
    setMatchRound((current) => current + 1);
  }, [course?.id, matchSeedSignature, mode]);

  useEffect(() => {
    if (mode !== 'match') return;
    const byKey = new Map(terms.map((term) => [term.termKey, term]));
    const sample = matchTurnKeys.map((key) => byKey.get(key)).filter(Boolean);
    setMatchPairs(shuffleArray([
      ...sample.map((term) => ({ id: `${term.termKey}-term`, termKey: term.termKey, type: 'term', text: term.term })),
      ...sample.map((term) => ({ id: `${term.termKey}-meaning`, termKey: term.termKey, type: 'meaning', text: term.meaning })),
    ]));
    setSelectedMatch(null);
    setMatchedKeys(new Set());
    setMatchFeedback(null);
    setMatchAdvancing(false);
  }, [mode, matchRound, matchTurnKeys]);

  useEffect(() => {
    const onKeyDown = (event) => {
      if (settingsOpen || fullscreenOpen === 'typing') return;
      if (event.code === 'Space') {
        event.preventDefault();
        setFlipped((value) => !value);
      }
      if (event.key === 'ArrowLeft') handleArrow(-1);
      if (event.key === 'ArrowRight') handleArrow(1);
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  });

  const persistTerm = async (termKey, payload) => {
    const previousTerm = terms.find((term) => term.termKey === termKey);
    setTerms((current) => current.map((term) => (term.termKey === termKey ? { ...term, ...payload } : term)));
    if (!course?.id) return;

    setError('');
    try {
      const updated = await courseApi.updateVocabularyProgress(course.id, termKey, payload);
      setTerms((current) => current.map((term) => (term.termKey === termKey ? { ...term, ...updated } : term)));
    } catch (err) {
      if (previousTerm) {
        setTerms((current) => current.map((term) => (term.termKey === termKey ? previousTerm : term)));
      }
      setError(err?.response?.data?.message || 'Không lưu được tiến độ flashcard. Vui lòng thử lại.');
    }
  };

  const moveCard = (direction) => {
    if (!studyTerms.length) return;
    setActiveIndex((current) => (current + direction + studyTerms.length) % studyTerms.length);
  };

  const handleArrow = async (direction) => {
    if (!activeTerm) return;
    if (trackingEnabled) {
      await persistTerm(activeTerm.termKey, {
        status: direction < 0 ? 'LEARNING' : 'MASTERED',
        reviewed: true,
        correct: direction > 0,
      });
      moveCard(1);
      return;
    }
    moveCard(direction);
  };

  const toggleStar = (term = activeTerm) => {
    if (!term) return;
    persistTerm(term.termKey, { starred: !term.starred });
  };

  const handleQuizSelect = (optionTermKey) => {
    if (!currentQuizTerm || hasAnsweredQuiz) return;
    setQuizChoice(optionTermKey);
  };

  const finishLearnRound = (nextTurnAnsweredCount) => {
    const masteredNow = learnSeedKeys.filter((key) => (learnMasteryMap[key]?.correctCount || 0) > 0).length;
    setLearnCheckpoint({
      answeredCount: nextTurnAnsweredCount,
      masteredCount: masteredNow,
      learnedKeys: learnRoundKeys,
      allDone: masteredNow >= learnBaseCount && learnIntroducedCount >= learnBaseCount,
    });
    setLearnTurnAnsweredCount(nextTurnAnsweredCount);
  };

  const continueLearnAfterCheckpoint = () => {
    const nextRoundKeys = learnSeedKeys.slice(learnIntroducedCount, learnIntroducedCount + LEARN_TURN_SIZE);
    setLearnQueue(nextRoundKeys);
    setLearnNextQueue([]);
    setLearnIntroducedCount(Math.min(learnIntroducedCount + nextRoundKeys.length, learnBaseCount));
    setLearnTurnNumber((current) => (nextRoundKeys.length ? current + 1 : current));
    setLearnTurnAnsweredCount(0);
    setLearnCheckpoint(null);
    setLearnRetryMode(false);
    setLearnRoundKeys(nextRoundKeys);
    setQuizChoice('');
  };

  const advanceLearnQueue = (answeredRight) => {
    setLearnQueue((current) => {
      if (!current.length) return current;
      const [currentKey, ...rest] = current;
      const queuedForNextTurn = answeredRight ? learnNextQueue : [...learnNextQueue, currentKey];
      const nextTurnAnsweredCount = learnTurnAnsweredCount + 1;

      if (rest.length) {
        setLearnNextQueue(queuedForNextTurn);
        setLearnTurnAnsweredCount(nextTurnAnsweredCount);
        return rest;
      }

      const retryKeys = [...new Set(queuedForNextTurn)];
      if (retryKeys.length) {
        setLearnNextQueue([]);
        setLearnRetryMode(true);
        setLearnTurnAnsweredCount(0);
        return retryKeys;
      }

      finishLearnRound(nextTurnAnsweredCount);
      return [];
    });
    setQuizChoice('');
  };

  const handleQuizNext = async () => {
    if (!currentQuizTerm || !hasAnsweredQuiz) return;
    if (mode === 'learn') {
      advanceLearnQueue(answeredCorrectly);
      return;
    }
    await persistTerm(activeTerm.termKey, {
      status: answeredCorrectly ? 'MASTERED' : 'LEARNING',
      reviewed: true,
      correct: answeredCorrectly,
    });
    moveCard(1);
  };

  const handleLearnAnswer = async (optionTermKey) => {
    if (!currentQuizTerm || hasAnsweredQuiz) return;
    const isCorrect = optionTermKey === currentQuizTerm.termKey;
    const currentMastery = learnMasteryMap[currentQuizTerm.termKey] || { correctCount: 0, wrongCount: 0, seen: false };
    const nextCorrectCount = currentMastery.correctCount + (isCorrect ? 1 : 0);
    setQuizChoice(optionTermKey);
    setLearnMasteryMap((current) => ({
      ...current,
      [currentQuizTerm.termKey]: {
        correctCount: isCorrect ? Math.max(1, nextCorrectCount) : currentMastery.correctCount,
        wrongCount: currentMastery.wrongCount + (isCorrect ? 0 : 1),
        seen: true,
      },
    }));
    await persistTerm(currentQuizTerm.termKey, {
      status: isCorrect ? 'MASTERED' : 'LEARNING',
      reviewed: true,
      correct: isCorrect,
    });
  };

  const handleLearnSkip = async () => {
    if (!currentQuizTerm || hasAnsweredQuiz) return;
    const currentMastery = learnMasteryMap[currentQuizTerm.termKey] || { correctCount: 0, wrongCount: 0, seen: false };
    setQuizChoice('__skip__');
    setLearnMasteryMap((current) => ({
      ...current,
      [currentQuizTerm.termKey]: {
        correctCount: currentMastery.correctCount,
        wrongCount: currentMastery.wrongCount + 1,
        seen: true,
      },
    }));
    await persistTerm(currentQuizTerm.termKey, {
      status: 'LEARNING',
      reviewed: true,
      correct: false,
    });
  };

  const startNextMatchTurn = () => {
    const nextKeys = matchSeedKeys.slice(matchIntroducedCount, matchIntroducedCount + MATCH_TURN_SIZE);
    if (nextKeys.length) {
      setMatchTurnKeys(nextKeys);
      setMatchIntroducedCount((current) => Math.min(current + MATCH_TURN_SIZE, matchSeedKeys.length));
      setMatchTurnNumber((current) => current + 1);
      setMatchRound((current) => current + 1);
      return;
    }

    setMatchTurnKeys(matchSeedKeys.slice(0, MATCH_TURN_SIZE));
    setMatchIntroducedCount(Math.min(MATCH_TURN_SIZE, matchSeedKeys.length));
    setMatchTurnNumber(1);
    setMatchRound((current) => current + 1);
  };

  const toggleShuffle = () => {
    setShuffleEnabled((current) => {
      const next = !current;
      if (next) {
        setShuffledOrder(shuffleArray(baseStudyTerms.map((term) => term.termKey)));
      }
      setShuffleNotice(`Trộn thẻ đang ${next ? 'BẬT' : 'TẮT'}`);
      return next;
    });
  };

  const playSpeech = (key, text) => {
    if (!window.speechSynthesis || !text) return;

    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = 'en-US';
    setSpeakingKey(key);

    const clearSpeaking = () => {
      setSpeakingKey((current) => (current === key ? '' : current));
    };
    utterance.onend = clearSpeaking;
    utterance.onerror = clearSpeaking;

    window.speechSynthesis.speak(utterance);
  };

  const selectMatch = (card) => {
    if (matchedKeys.has(card.termKey)) return;
    if (matchFeedback || matchAdvancing) return;
    if (!selectedMatch) {
      setSelectedMatch(card);
      return;
    }
    if (selectedMatch.id === card.id) {
      setSelectedMatch(null);
      return;
    }
    if (selectedMatch.termKey === card.termKey && selectedMatch.type !== card.type) {
      setMatchFeedback({ termKey: card.termKey, status: 'correct' });
      persistTerm(card.termKey, { status: 'MASTERED', reviewed: true, correct: true });
      window.setTimeout(() => {
        setMatchedKeys((current) => {
          const nextMatchedKeys = new Set(current).add(card.termKey);
          const totalPairs = new Set(matchPairs.map((item) => item.termKey)).size;
          if (totalPairs > 0 && nextMatchedKeys.size === totalPairs) {
            setMatchAdvancing(true);
            window.setTimeout(() => {
              startNextMatchTurn();
            }, 900);
          }
          return nextMatchedKeys;
        });
        setMatchFeedback(null);
      }, 420);
    } else {
      setMatchFeedback({ ids: [selectedMatch.id, card.id], status: 'wrong' });
      window.setTimeout(() => setMatchFeedback(null), 520);
    }
    setSelectedMatch(null);
  };

  const renderStar = (term, size = 22) => (
    <span
      className={`material-symbols-outlined ${term?.starred ? 'text-[#f6b100]' : 'text-[#59627a]'}`}
      style={{ fontSize: size, fontVariationSettings: term?.starred ? "'FILL' 1" : "'FILL' 0" }}
    >
      star
    </span>
  );

  const renderSpeaker = (key, size = 22) => (
    <span
      className={`material-symbols-outlined transition ${speakingKey === key ? 'animate-pulse text-[#8a0018] drop-shadow-[0_0_8px_rgba(138,0,24,0.35)]' : 'text-[#59627a]'}`}
      style={{ fontSize: size, fontVariationSettings: speakingKey === key ? "'FILL' 1" : "'FILL' 0" }}
    >
      volume_up
    </span>
  );

  const renderTermRows = (items) => (
    <div className="space-y-3">
      {items.map((term) => (
        <button
          key={term.termKey}
          className="grid w-full cursor-pointer gap-4 rounded-2xl border border-[#e6d7d5] bg-white p-5 text-left shadow-sm transition hover:border-[#8a0018]/30 md:grid-cols-[minmax(180px,0.7fr)_1fr_80px]"
          type="button"
          onClick={() => {
            const nextIndex = studyTerms.findIndex((item) => item.termKey === term.termKey);
            setActiveIndex(Math.max(nextIndex, 0));
            setMode('cards');
            setFlipped(false);
          }}
        >
          <div>
            <h4 className="font-['Manrope'] text-xl font-extrabold text-[#25222a]">{term.term}</h4>
            <p className="mt-1 text-xs font-bold uppercase tracking-[0.12em] text-[#8c716f]">{term.moduleTitle}</p>
          </div>
          <p className="border-[#e6d7d5] text-sm leading-7 text-[#584140] md:border-l md:pl-6">{term.meaning}</p>
          <div className="flex items-start justify-end gap-3 text-[#59627a]">
            <span
              role="button"
              tabIndex={0}
              onClick={(event) => {
                event.stopPropagation();
                toggleStar(term);
              }}
              onKeyDown={(event) => {
                if (event.key === 'Enter' || event.key === ' ') {
                  event.preventDefault();
                  event.stopPropagation();
                  toggleStar(term);
                }
              }}
            >
              {renderStar(term)}
            </span>
            <span onClick={(event) => { event.stopPropagation(); playSpeech(`row-${term.termKey}`, term.term); }}>{renderSpeaker(`row-${term.termKey}`)}</span>
          </div>
        </button>
      ))}
    </div>
  );

  const renderCard = (fullscreen = false) => (
    <motion.div
      className={`relative ${fullscreen ? 'h-[62vh]' : 'h-[430px]'} w-full cursor-pointer rounded-[18px] text-left [perspective:1200px]`}
      role="button"
      tabIndex={0}
      initial={{ opacity: 0, y: 16, scale: 0.985 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      transition={{ duration: 0.35, ease: 'easeOut' }}
      onClick={() => setFlipped((value) => !value)}
      onKeyDown={(event) => {
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault();
          setFlipped((value) => !value);
        }
      }}
    >
      <div key={activeTerm?.termKey} className="flashcard-card-enter relative h-full w-full transition-transform duration-500 [transform-style:preserve-3d]" style={{ transform: flipped ? 'rotateY(180deg)' : 'rotateY(0deg)' }}>
        <div className="absolute inset-0 overflow-hidden rounded-[18px] border border-[#e8e8e8] bg-[linear-gradient(135deg,#fff8f5_0%,#ffffff_55%,#fff3f5_100%)] p-10 shadow-[0_20px_60px_rgba(26,28,28,0.08)] [backface-visibility:hidden]">
          <div className="pointer-events-none absolute -left-12 top-10 h-36 w-36 rounded-full bg-[#ffd7df]/55 blur-3xl" />
          <div className="pointer-events-none absolute bottom-8 right-8 h-24 w-24 rounded-full bg-[#ffe6b5]/45 blur-2xl" />
          <div className="absolute right-8 top-7 flex items-center gap-4">
            <button className="cursor-pointer" type="button" onClick={(event) => { event.stopPropagation(); playSpeech(`front-${activeTerm?.termKey}`, frontText); }}>
              {renderSpeaker(`front-${activeTerm?.termKey}`)}
            </button>
            <button className="cursor-pointer" type="button" onClick={(event) => { event.stopPropagation(); toggleStar(); }}>
              {renderStar(activeTerm)}
            </button>
          </div>
          <div className="flex h-full items-center justify-center">
            <p className={`max-h-[310px] overflow-y-auto px-6 text-center font-['Manrope'] font-semibold leading-[1.45] text-[#252b3a] ${fullscreen ? 'text-[42px]' : 'text-[38px]'}`}>{frontText}</p>
          </div>
        </div>

        <div className="absolute inset-0 overflow-hidden rounded-[18px] border border-[#e8e8e8] bg-[linear-gradient(160deg,#fffdf7_0%,#ffffff_60%,#fff4ef_100%)] p-10 shadow-[0_20px_60px_rgba(26,28,28,0.08)] [backface-visibility:hidden]" style={{ transform: 'rotateY(180deg)' }}>
          <div className="pointer-events-none absolute -right-10 top-10 h-32 w-32 rounded-full bg-[#ffe1cc]/55 blur-3xl" />
          <div className="pointer-events-none absolute bottom-10 left-8 h-20 w-20 rounded-full bg-[#ffd7df]/45 blur-2xl" />
          <div className="absolute right-8 top-7 flex items-center gap-4">
            <button className="cursor-pointer" type="button" onClick={(event) => { event.stopPropagation(); playSpeech(`back-${activeTerm?.termKey}`, backTitle); }}>
              {renderSpeaker(`back-${activeTerm?.termKey}`)}
            </button>
            <button className="cursor-pointer" type="button" onClick={(event) => { event.stopPropagation(); toggleStar(); }}>
              {renderStar(activeTerm)}
            </button>
          </div>
          <div className="flex h-full items-center">
            <div className="max-h-[310px] overflow-y-auto pr-6">
              <h3 className="font-['Manrope'] text-[30px] font-semibold leading-[1.45] text-[#252b3a]">{backTitle}</h3>
              {backBody ? <p className="mt-8 text-lg leading-8 text-[#584140]">{backBody}</p> : null}
              {activeTerm?.commonError ? <p className="mt-5 text-sm font-semibold leading-7 text-[#8a0018]">Lưu ý: {activeTerm.commonError}</p> : null}
            </div>
          </div>
        </div>
      </div>
      {shuffleNotice ? (
        <div className="pointer-events-none absolute inset-x-0 bottom-0 z-10 rounded-b-[18px] bg-[#e2e5ff] px-4 py-3 text-center text-sm font-medium text-[#59627a] shadow-sm">
          {shuffleNotice}
        </div>
      ) : null}
    </motion.div>
  );

  const renderControls = (compact = false) => (
    <div className={`mt-5 flex items-center justify-between gap-4 ${compact ? 'mx-auto max-w-[600px]' : ''}`}>
      <label className="flex cursor-pointer items-center gap-2 text-sm font-bold text-[#8a0018]">
        <span>Theo dõi tiến độ</span>
        <span className={`relative h-5 w-9 rounded-full transition ${trackingEnabled ? 'bg-[#8a0018]' : 'bg-[#c7ccd8]'}`}>
          <input className="sr-only" type="checkbox" checked={trackingEnabled} onChange={(event) => setTrackingEnabled(event.target.checked)} />
          <span className={`absolute top-0.5 h-4 w-4 rounded-full bg-white shadow transition ${trackingEnabled ? 'left-[18px]' : 'left-0.5'}`} />
        </span>
      </label>

      <div className="flex items-center gap-4">
        <button
          className={`flex h-14 w-14 cursor-pointer items-center justify-center rounded-full transition ${trackingEnabled ? 'bg-[#fff0f1] text-[#93000a]' : 'bg-[#f1f3f8] text-[#59627a]'}`}
          type="button"
          title={trackingEnabled ? 'Chưa thuộc' : 'Thẻ trước'}
          onClick={() => handleArrow(-1)}
        >
          <span className="material-symbols-outlined text-[30px]">{trackingEnabled ? 'close' : 'arrow_back'}</span>
        </button>
        <span className="min-w-[74px] text-center text-sm font-extrabold text-[#59627a]">{activeIndex + 1} / {studyTerms.length}</span>
        <button
          className={`flex h-14 w-14 cursor-pointer items-center justify-center rounded-full transition ${trackingEnabled ? 'bg-[#e7f6ec] text-[#176b3a]' : 'bg-[#f1f3f8] text-[#59627a]'}`}
          type="button"
          title={trackingEnabled ? 'Đã thuộc' : 'Thẻ sau'}
          onClick={() => handleArrow(1)}
        >
          <span className="material-symbols-outlined text-[30px]">{trackingEnabled ? 'check' : 'arrow_forward'}</span>
        </button>
      </div>

      {!compact ? (
        <div className="flex items-center gap-3">
          <button
            className={`flex h-11 w-11 cursor-pointer items-center justify-center rounded-full transition ${shuffleEnabled ? 'bg-[#eef0ff] text-[#596dff] ring-2 ring-[#596dff]/40' : 'bg-[#f1f3f8] text-[#59627a] hover:bg-[#e8eaf2]'}`}
            type="button"
            title={shuffleEnabled ? 'Tắt trộn thẻ' : 'Bật trộn thẻ'}
            onClick={toggleShuffle}
          >
            <span className="material-symbols-outlined">shuffle</span>
          </button>
          <button className="flex h-11 w-11 cursor-pointer items-center justify-center rounded-full bg-[#f1f3f8] text-[#59627a] transition hover:bg-[#e8eaf2]" type="button" onClick={() => setSettingsOpen(true)}>
            <span className="material-symbols-outlined">settings</span>
          </button>
          <button className="flex h-11 w-11 cursor-pointer items-center justify-center rounded-full bg-[#f1f3f8] text-[#59627a] transition hover:bg-[#e8eaf2]" type="button" onClick={() => setFullscreenOpen(true)}>
            <span className="material-symbols-outlined">fullscreen</span>
          </button>
        </div>
      ) : null}
    </div>
  );

  if (loading && !terms.length) return <BrandLoadingState compact className="rounded-[28px]" message="Đang mở flashcards..." />;

  if (!terms.length) {
    return (
      <section className="rounded-[28px] border border-[#dfbfbd]/20 bg-white p-8 text-center shadow-sm">
        <span className="material-symbols-outlined text-4xl text-[#8c716f]">style</span>
        <h2 className="mt-3 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">Chưa có bộ từ vựng</h2>
        <p className="mt-2 text-sm leading-7 text-[#584140]">{emptyStateDescription}</p>
      </section>
    );
  }

  return (
    <section className="space-y-8">
      <div className="mx-auto max-w-[980px] rounded-[28px] border border-[#dfbfbd]/20 bg-white p-6 shadow-sm">
        <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <p className="text-[11px] font-bold uppercase tracking-[0.14em] text-[#8c716f]">New words</p>
            <h2 className="mt-2 font-['Manrope'] text-3xl font-extrabold text-[#2b2828]">Flashcards từ vựng</h2>
            <p className="mt-2 max-w-2xl text-sm leading-7 text-[#584140]">Lật thẻ, luyện trắc nghiệm, ghép cặp và lưu tiến độ học từ vựng vào tài khoản.</p>
          </div>
          <div className="min-w-[240px]">
            <div className="flex items-center justify-between text-xs font-bold uppercase tracking-[0.1em] text-[#8c716f]">
              <span>Tiến độ</span>
              <span>{reviewedTerms.length}/{terms.length}</span>
            </div>
            <div className="mt-2 h-2 overflow-hidden rounded-full bg-[#f1f1f0]">
              <div className="h-full rounded-full bg-[#8a0018] transition-all duration-500" style={{ width: `${reviewedPercent}%` }} />
            </div>
          </div>
        </div>

        <div className="mt-6 grid gap-4 md:grid-cols-3">
          {modeTabs.map((tab) => (
            <button key={tab.id} className={`inline-flex min-h-[72px] cursor-pointer items-center justify-center gap-3 rounded-xl border px-5 py-4 text-base font-extrabold shadow-sm transition hover:-translate-y-0.5 hover:border-[#8a0018]/35 hover:text-[#8a0018] ${mode === tab.id ? 'border-[#8a0018]/30 bg-[#fff0f1] text-[#8a0018]' : 'border-transparent bg-[#f6f7fb] text-[#252b3a]'}`} type="button" onClick={() => setMode(tab.id)}>
              <span className={`material-symbols-outlined text-[24px] ${mode === tab.id ? 'text-[#8a0018]' : 'text-[#8c716f]'}`}>{tab.icon}</span>
              {tab.label}
            </button>
          ))}
        </div>
        {error ? <div className="mt-4 rounded-2xl bg-[#fff4d8] px-4 py-3 text-sm font-semibold text-[#7a4b00]">{error}</div> : null}
      </div>

      {mode === 'cards' ? (
        <div className="mx-auto max-w-[980px]">
          {renderCard(false)}
          {renderControls(false)}
        </div>
      ) : null}

      {mode === 'learn' ? (
        <div className="fixed inset-0 z-[95] min-h-screen overflow-y-auto bg-[#fffaf6] px-6 py-5 md:px-8">
          <div className="mb-8 flex items-center justify-between">
            <button className="inline-flex cursor-pointer items-center gap-2 rounded-full bg-[#f1f3f8] px-5 py-3 text-sm font-extrabold text-[#59627a]" type="button">
              <span className="material-symbols-outlined text-[20px] text-[#8a0018]">school</span>
              Học
              <span className="material-symbols-outlined text-[18px]">expand_more</span>
            </button>
            <div className="flex items-center gap-4 text-[#59627a]">
              {/* <button className="rounded-full bg-[#ffc928] px-5 py-3 text-sm font-extrabold text-[#2b2828]" type="button">Bắt đầu dùng thử</button> */}
              <button className="cursor-pointer" type="button" onClick={() => setSettingsOpen(true)}>
                <span className="material-symbols-outlined">settings</span>
              </button>
              <button className="cursor-pointer" type="button" onClick={() => setMode('cards')}>
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>
          </div>

          <div className="mx-auto max-w-[960px]">
            <div className="mb-6 flex items-center gap-2 md:gap-3">
              <span className="flex h-10 min-w-10 items-center justify-center rounded-full bg-[#11835f] px-2 text-sm font-extrabold text-white">
                {learnMasteredCount}
              </span>
              <div className="flex flex-1 items-center gap-2">
                {Array.from({ length: learnProgressSegments }).map((_, index) => {
                  const segmentStart = index * LEARN_TURN_SIZE;
                  const segmentEnd = Math.min(segmentStart + LEARN_TURN_SIZE, learnBaseCount || LEARN_TURN_SIZE);
                  const segmentSize = Math.max(1, segmentEnd - segmentStart);
                  const segmentDone = Math.max(0, Math.min(segmentSize, learnMasteredCount - segmentStart));
                  return (
                    <div key={`learn-segment-${index}`} className="h-4 flex-1 overflow-hidden rounded-full bg-[#dfe3ee]">
                      <div
                        className="h-full rounded-full bg-[#8a0018] transition-all duration-500"
                        style={{ width: `${Math.round((segmentDone / segmentSize) * 100)}%` }}
                      />
                    </div>
                  );
                })}
              </div>
              <span className="flex h-10 min-w-10 items-center justify-center rounded-full bg-[#dfe3ee] px-2 text-sm font-extrabold text-[#252b3a]">
                {learnBaseCount}
              </span>
            </div>
            {/* <p className="mb-4 text-sm text-[#584140]">
              Hệ thống sẽ tự chia bộ từ thành các lượt học ngắn. Mỗi lượt có tối đa 10 câu mới; câu nào sai sẽ được làm lại ngay trước khi sang lượt tiếp theo.
              {learnNeedsReviewCount ? ` Hiện có ${learnNeedsReviewCount} từ cần ôn lại.` : ''}
            </p> */}

            {learnActiveTerm ? (
            <div className="rounded-[18px] border border-[#dfe3ee] bg-white p-8 shadow-sm">
              <div className="flex items-center gap-3">
                <p className="text-sm font-bold text-[#59627a]">Thuật ngữ</p>
                {learnRetryMode ? (
                  <span className="rounded-full bg-[#fff4d8] px-3 py-1 text-xs font-extrabold text-[#9a5b00]">Hãy thử lại lần nữa</span>
                ) : null}
              </div>
              <h3 className="mt-6 min-h-[170px] text-2xl font-medium leading-10 text-[#0f1b3d]">{learnActiveTerm?.meaning}</h3>
              <p className="mt-4 text-sm font-bold text-[#59627a]">Chọn đáp án đúng</p>
              <div className="mt-5 grid gap-4 md:grid-cols-2">
                {quizOptions.map((option, index) => {
                  const selected = quizChoice === option.termKey;
                  const correct = hasAnsweredQuiz && option.termKey === learnActiveTerm?.termKey;
                  const wrong = selected && option.termKey !== learnActiveTerm?.termKey;
                  return (
                    <motion.button
                      key={option.termKey}
                      className={`cursor-pointer rounded-2xl border px-5 py-4 text-left font-bold transition ${hasAnsweredQuiz ? '' : 'hover:-translate-y-0.5 hover:border-[#8a0018]/40 hover:shadow-sm'} ${correct ? 'border-[#176b3a] bg-[#e7f6ec] text-[#176b3a]' : wrong ? 'border-[#ba1a1a] bg-[#ffdad6] text-[#93000a]' : selected ? 'border-[#8a0018] bg-[#fff0f1] text-[#8a0018]' : 'border-[#dfe3ee] bg-white text-[#0f1b3d]'}`}
                      type="button"
                      initial={{ opacity: 0, y: 12 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ delay: 0.05 * index, duration: 0.2 }}
                      onClick={() => handleLearnAnswer(option.termKey)}
                    >
                      <span className={`mr-3 rounded-full px-3 py-1 text-xs ${selected || correct ? 'bg-white/80' : 'bg-[#eef1f7]'}`}>{index + 1}</span>
                      {option.term}
                    </motion.button>
                  );
                })}
              </div>
              <AnimatePresence>
                {hasAnsweredQuiz ? (
                  <motion.div
                    className={`mt-5 rounded-2xl border px-5 py-4 ${answeredCorrectly ? 'border-[#176b3a]/20 bg-[#f2fbf5]' : 'border-[#ba1a1a]/20 bg-[#fff6f5]'}`}
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -8 }}
                    transition={{ duration: 0.2 }}
                  >
                    <p className={`text-sm font-extrabold ${answeredCorrectly ? 'text-[#176b3a]' : 'text-[#93000a]'}`}>
                      {answeredCorrectly ? 'Chính xác.' : 'Chưa đúng.'} Đáp án đúng là "{learnActiveTerm?.term}".
                    </p>
                    {learnActiveTerm?.example ? (
                      <p className="mt-2 text-sm leading-7 text-[#584140]">{learnActiveTerm.example}</p>
                    ) : null}
                    {learnActiveTerm?.commonError ? (
                      <p className="mt-2 text-sm font-semibold leading-7 text-[#8a0018]">Lưu ý: {learnActiveTerm.commonError}</p>
                    ) : null}
                    {!answeredCorrectly ? (
                      <p className="mt-2 text-sm font-semibold text-[#93000a]">Câu này sẽ được làm lại ngay trong lượt này.</p>
                    ) : null}
                  </motion.div>
                ) : null}
              </AnimatePresence>
              <div className="mt-6 flex items-center justify-end gap-8">
                <button
                  className="inline-flex cursor-pointer items-center gap-2 text-sm font-bold text-[#3155ff] disabled:cursor-not-allowed disabled:opacity-40"
                  disabled={hasAnsweredQuiz}
                  type="button"
                  onClick={handleLearnSkip}
                >
                  <span className="material-symbols-outlined text-[18px] text-[#59627a]">flag</span>
                  Bạn không biết?
                </button>
                {quizChoice ? (
                  <button className="cursor-pointer rounded-2xl bg-[#2b2828] px-5 py-3 text-sm font-extrabold text-white transition hover:bg-[#8a0018]" type="button" onClick={handleQuizNext}>
                    {learnQueue.length <= 1 ? 'Xem kết quả lượt này' : 'Câu tiếp theo'}
                  </button>
                ) : null}
              </div>
            </div>
            ) : learnCheckpoint ? (
              <div className="rounded-[18px] border border-[#dfe3ee] bg-white p-8 shadow-sm">
                <h3 className="font-['Manrope'] text-2xl font-black text-[#0f1b3d]">
                  {learnCheckpoint.allDone ? 'Bạn đã hoàn thành bộ từ này.' : 'Mạnh mẽ lên, bạn có thể thành công.'}
                </h3>
                <p className="mt-4 text-sm font-bold text-[#0f1b3d]">
                  Tiến trình tổng thể: {learnBaseCount ? Math.round((learnCheckpoint.masteredCount / learnBaseCount) * 100) : 0}%
                </p>
                <div className="mt-2 flex items-center gap-3">
                  <span className="flex h-8 min-w-8 items-center justify-center rounded-full bg-[#11835f] px-2 text-xs font-extrabold text-white">{learnCheckpoint.masteredCount}</span>
                  <div className="h-3 flex-1 overflow-hidden rounded-full bg-[#dfe3ee]">
                    <div className="h-full rounded-full bg-[#11835f] transition-all duration-500" style={{ width: `${learnBaseCount ? Math.round((learnCheckpoint.masteredCount / learnBaseCount) * 100) : 0}%` }} />
                  </div>
                  <span className="flex h-8 min-w-8 items-center justify-center rounded-full bg-[#dfe3ee] px-2 text-xs font-extrabold text-[#252b3a]">{learnBaseCount}</span>
                </div>
                <div className="mt-2 flex justify-between text-xs font-extrabold text-[#59627a]">
                  <span>Đúng</span>
                  <span>Tổng số câu hỏi</span>
                </div>
                <div className="mt-8 border-t border-[#edf0f5] pt-6">
                  <p className="mb-4 text-sm font-bold text-[#59627a]">Thuật ngữ đã học trong vòng này</p>
                  <div className="space-y-3">
                    {learnCheckpoint.learnedKeys.map((key) => {
                      const item = termMap.get(key);
                      if (!item) return null;
                      return (
                        <div key={`learned-${key}`} className="grid gap-4 rounded-xl border border-[#dfe3ee] bg-white px-5 py-4 text-sm shadow-sm md:grid-cols-[1.2fr_1fr_auto] md:items-center">
                          <p className="leading-6 text-[#0f1b3d]">{item.meaning}</p>
                          <p className="border-[#edf0f5] font-bold text-[#0f1b3d] md:border-l md:pl-5">{item.term}</p>
                          <div className="flex items-center gap-3 text-[#8c9ab7]">
                            <button className="cursor-pointer transition hover:text-[#8a0018]" type="button" onClick={() => toggleStar(item)}>
                              <span className="material-symbols-outlined text-[18px]">{item.starred ? 'star' : 'star_outline'}</span>
                            </button>
                            <button className="cursor-pointer transition hover:text-[#8a0018]" type="button" onClick={() => playSpeech(`learned-${key}`, item.term)}>
                              <span className="material-symbols-outlined text-[18px]">volume_up</span>
                            </button>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
                <div className="mt-6 flex flex-wrap items-center justify-end gap-3">
                  {learnCheckpoint.allDone ? (
                    <button className="cursor-pointer rounded-2xl bg-[#2b2828] px-5 py-3 text-sm font-extrabold text-white transition hover:bg-[#8a0018]" type="button" onClick={() => {
                      const firstRoundKeys = learnSeedKeys.slice(0, LEARN_TURN_SIZE);
                      setLearnQueue(firstRoundKeys);
                      setLearnNextQueue([]);
                      setLearnIntroducedCount(Math.min(LEARN_TURN_SIZE, learnSeedKeys.length));
                      setLearnTurnNumber(1);
                      setLearnTurnAnsweredCount(0);
                      setLearnMasteryMap(
                        Object.fromEntries(learnSeedKeys.map((key) => [key, { correctCount: 0, wrongCount: 0, seen: false }]))
                      );
                      setLearnCheckpoint(null);
                      setLearnRetryMode(false);
                      setLearnRoundKeys(firstRoundKeys);
                      setQuizChoice('');
                    }}>
                      Học lại từ đầu
                    </button>
                  ) : (
                    <button className="cursor-pointer rounded-2xl bg-[#8a0018] px-5 py-3 text-sm font-extrabold text-white shadow-lg shadow-[#8a0018]/15 transition hover:-translate-y-0.5 hover:bg-[#6f0014]" type="button" onClick={continueLearnAfterCheckpoint}>
                      Tiếp tục học
                    </button>
                  )}
                </div>
              </div>
            ) : (
              <div className="rounded-[18px] border border-[#dfe3ee] bg-white p-8 shadow-sm">
                <p className="text-sm font-bold uppercase tracking-[0.12em] text-[#8c716f]">Hoàn thành lượt học</p>
                <h3 className="mt-3 font-['Manrope'] text-3xl font-extrabold text-[#2b2828]">Bạn đã hoàn thành lượt học này</h3>
                <p className="mt-3 text-sm leading-7 text-[#584140]">Bạn đã xử lý hết các lượt học hiện tại. Có thể bắt đầu lại để ôn toàn bộ bộ từ một lượt mới.</p>
                <button className="mt-5 cursor-pointer rounded-2xl bg-[#2b2828] px-5 py-3 text-sm font-extrabold text-white transition hover:bg-[#8a0018]" type="button" onClick={() => {
                  const firstRoundKeys = learnSeedKeys.slice(0, LEARN_TURN_SIZE);
                  setLearnQueue(firstRoundKeys);
                  setLearnNextQueue([]);
                  setLearnIntroducedCount(Math.min(LEARN_TURN_SIZE, learnSeedKeys.length));
                  setLearnTurnNumber(1);
                  setLearnTurnAnsweredCount(0);
                  setLearnMasteryMap(
                    Object.fromEntries(learnSeedKeys.map((key) => [key, { correctCount: 0, wrongCount: 0, seen: false }]))
                  );
                  setLearnCheckpoint(null);
                  setLearnRetryMode(false);
                  setLearnRoundKeys(firstRoundKeys);
                  setQuizChoice('');
                }}>
                  Học lại từ đầu
                </button>
              </div>
            )}
          </div>
        </div>
      ) : null}

      {mode === 'learn-old' ? (
        <div className="mx-auto max-w-[900px] rounded-[28px] border border-[#dfbfbd]/20 bg-white p-8 shadow-sm">
          <div className="mb-8 flex items-center gap-2">
            {Array.from({ length: 6 }).map((_, index) => (
              <div key={index} className="h-4 flex-1 rounded-full bg-[#dfe3ee]">
                <div className={`h-full rounded-full ${index === 0 ? 'w-1/2 bg-[#8a0018]' : 'w-0'}`} />
              </div>
            ))}
          </div>
          <div className="flex items-center justify-between gap-4">
            <p className="text-sm font-bold text-[#59627a]">Thuật ngữ</p>
            <span className="text-sm font-extrabold text-[#8a0018]">{activeIndex + 1}/{studyTerms.length}</span>
          </div>
          <h3 className="mt-6 min-h-[130px] text-2xl font-medium leading-10 text-[#252b3a]">{activeTerm.meaning}</h3>
          <p className="mt-4 text-sm font-bold text-[#59627a]">Chọn đáp án đúng</p>
          <div className="mt-5 grid gap-4 md:grid-cols-2">
            {quizOptions.map((option, index) => {
              const selected = quizChoice === option.termKey;
              const correct = quizChoice && option.termKey === activeTerm.termKey;
              const wrong = selected && option.termKey !== activeTerm.termKey;
              return (
                <button
                  key={option.termKey}
                  className={`cursor-pointer rounded-2xl border px-5 py-4 text-left font-bold transition hover:border-[#8a0018]/40 ${correct ? 'border-[#176b3a] bg-[#e7f6ec] text-[#176b3a]' : wrong ? 'border-[#ba1a1a] bg-[#ffdad6] text-[#93000a]' : 'border-[#dfbfbd]/30 bg-[#fffdfc] text-[#2b2828]'}`}
                  type="button"
                  onClick={() => {
                    setQuizChoice(option.termKey);
                    persistTerm(activeTerm.termKey, { status: option.termKey === activeTerm.termKey ? 'MASTERED' : 'LEARNING' });
                  }}
                >
                  <span className="mr-3 rounded-full bg-[#f4f3f3] px-3 py-1 text-xs">{index + 1}</span>
                  {option.term}
                </button>
              );
            })}
          </div>
          <div className="mt-6 flex items-center justify-between">
            <button className="cursor-pointer text-sm font-bold text-[#3155ff]" type="button" onClick={() => { persistTerm(activeTerm.termKey, { status: 'LEARNING' }); moveCard(1); }}>Bạn không biết?</button>
            <button className="cursor-pointer rounded-2xl bg-[#2b2828] px-5 py-3 text-sm font-extrabold text-white transition hover:bg-[#8a0018]" type="button" onClick={() => moveCard(1)}>Câu tiếp theo</button>
          </div>
        </div>
      ) : null}

      {mode === 'match' ? (
        <div className="fixed inset-0 z-[95] min-h-screen overflow-y-auto bg-[#f6f7fb] px-6 py-5 md:px-8">
          <div className="mb-8 flex items-center justify-between">
            <button className="inline-flex cursor-pointer items-center gap-2 rounded-full bg-[#f1f3f8] px-5 py-3 text-sm font-extrabold text-[#59627a]" type="button">
              <span className="material-symbols-outlined text-[20px] text-[#8a0018]">extension</span>
              Ghép thẻ
              <span className="material-symbols-outlined text-[18px]">expand_more</span>
            </button>
            <div className="text-sm font-extrabold text-[#0f1b3d]">
              Lượt {matchTurnNumber}/{matchTotalTurns} · Đã ghép {matchedKeys.size}/{matchTurnTotal}
            </div>
            <div className="flex items-center gap-4 text-[#59627a]">
              <button
                className="cursor-pointer"
                type="button"
                onClick={() => {
                  setMatchTurnKeys(matchSeedKeys.slice(0, MATCH_TURN_SIZE));
                  setMatchIntroducedCount(Math.min(MATCH_TURN_SIZE, matchSeedKeys.length));
                  setMatchTurnNumber(1);
                  setMatchRound((current) => current + 1);
                }}
              >
                <span className="material-symbols-outlined">restart_alt</span>
              </button>
              <button className="cursor-pointer" type="button" onClick={() => setSettingsOpen(true)}>
                <span className="material-symbols-outlined">settings</span>
              </button>
              <button className="cursor-pointer" type="button" onClick={() => setMode('cards')}>
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>
          </div>
          <div className="mx-auto grid min-h-[calc(100vh-140px)] max-w-[1500px] grid-cols-1 gap-3 rounded-[28px] bg-[#f6f7fb] p-2 md:grid-cols-4 md:grid-rows-3">
            {matchPairs.map((card) => {
              const matched = matchedKeys.has(card.termKey);
              const selected = selectedMatch?.id === card.id;
              const correct = matched || (matchFeedback?.status === 'correct' && matchFeedback.termKey === card.termKey);
              const wrong = matchFeedback?.status === 'wrong' && matchFeedback.ids?.includes(card.id);
              return (
                <button
                  key={card.id}
                  className={`flex min-h-[150px] items-center justify-center rounded-2xl border p-5 text-center text-lg font-semibold leading-7 shadow-sm transition ${matched ? 'cursor-default border-[#dfe3ee] bg-white text-transparent shadow-none' : correct ? 'cursor-default scale-[1.02] border-[#176b3a] bg-[#e7f6ec] text-[#176b3a] shadow-[0_12px_26px_rgba(23,107,58,0.12)]' : wrong ? 'translate-x-1 cursor-pointer border-[#ba1a1a] bg-[#ffdad6] text-[#93000a]' : selected ? 'scale-[1.02] cursor-pointer border-[#8a0018] bg-[#fff0f1] text-[#8a0018]' : 'cursor-pointer border-[#d8deea] bg-white text-[#0f1b3d] hover:border-[#8a0018]/30'}`}
                  disabled={matched || matchAdvancing}
                  type="button"
                  onClick={() => selectMatch(card)}
                >
                  <span className={matched ? 'opacity-0' : ''}>{card.text}</span>
                </button>
              );
            })}
          </div>
          {matchAdvancing ? (
            <p className="mt-4 text-center text-sm font-bold text-[#176b3a]">Hoàn thành lượt này. Chuẩn bị sang lượt tiếp theo...</p>
          ) : null}
        </div>
      ) : null}

      {mode === 'match-old' ? (
        <div className="rounded-[28px] border border-[#dfbfbd]/20 bg-white p-8 shadow-sm">
          <div className="flex items-center justify-between gap-4">
            <div>
              <p className="text-[11px] font-bold uppercase tracking-[0.14em] text-[#8c716f]">Match</p>
              <h3 className="mt-2 font-['Manrope'] text-2xl font-extrabold text-[#2b2828]">Ghép từ với nghĩa</h3>
            </div>
            <button className="cursor-pointer rounded-2xl border border-[#8a0018]/20 px-4 py-3 text-sm font-bold text-[#8a0018] transition hover:bg-[#fff0f1]" type="button" onClick={() => setMode('match')}>Làm lại</button>
          </div>
          <div className="mt-6 grid gap-3 md:grid-cols-3">
            {matchPairs.map((card) => {
              const matched = matchedKeys.has(card.termKey);
              const selected = selectedMatch?.id === card.id;
              return (
                <button key={card.id} className={`min-h-[110px] cursor-pointer rounded-2xl border p-4 text-left text-sm font-bold leading-6 transition ${matched ? 'scale-95 border-[#176b3a] bg-[#e7f6ec] text-[#176b3a] opacity-0' : matchFeedback?.status === 'correct' && matchFeedback.termKey === card.termKey ? 'scale-105 border-[#176b3a] bg-[#e7f6ec] text-[#176b3a]' : matchFeedback?.status === 'wrong' && matchFeedback.ids?.includes(card.id) ? 'translate-x-1 border-[#ba1a1a] bg-[#ffdad6] text-[#93000a]' : selected ? 'border-[#8a0018] bg-[#fff0f1] text-[#8a0018]' : 'border-[#dfbfbd]/30 bg-[#fffdfc] text-[#2b2828] hover:border-[#8a0018]/30'}`} type="button" onClick={() => selectMatch(card)}>
                  {card.text}
                </button>
              );
            })}
          </div>
        </div>
      ) : null}

      {mode === 'detail-old' ? (
        <div className="rounded-[28px] border border-[#dfbfbd]/20 bg-white p-8 shadow-sm">
          <div className="flex items-center justify-between">
            <p className="text-[11px] font-bold uppercase tracking-[0.14em] text-[#8c716f]">{activeTerm.moduleTitle}</p>
            <button className="cursor-pointer" type="button" onClick={() => toggleStar()}>{renderStar(activeTerm)}</button>
          </div>
          <div className="mt-8 rounded-[24px] border border-[#dfbfbd]/20 bg-[#fffdfc] p-8">
            <h3 className="font-['Manrope'] text-4xl font-extrabold text-[#2b2828]">{activeTerm.term}</h3>
            <p className="mt-5 text-lg leading-8 text-[#584140]">{activeTerm.meaning}</p>
            {activeTerm.example ? <p className="mt-6 rounded-2xl bg-white p-5 text-sm leading-7 text-[#584140]">{activeTerm.example}</p> : null}
          </div>
          <div className="mt-6 grid gap-4 md:grid-cols-2">
            <button className="cursor-pointer rounded-2xl border border-[#ba1a1a]/20 bg-[#ffdad6] px-5 py-4 text-sm font-extrabold text-[#93000a] transition hover:-translate-x-1" type="button" onClick={() => { persistTerm(activeTerm.termKey, { status: 'LEARNING' }); moveCard(1); }}>
              <span className="material-symbols-outlined align-middle">arrow_back</span> Chưa thuộc
            </button>
            <button className="cursor-pointer rounded-2xl border border-[#176b3a]/20 bg-[#e7f6ec] px-5 py-4 text-sm font-extrabold text-[#176b3a] transition hover:translate-x-1" type="button" onClick={() => { persistTerm(activeTerm.termKey, { status: 'MASTERED' }); moveCard(1); }}>
              Đã thuộc <span className="material-symbols-outlined align-middle">arrow_forward</span>
            </button>
          </div>
        </div>
      ) : null}

      <div className="mx-auto max-w-[980px] space-y-6 rounded-[28px] bg-[#f6f7fb] p-5">
        <section>
          <div className="mb-4 flex items-center justify-between">
            <div>
              <h3 className="font-['Manrope'] text-xl font-extrabold text-[#ff7a00]">Đang học ({learningTerms.length})</h3>
              <p className="mt-1 text-sm text-[#252b3a]">Bạn đã bắt đầu học những thuật ngữ này. Tiếp tục phát huy nhé!</p>
            </div>
            <button className="inline-flex cursor-pointer items-center gap-2 text-sm font-bold text-[#59627a]" type="button">
              <span className="material-symbols-outlined text-[18px]">star</span>
              Chọn {learningTerms.length}
            </button>
          </div>
          {learningTerms.length ? renderTermRows(learningTerms) : <p className="rounded-2xl bg-white p-5 text-sm text-[#584140]">Chưa có thuật ngữ đang học.</p>}
        </section>

        <section>
          <h3 className="mb-4 font-['Manrope'] text-xl font-extrabold text-[#8a0018]">Chưa học ({newTerms.length})</h3>
          {newTerms.length ? renderTermRows(newTerms) : <p className="rounded-2xl bg-white p-5 text-sm text-[#584140]">Bạn đã mở hết các thuật ngữ mới.</p>}
        </section>

        <section>
          <h3 className="mb-4 font-['Manrope'] text-xl font-extrabold text-[#176b3a]">Đã thuộc ({masteredTerms.length})</h3>
          {masteredTerms.length ? renderTermRows(masteredTerms) : <p className="rounded-2xl bg-white p-5 text-sm text-[#584140]">Chưa có thuật ngữ đã thuộc.</p>}
        </section>
      </div>

      {settingsOpen ? (
        <div className="fixed inset-0 z-[80] flex items-center justify-center bg-[#111827]/45 p-4">
          <div className="w-full max-w-[640px] rounded-[22px] bg-white p-8 shadow-2xl">
            <div className="flex items-center justify-between">
              <h3 className="font-['Manrope'] text-3xl font-extrabold text-[#252b3a]">Tùy chọn</h3>
              <button className="flex h-11 w-11 cursor-pointer items-center justify-center rounded-full bg-[#f1f3f8] text-[#59627a]" type="button" onClick={() => setSettingsOpen(false)}>
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>
            <div className="mt-8 space-y-6">
              <div className="flex items-start justify-between gap-6">
                <div>
                  <p className="font-bold text-[#252b3a]">Theo dõi tiến độ</p>
                  <p className="mt-2 max-w-md text-sm leading-6 text-[#59627a]">Khi bật, mũi tên trái lưu là chưa thuộc, mũi tên phải lưu là đã thuộc.</p>
                </div>
                <Toggle checked={trackingEnabled} onChange={setTrackingEnabled} />
              </div>
              <div className="flex items-center justify-between border-t border-[#e8eaf2] pt-6">
                <p className="font-bold text-[#252b3a]">Chỉ học thuật ngữ có gắn sao</p>
                <Toggle checked={starredOnly} onChange={setStarredOnly} />
              </div>
              <div className="flex items-center justify-between border-t border-[#e8eaf2] pt-6">
                <p className="font-bold text-[#252b3a]">Mặt trước</p>
                <button className="inline-flex cursor-pointer items-center gap-2 rounded-full bg-[#f1f3f8] px-5 py-3 text-sm font-bold text-[#59627a]" type="button" onClick={() => setFrontSide((value) => (value === 'term' ? 'meaning' : 'term'))}>
                  {frontSide === 'term' ? 'Thuật ngữ' : 'Định nghĩa'}
                  <span className="material-symbols-outlined text-[18px]">expand_more</span>
                </button>
              </div>
              <div className="flex items-center justify-between border-t border-[#e8eaf2] pt-6">
                <p className="font-bold text-[#252b3a]">Phím tắt bàn phím</p>
                <button className="inline-flex cursor-pointer items-center gap-2 text-sm font-bold text-[#3155ff]" type="button" onClick={() => setKeyboardOpen((value) => !value)}>
                  Xem <span className="material-symbols-outlined text-[18px]">{keyboardOpen ? 'expand_less' : 'expand_more'}</span>
                </button>
              </div>
              {keyboardOpen ? (
                <div className="rounded-2xl bg-[#f6f7fb] p-4 text-sm leading-7 text-[#59627a]">
                  <p>Space: lật thẻ.</p>
                  <p>←: thẻ trước, hoặc chưa thuộc nếu bật theo dõi.</p>
                  <p>→: thẻ sau, hoặc đã thuộc nếu bật theo dõi.</p>
                </div>
              ) : null}
              <div className="flex items-center justify-between border-t border-[#e8eaf2] pt-6">
                <p className="font-bold text-[#252b3a]">Chuyển văn bản thành lời nói</p>
                <Toggle checked={textToSpeech} onChange={setTextToSpeech} />
              </div>
              <button className="block border-t border-[#e8eaf2] pt-6 text-left text-sm font-bold text-[#e53935]" type="button" onClick={() => { setTerms((current) => current.map((term) => ({ ...term, status: 'NEW' }))); setActiveIndex(0); setFlipped(false); setSettingsOpen(false); }}>Khởi động lại Thẻ ghi nhớ</button>
              <a className="block border-t border-[#e8eaf2] pt-6 text-sm font-bold text-[#3155ff]" href="/privacy">Chính sách quyền riêng tư</a>
            </div>
          </div>
        </div>
      ) : null}

      {fullscreenOpen ? (
        <div className="fixed inset-0 z-[95] bg-white">
          <div className="flex h-16 items-center justify-between border-b border-[#dfe3ee] px-8">
            <div className="flex items-center gap-3 font-bold text-[#59627a]">
              <span className="material-symbols-outlined text-[#8a0018]">style</span>
              Thẻ ghi nhớ
            </div>
            <div className="text-center text-sm font-extrabold text-[#252b3a]">
              <p>{activeIndex + 1} / {studyTerms.length}</p>
              <p>{course.title}</p>
            </div>
            <div className="flex items-center gap-4 text-[#59627a]">
              <button className="cursor-pointer" type="button" onClick={() => setSettingsOpen(true)}><span className="material-symbols-outlined">settings</span></button>
              <button className="cursor-pointer" type="button" onClick={() => setFullscreenOpen(false)}><span className="material-symbols-outlined">close</span></button>
            </div>
          </div>
          <div className="mx-auto flex h-[calc(100vh-64px)] max-w-[1100px] flex-col justify-center px-8 pb-10">
            {renderCard(true)}
            {renderControls(true)}
          </div>
        </div>
      ) : null}
    </section>
  );
};

export default WorkspaceFlashcards;
