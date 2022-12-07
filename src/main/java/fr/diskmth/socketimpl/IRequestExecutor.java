package fr.diskmth.socketimpl;

import fr.diskmth.loggy.Logger;
import fr.diskmth.loggy.LogsFile;

import java.io.IOException;
import java.net.Socket;
import java.util.UUID;

public interface IRequestExecutor
{
    int identifier();

    default Result clientSideExecution(Socket clientSocket, Logger logger, LogsFile genericsLogs)
    {
        logger.warn("#####");
        return Result.FAIL;
    }

    default void serverSideExecution(Socket clientSocket, Logger logger, UUID requestId, LogsFile genericsLogs, LogsFile serverCallsLogs)
    {
        try
        {
            logger.log(requestId + ": Finish process. Closing request", genericsLogs, serverCallsLogs);
            clientSocket.close();
        }
        catch (IOException exception)
        {
            logger.warn(requestId + ": Error while closing request", exception, genericsLogs, serverCallsLogs);
        }
    }
}
