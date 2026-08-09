package fu.sap490.g23.backend;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BackendApplication {

    static {
        // JDBC/pgjdbc uses ZoneId.systemDefault() (reads user.timezone). Windows often
        // reports Asia/Saigon, which some Postgres images reject. Fix both property + default
        // so @SpringBootTest also works (main() is not called in tests).
        System.setProperty("user.timezone", "Asia/Ho_Chi_Minh");
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

}
