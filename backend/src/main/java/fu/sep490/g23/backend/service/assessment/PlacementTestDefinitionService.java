package fu.sep490.g23.backend.service.assessment;

import com.fasterxml.jackson.databind.JsonNode;
import fu.sep490.g23.backend.dto.request.assessment.PlacementTestDefinitionRequest;
import fu.sep490.g23.backend.dto.response.assessment.PlacementTestDefinitionResponse;
import fu.sep490.g23.backend.dto.response.assessment.PlacementTestMonitoringResponse;
import fu.sep490.g23.backend.entity.assessment.PlacementTestDefinition;

public interface PlacementTestDefinitionService {

    String TEST_CODE = "IELTS_PLACEMENT_CURRENT";

    PlacementTestDefinition getDefinition();

    PlacementTestDefinitionResponse getManagementDefinition();

    PlacementTestDefinitionResponse updateDefinition(PlacementTestDefinitionRequest request);

    PlacementTestMonitoringResponse getMonitoring();

    PlacementTestMonitoringResponse getMonitoring(String examType);

    JsonNode getConfig(PlacementTestDefinition definition, String skill);
}
