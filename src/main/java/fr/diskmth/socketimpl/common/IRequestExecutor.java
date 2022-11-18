package fr.diskmth.socketimpl.common;

import fr.diskmth.loggy.Logger;
import fr.diskmth.loggy.LogsFile;

import java.io.IOException;
import java.net.Socket;

public interface IRequestExecutor
{
    int identifier();

    default Result clientSideExecution(Socket clientSocket, Logger logger, LogsFile genericsLogs)
    {
        try
        {
            logger.log("Finish process. Closing request", genericsLogs);
            clientSocket.close();
        }
        catch (IOException exception)
        {
            logger.warn("Error while closing request", exception, genericsLogs);
        }
        return Result.SUCCESS;
    }

    default void serverSideExecution(Socket clientSocket, Logger logger, LogsFile genericsLogs, LogsFile serverCallsLogs)
    {
        /*try
        {
            logger.log("Finish process. Closing request", genericsLogs);
            clientSocket.close();
        }
        catch (IOException exception)
        {
            logger.warn("Error while closing request", exception, genericsLogs);
        }*/
        /*try
        {
            new DataOutputStream(clientSocket.getOutputStream()).writeInt(identifier());
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }*/
    }
}
