package fr.diskmth.socketimpl.server;

import fr.diskmth.loggy.Logger;
import fr.diskmth.loggy.LogsFile;

import java.io.IOException;
import java.net.Socket;

public interface IRequestExecutor
{
    default void processRequest(Socket clientSocket, Logger logger, LogsFile genericsLogs, LogsFile serverCallsLogs)
    {
        try
        {
            logger.log("Finish process for: " + clientSocket.getInetAddress().getHostAddress() + ". Closing request", genericsLogs, serverCallsLogs);
            clientSocket.close();
        }
        catch (IOException exception)
        {
            logger.warn("Error while closing request from: " + clientSocket.getInetAddress().getHostAddress(), exception, genericsLogs, serverCallsLogs);
        }
    }
}
