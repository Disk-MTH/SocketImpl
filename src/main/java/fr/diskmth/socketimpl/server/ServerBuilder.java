package fr.diskmth.socketimpl.server;

import fr.diskmth.loggy.Logger;
import fr.diskmth.loggy.LogsFile;
import fr.diskmth.socketimpl.common.IRequestExecutor;
import fr.diskmth.socketimpl.common.SSLCertificate;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerBuilder
{
    protected final Logger logger;
    protected final IRequestExecutor requestExecutor;
    protected InetSocketAddress address = new InetSocketAddress("localhost", 8080);
    protected SSLCertificate sslCertificate = null;
    protected LogsFile genericsLogs = null;
    protected LogsFile serverCallsLogs = null;
    protected LogsFile forbiddenCallsLogs = null;
    protected int maxEnqueuedRequests = -1;
    protected ExecutorService threadPool = Executors.newFixedThreadPool(1);
    protected CommandsHandler commandsHandler = null;
    protected final List<String> forbiddenIps = new ArrayList<>();

    public ServerBuilder(Logger logger, IRequestExecutor requestExecutor)
    {
        this.logger = logger;
        this.requestExecutor = requestExecutor;
    }

    public ServerBuilder setAddress(String host, int port)
    {
        address = new InetSocketAddress(host, port);
        return this;
    }

    public ServerBuilder withSSL(SSLCertificate sslCertificate)
    {
        this.sslCertificate = sslCertificate;
        return this;
    }

    public ServerBuilder genericsLogsFile(LogsFile logsFile)
    {
        genericsLogs = logsFile;
        return this;
    }

    public ServerBuilder serverCallsLogsFile(LogsFile logsFile)
    {
        serverCallsLogs = logsFile;
        return this;
    }

    public ServerBuilder forbiddenCallsLogsFile(LogsFile logsFile)
    {
        forbiddenCallsLogs = logsFile;
        return this;
    }

    public ServerBuilder maxEnqueuedRequests(int maxEnqueuedRequests)
    {
        this.maxEnqueuedRequests = maxEnqueuedRequests;
        return this;
    }

    public ServerBuilder multiThread(int maxThreadCount)
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

    public ServerBuilder addCommandsHandler(CommandsHandler commandsHandler)
    {
        this.commandsHandler = commandsHandler;
        return this;
    }

    public ServerBuilder addDefaultCommandsHandler()
    {
        return addCommandsHandler(new CommandsHandler(null, false));
    }

    public ServerBuilder forbiddenIps(List<String> forbiddenIps)
    {
        this.forbiddenIps.addAll(forbiddenIps);
        return this;
    }

    public Server build()
    {
        if (logger == null) throw new NullPointerException("Server logger can't be null");
        if (requestExecutor == null) throw new NullPointerException("Request executor can't be null");
        if (address == null) throw new NullPointerException("Server address can't be null");

        return new Server(logger, requestExecutor, address, sslCertificate, genericsLogs, serverCallsLogs, forbiddenCallsLogs, maxEnqueuedRequests, threadPool, commandsHandler, forbiddenIps);
    }
}
