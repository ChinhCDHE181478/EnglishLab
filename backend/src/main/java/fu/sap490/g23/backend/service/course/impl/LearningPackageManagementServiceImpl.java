package fu.sap490.g23.backend.service.course.impl;

import fu.sap490.g23.backend.dto.request.course.LearningPackageRequest;
import fu.sap490.g23.backend.dto.request.course.UpdatePackageBundleItemsRequest;
import fu.sap490.g23.backend.dto.response.course.LearningPackageResponse;
import fu.sap490.g23.backend.dto.response.course.LearningPackageSummaryResponse;
import fu.sap490.g23.backend.dto.response.course.PackageTypeResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.course.LearningPackage;
import fu.sap490.g23.backend.entity.course.PackageBundleItem;
import fu.sap490.g23.backend.entity.course.PackageType;
import fu.sap490.g23.backend.entity.course.enums.PackageStatus;
import fu.sap490.g23.backend.entity.course.enums.PackageTypeCode;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.course.LearningPackageRepository;
import fu.sap490.g23.backend.repository.course.PackageBundleItemRepository;
import fu.sap490.g23.backend.repository.course.PackageTypeRepository;
import fu.sap490.g23.backend.service.course.LearningPackageManagementService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class LearningPackageManagementServiceImpl implements LearningPackageManagementService {

    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Set<PackageTypeCode> BUNDLE_CHILD_TYPES = Set.of(
            PackageTypeCode.ONLINE_COURSE,
            PackageTypeCode.CLASSROOM
    );

    private final LearningPackageRepository learningPackageRepository;
    private final PackageTypeRepository packageTypeRepository;
    private final PackageBundleItemRepository packageBundleItemRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PackageTypeResponse> getPackageTypes() {
        return packageTypeRepository.findAll().stream()
                .sorted((left, right) -> left.getCode().name().compareTo(right.getCode().name()))
                .map(this::toTypeResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LearningPackageResponse> listPackages(
            String keyword,
            PackageTypeCode packageTypeCode,
            PackageStatus status,
            Pageable pageable
    ) {
        Specification<LearningPackage> specification = (root, query, builder) -> {
            if (query != null) {
                query.distinct(true);
            }
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.isFalse(root.get("deleted")));

            if (packageTypeCode != null) {
                Join<LearningPackage, PackageType> typeJoin = root.join("packageType");
                predicates.add(builder.equal(typeJoin.get("code"), packageTypeCode));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            String cleaned = clean(keyword);
            if (cleaned != null) {
                String like = "%" + cleaned.toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("title")), like),
                        builder.like(builder.lower(root.get("slug")), like)
                ));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };

        return learningPackageRepository.findAll(specification, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LearningPackageSummaryResponse> listBundleCandidates() {
        return learningPackageRepository.findCandidatesByTypeCodes(BUNDLE_CHILD_TYPES).stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LearningPackageResponse getPackage(Long id) {
        return toResponse(findActivePackage(id));
    }

    @Override
    public LearningPackageResponse createBundle(LearningPackageRequest request, String creatorEmail) {
        PackageTypeCode typeCode = request.getPackageTypeCode() == null
                ? PackageTypeCode.BUNDLE
                : request.getPackageTypeCode();
        if (typeCode != PackageTypeCode.BUNDLE) {
            throw new IllegalArgumentException("Content Manager chỉ tạo gói loại BUNDLE tại đây. Khóa online/lớp học dùng màn hình tương ứng.");
        }

        PackageType packageType = packageTypeRepository.findByCode(PackageTypeCode.BUNDLE)
                .orElseThrow(() -> new IllegalStateException("Thiếu cấu hình loại gói BUNDLE. Hãy chạy seed package types."));

        User creator = creatorEmail == null || creatorEmail.isBlank()
                ? null
                : userRepository.findByEmail(creatorEmail).orElse(null);

        LearningPackage learningPackage = LearningPackage.builder()
                .packageType(packageType)
                .title(requireTitle(request.getTitle()))
                .slug(generateUniqueSlug(request.getTitle()))
                .shortDescription(clean(request.getShortDescription()))
                .description(clean(request.getDescription()))
                .targetScore(clean(request.getTargetScore()))
                .duration(clean(request.getDuration()))
                .studyMode(clean(request.getStudyMode()))
                .price(defaultPrice(request.getPrice()))
                .salePrice(resolveSalePrice(request.getPrice(), request.getSalePrice()))
                .thumbnailUrl(clean(request.getThumbnailUrl()))
                .status(request.getStatus() == null ? PackageStatus.DRAFT : request.getStatus())
                .displayOrder(defaultOrder(request.getDisplayOrder()))
                .featured(Boolean.TRUE.equals(request.getFeatured()))
                .createdBy(creator)
                .build();

        LearningPackage saved = learningPackageRepository.save(learningPackage);
        if (request.getChildPackageIds() != null && !request.getChildPackageIds().isEmpty()) {
            replaceItems(saved, request.getChildPackageIds());
        }
        return reloadResponse(saved.getId());
    }

    @Override
    public LearningPackageResponse updateBundle(Long id, LearningPackageRequest request) {
        LearningPackage learningPackage = requireBundle(findActivePackage(id));
        learningPackage.setTitle(requireTitle(request.getTitle()));
        learningPackage.setShortDescription(clean(request.getShortDescription()));
        learningPackage.setDescription(clean(request.getDescription()));
        learningPackage.setTargetScore(clean(request.getTargetScore()));
        learningPackage.setDuration(clean(request.getDuration()));
        learningPackage.setStudyMode(clean(request.getStudyMode()));
        learningPackage.setPrice(defaultPrice(request.getPrice()));
        learningPackage.setSalePrice(resolveSalePrice(request.getPrice(), request.getSalePrice()));
        learningPackage.setThumbnailUrl(clean(request.getThumbnailUrl()));
        if (request.getStatus() != null) {
            learningPackage.setStatus(request.getStatus());
        }
        learningPackage.setDisplayOrder(defaultOrder(request.getDisplayOrder()));
        learningPackage.setFeatured(Boolean.TRUE.equals(request.getFeatured()));

        LearningPackage saved = learningPackageRepository.save(learningPackage);
        if (request.getChildPackageIds() != null) {
            replaceItems(saved, request.getChildPackageIds());
        }
        return reloadResponse(saved.getId());
    }

    @Override
    public LearningPackageResponse replaceBundleItems(Long id, UpdatePackageBundleItemsRequest request) {
        LearningPackage bundle = requireBundle(findActivePackage(id));
        List<Long> childIds = request == null || request.getChildPackageIds() == null
                ? List.of()
                : request.getChildPackageIds();
        replaceItems(bundle, childIds);
        return reloadResponse(bundle.getId());
    }

    @Override
    public LearningPackageResponse publishBundle(Long id) {
        LearningPackage bundle = requireBundle(findActivePackage(id));
        assertBundlePublishable(bundle);
        if (bundle.getStatus() != PackageStatus.DRAFT
                && bundle.getStatus() != PackageStatus.PENDING_REVIEW
                && bundle.getStatus() != PackageStatus.REJECTED) {
            throw new IllegalArgumentException("Gói không ở trạng thái có thể xuất bản.");
        }
        bundle.setStatus(PackageStatus.PUBLISHED);
        bundle.setReviewNote(null);
        learningPackageRepository.save(bundle);
        return reloadResponse(id);
    }

    @Override
    public LearningPackageResponse submitBundleForReview(Long id) {
        LearningPackage bundle = requireBundle(findActivePackage(id));
        assertBundlePublishable(bundle);
        if (bundle.getStatus() != PackageStatus.DRAFT && bundle.getStatus() != PackageStatus.REJECTED) {
            throw new IllegalArgumentException("Chỉ gói nháp hoặc bị từ chối mới có thể gửi duyệt.");
        }
        bundle.setStatus(PackageStatus.PENDING_REVIEW);
        bundle.setSubmittedForReviewAt(java.time.LocalDateTime.now());
        bundle.setReviewNote(null);
        learningPackageRepository.save(bundle);
        return reloadResponse(id);
    }

    @Override
    public LearningPackageResponse archiveBundle(Long id) {
        LearningPackage bundle = requireBundle(findActivePackage(id));
        bundle.setStatus(PackageStatus.ARCHIVED);
        learningPackageRepository.save(bundle);
        return reloadResponse(id);
    }

    @Override
    public void deleteBundle(Long id) {
        LearningPackage bundle = requireBundle(findActivePackage(id));
        packageBundleItemRepository.deleteByBundlePackageId(bundle.getId());
        bundle.setDeleted(true);
        bundle.setStatus(PackageStatus.ARCHIVED);
        learningPackageRepository.save(bundle);
    }

    private void replaceItems(LearningPackage bundle, List<Long> childPackageIds) {
        List<Long> orderedUniqueIds = new ArrayList<>(new LinkedHashSet<>(
                childPackageIds.stream().filter(Objects::nonNull).toList()
        ));
        if (orderedUniqueIds.contains(bundle.getId())) {
            throw new IllegalArgumentException("Gói bundle không thể chứa chính nó.");
        }

        Map<Long, LearningPackage> childrenById = orderedUniqueIds.isEmpty()
                ? Map.of()
                : learningPackageRepository.findByIdInAndDeletedFalse(orderedUniqueIds).stream()
                .collect(Collectors.toMap(LearningPackage::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        if (childrenById.size() != orderedUniqueIds.size()) {
            throw new IllegalArgumentException("Một hoặc nhiều gói con không tồn tại hoặc đã bị xóa.");
        }

        List<PackageBundleItem> items = new ArrayList<>();
        int order = 0;
        for (Long childId : orderedUniqueIds) {
            LearningPackage child = childrenById.get(childId);
            PackageTypeCode childType = child.getPackageType() == null ? null : child.getPackageType().getCode();
            if (childType == null || !BUNDLE_CHILD_TYPES.contains(childType)) {
                throw new IllegalArgumentException(
                        "Gói con chỉ được là ONLINE_COURSE hoặc CLASSROOM. Sai loại: " + child.getTitle()
                );
            }
            if (childType == PackageTypeCode.BUNDLE) {
                throw new IllegalArgumentException("Không hỗ trợ bundle lồng bundle.");
            }
            items.add(PackageBundleItem.builder()
                    .bundlePackage(bundle)
                    .childPackage(child)
                    .displayOrder(order++)
                    .build());
        }

        packageBundleItemRepository.deleteByBundlePackageId(bundle.getId());
        if (!items.isEmpty()) {
            packageBundleItemRepository.saveAll(items);
        }
        packageBundleItemRepository.flush();
    }

    private LearningPackageResponse reloadResponse(Long id) {
        return toResponse(findActivePackage(id));
    }

    private void assertBundlePublishable(LearningPackage bundle) {
        if (packageBundleItemRepository.countByBundlePackageId(bundle.getId()) <= 0) {
            throw new IllegalArgumentException("Gói bundle cần ít nhất một sản phẩm con trước khi xuất bản/gửi duyệt.");
        }
    }

    private LearningPackage findActivePackage(Long id) {
        return learningPackageRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói học."));
    }

    private LearningPackage requireBundle(LearningPackage learningPackage) {
        PackageTypeCode code = learningPackage.getPackageType() == null
                ? null
                : learningPackage.getPackageType().getCode();
        if (code != PackageTypeCode.BUNDLE) {
            throw new IllegalArgumentException("Chỉ gói loại BUNDLE mới chỉnh sửa tại đây.");
        }
        return learningPackage;
    }

    private LearningPackageResponse toResponse(LearningPackage learningPackage) {
        List<PackageBundleItem> items = packageBundleItemRepository
                .findByBundlePackageIdOrderByDisplayOrderAscIdAsc(learningPackage.getId());
        List<LearningPackageSummaryResponse> children = items.stream()
                .map(PackageBundleItem::getChildPackage)
                .filter(Objects::nonNull)
                .map(this::toSummary)
                .toList();

        PackageType packageType = learningPackage.getPackageType();
        return LearningPackageResponse.builder()
                .id(learningPackage.getId())
                .packageTypeCode(packageType == null ? null : packageType.getCode())
                .packageTypeName(packageType == null ? null : packageType.getName())
                .title(learningPackage.getTitle())
                .slug(learningPackage.getSlug())
                .shortDescription(learningPackage.getShortDescription())
                .description(learningPackage.getDescription())
                .targetScore(learningPackage.getTargetScore())
                .duration(learningPackage.getDuration())
                .studyMode(learningPackage.getStudyMode())
                .price(learningPackage.getPrice())
                .salePrice(learningPackage.getSalePrice())
                .thumbnailUrl(learningPackage.getThumbnailUrl())
                .status(learningPackage.getStatus())
                .displayOrder(learningPackage.getDisplayOrder())
                .featured(learningPackage.isFeatured())
                .createdByName(resolveCreatedByName(learningPackage))
                .createdAt(learningPackage.getCreatedAt())
                .updatedAt(learningPackage.getUpdatedAt())
                .childCount(children.size())
                .childPackages(children)
                .build();
    }

    private String resolveCreatedByName(LearningPackage learningPackage) {
        try {
            return learningPackage.getCreatedBy() == null ? null : learningPackage.getCreatedBy().getFullName();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private LearningPackageSummaryResponse toSummary(LearningPackage learningPackage) {
        PackageType packageType = learningPackage.getPackageType();
        return LearningPackageSummaryResponse.builder()
                .id(learningPackage.getId())
                .title(learningPackage.getTitle())
                .slug(learningPackage.getSlug())
                .packageTypeCode(packageType == null ? null : packageType.getCode())
                .packageTypeName(packageType == null ? null : packageType.getName())
                .status(learningPackage.getStatus())
                .price(learningPackage.getPrice())
                .salePrice(learningPackage.getSalePrice())
                .displayOrder(learningPackage.getDisplayOrder())
                .build();
    }

    private PackageTypeResponse toTypeResponse(PackageType packageType) {
        return PackageTypeResponse.builder()
                .id(packageType.getId())
                .code(packageType.getCode())
                .name(packageType.getName())
                .description(packageType.getDescription())
                .active(packageType.isActive())
                .build();
    }

    private String requireTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Tên gói không được để trống.");
        }
        return title.trim();
    }

    private String generateUniqueSlug(String title) {
        String baseSlug = toSlug(title);
        String slug = baseSlug;
        int index = 2;
        while (learningPackageRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + index++;
        }
        return slug;
    }

    private String toSlug(String input) {
        String nowhitespace = WHITESPACE.matcher(input.trim()).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        slug = slug.replaceAll("-+", "-").toLowerCase(Locale.ENGLISH);
        return slug.isBlank() ? "bundle-package" : slug;
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private int defaultOrder(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal defaultPrice(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal resolveSalePrice(BigDecimal price, BigDecimal salePrice) {
        BigDecimal originalPrice = defaultPrice(price);
        if (salePrice == null || salePrice.compareTo(BigDecimal.ZERO) < 0 || salePrice.compareTo(originalPrice) >= 0) {
            return null;
        }
        return salePrice;
    }
}
