package de.emphasize.time2.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
public class CorsConfig implements WebMvcConfigurer {

  @Value("${allowedOrigins}")
  String allowedOrigins;

  @Bean
  public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
        registry
            .addMapping("/")
            .allowedMethods("GET", "POST", "DELETE")
            .allowedOrigins(allowedOrigins);
        registry
            .addMapping("/event")
            .allowedMethods("GET", "POST", "DELETE")
            .allowedOrigins(allowedOrigins);
        registry
            .addMapping("/layout")
            .allowedMethods("GET", "POST", "DELETE")
            .allowedOrigins(allowedOrigins);
        registry
            .addMapping("/note")
            .allowedMethods("GET", "POST", "DELETE")
            .allowedOrigins(allowedOrigins);
        registry
            .addMapping("/overview")
            .allowedMethods("GET", "POST", "DELETE")
            .allowedOrigins(allowedOrigins);
      }
    };
  }
}
