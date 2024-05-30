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
package io.jenkins.plugins.twofactor.jenkins.user;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.RootAction;
import hudson.model.User;
import io.jenkins.plugins.twofactor.constants.PluginUrls;
import io.jenkins.plugins.twofactor.jenkins.globalConfig.GlobalConfig;
import io.jenkins.plugins.twofactor.jenkins.tfaMethods.auth.OtpOverEmailAuth;
import io.jenkins.plugins.twofactor.jenkins.tfaMethods.auth.SecurityQuestionAuth;
import io.jenkins.plugins.twofactor.jenkins.tfaMethods.auth.TotpAuth;
import io.jenkins.plugins.twofactor.jenkins.tfaMethods.config.AbstractTfaConfig;
import io.jenkins.plugins.twofactor.jenkins.tfaMethods.config.OtpOverEmailConfig;
import io.jenkins.plugins.twofactor.jenkins.tfaMethods.config.SecurityQuestionConfig;
import io.jenkins.plugins.twofactor.jenkins.tfaMethods.config.TotpConfig;
import jenkins.model.Jenkins;

import java.util.logging.Logger;

@SuppressWarnings("unused")
@Extension
public class UserTfaAuth implements RootAction, Describable<UserTfaAuth> {
    private static final Logger LOGGER = Logger.getLogger(UserTfaAuth.class.getName());

    @Override
    public String getIconFileName() {
      return null;
    }

    @Override
    public String getDisplayName() {
      return PluginUrls.USER_AUTH_PATH;
    }

    @Override
    public String getUrlName() {
      return PluginUrls.USER_AUTH_PATH;
    }

    public String getUserId() {
      User currentUser = User.current();
      if (currentUser == null) {
        return "";
      }
      return currentUser.getId();
    }

  @SuppressWarnings("unused")
  public SecurityQuestionAuth getSecurityQuestionAuth() {
        return new SecurityQuestionAuth();
  }

  @SuppressWarnings("unused")
  public OtpOverEmailAuth getOtpOverEmailAuth() {
        return new OtpOverEmailAuth();
  }

    public TotpAuth getTotpAuth() {
        return new TotpAuth();
    }

    private boolean showTfaForConfiguration(Class<? extends AbstractTfaConfig> clazz) {
        var user = User.current();
        if (user == null) return false;

        AbstractTfaConfig tfaConfig = user.getProperty(clazz);
        if (tfaConfig == null) return false;

        boolean isConfigured = tfaConfig.isConfigured();
        boolean isEnabled = GlobalConfig.get().isSpecificTfaEnabled(tfaConfig.getMethodType());

        return isConfigured && isEnabled;
    }

    public boolean showSecurityQuestionForConfiguration() {
        return showTfaForConfiguration(SecurityQuestionConfig.class);
    }

  @SuppressWarnings("unused")
    public boolean showOtpOverEmailForConfiguration() {
        return showTfaForConfiguration(OtpOverEmailConfig.class);
    }

    public boolean showTotpForConfiguration() {
        return showTfaForConfiguration(TotpConfig.class);
    }

  @SuppressWarnings("unchecked")
  @Override
  public Descriptor<UserTfaAuth> getDescriptor() {
    return Jenkins.get().getDescriptorOrDie(getClass());
  }
}
