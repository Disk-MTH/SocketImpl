package fr.diskmth.socketimpl.server;

import fr.diskmth.loggy.Logger;
import fr.diskmth.loggy.LogsFile;
import fr.diskmth.socketimpl.common.Pair;
import fr.diskmth.socketimpl.common.Utils;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

public class ServerSocket
{
    private final Logger logger;
    private final IRequestExecutor requestExecutor;
    private final LogsFile genericsLogs;
    private final LogsFile serverCallsLogs;
    private final LogsFile forbiddenCallsLogs;
    private final InetSocketAddress address;
    private final Pair<InputStream, String> keyStore;
    private final Pair<InputStream, String> trustStore;
    private final int maxEnqueuedRequests;
    private final ExecutorService threadPool;
    private final CommandsHandler commandsHandler;
    private final List<String> forbiddenIps;

    private volatile boolean run = true;
    private volatile boolean pause = false;
    private java.net.ServerSocket serverSocket;

    ServerSocket(Logger logger, IRequestExecutor requestExecutor, InetSocketAddress address,
                 Pair<InputStream, String> keyStore, Pair<InputStream, String> trustStore,
                 LogsFile genericsLogs, LogsFile serverCallsLogs, LogsFile forbiddenCallsLogs,
                 int maxEnqueuedRequests, ExecutorService threadPool,
                 CommandsHandler commandsHandler, List<String> forbiddenIps
    )
    {
        this.logger = logger;
        this.requestExecutor = requestExecutor;
        this.address = address;
        this.keyStore = keyStore;
        this.trustStore = trustStore;
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
        /* -------------------------------------------------- Init logs files -------------------------------------------------- */

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

        /* -------------------------------------------------- Log info -------------------------------------------------- */

        logger.log("ServerSocketImpl is starting on: " + address.getAddress().getHostAddress() + ":" + address.getPort(), genericsLogs);

        /* -------------------------------------------------- Create server socket -------------------------------------------------- */

        if (keyStore.getFirst() != null && trustStore.getFirst() != null)
        {
            logger.log("Try to create server socket with SSl (https mode)", genericsLogs);

            SSLContext sslContext = null;
            try
            {
                sslContext = Utils.createSSLContext(keyStore, trustStore);
            }
            catch (Exception exception)
            {
                logger.error("Error when creating SSl context", exception, genericsLogs);
                stop();
            }

            logger.log("A SSl context has been generated", genericsLogs);

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

        /* -------------------------------------------------- Init commands handler -------------------------------------------------- */

        if (commandsHandler != null)
        {
            commandsHandler.init(this);
            logger.log("Commands are enabled", genericsLogs);
        }

        /* -------------------------------------------------- Log info -------------------------------------------------- */

        logger.log("Server is started", genericsLogs);

        /* -------------------------------------------------- Server loop -------------------------------------------------- */

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

                        threadPool.submit(() -> requestExecutor.processRequest(clientSocket, logger, genericsLogs, serverCallsLogs));
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
        /* -------------------------------------------------- Log info -------------------------------------------------- */

        logger.log("Server is stopping", genericsLogs);

        /* -------------------------------------------------- Interrupt server loop -------------------------------------------------- */

        run = false;

        /* -------------------------------------------------- Close server socket -------------------------------------------------- */

        try
        {
            logger.log("Close server socket", genericsLogs);
            serverSocket.close();
        }
        catch (IOException | NullPointerException exception)
        {
            logger.warn("Unable to close server socket", exception, genericsLogs);
        }

        /* -------------------------------------------------- Close thread pool -------------------------------------------------- */

        try
        {
            logger.log("Close threadPool", genericsLogs);
            threadPool.shutdown();
        }
        catch (NullPointerException exception)
        {
            logger.warn("Unable to close threadPool", exception, genericsLogs);
        }

        /* -------------------------------------------------- Log info -------------------------------------------------- */

        logger.log("Server is stopped", genericsLogs);

        /* -------------------------------------------------- Close logs files -------------------------------------------------- */

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
}
