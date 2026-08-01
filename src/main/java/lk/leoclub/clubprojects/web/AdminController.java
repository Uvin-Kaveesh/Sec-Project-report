package lk.leoclub.clubprojects.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lk.leoclub.clubprojects.service.AdminService;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService admin;

    public AdminController(AdminService admin) {
        this.admin = admin;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        String token = admin.login(body == null ? null : body.get("password"));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("token", token);
        out.put("signedIn", true);
        return out;
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(
            @RequestHeader(value = AdminGuard.HEADER, required = false) String token) {
        admin.logout(token);
        return Map.of("signedIn", false);
    }

    /** Lets the UI check on load whether a stored token is still good. */
    @GetMapping("/session")
    public Map<String, Object> session(
            @RequestHeader(value = AdminGuard.HEADER, required = false) String token) {
        return Map.of("signedIn", admin.isSignedIn(token));
    }
}
