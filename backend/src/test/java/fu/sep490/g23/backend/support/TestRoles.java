package fu.sep490.g23.backend.support;

import fu.sep490.g23.backend.entity.Role;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public final class TestRoles {

    private TestRoles() {
    }

    public static Set<Role> roles(String... roleCodes) {
        LinkedHashSet<Role> roles = new LinkedHashSet<>();
        Arrays.stream(roleCodes)
                .map(code -> Role.builder()
                        .code(code)
                        .displayName(code)
                        .active(true)
                        .build())
                .forEach(roles::add);
        return roles;
    }
}
