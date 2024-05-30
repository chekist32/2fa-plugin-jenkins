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
package io.jenkins.plugins.twofactor.jenkins.tfaMethods.config;


import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.User;
import hudson.model.UserProperty;
import hudson.util.FormApply;
import io.jenkins.plugins.twofactor.jenkins.globalConfig.GlobalConfig;
import io.jenkins.plugins.twofactor.jenkins.tfaMethods.TfaMethodType;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.logging.Logger;

public class OtpOverEmailConfig extends AbstractTfaConfig {
  private static final Logger LOGGER = Logger.getLogger(OtpOverEmailConfig.class.getName());

  @DataBoundConstructor
  public OtpOverEmailConfig(boolean isConfigured) {
      super(isConfigured, TfaMethodType.OTP_OVER_EMAIL);
  }

  @Override
  public String getDisplayName() {
    return "OTP over email";
  }

    @RequirePOST
    @Override
    public void doConfigure(StaplerRequest req, StaplerResponse res)
            throws IOException, ServletException {
        redirectToAuthPath(req, res);
    }

    @SuppressWarnings("unused")
    @RequirePOST
    public void doReset(StaplerRequest req, StaplerResponse rsp)
            throws IOException, ServletException {
            Jenkins.get().checkPermission(Jenkins.READ);

              try {
                  this.setConfigured(false);
                  LOGGER.fine("Resetting the OTP over email authentication method");
                  user.save();
              } catch (Exception e) {
                  LOGGER.fine("Error in resetting the OTP over email config");
              }

              FormApply.success(req.getContextPath() + "../")
                .generateResponse(req, rsp, null);
      }

  @Extension
  public static class DescriptorImpl extends AbstractTfaConfigDescriptor {
      public DescriptorImpl() {
        super(OtpOverEmailConfig.class);
      }

      @Override
      public UserProperty newInstance(User user) {
        return new OtpOverEmailConfig(false);
      }

      @NonNull
      @Override
      public String getDisplayName() {
        return "OTP over email";
      }

      public boolean showInUserProfile() {
          var globalConfig = GlobalConfig.get();
          return globalConfig.getEnableTfa() && globalConfig.isEnableOtpOverEmailAuthentication();
      }
  }
}
