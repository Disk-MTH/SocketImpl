package fr.diskmth.socketimpl.client;

import fr.diskmth.loggy.Logger;
import fr.diskmth.loggy.LogsFile;
import fr.diskmth.socketimpl.IComplete;
import fr.diskmth.socketimpl.IRequestExecutor;
import fr.diskmth.socketimpl.Result;
import fr.diskmth.socketimpl.common.SSLCertificate;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client
{
    public final Logger logger;
    public final String host;
    public final int port;
    public final SSLCertificate sslCertificate;
    public final LogsFile genericsLogs;
    public final boolean genericsLogsInit;
    public final ExecutorService executor = Executors.newSingleThreadExecutor();

    protected volatile boolean isInit = false;

    private Socket clientSocket;

    protected Client(Logger logger, String host, int port, SSLCertificate sslCertificate, LogsFile genericsLogs, boolean genericsLogsInit)
    {
        this.logger = logger;
        this.host = host;
        this.port = port;
        this.sslCertificate = sslCertificate;
        this.genericsLogs = genericsLogs;
        this.genericsLogsInit = genericsLogsInit;
    }

    public void init()
    {
        if (isInit)
        {
            logger.log("Client already initialized", genericsLogs);
            return;
        }

        if (genericsLogsInit && genericsLogs != null)
        {
            genericsLogs.init();
        }

        if (sslCertificate != null)
        {
            logger.log("Client is initializing with SSL (https mode)", genericsLogs);

            SSLContext sslContext;
            try
            {
                logger.log("Generation of SSL context", genericsLogs);
                sslContext = sslCertificate.createSSLContext();
                logger.log("SSL context has been generated", genericsLogs);
            }
            catch (Exception exception)
            {
                logger.error("Error when creating SSL context", exception, genericsLogs);
                close();
                return;
            }

            try
            {
                logger.log("Generation of client socket", genericsLogs);
                clientSocket = Objects.requireNonNull(sslContext).getSocketFactory().createSocket(host, port);
                ((SSLSocket) clientSocket).setEnabledProtocols(new String[]{"TLSv1.3"});
                logger.log("Client socket has been generated", genericsLogs);
            }
            catch (IOException exception)
            {
                logger.error("Error during generation of client socket", exception, genericsLogs);
                close();
                return;
            }
        }
        else
        {
            logger.log("Client is initializing without SSl (http mode)", genericsLogs);

            try
            {
                logger.log("Generation of client socket", genericsLogs);
                clientSocket = SocketFactory.getDefault().createSocket(host, port);
                logger.log("Client socket has been generated", genericsLogs);
            }
            catch (IOException exception)
            {
                logger.error("Error during generation of client socket", exception, genericsLogs);
                close();
                return;
            }
        }

        isInit = true;
        logger.log("Ready to process requests on " + host + ":" + port, genericsLogs);
    }

    public IComplete<Result> request(IRequestExecutor requestExecutor)
    {
        if (!isInit)
        {
            logger.error("Client is not initialized so request can't be send", genericsLogs);
            return new IComplete<>()
            {
                @Override
                public Result result()
                {
                    return Result.CLIENT_CLOSED;
                }

                @Override
                public void onComplete(Runnable onComplete)
                {
                    onComplete.run();
                }
            };
        }

        return new IComplete<>()
        {
            @Override
            public Result result()
            {
                return requestExecutor.clientSideExecution(clientSocket, logger, genericsLogs);
            }

            @Override
            public void onComplete(Runnable onComplete)
            {
                onComplete.run();
            }
        };
    }

    public void close()
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

        if (genericsLogsInit && genericsLogs != null)
        {
            genericsLogs.close();
        }
    }

    public boolean isInit()
    {
        return isInit;
    }
}
