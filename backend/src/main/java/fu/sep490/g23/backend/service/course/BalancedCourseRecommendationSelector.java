package fu.sep490.g23.backend.service.course;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

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
