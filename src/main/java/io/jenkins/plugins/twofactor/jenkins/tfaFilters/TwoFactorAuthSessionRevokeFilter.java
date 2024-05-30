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
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public class TwoFactorAuthSessionRevokeFilter implements Filter {
    private static final RequestMatcher SESSION_TERMINATION_MATCHER = new AntPathRequestMatcher("/user/**/descriptorByName/jenkins.security.seed.UserSeedProperty/renewSessionSeed");

    private final UserTfaCookieTokenService userTfaCookieTokenService;

    public TwoFactorAuthSessionRevokeFilter() {
        this(UserTfaCookieTokenService.getInstance());
    }

    public TwoFactorAuthSessionRevokeFilter(UserTfaCookieTokenService userTfaCookieTokenService) {
        this.userTfaCookieTokenService = userTfaCookieTokenService;
    }

    @Initializer
    public static void setUp() throws ServletException {
        PluginServletFilter.addFilter(new TwoFactorAuthSessionRevokeFilter());
    }

    private void invalidateAllTokens(HttpServletResponse res, Collection<UUID> tokens) {
        for (var token : tokens) {
            res.addCookie(CookieUtils.createTfaSessionCookie(token, 0));
        }
    }

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {
        var req = (HttpServletRequest) request;
        var res = (HttpServletResponse) response;

        if (!SESSION_TERMINATION_MATCHER.matches(req)) {
            chain.doFilter(request, response);
            return;
        }

        Optional<Cookie> optionalCookie = CookieUtils.findCookieByName(req, TwoFactorAuthFilter.TFA_COOKIE_NAME);
        if (optionalCookie.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        var user = User.current();
        if (user != null) {
            invalidateAllTokens(res, userTfaCookieTokenService.getAllTokens(user.getId()));
            userTfaCookieTokenService.invalidateAllTokens(user.getId());
            res.sendRedirect("/login");
        }

        chain.doFilter(request, response);
    }
}
