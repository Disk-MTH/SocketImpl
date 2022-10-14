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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ServerSocketImpl
{
    private final Logger logger;
    private final InetSocketAddress address;
    private final Pair<Boolean, LogsFile> genericsLogs;
    private final Pair<Boolean, LogsFile> serverCallsLogs;
    private final Pair<Boolean, LogsFile> forbiddenCallsLogs;
    private final ExecutorService threadPool;
    private final int maxEnqueuedRequests;
    private final CommandsHandler commandsHandler;
    private final Pair<InputStream, String> keyStore;
    private final Pair<InputStream, String> trustStore;
    private volatile boolean run = true;
    private ServerSocket serverSocket;

    private ServerSocketImpl(Logger logger, InetSocketAddress address,
                             Pair<Boolean, LogsFile> genericsLogs, Pair<Boolean, LogsFile> serverCallsLogs, Pair<Boolean, LogsFile> forbiddenCallsLogs,
                             ExecutorService threadPool, int maxEnqueuedRequests, CommandsHandler commandsHandler,
                             Pair<InputStream, String> keyStore, Pair<InputStream, String> trustStore
    )
    {
        this.logger = logger;
        this.address = address;
        this.genericsLogs = genericsLogs;
        this.serverCallsLogs = serverCallsLogs;
        this.forbiddenCallsLogs = forbiddenCallsLogs;
        this.threadPool = threadPool;
        this.maxEnqueuedRequests = maxEnqueuedRequests;
        this.commandsHandler = commandsHandler;
        this.keyStore = keyStore;
        this.trustStore = trustStore;
    }

    public void start()
    {
        if (genericsLogs.getFirst() && genericsLogs.getSecond() != null)
        {
            genericsLogs.getSecond().init();
        }

        if (serverCallsLogs.getFirst() && serverCallsLogs.getSecond() != null)
        {
            serverCallsLogs.getSecond().init();
        }

        if (forbiddenCallsLogs.getFirst() && forbiddenCallsLogs.getSecond() != null)
        {
            forbiddenCallsLogs.getSecond().init();
        }

        if (genericsLogs.getFirst())
        {
            logger.log("ServerSocketImpl is starting on: " + address.getAddress().getHostAddress() + ":" + address.getPort(), genericsLogs.getSecond());
        }

        if (keyStore.getFirst() != null && trustStore.getFirst() != null)
        {
            if (genericsLogs.getFirst())
            {
                logger.log("Try to create server socket with SSl (https mode)", genericsLogs.getSecond());
            }

            SSLContext sslContext = null;
            try
            {
                sslContext = Utils.createSSLContext(keyStore, trustStore);
            }
            catch (Exception exception)
            {
                if (genericsLogs.getFirst())
                {
                    logger.error("Error when creating SSl context", exception, genericsLogs.getSecond());
                }
                stop();
            }

            if (genericsLogs.getFirst())
            {
                logger.log("A SSl context has been generated", genericsLogs.getSecond());
            }

            try
            {
                serverSocket = sslContext.getServerSocketFactory().createServerSocket(address.getPort(), maxEnqueuedRequests, address.getAddress());
            }
            catch (IOException exception)
            {
                if (genericsLogs.getFirst())
                {
                    logger.error("Error when creating server socket", exception, genericsLogs.getSecond());
                }
                stop();
            }
            ((SSLServerSocket) serverSocket).setNeedClientAuth(true);
            ((SSLServerSocket) serverSocket).setEnabledProtocols(new String[]{"TLSv1.3"});
        }
        else
        {
            if (genericsLogs.getFirst())
            {
                logger.log("Try to create server socket without SSL (http mode)", genericsLogs.getSecond());
            }

            try
            {
                serverSocket = ServerSocketFactory.getDefault().createServerSocket(address.getPort(), maxEnqueuedRequests, address.getAddress());
            }
            catch (IOException exception)
            {
                if (genericsLogs.getFirst())
                {
                    logger.error("Error when creating server socket", exception, genericsLogs.getSecond());
                }
                stop();
            }
        }

        if (genericsLogs.getFirst())
        {
            logger.log("Server socket has been generated", genericsLogs.getSecond());
        }

        commandsHandler.start();

        if (genericsLogs.getFirst())
        {
            logger.log("Commands are enabled", genericsLogs.getSecond());
            logger.log("Server is started", genericsLogs.getSecond());
        }

        while (run)
        {
            try
            {
                final Socket clientSocket = serverSocket.accept();

                threadPool.submit(() ->
                {

                });
            }
            catch (IOException exception)
            {
                if (run && genericsLogs.getFirst())
                {
                    logger.warn("Error while handling a request", exception, genericsLogs.getSecond(), serverCallsLogs.getSecond());
                }
            }
        }
    }

    public void suspend()
    {

    }

    public void stop()
    {
        System.exit(0);
    }

    public static final class Builder
    {
        private final Logger logger;
        private final Pair<Boolean, LogsFile> genericsLogs = Pair.of(true, null);
        private final Pair<Boolean, LogsFile> serverCallsLogs = Pair.of(false, null);
        private final Pair<Boolean, LogsFile> forbiddenCallsLogs = Pair.of(false, null);
        private final Pair<InputStream, String> keyStore = Pair.of(null, "");
        private final Pair<InputStream, String> trustStore = Pair.of(null, "");
        private InetSocketAddress address = new InetSocketAddress("localhost", 8080);
        private ExecutorService threadPool = Executors.newFixedThreadPool(1);
        private int maxEnqueuedRequests = -1;
        private CommandsHandler commandsHandler = null;

        public Builder(Logger logger)
        {
            this.logger = logger;
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

        public Builder genericsLogs(boolean state, LogsFile file)
        {
            genericsLogs.setFirst(state).setSecond(file);
            return this;
        }

        public Builder serverCallsLogs(boolean state, LogsFile file)
        {
            serverCallsLogs.setFirst(state).setSecond(file);
            return this;
        }

        public Builder forbiddenCallsLogs(boolean state, LogsFile file)
        {
            forbiddenCallsLogs.setFirst(state).setSecond(file);
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

        public ServerSocketImpl build()
        {
            if (logger == null) throw new NullPointerException("Server logger can't be null");
            if (address == null) throw new NullPointerException("Server address can't be null");

            return new ServerSocketImpl(logger, address, genericsLogs, serverCallsLogs, forbiddenCallsLogs, threadPool, maxEnqueuedRequests, commandsHandler, keyStore, trustStore);
        }
    }
}
