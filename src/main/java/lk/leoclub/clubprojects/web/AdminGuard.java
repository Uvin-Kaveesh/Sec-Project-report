package lk.leoclub.clubprojects.web;

import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.leoclub.clubprojects.service.AdminService;

/**
 * Blocks deletions unless the caller presents a valid admin token.
 *
 * <p>This is the part that actually enforces the rule. Hiding buttons in the
 * browser would stop nobody — anyone could call the API directly — so the check
 * lives here, on the way in.
 */
public class AdminGuard implements HandlerInterceptor {

    /** Sent by the UI on every request once an admin has signed in. */
    public static final String HEADER = "X-Admin-Token";

    static final String MESSAGE = "Only admins can delete a project or a committee member.";

    private final AdminService admin;

    public AdminGuard(AdminService admin) {
        this.admin = admin;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        // Only deletions are restricted; adding and editing stay open to everyone.
        if (!"DELETE".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (admin.isSignedIn(request.getHeader(HEADER))) {
            return true;
        }

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"status\":403,\"message\":\"" + MESSAGE + "\"}");
        return false;
    }
}
