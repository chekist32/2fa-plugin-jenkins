package io.jenkins.plugins.twofactor.jenkins.globalConfig;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormApply;
import hudson.util.FormValidation;
import io.jenkins.plugins.twofactor.constants.PluginUrls;
import io.jenkins.plugins.twofactor.jenkins.dto.AdvancedSettingsDTO;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.kohsuke.stapler.verb.POST;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.logging.Logger;

import static io.jenkins.plugins.twofactor.constants.GlobalConfigConstant.AdvancedSettingsConstants.DEFAULT_OTP_EMAIL_SUBJECT;
import static io.jenkins.plugins.twofactor.constants.GlobalConfigConstant.AdvancedSettingsConstants.DEFAULT_OTP_EMAIL_TEMPLATE;
import static org.apache.commons.lang.StringUtils.isBlank;

public class GlobalConfigAdvancedSettings implements Action, Describable<GlobalConfigAdvancedSettings> {
  private static final Logger LOGGER = Logger.getLogger(GlobalConfigAdvancedSettings.class.getName());

  private final AdvancedSettingsDTO advancedSettingsDto;

  public GlobalConfigAdvancedSettings(AdvancedSettingsDTO advancedSettingsDto) {
    this.advancedSettingsDto = advancedSettingsDto;
  }

  @Override
  public String getIconFileName() {
    return "symbol-settings";
  }

  @Override
  public String getDisplayName() {
    return "Advanced Settings";
  }

  @Override
  public String getUrlName() {
    return PluginUrls.TFA_GLOBAL_ADVANCED_SETTINGS_PATH;
  }

  @SuppressWarnings("unused")
  @Override
  public Descriptor<GlobalConfigAdvancedSettings> getDescriptor() {
    Jenkins jenkins = Jenkins.get();
    return (Descriptor<GlobalConfigAdvancedSettings>) jenkins.getDescriptorOrDie(getClass());
  }


  @SuppressWarnings("unused")
  @RequirePOST
  @Restricted(NoExternalUse.class)
  public void doSaveAdvancedSettingsConfiguration(StaplerRequest request, StaplerResponse response)
      throws ServletException, IOException {
    try {
      Jenkins.get().checkPermission(Jenkins.ADMINISTER);
      JSONObject json = request.getSubmittedForm();
      GlobalConfig globalConfig = GlobalConfig.get();
      globalConfig.configure(request, json);
    } catch (Exception e) {
      LOGGER.fine("Error while submitting global configurations, error: " + e.getMessage());
    }

    FormApply.success("./").generateResponse(request, response, null);
  }

  public String getCustomOTPEmailSubject() {
    String subject = null;
    if(advancedSettingsDto != null) {
      subject = advancedSettingsDto.getCustomOTPEmailSubject();
    }
    if(isBlank(subject)){
      subject = DEFAULT_OTP_EMAIL_SUBJECT.getValue();
    }
    return subject;
  }
  public String getCustomOTPEmailTemplate() {
    String template = null;
    if(advancedSettingsDto != null) {
      template = advancedSettingsDto.getCustomOTPEmailTemplate();
    }
    if(isBlank(template)){
      template = DEFAULT_OTP_EMAIL_TEMPLATE.getValue();
    }
    return template;
  }

  @SuppressWarnings("unused")
  @Extension
  public static final class DescriptorImpl extends Descriptor<GlobalConfigAdvancedSettings> {
    public DescriptorImpl() {}

    @POST
    @SuppressWarnings("unused")
    public FormValidation doCheckSkipTfaForApi(@QueryParameter Boolean skipTfaForApi) {
      Jenkins.get().checkPermission(Jenkins.ADMINISTER);
      return FormValidation.warning("Not implemented yet");
    }

    @POST
    @SuppressWarnings("unused")
    public FormValidation doCheckEnableTfaOnBuild(@QueryParameter Boolean enableTfaOnBuild) {
      Jenkins.get().checkPermission(Jenkins.ADMINISTER);
      return FormValidation.warning("Not implemented yet");
    }

  }
}
