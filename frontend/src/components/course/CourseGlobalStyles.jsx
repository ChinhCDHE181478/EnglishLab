const CourseGlobalStyles = () => (
  <style>{`
    @import url('https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:wght,FILL@100..700,0..1&display=swap');

    .course-card {
      transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), box-shadow 0.3s ease;
    }

    .course-card:hover {
      transform: translateY(-6px);
      box-shadow: 0 10px 25px -5px rgba(26, 28, 28, 0.08);
    }

    .course-scrollbar::-webkit-scrollbar {
      width: 6px;
      height: 6px;
    }

    .course-scrollbar::-webkit-scrollbar-thumb {
      background: #e2e2e2;
      border-radius: 10px;
    }

    .glow-card {
      position: relative;
      --mouse-x: 50%;
      --mouse-y: 50%;
    }

    .glow-card::before {
      content: '';
      position: absolute;
      inset: 0;
      background: radial-gradient(400px circle at var(--mouse-x) var(--mouse-y), rgba(255,255,255,0.15), transparent 80%);
      z-index: 1;
      pointer-events: none;
    }
  `}</style>
);

export default CourseGlobalStyles;
