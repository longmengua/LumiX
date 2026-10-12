package com.lumix.signer;

import org.web3j.crypto.Credentials;

/** 私鑰能力只存在獨立 signer；主 application 不可引用此型別。 */
public interface SigningKeyProvider { Credentials credentials(); }
