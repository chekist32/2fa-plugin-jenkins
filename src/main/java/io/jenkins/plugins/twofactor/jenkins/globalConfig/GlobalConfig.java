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

import hudson.Extension;
import hudson.XmlFile;
import io.jenkins.plugins.twofactor.jenkins.dto.AdvancedSettingsDTO;
import io.jenkins.plugins.twofactor.jenkins.dto.OtpOverEmailDTO;
import io.jenkins.plugins.twofactor.jenkins.tfaMethods.TfaMethodType;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Logger;

@Extension
public class GlobalConfig extends GlobalConfiguration {
  private static final Logger LOGGER = Logger.getLogger(GlobalConfig.class.getName());

  private boolean enableTfa;
  private Set<TfaMethodType> enabledTfaMethods;
  private OtpOverEmailDTO otpOverEmailDto;
  private AdvancedSettingsDTO advancedSettingsDTO;


  public GlobalConfig() { init(); }

  private void init() {
      load();

      if (enabledTfaMethods == null) enabledTfaMethods = EnumSet.noneOf(TfaMethodType.class);
      if (otpOverEmailDto == null) otpOverEmailDto = OtpOverEmailDTO.getDefault();
      if (advancedSettingsDTO == null) advancedSettingsDTO = AdvancedSettingsDTO.getDefault();
      save();
  }

  public boolean getEnableTfa() { return enableTfa; }

  public GlobalConfigAdvancedSettings getAdvancedSettings() {
    return new GlobalConfigAdvancedSettings(GlobalConfig.get().getAdvancedSettingsDTO());
  }

  @SuppressWarnings("unused")
  @DataBoundSetter
  public void setEnableTfa(boolean unableTfa) {
    enableTfa = unableTfa;
  }

  @SuppressWarnings("unused")
  public boolean isEnableSecurityQuestionsAuthentication() {
    return enabledTfaMethods.contains(TfaMethodType.SECURITY_QUESTION);
  }

  public boolean isSpecificTfaEnabled(TfaMethodType methodType) {
    return enabledTfaMethods.contains(methodType);
  }

  @SuppressWarnings("unused")
  @DataBoundSetter
  public void setEnableSecurityQuestionsAuthentication(
      boolean enableSecurityQuestionsAuthentication) {
    if (enableSecurityQuestionsAuthentication) enabledTfaMethods.add(TfaMethodType.SECURITY_QUESTION);
    else enabledTfaMethods.remove(TfaMethodType.SECURITY_QUESTION);
  }

  public boolean isEnableTotpAuthentication() {
      return enabledTfaMethods.contains(TfaMethodType.TOTP);
  }

  public Set<TfaMethodType> getEnabledTfaMethods() {
    return Collections.unmodifiableSet(enabledTfaMethods);
  }

  @SuppressWarnings("unused")
  public boolean isEnableOtpOverEmailAuthentication() {
    return otpOverEmailDto.getIsEnabled();
  }

  @SuppressWarnings("unused")
  public String getSenderEmailAddress() {
    return otpOverEmailDto.getSenderEmailAddress();
  }

  public OtpOverEmailDTO getOtpOverEmailDto() {
    return otpOverEmailDto;
  }
  public AdvancedSettingsDTO getAdvancedSettingsDTO() {
    return advancedSettingsDTO;
  }

  public void saveGlobalConfigViewForm(JSONObject formData) {
    try {
      enableTfa = formData.getBoolean("enableTfa");

      if (formData.getBoolean("enableSecurityQuestion")) enabledTfaMethods.add(TfaMethodType.SECURITY_QUESTION);
      else enabledTfaMethods.remove(TfaMethodType.SECURITY_QUESTION);

      if (formData.containsKey("enableOtpOverEmail")) {
        JSONObject otpOverEmail = formData.getJSONObject("enableOtpOverEmail");
        var senderEmailAddress = otpOverEmail.getString("senderEmailAddress");

        if (senderEmailAddress.isEmpty()) {
          throw new UnsupportedOperationException("Sender Email address can not be kept as empty");
        }
        otpOverEmailDto = new OtpOverEmailDTO(true, senderEmailAddress);
        enabledTfaMethods.add(TfaMethodType.OTP_OVER_EMAIL);
      } else {
        otpOverEmailDto = OtpOverEmailDTO.getDefault();
        enabledTfaMethods.remove(TfaMethodType.OTP_OVER_EMAIL);
      }

      if (formData.containsKey("enableTotpAuthenticator")) {
          var totpAuth = formData.getBoolean("enableTotpAuthenticator");
          if (totpAuth) enabledTfaMethods.add(TfaMethodType.TOTP);
          else enabledTfaMethods.remove(TfaMethodType.TOTP);
      }

      if (advancedSettingsDTO == null) {
        advancedSettingsDTO = AdvancedSettingsDTO.getDefault();
      }

      this.save();
      LOGGER.fine("Saving global configuration ");
    } catch (Exception e) {
      LOGGER.fine("Error in saving mo global configuration " + e.getMessage());
    }
  }

  public void saveGlobalAdvancedSettingsForm(JSONObject formData) {
    try {
      LOGGER.fine("Saving advanced setting details");
      String customOTPEmailSubject = formData.getString("customOTPEmailSubject");
      String customOTPEmailTemplate = formData.getString("customOTPEmailTemplate");
      advancedSettingsDTO = new AdvancedSettingsDTO(customOTPEmailSubject, customOTPEmailTemplate);
      this.save();
    } catch (Exception e) {
      LOGGER.fine("Error in saving 2FA global advance settings");
    }
  }

  @Override
  public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {

    try{
      String formPage = formData.getString("formPage");
      switch (formPage) {
        case "basicConfig":
          saveGlobalConfigViewForm(formData);
          break;
        case "advanceSettingsConfig":
          saveGlobalAdvancedSettingsForm(formData);
          break;
        default:
          LOGGER.fine("Error in saving 2FA global settings for " + formPage);
          break;
      }
      if (formData.has("enableTfa")) {
        saveGlobalConfigViewForm(formData);
      }
    } catch (Exception e){
      LOGGER.fine("Not saved 2FA global settings for our plugin");
      return false;
    }
    return true;
  }

  @Override
  protected XmlFile getConfigFile() {
    File pluginDir = new File(Jenkins.get().getRootDir(), "/plugins/moTfaGlobalConfig");
    return new XmlFile(new File(pluginDir, this.getId() + ".xml"));
  }

  public static GlobalConfig get() {
    final GlobalConfig config;
    try {
      config = GlobalConfiguration.all().get(GlobalConfig.class);
    } catch (IllegalStateException e) {
      LOGGER.fine("Error in fetching global configuration class " + e.getMessage());
      throw e;
    }
    return config;
  }
}
