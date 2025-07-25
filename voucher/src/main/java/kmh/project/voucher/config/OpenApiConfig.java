package kmh.project.voucher.config;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI openApiInfo() {
        return new OpenAPI()
                .info(new Info()
                        .title("Practice Voucher API")
                        .description("Practice Voucher API")
                        .version(LocalDateTime.now().toString()));
    }
}
