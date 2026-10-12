package com.lumix.signer;

import java.util.Objects;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** dev/test/Sepolia signer HTTP boundary；production profile 在 startup guard 前即拒絕啟動。 */
@RestController @Profile({"dev","test","sepolia"}) @RequestMapping("/internal/v1/signer")
public class SignerController {
    private final SignerService service;
    public SignerController(SignerService service){this.service=Objects.requireNonNull(service);}
    @PostMapping("/sign") public SignerResult sign(@RequestBody SignerRequest request){return service.sign(request);}
    @org.springframework.web.bind.annotation.GetMapping("/sign/{withdrawalId}") public SignerResult result(@org.springframework.web.bind.annotation.PathVariable String withdrawalId,@org.springframework.web.bind.annotation.RequestParam String intentHash){return service.result(withdrawalId,intentHash);}
}
