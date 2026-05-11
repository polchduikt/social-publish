package com.socialpublish.integrations.entity;

import com.socialpublish.auth.entity.User;

public interface BaseIntegrationSettings {
    void setUser(User user);
    User getUser();
    boolean isEnabled();
    void setEnabled(boolean enabled);
}
