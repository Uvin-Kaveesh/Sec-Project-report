package lk.leoclub.clubprojects.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lk.leoclub.clubprojects.web.BadRequestException;

/**
 * A single shared admin password that unlocks deleting projects and committee
 * members. Everything else — adding and editing — stays open to the whole club.
 *
 * <p>Sessions are held in memory, so restarting the app signs everyone out.
 * That is fine for a club workspace; it also means there is nothing to leak.
 */
@Service
public class AdminService {

    private static final Duration SESSION_LENGTH = Duration.ofHours(8);

    private final String password;
    private final Map<String, Instant> sessions = new ConcurrentHashMap<>();

    public AdminService(@Value("${club.admin.password:leo-admin-2026}") String password) {
        this.password = password;
    }

    /** Returns a session token, or throws if the password is wrong. */
    public String login(String candidate) {
        if (candidate == null || !matches(candidate.trim())) {
            throw new BadRequestException("That password is not right.");
        }
        expireOldSessions();
        String token = UUID.randomUUID().toString();
        sessions.put(token, Instant.now().plus(SESSION_LENGTH));
        return token;
    }

    public boolean isSignedIn(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        Instant expiry = sessions.get(token);
        if (expiry == null) {
            return false;
        }
        if (expiry.isBefore(Instant.now())) {
            sessions.remove(token);
            return false;
        }
        return true;
    }

    public void logout(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    /** Compared byte-by-byte in constant time so timing gives nothing away. */
    private boolean matches(String candidate) {
        return MessageDigest.isEqual(
                candidate.getBytes(StandardCharsets.UTF_8),
                password.getBytes(StandardCharsets.UTF_8));
    }

    private void expireOldSessions() {
        Instant now = Instant.now();
        sessions.entrySet().removeIf(e -> e.getValue().isBefore(now));
    }
}
