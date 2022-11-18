package fr.diskmth.socketimpl.client;

import fr.diskmth.loggy.Logger;
import fr.diskmth.loggy.LogsFile;
import fr.diskmth.socketimpl.common.IComplete;
import fr.diskmth.socketimpl.common.IRequestExecutor;
import fr.diskmth.socketimpl.common.Result;
import fr.diskmth.socketimpl.common.SSLCertificate;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Client
{
    protected final Logger logger;
    protected final String host;
    protected final int port;
    protected final LogsFile genericsLogs;
    protected final boolean genericsLogsInit;
    protected final ExecutorService executor = Executors.newSingleThreadExecutor();
    protected boolean isReady = false;

    protected Socket clientSocket;

    protected Client(Logger logger, String host, int port, SSLCertificate sslCertificate, LogsFile genericsLogs, boolean genericsLogsInit)
    {
        this.logger = logger;
        this.host = host;
        this.port = port;
        this.genericsLogs = genericsLogs;
        this.genericsLogsInit = genericsLogsInit;

        if (genericsLogsInit && genericsLogs != null)
        {
            genericsLogs.init();
        }

        logger.log("Client try to request " + host + ":" + port, genericsLogs);

        if (sslCertificate != null)
        {
            logger.log("Try to create client socket with SSL (https mode)", genericsLogs);

            SSLContext sslContext;
            try
            {
                sslContext = sslCertificate.createSSLContext();
            }
            catch (Exception exception)
            {
                logger.error("Error when creating SSL context", exception, genericsLogs);
                close();
                return;
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
                close();
                return;
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
                close();
                return;
            }

            logger.log("Client socket has been created", genericsLogs);
        }

        isReady = true;
        logger.log("Ready to process requests", genericsLogs);
    }

    public IComplete<Result> request(IRequestExecutor requestExecutor)
    {
        //Future<Result> result;
        /*try
        {
            if (new DataInputStream(clientSocket.getInputStream()).readInt() != requestExecutor.identifier())
            {
                logger.log("No process match on the server with the identifier: " + requestExecutor.identifier(), genericsLogs);
                result =  executor.submit(() -> Result.HANDSHAKE_FAIL);
            }
            else
            {
                logger.log("Handshake passed, start process", genericsLogs);
                result = executor.submit(() -> requestExecutor.clientSideExecution(clientSocket, logger, genericsLogs));
            }
        }
        catch (IOException exception)
        {
            logger.error("Error during handshake", exception, genericsLogs);
            result = executor.submit(() -> Result.HANDSHAKE_FAIL);
        }*/
        try
        {
            if (isReady && new DataInputStream(clientSocket.getInputStream()).readInt() == requestExecutor.identifier())
            {
                final Future<Result> result = executor.submit(() -> requestExecutor.clientSideExecution(clientSocket, logger, genericsLogs));

                while (!result.isDone() || result.isCancelled()) {}

                return new IComplete<>()
                {
                    @Override
                    public Result result()
                    {
                        try
                        {
                            return result.get();
                        }
                        catch (InterruptedException | ExecutionException exception)
                        {
                            return Result.FAIL;
                        }
                    }

                    @Override
                    public void onComplete(Runnable onComplete)
                    {
                        onComplete.run();
                    }
                };
            }
            else
            {
                logger.error("Impossible to send a request if the client is closed", genericsLogs);
            }
        }
        catch (IOException exception)
        {
            logger.error("Handshake error", exception, genericsLogs);
        }
        return null;
    }

    public void close()
    {
        logger.log("Stopping client", genericsLogs);

        isReady = false;

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
