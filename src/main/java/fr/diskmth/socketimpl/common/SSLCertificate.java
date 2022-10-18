package fr.diskmth.socketimpl.common;

import javax.net.ssl.*;
import java.io.InputStream;
import java.security.KeyStore;

public class SSLCertificate
{
    protected final InputStream keystore;
    protected final char[] keystorePassword;
    protected final InputStream truststore;
    protected final char[] truststorePassword;

    public SSLCertificate(InputStream keystore, String keystorePassword, InputStream truststore, String truststorePassword)
    {
        this.keystore = keystore;
        this.keystorePassword = keystorePassword.toCharArray();
        this.truststore = truststore;
        this.truststorePassword = truststorePassword.toCharArray();
    }

    public SSLContext createSSLContext() throws Exception
    {
        final KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(keystore, keystorePassword);

        final KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(truststore, truststorePassword);

        final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);

        X509TrustManager x509TrustManager = null;
        for (TrustManager trustManager : trustManagerFactory.getTrustManagers())
        {
            if (trustManager instanceof X509TrustManager)
            {
                x509TrustManager = (X509TrustManager) trustManager;
                break;
            }
        }

        if (x509TrustManager == null) throw new NullPointerException("Trust manager can't be null");

        final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, keystorePassword);

        X509KeyManager x509KeyManager = null;
        for (KeyManager keyManager : keyManagerFactory.getKeyManagers())
        {
            if (keyManager instanceof X509KeyManager)
            {
                x509KeyManager = (X509KeyManager) keyManager;
                break;
            }
        }

        if (x509KeyManager == null) throw new NullPointerException("Key manager can't be null");

        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(new KeyManager[]{x509KeyManager}, new TrustManager[]{x509TrustManager}, null);
        return sslContext;
    }
}
