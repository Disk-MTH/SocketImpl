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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ServerSocketImpl
{
    private final Logger logger;
    private final IProcessRequest processRequest;
    private final InetSocketAddress address;
    private final ExecutorService threadPool;
    private final int maxEnqueuedRequests;
    private final CommandsHandler commandsHandler;
    private final Pair<InputStream, String> keyStore;
    private final Pair<InputStream, String> trustStore;
    private final List<String> forbiddenIps;
    private final LogsFile genericsLogs;
    private final LogsFile serverCallsLogs;
    private final LogsFile forbiddenCallsLogs;

    private volatile boolean run = true;
    private volatile boolean pause = false;
    private ServerSocket serverSocket;

    private ServerSocketImpl(Logger logger, IProcessRequest processRequest, InetSocketAddress address,
                             ExecutorService threadPool, int maxEnqueuedRequests, CommandsHandler commandsHandler,
                             Pair<InputStream, String> keyStore, Pair<InputStream, String> trustStore, List<String> forbiddenIps,
                             LogsFile genericsLogs, LogsFile serverCallsLogs, LogsFile forbiddenCallsLogs
    )
    {
        this.logger = logger;
        this.processRequest = processRequest;
        this.address = address;
        this.threadPool = threadPool;
        this.maxEnqueuedRequests = maxEnqueuedRequests;
        this.commandsHandler = commandsHandler;
        this.keyStore = keyStore;
        this.trustStore = trustStore;
        this.forbiddenIps = forbiddenIps;
        this.genericsLogs = genericsLogs;
        this.serverCallsLogs = serverCallsLogs;
        this.forbiddenCallsLogs = forbiddenCallsLogs;
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

                        threadPool.submit(() -> processRequest.process(clientSocket, logger, genericsLogs, serverCallsLogs));
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

    public static final class Builder
    {
        private final Logger logger;
        private final IProcessRequest processRequest;
        private final Pair<InputStream, String> keyStore = Pair.of(null, "");
        private final Pair<InputStream, String> trustStore = Pair.of(null, "");
        private final List<String> forbiddenIps = new ArrayList<>();
        private InetSocketAddress address = new InetSocketAddress("localhost", 8080);
        private ExecutorService threadPool = Executors.newFixedThreadPool(1);
        private int maxEnqueuedRequests = -1;
        private CommandsHandler commandsHandler = null;
        private LogsFile genericsLogs = null;
        private LogsFile serverCallsLogs = null;
        private LogsFile forbiddenCallsLogs = null;

        public Builder(Logger logger, IProcessRequest processRequest)
        {
            this.logger = logger;
            this.processRequest = processRequest;
        }

        public Builder address(String host, int port)
        {
            this.address = new InetSocketAddress(host, port);
            return this;
        }

        public Builder httpUrl(String url)
        {
            return address(url, 80);
        }

        public Builder httpsUrl(String url)
        {
            return address(url, 443);
        }

        public Builder genericsLogsFile(LogsFile file)
        {
            genericsLogs = file;
            return this;
        }

        public Builder serverCallsLogsFile(LogsFile file)
        {
            serverCallsLogs = file;
            return this;
        }

        public Builder forbiddenCallsLogsFile(LogsFile file)
        {
            forbiddenCallsLogs = file;
            return this;
        }

        public Builder multiThread(int maxThreadCount)
        {
            if (maxThreadCount > 0)
            {
                threadPool = Executors.newFixedThreadPool(maxThreadCount);
            }
            else
            {
                threadPool = Executors.newCachedThreadPool();
            }
            return this;
        }

        public Builder maxEnqueuedRequests(int maxEnqueuedRequests)
        {
            this.maxEnqueuedRequests = maxEnqueuedRequests;
            return this;
        }

        public Builder commandsHandler(CommandsHandler commandsHandler)
        {
            this.commandsHandler = commandsHandler;
            return this;
        }

        public Builder withSSL(Pair<InputStream, String> keyStore, Pair<InputStream, String> trustStore)
        {
            this.keyStore.setFirst(keyStore.getFirst()).setSecond(keyStore.getSecond());
            this.trustStore.setFirst(trustStore.getFirst()).setSecond(trustStore.getSecond());
            return this;
        }

        public Builder forbiddenIps(List<String> forbiddenIps)
        {
            this.forbiddenIps.addAll(forbiddenIps);
            return this;
        }

        public ServerSocketImpl build()
        {
            if (logger == null) throw new NullPointerException("Server logger can't be null");
            if (address == null) throw new NullPointerException("Server address can't be null");

            return new ServerSocketImpl(logger, processRequest, address, threadPool, maxEnqueuedRequests, commandsHandler, keyStore, trustStore, forbiddenIps, genericsLogs, serverCallsLogs, forbiddenCallsLogs);
        }
    }
}
