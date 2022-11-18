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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

public class Server
{
    public final Logger logger;
    public final InetSocketAddress address;
    private final SSLCertificate sslCertificate;
    public final LogsFile genericsLogs;
    private final boolean genericsLogsInit;
    public final LogsFile serverCallsLogs;
    private final boolean serverCallsLogsInit;
    public final LogsFile forbiddenCallsLogs;
    private final boolean forbiddenCallsLogsInit;
    private final List<IRequestExecutor> requestExecutors;
    public final int maxEnqueuedRequests;
    private final ExecutorService threadPool;
    public final CommandsHandler commandsHandler;
    public final List<String> forbiddenIps;

    protected volatile boolean isInit = false;
    protected volatile boolean isPaused = false;
    protected volatile boolean areCommandsPaused = false;
    protected volatile boolean isListening = false;

    private ServerSocket serverSocket;

    protected Server(Logger logger, InetSocketAddress address, SSLCertificate sslCertificate, List<IRequestExecutor> requestExecutors,
                     LogsFile genericsLogs, boolean genericsLogsInit, LogsFile serverCallsLogs, boolean serverCallsLogsInit, LogsFile forbiddenCallsLogs, boolean forbiddenCallsLogsInit,
                     int maxEnqueuedRequests, ExecutorService threadPool,
                     CommandsHandler commandsHandler, List<String> forbiddenIps
    )
    {
        this.logger = logger;
        this.address = address;
        this.sslCertificate = sslCertificate;
        this.requestExecutors = requestExecutors;
        this.genericsLogs = genericsLogs;
        this.genericsLogsInit = genericsLogsInit;
        this.serverCallsLogs = serverCallsLogs;
        this.serverCallsLogsInit = serverCallsLogsInit;
        this.forbiddenCallsLogs = forbiddenCallsLogs;
        this.forbiddenCallsLogsInit = forbiddenCallsLogsInit;
        this.maxEnqueuedRequests = maxEnqueuedRequests;
        this.threadPool = threadPool;
        this.commandsHandler = commandsHandler;
        this.forbiddenIps = forbiddenIps;
    }

    public void init()
    {
        if (isInit)
        {
            logger.log("Server already initialized", genericsLogs);
            return;
        }

        if (genericsLogsInit && genericsLogs != null)
        {
            genericsLogs.init();
        }

        if (serverCallsLogsInit && serverCallsLogs != null)
        {
            serverCallsLogs.init();
        }

        if (forbiddenCallsLogsInit && forbiddenCallsLogs != null)
        {
            forbiddenCallsLogs.init();
        }

        if (sslCertificate != null)
        {
            logger.log("Server is initializing with SSl (https mode)", genericsLogs);

            SSLContext sslContext;
            try
            {
                logger.log("Generation of SSL context", genericsLogs);
                sslContext = sslCertificate.createSSLContext();
                logger.log("SSL context has been generated", genericsLogs);
            }
            catch (Exception exception)
            {
                logger.error("Error during generation of SSL context", exception, genericsLogs);
                close();
                return;
            }

            try
            {
                logger.log("Generation of server socket", genericsLogs);
                serverSocket = Objects.requireNonNull(sslContext).getServerSocketFactory().createServerSocket(address.getPort(), maxEnqueuedRequests, address.getAddress());
                ((SSLServerSocket) serverSocket).setNeedClientAuth(true);
                ((SSLServerSocket) serverSocket).setEnabledProtocols(new String[]{"TLSv1.3"});
                logger.log("Server socket has been generated", genericsLogs);
            }
            catch (IOException exception)
            {
                logger.error("Error during generation of server socket", exception, genericsLogs);
                close();
                return;
            }
        }
        else
        {
            logger.log("Server is initializing without SSl (http mode)", genericsLogs);

            try
            {
                logger.log("Generation of server socket", genericsLogs);
                serverSocket = ServerSocketFactory.getDefault().createServerSocket(address.getPort(), maxEnqueuedRequests, address.getAddress());
                logger.log("Server socket has been generated", genericsLogs);
            }
            catch (IOException exception)
            {
                logger.error("Error during generation of server socket", exception, genericsLogs);
                close();
                return;
            }
        }

        if (commandsHandler != null)
        {
            commandsHandler.init(this);
        }

        isInit = true;
        logger.log("Server is initialized on: " + address.getAddress().getHostAddress() + ":" + address.getPort(), genericsLogs);
    }

    public synchronized void listen()
    {
        logger.log("Server starts listening", genericsLogs);
        isListening = true;

        while (isListening)
        {
            /*try
            {
                final Socket clientSocket = serverSocket.accept();
                System.out.println("@@@@@@");
            }
            catch (IOException e)
            {
                System.out.println("####");
            }*/
            /*try
            {
                final Socket clientSocket = serverSocket.accept();
                if (!pause)
                {
                    if (!forbiddenIps.contains(clientSocket.getInetAddress().getHostAddress()))
                    {
                        logger.log("Request handled from: " + clientSocket.getInetAddress().getHostAddress(), genericsLogs, serverCallsLogs);

                        threadPool.submit(() ->
                        {
                            try
                            {
                                new DataOutputStream(clientSocket.getOutputStream()).writeInt(requestExecutor.identifier());
                                requestExecutor.serverSideExecution(clientSocket, logger, genericsLogs, serverCallsLogs);
                            }
                            catch (IOException exception)
                            {
                                logger.error("Unable to send info for handshake", exception, genericsLogs);
                            }
                        });
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
            }*/
        }

        logger.log("Server stops listening", genericsLogs);
    }

    public void asyncListen()
    {
        if (isListening)
        {
            logger.log("Server is already listening", genericsLogs);
            return;
        }

        new Thread(this::listen).start();
    }

    public void pause(boolean pause)
    {
        if (!isListening)
        {
            logger.warn("Server is not listening so it cannot be paused", genericsLogs);
            return;
        }

        if (this.isPaused && pause)
        {
            logger.warn("The server is already paused", genericsLogs);
        }
        else if (!this.isPaused && !pause)
        {
            logger.warn("The server is already resumed", genericsLogs);
        }
        else
        {
            this.isPaused = pause;
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

    public void pauseCommands(boolean pauseCommands)
    {
        if (!isInit)
        {
            logger.warn("Server is not initialized so commands cannot be paused", genericsLogs);
            return;
        }

        if (this.areCommandsPaused && pauseCommands)
        {
            logger.warn("Commands are already paused", genericsLogs);
        }
        else if (!this.areCommandsPaused && !pauseCommands)
        {
            logger.warn("Commands are already resumed", genericsLogs);
        }
        else
        {
            this.areCommandsPaused = pauseCommands;
            if (pauseCommands)
            {
                logger.log("Commands paused", genericsLogs);
            }
            else
            {
                logger.log("Commands resumed", genericsLogs);
            }
        }
    }

    public void close()
    {
        if (!isInit)
        {
            logger.log("Server already closed", genericsLogs);
            return;
        }

        logger.log("Server is closing", genericsLogs);
        isListening = false;

        try
        {
            serverSocket.close();
            serverSocket = null;
            logger.log("Server socket has been closed", genericsLogs);
        }
        catch (IOException | NullPointerException exception)
        {
            logger.log("Unable to close server socket", genericsLogs);
        }

        logger.log("Server is closed", genericsLogs);

        if (genericsLogsInit && genericsLogs != null)
        {
            genericsLogs.close();
        }

        if (serverCallsLogsInit && serverCallsLogs != null)
        {
            serverCallsLogs.close();
        }

        if (forbiddenCallsLogsInit && forbiddenCallsLogs != null)
        {
            forbiddenCallsLogs.close();
        }

        isInit = false;
    }

    public boolean isInit()
    {
        return isInit;
    }

    public boolean isPaused()
    {
        return isPaused;
    }

    public boolean isAreCommandsPaused()
    {
        return areCommandsPaused;
    }

    public boolean isListening()
    {
        return isListening;
    }
}