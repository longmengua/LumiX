package com.lumix.signer;

/** production key provider 的明確 fail-closed 實作；HSM/PKCS#11 必須另行受審核接入。 */
public final class DisabledProductionSigningKeyProvider implements SigningKeyProvider {
    @Override public org.web3j.crypto.Credentials credentials(){ throw new IllegalStateException("production signing key provider is disabled"); }
}
