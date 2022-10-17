package fr.diskmth.socketimpl.common;

import javax.net.ssl.*;
import java.io.InputStream;
import java.security.KeyStore;

public final class Utils
{
    public static SSLContext createSSLContext(Pair<InputStream, String> p12KeyStore, Pair<InputStream, String> JKSTrustStore) throws Exception
    {
        final KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(p12KeyStore.getFirst(), p12KeyStore.getSecond().toCharArray());

        final KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(JKSTrustStore.getFirst(), JKSTrustStore.getSecond().toCharArray());

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
        keyManagerFactory.init(keyStore, p12KeyStore.getSecond().toCharArray());

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
