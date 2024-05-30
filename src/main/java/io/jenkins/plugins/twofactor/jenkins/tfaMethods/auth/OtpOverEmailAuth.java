/*
 * Copyright (c) 2023
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.jenkins.plugins.twofactor.jenkins.tfaMethods.auth;

import hudson.Extension;
import hudson.model.User;
import hudson.tasks.Mailer;
import hudson.util.FormApply;
import io.jenkins.plugins.twofactor.jenkins.tfaMethods.TfaMethodType;
import io.jenkins.plugins.twofactor.jenkins.tfaMethods.config.OtpOverEmailConfig;
import io.jenkins.plugins.twofactor.jenkins.tfaMethods.service.OtpOverEmailTfaService;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.logging.Logger;

@Extension
public class OtpOverEmailAuth extends AbstractTfaAuth {
    private static final Logger LOGGER = Logger.getLogger(OtpOverEmailAuth.class.getName());

    private final OtpOverEmailTfaService otpOverEmailTfaService;

    public OtpOverEmailAuth(OtpOverEmailTfaService otpOverEmailTfaService) {
        super(TfaMethodType.OTP_OVER_EMAIL);
        this.otpOverEmailTfaService = otpOverEmailTfaService;
    }

    public OtpOverEmailAuth() {
        this(OtpOverEmailTfaService.getInstance());
    }

    public String getUserEmailAddress() {
        var user = User.current();
        if (user == null) return "";

        var mailerProp = user.getProperty(Mailer.UserProperty.class);
        if (mailerProp == null) return "";

        return mailerProp.getEmailAddress();
    }

    @SuppressWarnings("unused")
    public boolean isOtpSentToUser() {
        var user = User.current();
        if (user == null) return false;

        return otpOverEmailTfaService.isOtpSent(user.getId());
    }

    public boolean isOtpOverEmailConfigured() {
        var user = User.current();
        if (user == null) return false;

        var otpOverEmailConfig = user.getProperty(OtpOverEmailConfig.class);
        return otpOverEmailConfig != null && otpOverEmailConfig.isConfigured();
    }

    @SuppressWarnings("unused")
    @RequirePOST
    public void doResendOtp(StaplerRequest req, StaplerResponse rsp)
        throws ServletException, IOException {
        Jenkins.get().checkPermission(Jenkins.READ);

        var user = User.current();
        if (user != null) {
            otpOverEmailTfaService.sendOtp(user.getId());
            showWrongCredentialWarning.remove(user.getId());
        }

        FormApply.success("./").generateResponse(req, rsp, null);
    }

    @RequirePOST
    @Override
    public void doAuthenticate(
            StaplerRequest req,
            StaplerResponse res
    ) throws ServletException, IOException {
        Jenkins.get().checkPermission(Jenkins.READ);
        String redirectUrl = req.getContextPath() + "./";

        var user = User.current();
        if (user == null) {
            FormApply.success(redirectUrl).generateResponse(req, res, null);
            return;
        }

        JSONObject json = req.getSubmittedForm();
        try {
            String userInputOtp = json.getString("emailOtpForVerification");
            HttpSession session = req.getSession(false);

            if (otpOverEmailTfaService.validateOtp(user.getId(), userInputOtp)) {
                LOGGER.fine("Otp is authentic");
                if (!isOtpOverEmailConfigured()) {
                    var otpOverEmailConfig = user.getProperty(OtpOverEmailConfig.class);
                    if (otpOverEmailConfig != null) {
                        otpOverEmailConfig.setConfigured(true);
                        user.save();
                    }
                }
                allow2FaAccessAndRedirect(session, res, user);
                return;
            }

            LOGGER.fine("Entered wrong otp for otpOverEmailConfig");
            showWrongCredentialWarning.put(user.getId(), true);
        } catch (Exception e) {
            LOGGER.fine("Something went wrong in Otp Over Email, Form is not filled correctly ");
        }

        LOGGER.fine("Redirecting user from otpOverEmailAuth to " + redirectUrl);
        FormApply.success(redirectUrl).generateResponse(req, res, null);
    }
}
