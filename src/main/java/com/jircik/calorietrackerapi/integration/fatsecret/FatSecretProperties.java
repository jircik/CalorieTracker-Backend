package com.jircik.calorietrackerapi.integration.fatsecret;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "fatsecret")
@Getter
@Setter
public class FatSecretProperties {

    private String workerUrl;
    private String proxyKey;

}
