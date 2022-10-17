package fr.diskmth.socketimpl.server;

import fr.diskmth.loggy.Logger;
import fr.diskmth.loggy.LogsFile;

import java.net.Socket;

public interface IProcessRequest
{
    void process(Socket clientSocket, Logger logger, LogsFile genericsLogs, LogsFile serverCallsLogs);
}
