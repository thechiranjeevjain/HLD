package dev.interview.agent.config;
import org.springframework.context.annotation.*; import org.springframework.scheduling.annotation.EnableAsync; import org.springframework.security.config.annotation.web.builders.HttpSecurity; import org.springframework.security.web.SecurityFilterChain; import org.springframework.web.servlet.config.annotation.*; import java.time.Clock;
@Configuration @EnableAsync public class AppConfig {
 @Bean Clock clock(){return Clock.systemUTC();}
 @Bean SecurityFilterChain security(HttpSecurity h)throws Exception{return h.csrf(c->c.disable()).authorizeHttpRequests(a->a.requestMatchers("/api/health","/actuator/health","/ws/**").permitAll().anyRequest().permitAll()).build();}
 @Bean WebMvcConfigurer cors(){return new WebMvcConfigurer(){public void addCorsMappings(CorsRegistry r){r.addMapping("/api/**").allowedOrigins("http://localhost:3000").allowedMethods("*");}};}
}
