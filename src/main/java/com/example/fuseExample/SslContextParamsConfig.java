package com.example.fuseExample;

import org.apache.camel.util.jsse.KeyManagersParameters;
import org.apache.camel.util.jsse.KeyStoreParameters;
import org.apache.camel.util.jsse.SSLContextParameters;
import org.apache.camel.util.jsse.TrustManagersParameters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SslContextParamsConfig {


    @Bean
    public SSLContextParameters contextParameters() {
        KeyStoreParameters ksp = new KeyStoreParameters();
        ksp.setResource("mypath.jks");
        ksp.setPassword("admin");
        KeyStoreParameters tsp = new KeyStoreParameters();
        tsp.setResource("trustStore.jks");
        TrustManagersParameters trustManager = new TrustManagersParameters();
        trustManager.setKeyStore(tsp);
        KeyManagersParameters kmp = new KeyManagersParameters();
        kmp.setKeyStore(ksp);
        kmp.setKeyPassword("admin");
        SSLContextParameters scp = new SSLContextParameters();
        scp.setKeyManagers(kmp);
        scp.setTrustManagers(trustManager);
        return scp;
    }
}
