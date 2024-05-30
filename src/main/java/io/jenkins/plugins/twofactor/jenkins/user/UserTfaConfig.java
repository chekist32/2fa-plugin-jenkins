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
import hudson.model.RootAction;
import hudson.model.User;
import hudson.model.UserProperty;
import hudson.model.UserPropertyDescriptor;
import io.jenkins.plugins.twofactor.constants.PluginUrls;
import io.jenkins.plugins.twofactor.jenkins.globalConfig.GlobalConfig;
import io.jenkins.plugins.twofactor.jenkins.tfaMethods.config.OtpOverEmailConfig;
import io.jenkins.plugins.twofactor.jenkins.tfaMethods.config.SecurityQuestionConfig;
import io.jenkins.plugins.twofactor.jenkins.tfaMethods.config.TotpConfig;

import java.util.logging.Logger;

@SuppressWarnings("unused")
@Extension
public class UserTfaConfig extends UserProperty implements RootAction {
  private static final Logger LOGGER = Logger.getLogger(UserTfaConfig.class.getName());

  @Override
  public String getIconFileName() {
    return "/plugin/2fa-jenkins-plugin/images/tfaIcon.png";
  }

  @Override
  public String getDisplayName() {
    return "2FA Configuration";
  }

  @Override
  public String getUrlName() {
    return PluginUrls.USER_CONFIG_PATH;
  }

  public boolean isSecurityQuestionConfigurationIsEnabled() {
    return GlobalConfig.get().isEnableSecurityQuestionsAuthentication();
  }

  public boolean isSecurityQuestionConfigured() {
    var securityQuestionConfig = user.getProperty(SecurityQuestionConfig.class);
    return securityQuestionConfig != null && securityQuestionConfig.isConfigured();
  }

  public boolean isOtpOverEmailIsEnabled() {
    return GlobalConfig.get().isEnableOtpOverEmailAuthentication();
  }

  public boolean isOtpOverEmailConfigured() {
      var user = User.current();
      if (user == null) return false;

      var otpOverEmailConfig = user.getProperty(OtpOverEmailConfig.class);
      return otpOverEmailConfig != null && otpOverEmailConfig.isConfigured();
  }

  public boolean isTotpEnabled() { return GlobalConfig.get().isEnableTotpAuthentication(); }

  public boolean isTotpConfigured() {
    var user = User.current();
    if (user == null) return false;

      var totpConfig = user.getProperty(TotpConfig.class);
      return totpConfig != null && totpConfig.isConfigured();
  }

  public UserProperty getSecurityQuestionConfig() {
    var user = User.current();
    if (user == null) return null;

    var prop = user.getProperty(SecurityQuestionConfig.class);
    return prop != null ? prop : new SecurityQuestionConfig.DescriptorImpl().newInstance(user);
  }
  public UserProperty getOtpOverEmailConfig() {
    var user = User.current();
    if (user == null) return null;

    var prop = user.getProperty(OtpOverEmailConfig.class);
    return prop != null ? prop : new OtpOverEmailConfig.DescriptorImpl().newInstance(user);
  }
  public UserProperty getTotpConfig() {
    var user = User.current();
    if (user == null) return null;

    var prop = user.getProperty(TotpConfig.class);
    return prop != null ? prop : new TotpConfig.DescriptorImpl().newInstance(user);
  }

  @SuppressWarnings("unused")
  public String getUserId() { return user != null ? user.getId() : ""; }

  @Override
  public UserPropertyDescriptor getDescriptor() {
    return new UserTfaConfig.DescriptorImpl();
  }

  @Extension
  public static class DescriptorImpl extends UserPropertyDescriptor {
    public DescriptorImpl() {
      super(UserTfaConfig.class);
    }

    @Override
    public UserProperty newInstance(User user) {
      return new UserTfaConfig();
    }

    @SuppressWarnings("unused")
    public boolean showInUserProfile() {
      return GlobalConfig.get().getEnableTfa();
    }

    @SuppressWarnings("unused")
    public String getUserId() {
      User currentUser = User.current();
      if (currentUser == null) {
        return "";
      }

      return currentUser.getId();
    }
  }
}
