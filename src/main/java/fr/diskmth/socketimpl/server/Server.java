package fr.diskmth.socketimpl.server;

import fr.diskmth.loggy.Logger;
import fr.diskmth.loggy.LogsFile;
import fr.diskmth.socketimpl.common.SSLCertificate;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.ExecutorService;

public class Server
{
    /*---------------------------------------- Variables and constants ----------------------------------------*/

    private final Logger logger;
    private final InetSocketAddress address;
    private final SSLCertificate sslCertificate;
    private final LogsFile genericsLogs;
    private final boolean genericsLogsInit;
    private final LogsFile serverCallsLogs;
    private final boolean serverCallsLogsInit;
    private final int maxEnqueuedRequests;
    private final ExecutorService threadPool;
    private final CommandsHandler commandsHandler;
    private final List<String> forbiddenIps;

    private final List<ClientProcess> clients = new ArrayList<>();
    private boolean isInit = false;
    private boolean isStarted = false;
    private boolean isPaused = false;
    private boolean areCommandsPaused = false;
    private ServerSocket serverSocket;

    /*---------------------------------------- Constructors ----------------------------------------*/

    protected Server(
            Logger logger, InetSocketAddress address, SSLCertificate sslCertificate,
            LogsFile genericsLogs, boolean genericsLogsInit, LogsFile serverCallsLogs, boolean serverCallsLogsInit,
            int maxEnqueuedRequests, ExecutorService threadPool, CommandsHandler commandsHandler, List<String> forbiddenIps)
    {
        this.logger = logger;
        this.address = address;
        this.sslCertificate = sslCertificate;
        this.genericsLogs = genericsLogs;
        this.genericsLogsInit = genericsLogsInit;
        this.serverCallsLogs = serverCallsLogs;
        this.serverCallsLogsInit = serverCallsLogsInit;
        this.maxEnqueuedRequests = maxEnqueuedRequests;
        this.threadPool = threadPool;
        this.commandsHandler = commandsHandler;
        this.forbiddenIps = forbiddenIps;
    }

    /*---------------------------------------- Misc methods ----------------------------------------*/

    public void init()
    {
        if (isInit)
        {
            logger.warn("The server is already initialized", genericsLogs);
            return;
        }
        else if (isStarted)
        {
            logger.warn("The server is started so it cannot be initialize again", genericsLogs);
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

        if (sslCertificate != null)
        {
            logger.log("The server is initializing with SSL (https mode)", genericsLogs);

            SSLContext sslContext;
            try
            {
                logger.log("Generation of the SSL context", genericsLogs);
                sslContext = sslCertificate.createSSLContext();
                logger.log("The SSL context has been generated", genericsLogs);
            }
            catch (Exception exception)
            {
                logger.error("Error during generation of the SSL context", exception, genericsLogs);
                close();
                return;
            }

            try
            {
                logger.log("Generation of the server socket", genericsLogs);
                serverSocket = Objects.requireNonNull(sslContext).getServerSocketFactory().createServerSocket(address.getPort(), maxEnqueuedRequests, address.getAddress());
                ((SSLServerSocket) serverSocket).setNeedClientAuth(true);
                ((SSLServerSocket) serverSocket).setEnabledProtocols(new String[]{"TLSv1.3"});
                logger.log("The server socket has been generated", genericsLogs);
            }
            catch (IOException exception)
            {
                logger.error("Error during generation of the server socket", exception, genericsLogs);
                close();
                return;
            }
        }
        else
        {
            logger.log("The server is initializing without SSl (http mode)", genericsLogs);

            try
            {
                logger.log("Generation of the server socket", genericsLogs);
                serverSocket = ServerSocketFactory.getDefault().createServerSocket(address.getPort(), maxEnqueuedRequests, address.getAddress());
                logger.log("The server socket has been generated", genericsLogs);
            }
            catch (IOException exception)
            {
                logger.error("Error during generation of the server socket", exception, genericsLogs);
                close();
                return;
            }
        }

        isInit = true;

        if (commandsHandler != null)
        {
            commandsHandler.init(this);
        }

        logger.log("The server is initialized on: " + address.getAddress().getHostAddress() + ":" + address.getPort(), genericsLogs);
    }

    public void start()
    {
        if (!isInit)
        {
            logger.warn("The server is not initialized so it cannot be start", genericsLogs);
            return;
        }
        else if (isStarted)
        {
            logger.warn("The server is already started", genericsLogs);
            return;
        }

        logger.log("The server is started", genericsLogs);
        isStarted = true;

        while (isStarted)
        {
            try
            {
                final Socket clientSocket = serverSocket.accept();

                if (isPaused)
                {
                    logger.log("Request skipped from: " + clientSocket.getInetAddress().getHostAddress() + " because server is paused", genericsLogs);
                    clientSocket.close();
                    continue;
                }
                else if (forbiddenIps.contains(clientSocket.getInetAddress().getHostAddress()))
                {
                    logger.warn("Forbidden request skipped from: " + clientSocket.getInetAddress().getHostAddress(), genericsLogs, serverCallsLogs);
                    clientSocket.close();
                    continue;
                }

                clients.add(new ClientProcess(clientSocket, logger, genericsLogs, serverCallsLogs));
                logger.log("Request handled from: " + clientSocket.getInetAddress().getHostAddress() + ". Process id: " + clients.get(clients.size() - 1).clientId, genericsLogs, serverCallsLogs);

                threadPool.submit(() -> clients.get(clients.size() - 1).run());
            }
            catch (IOException exception)
            {
                if (exception instanceof SocketException)
                {
                    logger.log("Incoming client socket closed", genericsLogs);
                }

                logger.error("Error while closing incoming client socket", exception, genericsLogs);
            }
        }
    }

    public void asyncStart()
    {
        logger.log("The server is starting asynchronously", genericsLogs);
        new Thread(this::start).start();
    }

    public void stop()
    {
        if (!isStarted)
        {
            logger.warn("The server is already stopped", genericsLogs);
            return;
        }

        logger.log("Server is stopping", genericsLogs);

        //this.clients.forEach((client) -> client.);

        //TODO: kill clients

        clients.clear();
        logger.log("Server is stopped", genericsLogs);
        isStarted = false;
    }

    public void close()
    {
        if (isStarted)
        {
            logger.warn("The server is started and must be stopped to be closed", genericsLogs);
            return;
        }
        if (!isInit)
        {
            logger.warn("The server is already closed", genericsLogs);
            return;
        }

        logger.log("The server is closing", genericsLogs);

        try
        {
            serverSocket.close();
            serverSocket = null;
            logger.log("The server socket has been closed", genericsLogs);
        }
        catch (IOException | NullPointerException exception)
        {
            logger.log("Unable to close the server socket", genericsLogs);
        }

        logger.log("The server is closed", genericsLogs);
        isInit = false;

        if (genericsLogsInit && genericsLogs != null)
        {
            genericsLogs.close();
        }

        if (serverCallsLogsInit && serverCallsLogs != null)
        {
            serverCallsLogs.close();
        }
    }

    public void pause(boolean pause)
    {
        if (!isInit || !isStarted)
        {
            logger.warn("The server is not initialized and started so it cannot be paused/resumed", genericsLogs);
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
                logger.log("The server has been paused", genericsLogs);
            }
            else
            {
                logger.log("The server has benn resumed", genericsLogs);
            }
        }
    }

    public void pauseCommands(boolean pauseCommands)
    {
        if (!isInit)
        {
            logger.warn("The server is not initialized so commands cannot be paused/resumed", genericsLogs);
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
                logger.log("Commands have been paused", genericsLogs);
            }
            else
            {
                logger.log("Commands have been resumed", genericsLogs);
            }
        }
    }

    /*---------------------------------------- Getters ----------------------------------------*/

    //TODO: getters

    public Logger getLogger()
    {
        return logger;
    }

    public LogsFile getGenericsLogs()
    {
        return genericsLogs;
    }

    public LogsFile getServerCallsLogs()
    {
        return serverCallsLogs;
    }

    public boolean isInit()
    {
        return isInit;
    }

    public boolean areCommandsPaused()
    {
        return areCommandsPaused;
    }

    /*---------------------------------------- Setters ----------------------------------------*/

    //TODO: setters
}