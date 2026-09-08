package com.lumix.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** API transport security 的部署開關；正式環境必須要求 HTTPS。 */
@ConfigurationProperties("lumix.security")
public class ApiSecurityProperties {

    private boolean requireHttps;

    public boolean isRequireHttps() {
        return requireHttps;
    }

    public void setRequireHttps(boolean requireHttps) {
        this.requireHttps = requireHttps;
    }
}
