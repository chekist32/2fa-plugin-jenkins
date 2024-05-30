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
import hudson.util.FormApply;
import io.jenkins.plugins.twofactor.jenkins.dto.SimpleQADTO;
import io.jenkins.plugins.twofactor.jenkins.tfaMethods.TfaMethodType;
import io.jenkins.plugins.twofactor.jenkins.tfaMethods.service.SecurityQuestionTfaService;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.logging.Logger;

@Extension
public class SecurityQuestionAuth extends AbstractTfaAuth {
    private static final Logger LOGGER = Logger.getLogger(SecurityQuestionAuth.class.getName());

    private SimpleQADTO[] userTfaQA;
    private final SecurityQuestionTfaService securityQuestionTfaService;

    public SecurityQuestionAuth() { this(SecurityQuestionTfaService.getInstance()); }

    public SecurityQuestionAuth(SecurityQuestionTfaService securityQuestionTfaService) {
        super(TfaMethodType.SECURITY_QUESTION);
        this.securityQuestionTfaService = securityQuestionTfaService;
        generateSecurityQuestions();
    }

    public String[] getQuestions() {
          String[] questions = new String[userTfaQA.length];
          for (int i = 0; i < userTfaQA.length; i++) {
              questions[i] = userTfaQA[i].getQuestion().getQuestion();
          }

          return questions;
    }

    private void generateSecurityQuestions() {
        var user = User.current();
        if (user == null) {
            userTfaQA = new SimpleQADTO[0];
            return;
        }

        userTfaQA = securityQuestionTfaService.getSecurityQuestionsAndAnswers(user.getId());
    }

    private boolean validateUserAnswers(JSONObject formData) {
        try {
            JSONArray answers = formData.getJSONArray("answer");
            if (userTfaQA.length != answers.size()) return false;

            for (int i = 0; i < userTfaQA.length; i++) {
                if (!userTfaQA[i].getAnswer().getPlainText()
                        .equals(answers.getString(i))) return false;
            }
        } catch (Exception e) { return false; }

        return true;
    }


    @RequirePOST
    @Override
    public void doAuthenticate(
            StaplerRequest request,
            StaplerResponse response
    ) throws ServletException, IOException {
        Jenkins.get().checkPermission(Jenkins.READ);
        String redirectUrl = "./";

        JSONObject formData = request.getSubmittedForm();
        HttpSession session = request.getSession(false);

        var user = User.current();
        if (user == null) {
            FormApply.success(redirectUrl).generateResponse(request, response, null);
            return;
        }

        LOGGER.fine("Authenticating user tfa security answers");
        try {
            if (validateUserAnswers(formData)) {
                LOGGER.fine(user.getId() + " user is authentic");
                allow2FaAccessAndRedirect(session, response, user);
                return;
            }

            LOGGER.fine("User is not authentic");
            showWrongCredentialWarning.put(user.getId(), true);
            generateSecurityQuestions();

            LOGGER.fine("Redirecting" + user.getId() + " from  MoSecurityQuestionAuth to " + redirectUrl);
            FormApply.success(redirectUrl).generateResponse(request, response, null);
        } catch (Exception e) {
            LOGGER.fine("Exception while authenticating/Logging out the user " + e.getMessage());
        }
    }
}
