package lk.leoclub.clubprojects.web;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.leoclub.clubprojects.service.AdminService;

/**
 * Blocks restricted requests unless the caller presents a valid admin token.
 *
 * <p>This is the part that actually enforces the rule. Hiding buttons in the
 * browser would stop nobody — anyone could call the API directly — so the check
 * lives here, on the way in.
 */
public class AdminGuard implements HandlerInterceptor {

    /** Sent by the UI on every request once an admin has signed in. */
    public static final String HEADER = "X-Admin-Token";

    public static final String DELETE_MESSAGE =
            "Only admins can delete a project or a committee member.";
    public static final String STRUCTURE_MESSAGE =
            "Only admins can change project types and categories.";
    public static final String SITE_MESSAGE =
            "Only admins can change the club logo.";

    private final AdminService admin;
    private final Set<String> guardedMethods;
    private final String message;

    public AdminGuard(AdminService admin, Set<String> guardedMethods, String message) {
        this.admin = admin;
        this.guardedMethods = guardedMethods.stream()
                .map(m -> m.toUpperCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        this.message = message;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        if (!guardedMethods.contains(request.getMethod().toUpperCase(Locale.ROOT))) {
            return true;
        }
        if (admin.isSignedIn(request.getHeader(HEADER))) {
            return true;
        }

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"status\":403,\"message\":\"" + message + "\"}");
        return false;
    }
}
