package io.jenkins.plugins.twofactor.jenkins.tfaMethods.config;

import hudson.model.Action;
import hudson.model.UserProperty;
import io.jenkins.plugins.twofactor.constants.PluginUrls;
import io.jenkins.plugins.twofactor.jenkins.tfaMethods.TfaMethodType;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public abstract class AbstractTfaConfig extends UserProperty implements Action {
    private final TfaMethodType methodType;
    protected boolean isConfigured;

    public AbstractTfaConfig(boolean isConfigured, TfaMethodType methodType) {
        this.isConfigured = isConfigured;
        this.methodType = methodType;
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getUrlName() { return methodType.getConfigPath(); }

    public boolean isConfigured() {
        return isConfigured;
    }
    public void setConfigured(boolean isConfigured) {
        this.isConfigured = isConfigured;
    }

    public TfaMethodType getMethodType() {
        return methodType;
    }

    protected void redirectToAuthPath(StaplerRequest req, StaplerResponse res) {
        res.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
        res.setHeader("Location", "/"+ PluginUrls.USER_AUTH_PATH+"/"+methodType.getAuthPath());
    }

    abstract public void doConfigure(StaplerRequest req, StaplerResponse res)
            throws IOException, ServletException;

    abstract public void doReset(StaplerRequest req, StaplerResponse rsp)
            throws IOException, ServletException;
}
