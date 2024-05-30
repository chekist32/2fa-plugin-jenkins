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

import hudson.Extension;
import hudson.model.User;
import hudson.model.UserProperty;
import hudson.model.UserPropertyDescriptor;
import hudson.util.FormApply;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import io.jenkins.plugins.twofactor.constants.SecurityQuestions;
import io.jenkins.plugins.twofactor.jenkins.dto.SimpleQADTO;
import io.jenkins.plugins.twofactor.jenkins.globalConfig.GlobalConfig;
import io.jenkins.plugins.twofactor.jenkins.tfaFilters.TwoFactorAuthFilter;
import io.jenkins.plugins.twofactor.jenkins.tfaMethods.TfaMethodType;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.springframework.lang.NonNull;

import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Logger;

public class SecurityQuestionConfig extends AbstractTfaConfig {
  private static final Logger LOGGER = Logger.getLogger(SecurityQuestionConfig.class.getName());
  private static final int NUM_OF_QA = 3;
  private transient String errorMsg = null;

  private final SimpleQADTO[] userTfaQA;

  @DataBoundConstructor
  public SecurityQuestionConfig(SimpleQADTO[] userTfaQA, boolean isConfigured) {
      super(isConfigured, TfaMethodType.SECURITY_QUESTION);
      this.userTfaQA = userTfaQA;
  }

  @Override
  public String getDisplayName() {
    return "Security question";
  }

  private boolean isFormFilledCorrectly(JSONObject json) {
      JSONArray questions = json.getJSONArray("question");
      JSONArray answers = json.getJSONArray("answer");
      int size = questions.size();
      if (size != answers.size() || size != userTfaQA.length) return false;

      try {
          Set<SecurityQuestions> set = EnumSet.noneOf(SecurityQuestions.class);
          for (int i = 0; i < size; i++) {
              if (!set.add(SecurityQuestions.valueOf(questions.getString(i)))) {
                  errorMsg = "Security questions have to be unique";
                  return false;
              }
          }
      } catch (Exception e) { return false; }

      try {
          for (int i = 0; i < size; i++) {
              var answer = answers.getString(i);
              if (answer.isBlank()) {
                  errorMsg = "The answer field can't be blank";
                  return false;
              }
          }
      } catch (JSONException e) { return false; }

    return true;
  }

    public SimpleQADTO[] getUserTfaQA() { return userTfaQA; }

    public String getErrorMsg() { return errorMsg; }

  private static SimpleQADTO[] fillQAWithEmptyStrings(SimpleQADTO[] userTfaQA) {
        for (int i = 0; i < userTfaQA.length; i++) {
            userTfaQA[i] = new SimpleQADTO(SecurityQuestions.FIRST_COMPANY_NAME, Secret.fromString(""));
        }
        return userTfaQA;
  }

  public void parseJsonObjectIntoQA(JSONObject json) {
    JSONArray questions = json.getJSONArray("question");
    JSONArray answers = json.getJSONArray("answer");

    for (int i = 0; i < userTfaQA.length; i++) {
        userTfaQA[i] =
                new SimpleQADTO(
                  SecurityQuestions.valueOf(questions.getString(i)),
                  Secret.fromString(answers.getString(i))
                );
    }
  }

    @RequirePOST
    @Override
    public void doConfigure(StaplerRequest req, StaplerResponse res)
            throws IOException, ServletException {
        Jenkins.get().checkPermission(Jenkins.READ); errorMsg = null;
        LOGGER.fine("Saving user security questions");
        String redirectUrl = req.getContextPath() + "./";

        JSONObject json = req.getSubmittedForm();
        var user = User.current();
        if (!isFormFilledCorrectly(json) ||
                user == null) {
            FormApply.success(redirectUrl).generateResponse(req, res, null);
            return;
        }

        parseJsonObjectIntoQA(json);
        this.setConfigured(true);
        user.save();

        HttpSession session = req.getSession(false);
        if (session != null) {
            redirectUrl = (String) session.getAttribute(TwoFactorAuthFilter.REDIRECT_URL_NAME);
            session.removeAttribute(TwoFactorAuthFilter.REDIRECT_URL_NAME);
        }

        if (redirectUrl != null) {
            LOGGER.fine("Saved security questions, redirecting user to " + redirectUrl);
            FormApply.success(redirectUrl).generateResponse(req, res, null);
            return;
        }

        redirectUrl = req.getContextPath() + "../";
        LOGGER.fine("Redirecting user to " + redirectUrl);
        FormApply.success(redirectUrl).generateResponse(req, res, null);
    }

    @SuppressWarnings("unused")
  @RequirePOST
  public void doReset(StaplerRequest req, StaplerResponse rsp)
          throws ServletException, IOException {
    Jenkins.get().checkPermission(Jenkins.READ);
    try {
      fillQAWithEmptyStrings(this.userTfaQA);
      this.setConfigured(false);
      LOGGER.fine("Resetting the security question authentication method");
      user.save();
    } catch (Exception e) {
      LOGGER.fine("Error in resetting the configuration " + e.getMessage());
    }

    FormApply.success(
            req.getContextPath() + "../")
        .generateResponse(req, rsp, null);
  }

  @Override
  public UserPropertyDescriptor getDescriptor() {
    return new DescriptorImpl();
  }

  @SuppressWarnings("unused")
  public static final SecurityQuestionConfig.DescriptorImpl DESCRIPTOR =
      new SecurityQuestionConfig.DescriptorImpl();

    @Extension
    public static class DescriptorImpl extends AbstractTfaConfigDescriptor {
        public DescriptorImpl() {
            super(SecurityQuestionConfig.class);
        }

        @Override
        public UserProperty newInstance(User user) {
          return new SecurityQuestionConfig(
                  fillQAWithEmptyStrings(new SimpleQADTO[NUM_OF_QA]),
                  false
          );
        }

        @NonNull
        @Override
        public String getDisplayName() {
          return "Security Question";
        }

        @SuppressWarnings("unused")
        public boolean showInUserProfile() {
            var globalConfig = GlobalConfig.get();
            return globalConfig.getEnableTfa() && globalConfig.isEnableSecurityQuestionsAuthentication();
        }

        private ListBoxModel populateSecurityQuestions() {
            var securityQuestionsDropDown = new ListBoxModel();
            for (var securityQuestion : SecurityQuestions.values()) {
                securityQuestionsDropDown.add(
                        new ListBoxModel.Option(
                                securityQuestion.getQuestion(),
                                securityQuestion.name()
                        )
                );
            }
            return securityQuestionsDropDown;
        }

        @RequirePOST
        public ListBoxModel doFillQuestionItems() {
          return populateSecurityQuestions();
        }

        private FormValidation validateForm(boolean condition, String errorMessage) {
          return condition ? FormValidation.error(errorMessage) : FormValidation.ok();
        }

    }
}
