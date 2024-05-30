package io.jenkins.plugins.twofactor.jenkins.tfaFilters;

import hudson.init.Initializer;
import hudson.model.User;
import hudson.util.PluginServletFilter;
import io.jenkins.plugins.twofactor.jenkins.CookieUtils;
import io.jenkins.plugins.twofactor.jenkins.user.UserTfaCookieTokenService;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public class TwoFactorAuthLogoutFilter implements Filter {
    private static final RequestMatcher LOGOUT_MATCHER = new AntPathRequestMatcher("/logout");

    private final UserTfaCookieTokenService userTfaCookieTokenService;

    public TwoFactorAuthLogoutFilter() {
        this(UserTfaCookieTokenService.getInstance());
    }

    public TwoFactorAuthLogoutFilter(UserTfaCookieTokenService userTfaCookieTokenService) {
        this.userTfaCookieTokenService = userTfaCookieTokenService;
    }


    @Initializer
    public static void setUp() throws ServletException {
        PluginServletFilter.addFilter(new TwoFactorAuthLogoutFilter());
    }

    private void invalidateTfaToken(HttpServletResponse res, UUID token) {
        res.addCookie(CookieUtils.createTfaSessionCookie(token, 0));
    }

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {
        var req = (HttpServletRequest) request;
        var res = (HttpServletResponse) response;

        if (!LOGOUT_MATCHER.matches(req)) {
            chain.doFilter(request, response);
            return;
        }

        var user = User.current();
        if (user == null) {
            chain.doFilter(request, response);
            return;
        }

        Optional<Cookie> optionalCookie = CookieUtils.findCookieByName(req, TwoFactorAuthFilter.TFA_COOKIE_NAME);
        if (optionalCookie.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        try {
            var uuid = UUID.fromString(optionalCookie.get().getValue());
            invalidateTfaToken(res, uuid);
            userTfaCookieTokenService.invalidateToken(user.getId(), uuid);
            res.sendRedirect("/login");
        } catch (IllegalArgumentException ignored) {  }

        chain.doFilter(request, response);
    }
}
