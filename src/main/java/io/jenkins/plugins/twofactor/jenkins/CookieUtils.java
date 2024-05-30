package io.jenkins.plugins.twofactor.jenkins;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;

import static io.jenkins.plugins.twofactor.jenkins.tfaFilters.TwoFactorAuthFilter.TFA_COOKIE_NAME;

public class CookieUtils {
    private CookieUtils() { }

    public static Cookie createTfaSessionCookie(UUID token, int timeout) {
        var cookie = new Cookie(TFA_COOKIE_NAME, token.toString());
        cookie.setPath("/");
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        cookie.setMaxAge(timeout);

        return cookie;
    }

    public static Cookie createTfaSessionCookie(UUID token) {
        return createTfaSessionCookie(token, Integer.MAX_VALUE);
    }


    public static Optional<Cookie> findCookieByName(HttpServletRequest req, String name) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) return Optional.empty();

        for (var cookie : cookies) {
            if (cookie.getName().equals(name)) return Optional.of(cookie);
        }

        return Optional.empty();
    }
}