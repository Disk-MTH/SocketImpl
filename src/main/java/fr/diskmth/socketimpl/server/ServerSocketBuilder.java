package fr.diskmth.socketimpl.server;

import fr.diskmth.loggy.Logger;
import fr.diskmth.loggy.LogsFile;
import fr.diskmth.socketimpl.common.Pair;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerSocketBuilder
{
    private final Logger logger;
    private final IRequestExecutor requestExecutor;
    private InetSocketAddress address = new InetSocketAddress("localhost", 8080);
    private Pair<InputStream, String> keyStore = null;
    private Pair<InputStream, String> trustStore = null;
    private LogsFile genericsLogs = null;
    private LogsFile serverCallsLogs = null;
    private LogsFile forbiddenCallsLogs = null;
    private int maxEnqueuedRequests = -1;
    private ExecutorService threadPool = Executors.newFixedThreadPool(1);
    private CommandsHandler commandsHandler = null;
    private final List<String> forbiddenIps = new ArrayList<>();


    public ServerSocketBuilder(Logger logger, IRequestExecutor requestExecutor)
    {
        this.logger = logger;
        this.requestExecutor = requestExecutor;
    }

    public ServerSocketBuilder setAddress(String host, int port)
    {
        address = new InetSocketAddress(host, port);
        return this;
    }

    public ServerSocketBuilder setAddress(String httpUrl)
    {
        return setAddress(httpUrl, 80);
    }

    public ServerSocketBuilder setAddress(String host, int port, Pair<InputStream, String> keyStore, Pair<InputStream, String> trustStore)
    {
        this.keyStore = keyStore;
        this.trustStore = trustStore;
        return setAddress(host, port);
    }

    public ServerSocketBuilder setAddress(String httpsUrl, Pair<InputStream, String> keyStore, Pair<InputStream, String> trustStore)
    {
        return setAddress(httpsUrl, 443, keyStore, trustStore);
    }

    public ServerSocketBuilder genericsLogsFile(LogsFile logsFile)
    {
        genericsLogs = logsFile;
        return this;
    }

    public ServerSocketBuilder serverCallsLogsFile(LogsFile logsFile)
    {
        serverCallsLogs = logsFile;
        return this;
    }

    public ServerSocketBuilder forbiddenCallsLogsFile(LogsFile logsFile)
    {
        forbiddenCallsLogs = logsFile;
        return this;
    }

    public ServerSocketBuilder maxEnqueuedRequests(int maxEnqueuedRequests)
    {
        this.maxEnqueuedRequests = maxEnqueuedRequests;
        return this;
    }

    public ServerSocketBuilder multiThread(int maxThreadCount)
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

    public ServerSocketBuilder addCommandsHandler(CommandsHandler commandsHandler)
    {
        this.commandsHandler = commandsHandler;
        return this;
    }

    public ServerSocketBuilder addDefaultCommandsHandler()
    {
        return addCommandsHandler(new CommandsHandler(null, false));
    }

    public ServerSocketBuilder forbiddenIps(List<String> forbiddenIps)
    {
        this.forbiddenIps.addAll(forbiddenIps);
        return this;
    }

    public ServerSocket build()
    {
        if (logger == null) throw new NullPointerException("Server logger can't be null");
        if (address == null) throw new NullPointerException("Server address can't be null");

        return new ServerSocket(logger, requestExecutor, address, keyStore, trustStore, genericsLogs, serverCallsLogs, forbiddenCallsLogs, maxEnqueuedRequests, threadPool, commandsHandler, forbiddenIps);
    }
}
