package fr.diskmth.socketimpl.client;

import fr.diskmth.loggy.Logger;
import fr.diskmth.loggy.LogsFile;
import fr.diskmth.socketimpl.common.IRequestExecutor;
import fr.diskmth.socketimpl.common.SSLCertificate;

public class ClientBuilder
{
    protected final Logger logger;
    protected final IRequestExecutor requestExecutor;
    protected String host = "localhost";
    protected int port = 8080;
    protected SSLCertificate sslCertificate = null;
    protected LogsFile genericsLogs = null;

    public ClientBuilder(Logger logger, IRequestExecutor requestExecutor)
    {
        this.logger = logger;
        this.requestExecutor = requestExecutor;
    }

    public ClientBuilder setAddress(String host, int port)
    {
        this.host = host;
        this.port = port;
        return this;
    }

    public ClientBuilder setAddress(String httpUrl)
    {
        return setAddress(httpUrl, 80);
    }

    public ClientBuilder setAddress(String host, int port, SSLCertificate sslCertificate)
    {
        this.sslCertificate = sslCertificate;
        return setAddress(host, port);
    }

    public ClientBuilder setAddress(String httpsUrl, SSLCertificate sslCertificate)
    {
        return setAddress(httpsUrl, 443, sslCertificate);
    }

    public ClientBuilder genericsLogsFile(LogsFile logsFile)
    {
        genericsLogs = logsFile;
        return this;
    }

    public Client build()
    {
        if (logger == null) throw new NullPointerException("Server logger can't be null");
        if (requestExecutor == null) throw new NullPointerException("Request executor can't be null");
        if (host == null) throw new NullPointerException("Server address can't be null");

        return new Client(logger, requestExecutor, host, port, sslCertificate, genericsLogs);
    }
}
