package fu.sap490.g23.backend.service.assessment;

import com.fasterxml.jackson.databind.JsonNode;
import fu.sap490.g23.backend.dto.request.assessment.PlacementTestDefinitionRequest;
import fu.sap490.g23.backend.dto.response.assessment.PlacementTestDefinitionResponse;
import fu.sap490.g23.backend.dto.response.assessment.PlacementTestMonitoringResponse;
import fu.sap490.g23.backend.entity.assessment.PlacementTestDefinition;

public interface PlacementTestDefinitionService {

    String TEST_CODE = "IELTS_PLACEMENT_CURRENT";

    PlacementTestDefinition getDefinition();

    PlacementTestDefinitionResponse getManagementDefinition();

    PlacementTestDefinitionResponse updateDefinition(PlacementTestDefinitionRequest request);

    PlacementTestMonitoringResponse getMonitoring();

    JsonNode getConfig(PlacementTestDefinition definition, String skill);
}
