package com.lumix.signer;

import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/** production/Mainnet local signer 必須在啟動階段拒絕，而非只留下 runtime warning。 */
class SignerStartupGuardTest {
    @Test void rejectsMainnetLocalSigner(){SignerProperties properties=new SignerProperties();properties.setChainId(1L);properties.setNetwork("MAINNET");assertThrows(IllegalStateException.class,()->new SignerStartupGuard(properties,new MockEnvironment().withProperty("spring.profiles.active","production")).verify());}
}
