package fu.sep490.g23.backend.it;

import java.util.TimeZone;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Chạy trước khi Spring tạo DataSource — tránh Postgres từ chối Asia/Saigon khi chạy test từ IDE.
 */
public class ItTimezoneInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    static {
        System.setProperty("user.timezone", "Asia/Ho_Chi_Minh");
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        System.setProperty("user.timezone", "Asia/Ho_Chi_Minh");
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }
}
