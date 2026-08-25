package fu.sep490.g23.backend.service.course;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Last step of recommendCourses: turn a fully ranked list into a mixed shortlist.
 *
 * Why not just take top 6 by score?
 * Weak-skill courses and level-matched courses often compete. Taking raw top-N can
 * hide one of the two groups. This selector caps each group at 3 so the UI shows both.
 *
 * Pool:
 *   Prefer items whose current band sits in the course window (bandCompatible).
 *   If that subset is empty (courses often lack min/target), fall back to the full ranked list
 *   so we still recommend something.
 *
 * Then:
 *   No weak-skill data → top 6 of that pool.
 *   Has weak skills → up to 3 weak-skill courses, then up to 3 that are NOT weak-skill
 *   (those are the best remaining level/exam matches). Weak-skill items stay first.
 */
public final class BalancedCourseRecommendationSelector {

    private BalancedCourseRecommendationSelector() {
    }

    public static <T> List<T> select(
            List<T> rankedItems,
            Predicate<T> bandCompatible,
            Predicate<T> matchesWeakSkill,
            boolean hasWeakSkills
    ) {
        List<T> matchingBand = rankedItems.stream().filter(bandCompatible).toList();
        List<T> eligibleItems = matchingBand.isEmpty() ? rankedItems : matchingBand;
        if (!hasWeakSkills) {
            return eligibleItems.stream().limit(6).toList();
        }

        List<T> weakSkillItems = eligibleItems.stream()
                .filter(matchesWeakSkill)
                .limit(3)
                .toList();
        List<T> levelItems = eligibleItems.stream()
                .filter(matchesWeakSkill.negate())
                .limit(3)
                .toList();
        return Stream.concat(weakSkillItems.stream(), levelItems.stream()).toList();
    }
}
