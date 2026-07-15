package fu.sap490.g23.backend.service.course;

import fu.sap490.g23.backend.dto.request.course.LearningPackageRequest;
import fu.sap490.g23.backend.dto.request.course.UpdatePackageBundleItemsRequest;
import fu.sap490.g23.backend.dto.response.course.LearningPackageResponse;
import fu.sap490.g23.backend.entity.course.LearningPackage;
import fu.sap490.g23.backend.entity.course.PackageBundleItem;
import fu.sap490.g23.backend.entity.course.PackageType;
import fu.sap490.g23.backend.entity.course.enums.PackageStatus;
import fu.sap490.g23.backend.entity.course.enums.PackageTypeCode;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.course.LearningPackageRepository;
import fu.sap490.g23.backend.repository.course.PackageBundleItemRepository;
import fu.sap490.g23.backend.repository.course.PackageTypeRepository;
import fu.sap490.g23.backend.service.course.impl.LearningPackageManagementServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningPackageManagementServiceImplTest {

    @Mock private LearningPackageRepository learningPackageRepository;
    @Mock private PackageTypeRepository packageTypeRepository;
    @Mock private PackageBundleItemRepository packageBundleItemRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private LearningPackageManagementServiceImpl service;

    @Test
    void createBundle_PersistsBundleAndChildren() {
        PackageType bundleType = PackageType.builder().id(3L).code(PackageTypeCode.BUNDLE).name("Bundle").build();
        PackageType onlineType = PackageType.builder().id(1L).code(PackageTypeCode.ONLINE_COURSE).name("Online").build();
        LearningPackage child = LearningPackage.builder()
                .id(11L)
                .title("IELTS Online")
                .slug("ielts-online")
                .packageType(onlineType)
                .status(PackageStatus.PUBLISHED)
                .price(BigDecimal.TEN)
                .build();
        LearningPackage savedBundle = LearningPackage.builder()
                .id(99L)
                .title("Combo IELTS")
                .slug("combo-ielts")
                .packageType(bundleType)
                .status(PackageStatus.DRAFT)
                .price(new BigDecimal("1000000"))
                .build();

        when(packageTypeRepository.findByCode(PackageTypeCode.BUNDLE)).thenReturn(Optional.of(bundleType));
        when(learningPackageRepository.existsBySlug(any())).thenReturn(false);
        when(learningPackageRepository.save(any(LearningPackage.class))).thenReturn(savedBundle);
        when(learningPackageRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.of(savedBundle));
        when(learningPackageRepository.findByIdInAndDeletedFalse(anyCollection())).thenReturn(List.of(child));
        when(packageBundleItemRepository.findByBundlePackageIdOrderByDisplayOrderAscIdAsc(99L)).thenReturn(List.of(
                PackageBundleItem.builder().bundlePackage(savedBundle).childPackage(child).displayOrder(0).build()
        ));

        LearningPackageRequest request = LearningPackageRequest.builder()
                .packageTypeCode(PackageTypeCode.BUNDLE)
                .title("Combo IELTS")
                .price(new BigDecimal("1000000"))
                .childPackageIds(List.of(11L))
                .build();

        LearningPackageResponse response = service.createBundle(request, "cm@englishlab.vn");

        assertEquals(99L, response.getId());
        assertEquals(PackageTypeCode.BUNDLE, response.getPackageTypeCode());
        assertEquals(1, response.getChildCount());
        verify(packageBundleItemRepository).deleteByBundlePackageId(99L);
        verify(packageBundleItemRepository).saveAll(anyList());
    }

    @Test
    void createBundle_RejectsNonBundleType() {
        LearningPackageRequest request = LearningPackageRequest.builder()
                .packageTypeCode(PackageTypeCode.ONLINE_COURSE)
                .title("Should Fail")
                .build();

        assertThrows(IllegalArgumentException.class, () -> service.createBundle(request, "cm@englishlab.vn"));
        verify(learningPackageRepository, never()).save(any());
    }

    @Test
    void publishBundle_RequiresAtLeastOneChild() {
        PackageType bundleType = PackageType.builder().id(3L).code(PackageTypeCode.BUNDLE).name("Bundle").build();
        LearningPackage bundle = LearningPackage.builder()
                .id(99L)
                .title("Empty Bundle")
                .packageType(bundleType)
                .status(PackageStatus.DRAFT)
                .build();
        when(learningPackageRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.of(bundle));
        when(packageBundleItemRepository.countByBundlePackageId(99L)).thenReturn(0L);

        assertThrows(IllegalArgumentException.class, () -> service.publishBundle(99L));
    }

    @Test
    void replaceBundleItems_RejectsSelfReference() {
        PackageType bundleType = PackageType.builder().id(3L).code(PackageTypeCode.BUNDLE).name("Bundle").build();
        LearningPackage bundle = LearningPackage.builder()
                .id(99L)
                .title("Bundle")
                .packageType(bundleType)
                .status(PackageStatus.DRAFT)
                .build();
        when(learningPackageRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.of(bundle));

        UpdatePackageBundleItemsRequest request = new UpdatePackageBundleItemsRequest(List.of(99L));
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.replaceBundleItems(99L, request)
        );
        assertTrue(error.getMessage().contains("chính nó"));
    }

    @Test
    void updateBundle_RejectsNonBundlePackage() {
        PackageType onlineType = PackageType.builder().id(1L).code(PackageTypeCode.ONLINE_COURSE).name("Online").build();
        LearningPackage online = LearningPackage.builder()
                .id(11L)
                .title("Online")
                .packageType(onlineType)
                .status(PackageStatus.PUBLISHED)
                .build();
        when(learningPackageRepository.findByIdAndDeletedFalse(11L)).thenReturn(Optional.of(online));

        LearningPackageRequest request = LearningPackageRequest.builder().title("Hack").build();
        assertThrows(IllegalArgumentException.class, () -> service.updateBundle(11L, request));
    }
}
