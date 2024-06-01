package io.jenkins.plugins.twofactor.jenkins.tfaFilters;

import hudson.init.Initializer;
import hudson.util.PluginServletFilter;
import io.jenkins.plugins.twofactor.constants.PluginUrls;
import io.jenkins.plugins.twofactor.jenkins.tfaMethods.TfaMethodType;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class TwoFactorAuthResetFilter implements Filter {
    public static final String TFA_RESET_NAME = "isResetAllowed_";

    private static final RequestMatcher SECURITY_QUESTIONS_RESET_MATCHER = new AntPathRequestMatcher(
            "/**/" + TfaMethodType.SECURITY_QUESTION.getConfigPath() + "/reset"
    );
    private static final RequestMatcher OTP_OVER_EMAIL_RESET_MATCHER = new AntPathRequestMatcher(
            "/**/" + TfaMethodType.OTP_OVER_EMAIL.getConfigPath() + "/reset"
    );
    private static final RequestMatcher TOTP_RESET_MATCHER = new AntPathRequestMatcher(
            "/**/" + TfaMethodType.TOTP.getConfigPath() + "/reset"
    );

    @Initializer
    public static void setUp() throws ServletException {
        PluginServletFilter.addFilter(new TwoFactorAuthResetFilter());
    }

    private boolean redirect(
            HttpServletRequest req,
            HttpServletResponse res,
            HttpSession session,
            TfaMethodType methodType
    ) {
        var isResetAllowed = (Boolean) session.getAttribute(TFA_RESET_NAME+methodType.name());
        if (isResetAllowed == null || !isResetAllowed) {
            session.setAttribute(TFA_RESET_NAME+methodType.name(), false);
            session.setAttribute(TwoFactorAuthFilter.REDIRECT_URL_NAME, req.getRequestURI());

            res.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
            res.setHeader("Location", "/"+PluginUrls.USER_AUTH_PATH+"/"+methodType.getAuthPath());

            return true;
        }

        return false;
    }

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {
        var req = (HttpServletRequest) request;
        var res = (HttpServletResponse) response;
        HttpSession session = req.getSession(false);

        if (SECURITY_QUESTIONS_RESET_MATCHER.matches(req)) {
            if (redirect(req, res, session, TfaMethodType.SECURITY_QUESTION))
                return;
        }
        else if (OTP_OVER_EMAIL_RESET_MATCHER.matches(req)) {
            if (redirect(req, res, session, TfaMethodType.OTP_OVER_EMAIL))
                return;
        }
        else if (TOTP_RESET_MATCHER.matches(req)) {
            if (redirect(req, res, session, TfaMethodType.TOTP))
                return;
        }

        if (session != null) {
            for (var type : TfaMethodType.values()) {
                session.removeAttribute(TFA_RESET_NAME+type.name());
            }
        }

        chain.doFilter(request, response);
    }
}
