package io.jenkins.plugins.twofactor.jenkins.tfaMethods.auth;

import hudson.Extension;
import hudson.model.User;
import hudson.util.FormApply;
import io.jenkins.plugins.twofactor.jenkins.tfaMethods.TfaMethodType;
import io.jenkins.plugins.twofactor.jenkins.tfaMethods.config.TotpConfig;
import io.jenkins.plugins.twofactor.jenkins.tfaMethods.service.TotpTfaService;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@Extension
public class TotpAuth extends AbstractTfaAuth {
    private final TotpTfaService totpTfaService;

    public TotpAuth() {
        this(TotpTfaService.getInstance());
    }

    public TotpAuth(TotpTfaService totpTfaService) {
        super(TfaMethodType.TOTP);
        this.totpTfaService = totpTfaService;
    }

    public boolean isTotpConfigured() {
        var user = User.current();
        if (user == null) return false;

        var totpConfig = user.getProperty(TotpConfig.class);
        return totpConfig != null && totpConfig.isConfigured();
    }


    private boolean verifyTotpCode(
            JSONObject data,
            User user
    ) {
        try {
            var code = data.getString("totpCodeForVerification");
            boolean result = totpTfaService.verifyTotpCode(user.getId(), code);
            if (result) showWrongCredentialWarning.remove(user.getId());

            return result;
        } catch (Exception e) {
            return false;
        }
    }

    @RequirePOST
    @Override
    public void doAuthenticate(
            StaplerRequest req,
            StaplerResponse res
    ) throws ServletException, IOException {
        Jenkins.get().checkPermission(Jenkins.READ);
        String redirectUrl = "./";

        JSONObject formData = req.getSubmittedForm();
        HttpSession session = req.getSession(false);

        try {
            var user = User.current();
            if (user == null) {
                FormApply.success(redirectUrl).generateResponse(req, res, null);
                return;
            }

            if (verifyTotpCode(formData, user)) {
                if (!isTotpConfigured()) {
                    var totpConfig = user.getProperty(TotpConfig.class);
                    totpConfig.setConfigured(true);
                    user.save();
                }
                allow2FaAccessAndRedirect(session, res, user);
                return;
            }

            showWrongCredentialWarning.put(user.getId(), true);
        } catch (Exception ignored) { }

        FormApply.success(redirectUrl).generateResponse(req, res, null);
    }
}
