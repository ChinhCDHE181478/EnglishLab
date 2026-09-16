package fu.sep490.g23.backend.seed.master;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.seed.master")
public class MasterDemoProperties {

    /**
     * When true, import MASTER demo dataset from classpath JSON.
     * Default false — production/dev remain unchanged unless explicitly enabled.
     */
    private boolean enabled = false;

    /**
     * When true, delete previous MASTER-marked rows before import.
     * Never deletes preserved accounts or protected course content.
     */
    private boolean cleanupBeforeImport = true;

    private String datasetClasspath = "seed/master/master-dataset.json";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isCleanupBeforeImport() {
        return cleanupBeforeImport;
    }

    public void setCleanupBeforeImport(boolean cleanupBeforeImport) {
        this.cleanupBeforeImport = cleanupBeforeImport;
    }

    public String getDatasetClasspath() {
        return datasetClasspath;
    }

    public void setDatasetClasspath(String datasetClasspath) {
        this.datasetClasspath = datasetClasspath;
    }
}
