package fu.sep490.g23.backend.service.course;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BalancedCourseRecommendationSelectorTest {

    @Test
    void select_LimitsWeakSkillCoursesAndKeepsMatchingBandCourses() {
        List<Candidate> ranked = List.of(
                candidate("writing-5", true, true),
                candidate("writing-6", true, true),
                candidate("writing-7", true, true),
                candidate("writing-8", true, true),
                candidate("reading-5", true, false),
                candidate("listening-5", true, false),
                candidate("speaking-5", true, false),
                candidate("general-other-band", false, false)
        );

        List<Candidate> result = BalancedCourseRecommendationSelector.select(
                ranked,
                Candidate::bandCompatible,
                Candidate::matchesWeakSkill,
                true
        );

        assertThat(result).extracting(Candidate::id).containsExactly(
                "writing-5",
                "writing-6",
                "writing-7",
                "reading-5",
                "listening-5",
                "speaking-5"
        );
    }

    @Test
    void select_FallsBackToRankedCoursesWhenNoBandMetadataMatches() {
        List<Candidate> ranked = List.of(
                candidate("first", false, false),
                candidate("second", false, false)
        );

        List<Candidate> result = BalancedCourseRecommendationSelector.select(
                ranked,
                Candidate::bandCompatible,
                Candidate::matchesWeakSkill,
                false
        );

        assertThat(result).extracting(Candidate::id).containsExactly("first", "second");
    }

    private static Candidate candidate(String id, boolean bandCompatible, boolean matchesWeakSkill) {
        return new Candidate(id, bandCompatible, matchesWeakSkill);
    }

    private record Candidate(String id, boolean bandCompatible, boolean matchesWeakSkill) {
    }
}
