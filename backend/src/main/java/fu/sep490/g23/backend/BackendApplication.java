package fu.sep490.g23.backend;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BackendApplication {

    public static void main(String[] args) {
        // JDBC sets session TimeZone from JVM; Windows often reports Asia/Saigon,
        // which some Postgres images reject. Prefer the IANA name Postgres accepts.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SpringApplication.run(BackendApplication.class, args);
    }

}
