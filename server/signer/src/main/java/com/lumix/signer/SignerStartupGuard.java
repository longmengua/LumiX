package com.lumix.signer;

import jakarta.annotation.PostConstruct;
import java.util.Locale;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** production 或 Mainnet 永不以本地 keystore 啟動；這是啟動失敗，不是可忽略 warning。 */
@Component
public class SignerStartupGuard {
    private final SignerProperties properties; private final Environment environment;
    public SignerStartupGuard(SignerProperties properties,Environment environment){this.properties=properties;this.environment=environment;}
    @PostConstruct void verify(){ boolean prod=environment.matchesProfiles("production")||environment.matchesProfiles("prod"); if(prod||properties.getChainId()==1L||"MAINNET".equalsIgnoreCase(properties.getNetwork())) throw new IllegalStateException("local signer is prohibited for production and Ethereum Mainnet"); if(properties.getChainId()!=11155111L||!"SEPOLIA".equalsIgnoreCase(properties.getNetwork())) throw new IllegalStateException("only Sepolia signing is supported"); }
}
