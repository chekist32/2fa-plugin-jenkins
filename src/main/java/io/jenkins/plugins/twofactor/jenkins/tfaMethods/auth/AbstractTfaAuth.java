package io.jenkins.plugins.twofactor.jenkins.tfaMethods.auth;

import hudson.model.Action;
import hudson.model.User;
import io.jenkins.plugins.twofactor.jenkins.CookieUtils;
import io.jenkins.plugins.twofactor.jenkins.tfaFilters.TwoFactorAuthFilter;
import io.jenkins.plugins.twofactor.jenkins.tfaFilters.TwoFactorAuthResetFilter;
import io.jenkins.plugins.twofactor.jenkins.tfaMethods.TfaMethodType;
import io.jenkins.plugins.twofactor.jenkins.user.UserTfaCookieTokenService;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public abstract class AbstractTfaAuth implements Action {
    private static final Logger LOGGER_BASE = Logger.getLogger(AbstractTfaAuth.class.getName());

    private final TfaMethodType methodType;
    final static Map<String, Boolean> showWrongCredentialWarning = new HashMap<>();
    private final UserTfaCookieTokenService userTfaCookieTokenService;

    public AbstractTfaAuth(TfaMethodType methodType) {
        this(UserTfaCookieTokenService.getInstance(), methodType);
    }

    public AbstractTfaAuth(
            UserTfaCookieTokenService userTfaCookieTokenService,
            TfaMethodType methodType
    ) {
        this.userTfaCookieTokenService = userTfaCookieTokenService;
        this.methodType = methodType;
    }


    abstract public void doAuthenticate(StaplerRequest req, StaplerResponse res)
            throws ServletException, IOException;

    protected void allow2FaAccessAndRedirect(
            HttpSession session,
            HttpServletResponse res,
            User user
    ) {
        String redirectUrl = "/";
        if (session != null) {
            var tfaRelayState = (String) session.getAttribute(TwoFactorAuthFilter.REDIRECT_URL_NAME);
            if (tfaRelayState != null) redirectUrl = tfaRelayState;
            session.removeAttribute(TwoFactorAuthFilter.REDIRECT_URL_NAME);

            session.setAttribute( TwoFactorAuthResetFilter.TFA_RESET_NAME+methodType.name(), true);

            res.addCookie(
                    CookieUtils.createTfaSessionCookie(
                            userTfaCookieTokenService.addToken(user.getId()
                            )
                    )
            );

            showWrongCredentialWarning.remove(user.getId());
        } else {
            LOGGER_BASE.fine("Session is null, hence not authenticating the user");
        }
        res.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
        res.setHeader("Location", redirectUrl);
    }

    @Override
    public String getIconFileName() {
        return "";
    }

    @Override
    public String getDisplayName() {
        return methodType.getAuthPath();
    }

    @Override
    public String getUrlName() {
        return methodType.getAuthPath();
    }

    public String getUserId() {
        var user = User.current();
        return user != null ? user.getId() : "";
    }

    public boolean getShowWrongCredentialWarning(String userId) {
        return showWrongCredentialWarning.getOrDefault(userId, false);
    }
}
