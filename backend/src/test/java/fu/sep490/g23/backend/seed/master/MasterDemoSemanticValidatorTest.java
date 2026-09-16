package fu.sep490.g23.backend.seed.master;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MasterDemoSemanticValidatorTest {

    private final MasterDemoSemanticValidator validator = new MasterDemoSemanticValidator();

    @Test
    void acceptsNaturalVietnameseNames() {
        assertTrue(validator.isValidVietnameseName("Nguyễn Minh Anh"));
        assertTrue(validator.isValidVietnameseName("Trần Hoàng Long"));
        assertTrue(validator.isValidVietnameseName("Lê Thu Trang"));
    }

    @Test
    void rejectsJobTitlesAndNumberedNames() {
        assertFalse(validator.isValidVietnameseName("Quản lý Content"));
        assertFalse(validator.isValidVietnameseName("Nhân viên vận hành"));
        assertFalse(validator.isValidVietnameseName("Học viên 001"));
        assertFalse(validator.isValidVietnameseName("Teacher Demo"));
        assertFalse(validator.isValidVietnameseName("Nguyễn An 17"));
    }
}
