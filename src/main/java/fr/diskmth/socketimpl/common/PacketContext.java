package fr.diskmth.socketimpl.common;

import fr.diskmth.loggy.Logger;
import fr.diskmth.loggy.LogsFile;

public abstract class PacketContext
{
    private final Logger logger;
    private final LogsFile genericsLogs;

    public Logger getLogger()
    {
        return logger;
    }

    public LogsFile getGenericsLogs()
    {
        return genericsLogs;
    }

    protected PacketContext(Logger logger, LogsFile genericsLogs)
    {
        this.logger = logger;
        this.genericsLogs = genericsLogs;
    }

    public static class Client extends PacketContext
    {
        public Client(Logger logger, LogsFile genericsLogs)
        {
            super(logger, genericsLogs);
        }
    }

    public static class Server extends PacketContext
    {
        private final LogsFile serverCallsLogs;

        public Server(Logger logger, LogsFile genericsLogs, LogsFile serverCallsLogs)
        {
            super(logger, genericsLogs);
            this.serverCallsLogs = serverCallsLogs;
        }

        public LogsFile getServerCallsLogs()
        {
            return serverCallsLogs;
        }
    }
}
