package io.jenkins.plugins.twofactor.jenkins.tfaMethods.config;

import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import hudson.Extension;
import hudson.model.User;
import hudson.model.UserProperty;
import hudson.util.FormApply;
import hudson.util.Secret;
import io.jenkins.plugins.twofactor.jenkins.globalConfig.GlobalConfig;
import io.jenkins.plugins.twofactor.jenkins.tfaMethods.TfaMethodType;
import io.jenkins.plugins.twofactor.jenkins.tfaMethods.service.TotpTfaService;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.logging.Logger;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

public class TotpConfig extends AbstractTfaConfig {
    private static final Logger LOGGER = Logger.getLogger(TotpConfig.class.getName());

    private Secret totpSecret;
    private static final TotpTfaService totpTfaService = TotpTfaService.getInstance();

    public TotpConfig(boolean isConfigured) {
        super(isConfigured, TfaMethodType.TOTP);
    }


    public String generateSecretKey() throws QrGenerationException {
        QrData qrData = totpTfaService.generateSecretKeyQr();
        totpSecret = Secret.fromString(qrData.getSecret());

        var generator = new ZxingPngQrGenerator();
        String mimeType = generator.getImageMimeType();
        byte[] imageData = generator.generate(qrData);

        return getDataUriForImage(imageData, mimeType);
    }

    @RequirePOST
    @Override
    public void doConfigure(StaplerRequest req, StaplerResponse res)
            throws IOException, ServletException {
        user.save();
        redirectToAuthPath(req, res);
    }

    @RequirePOST
    @Override
    public void doReset(StaplerRequest req, StaplerResponse rsp)
            throws IOException, ServletException {
        Jenkins.get().checkPermission(Jenkins.READ);
        try {
            totpSecret = null;
            this.setConfigured(false);
            LOGGER.fine("Resetting the totp authentication method");
            user.save();
        } catch (Exception e) {
            LOGGER.fine("Error in resetting the configuration " + e.getMessage());
        }

        FormApply.success(req.getContextPath() + "../")
                .generateResponse(req, rsp, null);
    }

    @RequirePOST
    public void doGenerateTotpSecretKey(StaplerRequest req, StaplerResponse res)
        throws IOException, ServletException {
        Jenkins.get().checkPermission(Jenkins.READ);


    }

    @Override
    public String getDisplayName() {
        return "TOTP Configuration";
    }

    public Secret getTotpSecret() {
        return totpSecret;
    }

    @Extension
    public static class DescriptorImpl extends AbstractTfaConfigDescriptor {

        @Override
        public boolean showInUserProfile() {
            var config = GlobalConfig.get();
            return config != null && config.isEnableTotpAuthentication();
        }

        @Override
        public UserProperty newInstance(User user) {
            return new TotpConfig(false);
        }
    }

}
