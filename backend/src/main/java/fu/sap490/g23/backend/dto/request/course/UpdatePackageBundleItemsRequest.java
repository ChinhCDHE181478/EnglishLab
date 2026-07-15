package fu.sap490.g23.backend.dto.request.course;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePackageBundleItemsRequest {

    @NotNull(message = "Danh sách gói con không được null")
    @Builder.Default
    private List<Long> childPackageIds = new ArrayList<>();
}
