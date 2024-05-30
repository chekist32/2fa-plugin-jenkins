package io.jenkins.plugins.twofactor.jenkins.tfaMethods.config;

import hudson.model.User;
import hudson.model.UserPropertyDescriptor;

public abstract class AbstractTfaConfigDescriptor extends UserPropertyDescriptor {
    public AbstractTfaConfigDescriptor(Class<? extends AbstractTfaConfig> tfaConfigClazz) {
        super(tfaConfigClazz);
    }
    public AbstractTfaConfigDescriptor() { super(); }

    abstract public boolean showInUserProfile();

    public String getUserId() {
        var currentUser = User.current();
        return currentUser != null ? currentUser.getId() : "";
    }
}
