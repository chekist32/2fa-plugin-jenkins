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
package io.jenkins.plugins.twofactor.jenkins.globalConfig;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.ManagementLink;
import hudson.util.FormApply;
import hudson.util.FormValidation;
import io.jenkins.cli.shaded.org.apache.commons.lang.StringUtils;
import io.jenkins.plugins.twofactor.constants.PluginUrls;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.kohsuke.stapler.verb.POST;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
@Extension
public class GlobalConfigView extends ManagementLink implements Describable<GlobalConfigView> {

  private static final Logger LOGGER = Logger.getLogger(GlobalConfigView.class.getName());

  public GlobalConfigView() {}

  @Override
  public String getIconFileName() {
    if (isAdmin()) return "/plugin/2fa-jenkins-plugin/images/tfaIcon.png";
    return null;
  }

  @Override
  public String getDisplayName() {
    return "2FA Global Configurations";
  }

  @Override
  public String getUrlName() {
    return PluginUrls.TFA_GLOBAL_CONFIG_PATH;
  }

  @Override
  public String getDescription() {
    return "Configure two factor settings for your jenkins instance";
  }

  public boolean getEnableTfa() {
    return GlobalConfig.get().getEnableTfa();
  }

  public boolean getEnableSecurityQuestion() {
    return GlobalConfig.get().isEnableSecurityQuestionsAuthentication();
  }

  public boolean getEnableOtpOverEmail() {
    return GlobalConfig.get().isEnableOtpOverEmailAuthentication();
  }

  public boolean getEnableTotp() { return GlobalConfig.get().isEnableTotpAuthentication(); }

  public String getSenderEmailAddress() {
    return GlobalConfig.get().getSenderEmailAddress();
  }
  public GlobalConfigAdvancedSettings getAdvancedSettings() {
    return new GlobalConfigAdvancedSettings(GlobalConfig.get().getAdvancedSettingsDTO());
  }

  @RequirePOST
  public void doSaveGlobalTfaSettings(StaplerRequest req, StaplerResponse rsp)
      throws IOException, ServletException, Descriptor.FormException {
    try {
      Jenkins.get().checkPermission(Jenkins.ADMINISTER);
      JSONObject json = req.getSubmittedForm();
      GlobalConfig globalConfig = GlobalConfig.get();
      globalConfig.configure(req, json);
    } catch (Exception e) {
      LOGGER.fine("Error while submitting global configurations, error: " + e.getMessage());
      throw e;
    }

    FormApply.success("./").generateResponse(req, rsp, null);
  }

  @NonNull
  public String getCategoryName() {
    return "SECURITY";
  }

  public boolean isAdmin() {
    return Jenkins.get().getACL().hasPermission(Jenkins.ADMINISTER);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Descriptor<GlobalConfigView> getDescriptor() {
    return (Descriptor<GlobalConfigView>) Jenkins.get().getDescriptor(getClass());
  }

  public GlobalConfig getGlobalConfig() {
    return GlobalConfig.get();
  }

  @Extension
  public static final class DescriptorImpl extends Descriptor<GlobalConfigView> {
    public DescriptorImpl() {}

    @POST
    public FormValidation doCheckSenderEmailAddress(@QueryParameter String senderEmailAddress) {
      Jenkins.get().checkPermission(Jenkins.ADMINISTER);
      String regex = "^(.+)@(.+)$";
      Pattern pattern = Pattern.compile(regex);
      Matcher matcher = pattern.matcher(senderEmailAddress);
      if (StringUtils.isEmpty(senderEmailAddress)) {
        return FormValidation.error("Email is required.");
      } else if (!matcher.matches()) {
        return FormValidation.error("Please enter valid email");
      } else {
        return FormValidation.ok();
      }
    }

    @POST
    @SuppressWarnings("unused")
    public FormValidation doCheckEnableDuoPush(@QueryParameter Boolean enableDuoPush) {
      Jenkins.get().checkPermission(Jenkins.ADMINISTER);
      return FormValidation.warning("Not implemented yet");
    }

    @POST
    @SuppressWarnings("unused")
    public FormValidation doCheckEnableMobileAuthenticator(@QueryParameter Boolean enableMobileAuthenticator) {
      Jenkins.get().checkPermission(Jenkins.ADMINISTER);
      return FormValidation.warning("Not implemented yet");
    }

    @POST
    @SuppressWarnings("unused")
    public FormValidation doCheckEnableOtpOverSms(@QueryParameter Boolean enableOtpOverSms) {
      Jenkins.get().checkPermission(Jenkins.ADMINISTER);
      return FormValidation.warning("Not implemented yet");
    }

  }
}
