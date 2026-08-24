package fu.sep490.g23.backend;

import java.io.IOException;
import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BackendApplication {

    public static void main(String[] args) {
        configureUtf8Console();
        // JDBC sets session TimeZone from JVM; Windows often reports Asia/Saigon,
        // which some Postgres images reject. Prefer the IANA name Postgres accepts.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SpringApplication.run(BackendApplication.class, args);
    }

    private static void configureUtf8Console() {
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            switchWindowsConsoleToUtf8();
        }
    }

    private static void switchWindowsConsoleToUtf8() {
        try {
            Process process = new ProcessBuilder("chcp.com", "65001")
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            process.waitFor();
        } catch (IOException exception) {
            // Redirected IDE consoles may not expose a Windows console; their decoder handles UTF-8 directly.
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

}
