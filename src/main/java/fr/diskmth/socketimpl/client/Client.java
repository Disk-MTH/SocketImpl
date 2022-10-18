package fr.diskmth.socketimpl.client;

import fr.diskmth.loggy.Logger;
import fr.diskmth.loggy.LogsFile;
import fr.diskmth.socketimpl.common.IRequestExecutor;
import fr.diskmth.socketimpl.common.SSLCertificate;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.Socket;
import java.util.Objects;

public class Client
{
    protected final Logger logger;
    protected final IRequestExecutor requestExecutor;
    protected final String host;
    protected final int port;
    protected final SSLCertificate sslCertificate;
    protected final LogsFile genericsLogs;

    private Socket clientSocket;

    protected Client(Logger logger, IRequestExecutor requestExecutor, String host, int port, SSLCertificate sslCertificate, LogsFile genericsLogs)
    {
        this.logger = logger;
        this.requestExecutor = requestExecutor;
        this.host = host;
        this.port = port;
        this.sslCertificate = sslCertificate;
        this.genericsLogs = genericsLogs;
    }

    public void start()
    {
        if (genericsLogs != null)
        {
            genericsLogs.init();
        }

        logger.log("Client try to request " + host + ":" + port, genericsLogs);

        if (sslCertificate != null)
        {
            logger.log("Try to create client socket with SSL (https mode)", genericsLogs);

            SSLContext sslContext = null;
            try
            {
                sslContext = sslCertificate.createSSLContext();
            }
            catch (Exception exception)
            {
                logger.error("Error when creating SSL context", exception, genericsLogs);
                stop();
            }

            logger.log("A SSL context has been generated", genericsLogs);

            try
            {
                clientSocket = Objects.requireNonNull(sslContext).getSocketFactory().createSocket(host, port);
                ((SSLSocket) clientSocket).setEnabledProtocols(new String[]{"TLSv1.3"});
            }
            catch (IOException exception)
            {
                logger.error("Error when creating client socket", exception, genericsLogs);
                stop();
            }
        }
        else
        {
            logger.log("Try to create client socket without SSL (http mode)", genericsLogs);

            try
            {
                clientSocket = SocketFactory.getDefault().createSocket(host, port);
            }
            catch (IOException exception)
            {
                logger.error("Error when creating client socket", exception, genericsLogs);
                stop();
            }

            logger.log("Client socket has been created", genericsLogs);
        }

        logger.log("Start to process request", genericsLogs);

        requestExecutor.clientSideExecution(clientSocket, logger, genericsLogs);
    }

    public void stop()
    {
        logger.log("Stopping client", genericsLogs);

        try
        {
            logger.log("Close client socket", genericsLogs);
            clientSocket.close();
        }
        catch (IOException | NullPointerException exception)
        {
            logger.warn("Unable to close client socket", exception, genericsLogs);
        }

        logger.log("Client is stopped", genericsLogs);

        if (genericsLogs != null)
        {
            genericsLogs.close();
        }
    }

    public Logger getLogger()
    {
        return logger;
    }

    public String getHost()
    {
        return host;
    }

    public int getPort()
    {
        return port;
    }

    public LogsFile getGenericsLogs()
    {
        return genericsLogs;
    }
}
