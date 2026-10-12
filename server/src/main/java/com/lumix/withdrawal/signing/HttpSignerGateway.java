package com.lumix.withdrawal.signing;

import com.lumix.withdrawal.config.WithdrawalPlatformProperties;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Sepolia signer 的 HTTP adapter；production mTLS provider 尚未配置時不可建立此 bean。 */
@Component
@ConditionalOnProperty(prefix="lumix.withdrawal",name="enabled",havingValue="true")
public class HttpSignerGateway implements SignerGateway {
    private final RestClient client;
    public HttpSignerGateway(WithdrawalPlatformProperties properties) { String base=Objects.requireNonNull(properties.getSignerUrl(),"signerUrl must not be null").trim(); if(!base.startsWith("http://")&&!base.startsWith("https://")) throw new IllegalStateException("signer URL must use HTTP(S)"); this.client=RestClient.builder().baseUrl(base).build(); }
    @Override public SigningResult sign(SigningRequest request) { Response response=client.post().uri("/internal/v1/signer/sign").contentType(MediaType.APPLICATION_JSON).body(request).retrieve().body(Response.class); if(response==null||response.transactionHash()==null||response.signedRawTransaction()==null) throw new IllegalStateException("signer returned an ambiguous result"); return new SigningResult(response.transactionHash(),response.signedRawTransaction(),response.replayed()); }
    @Override public SigningResult result(String withdrawalId,String intentHash) { Response response=client.get().uri(builder->builder.path("/internal/v1/signer/sign/{withdrawalId}").queryParam("intentHash",intentHash).build(withdrawalId)).retrieve().body(Response.class); return response==null?null:new SigningResult(response.transactionHash(),response.signedRawTransaction(),response.replayed()); }
    private record Response(String transactionHash,String signedRawTransaction,boolean replayed) { }
}
