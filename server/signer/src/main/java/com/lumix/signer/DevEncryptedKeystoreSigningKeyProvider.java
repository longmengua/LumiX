package com.lumix.signer;

import java.nio.file.Files;
import java.util.Locale;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;

/** 只在受控測試網 profile 載入 Web3j 加密 keystore；不接受 raw private key 或 mnemonic。 */
@Component @Profile({"dev","test","sepolia"})
public final class DevEncryptedKeystoreSigningKeyProvider implements SigningKeyProvider {
    private final Credentials credentials;
    public DevEncryptedKeystoreSigningKeyProvider(SignerProperties properties) {
        try {
            if(properties.getChainId()!=11155111L || !"SEPOLIA".equals(properties.getNetwork().toUpperCase(Locale.ROOT)) || properties.getKeystorePath()==null || properties.getKeystorePassword()==null || !Files.isRegularFile(properties.getKeystorePath())) throw new IllegalStateException("Sepolia encrypted keystore configuration is required");
            credentials=WalletUtils.loadCredentials(properties.getKeystorePassword(),properties.getKeystorePath().toFile());
        } catch (Exception exception) { throw new IllegalStateException("encrypted test keystore cannot be loaded",exception); }
    }
    @Override public Credentials credentials(){return credentials;}
}
