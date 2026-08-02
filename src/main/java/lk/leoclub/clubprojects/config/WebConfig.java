package lk.leoclub.clubprojects.config;

import java.util.Set;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lk.leoclub.clubprojects.service.AdminService;
import lk.leoclub.clubprojects.web.AdminGuard;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AdminService admin;

    public WebConfig(AdminService admin) {
        this.admin = admin;
    }

    /**
     * Guards deletion of projects and committee members.
     *
     * <p>The patterns match a single path segment on purpose:
     * {@code /api/projects/*} covers {@code /api/projects/{id}} but deliberately
     * not {@code /api/projects/{id}/thumbnail}, so anyone can still swap a photo.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Deleting records: admins only. Adding and editing stay open.
        registry.addInterceptor(new AdminGuard(admin, Set.of("DELETE"), AdminGuard.DELETE_MESSAGE))
                .addPathPatterns("/api/projects/*", "/api/committee/*");

        // The form's own structure — types and categories — is admin-only to
        // change at all, since it affects every project card.
        registry.addInterceptor(new AdminGuard(admin, Set.of("POST", "PUT", "DELETE"),
                        AdminGuard.STRUCTURE_MESSAGE))
                .addPathPatterns("/api/catalog/**");

        // Same for the site logo — it appears on every screen for everyone.
        registry.addInterceptor(new AdminGuard(admin, Set.of("POST", "PUT", "DELETE"),
                        AdminGuard.SITE_MESSAGE))
                .addPathPatterns("/api/site/**");
    }

    /**
     * The UI is served from this same app, so CORS only matters when you run the
     * frontend from a separate dev server (Live Server, Vite, and friends).
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
