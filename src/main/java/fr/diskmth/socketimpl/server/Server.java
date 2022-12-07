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
import java.util.*;
import java.util.concurrent.ExecutorService;

public class Server
{
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
    private boolean isListening = false;
    private boolean isPaused = false;
    private boolean areCommandsPaused = false;
    private ServerSocket serverSocket;

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

    public synchronized void init()
    {
        if(isInit)
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

        /*if (commandsHandler != null)
        {
            commandsHandler.init(this);
        }*/

        isInit = true;
        logger.log("Server is initialized on: " + address.getAddress().getHostAddress() + ":" + address.getPort(), genericsLogs);
    }

    public synchronized void listen()
    {
        logger.log("Server starts listening", genericsLogs);
        isListening = true;

        while (isListening && isInit)
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
                else if(forbiddenIps.contains(clientSocket.getInetAddress().getHostAddress()))
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
                //todo
            }
        }
    }

    public synchronized void close()
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

        isInit = false;
    }

    public synchronized void pause(boolean pause)
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

    public synchronized void pauseCommands(boolean pauseCommands)
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

    public boolean isInit()
    {
        return isInit;
    }

    public boolean isListening()
    {
        return isListening;
    }

    public boolean isPaused()
    {
        return isPaused;
    }

    public boolean areCommandsPaused()
    {
        return areCommandsPaused;
    }
}