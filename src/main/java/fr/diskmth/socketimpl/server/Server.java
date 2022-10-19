package fr.diskmth.socketimpl.server;

import fr.diskmth.loggy.Logger;
import fr.diskmth.loggy.LogsFile;
import fr.diskmth.socketimpl.common.IRequestExecutor;
import fr.diskmth.socketimpl.common.SSLCertificate;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

public class Server
{
    protected final Logger logger;
    protected final IRequestExecutor requestExecutor;
    protected final LogsFile genericsLogs;
    protected final LogsFile serverCallsLogs;
    protected final LogsFile forbiddenCallsLogs;
    protected final InetSocketAddress address;
    protected final SSLCertificate sslCertificate;
    protected final int maxEnqueuedRequests;
    protected final ExecutorService threadPool;
    protected final CommandsHandler commandsHandler;
    protected final List<String> forbiddenIps;

    private volatile boolean run = true;
    private volatile boolean pause = false;
    private ServerSocket serverSocket;

    protected Server(Logger logger, IRequestExecutor requestExecutor, InetSocketAddress address, SSLCertificate sslCertificate,
           LogsFile genericsLogs, LogsFile serverCallsLogs, LogsFile forbiddenCallsLogs,
           int maxEnqueuedRequests, ExecutorService threadPool,
           CommandsHandler commandsHandler, List<String> forbiddenIps
    )
    {
        this.logger = logger;
        this.requestExecutor = requestExecutor;
        this.address = address;
        this.sslCertificate = sslCertificate;
        this.genericsLogs = genericsLogs;
        this.serverCallsLogs = serverCallsLogs;
        this.forbiddenCallsLogs = forbiddenCallsLogs;
        this.maxEnqueuedRequests = maxEnqueuedRequests;
        this.threadPool = threadPool;
        this.commandsHandler = commandsHandler;
        this.forbiddenIps = forbiddenIps;
    }

    public void start()
    {
        if (genericsLogs != null)
        {
            genericsLogs.init();
        }

        if (serverCallsLogs != null)
        {
            serverCallsLogs.init();
        }

        if (forbiddenCallsLogs != null)
        {
            forbiddenCallsLogs.init();
        }

        logger.log("Server is starting on: " + address.getAddress().getHostAddress() + ":" + address.getPort(), genericsLogs);

        if (sslCertificate != null)
        {
            logger.log("Try to create server socket with SSl (https mode)", genericsLogs);

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
                serverSocket = Objects.requireNonNull(sslContext).getServerSocketFactory().createServerSocket(address.getPort(), maxEnqueuedRequests, address.getAddress());
            }
            catch (IOException exception)
            {
                logger.error("Error when creating server socket", exception, genericsLogs);
                stop();
            }
            ((SSLServerSocket) serverSocket).setNeedClientAuth(true);
            ((SSLServerSocket) serverSocket).setEnabledProtocols(new String[]{"TLSv1.3"});
        }
        else
        {
            logger.log("Try to create server socket without SSL (http mode)", genericsLogs);

            try
            {
                serverSocket = ServerSocketFactory.getDefault().createServerSocket(address.getPort(), maxEnqueuedRequests, address.getAddress());
            }
            catch (IOException exception)
            {
                logger.error("Error when creating server socket", exception, genericsLogs);
                stop();
            }
        }

        logger.log("Server socket has been generated", genericsLogs);

        if (commandsHandler != null)
        {
            commandsHandler.init(this);
            logger.log("Commands are enabled", genericsLogs);
        }

        logger.log("Server is started", genericsLogs);

        while (run)
        {
            try
            {
                final Socket clientSocket = serverSocket.accept();
                if (!pause)
                {
                    if (!forbiddenIps.contains(clientSocket.getInetAddress().getHostAddress()))
                    {
                        logger.log("Request handled from: " + clientSocket.getInetAddress().getHostAddress(), genericsLogs, serverCallsLogs);

                        threadPool.submit(() -> requestExecutor.serverSideExecution(clientSocket, logger, genericsLogs, serverCallsLogs));
                    }
                    else
                    {
                        logger.warn("Forbidden request skipped from: " + clientSocket.getInetAddress().getHostAddress(), genericsLogs, forbiddenCallsLogs);
                        clientSocket.close();
                    }
                }
                else
                {
                    clientSocket.close();
                }
            }
            catch (IOException exception)
            {
                if (run)
                {
                    logger.warn("Error while handling a request", exception, genericsLogs, serverCallsLogs);
                }
            }
        }
    }

    public void pause(boolean pause)
    {
        if (pause && this.pause)
        {
            logger.warn("The server is already paused", genericsLogs);
        }
        else if (!pause && !this.pause)
        {
            logger.warn("The server is already active", genericsLogs);
        }
        else
        {
            this.pause = pause;
            if (pause)
            {
                logger.log("Server paused", genericsLogs);
            }
            else
            {
                logger.log("Server resumed", genericsLogs);
            }
        }
    }

    public void stop()
    {
        logger.log("Server is stopping", genericsLogs);
        run = false;

        try
        {
            logger.log("Close server socket", genericsLogs);
            serverSocket.close();
        }
        catch (IOException | NullPointerException exception)
        {
            logger.warn("Unable to close server socket", exception, genericsLogs);
        }

        try
        {
            logger.log("Close threadPool", genericsLogs);
            threadPool.shutdown();
        }
        catch (NullPointerException exception)
        {
            logger.warn("Unable to close threadPool", exception, genericsLogs);
        }

        logger.log("Server is stopped", genericsLogs);

        if (genericsLogs != null)
        {
            genericsLogs.close();
        }

        if (serverCallsLogs != null)
        {
            serverCallsLogs.close();
        }

        if (forbiddenCallsLogs != null)
        {
            forbiddenCallsLogs.close();
        }

        System.exit(0);
    }

    public Logger getLogger()
    {
        return logger;
    }

    public InetSocketAddress getAddress()
    {
        return address;
    }

    public LogsFile getGenericsLogs()
    {
        return genericsLogs;
    }

    public LogsFile getServerCallsLogs()
    {
        return serverCallsLogs;
    }

    public LogsFile getForbiddenCallsLogs()
    {
        return forbiddenCallsLogs;
    }

    public List<String> getForbiddenIps()
    {
        return forbiddenIps;
    }
}
