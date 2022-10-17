import fr.diskmth.loggy.Logger;
import fr.diskmth.loggy.LogsFile;
import fr.diskmth.socketimpl.server.CommandsHandler;
import fr.diskmth.socketimpl.server.IProcessRequest;
import fr.diskmth.socketimpl.server.ServerSocketImpl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;

public class Test
{
    public static void main(String[] args)
    {
        final ServerSocketImpl serverSocket = new ServerSocketImpl.Builder(
                new Logger("Test"), new IProcessRequest()
                    {
                        @Override
                        public void process(Socket clientSocket, Logger logger, LogsFile genericsLogs, LogsFile serverCallsLogs)
                        {
                            logger.warn("Test");
                            try
                            {
                                final DataInputStream fromClient = new DataInputStream(clientSocket.getInputStream());
                                final DataOutputStream toClient = new DataOutputStream(clientSocket.getOutputStream());

                                final File f = new File("C:/Users/gille/Desktop/client-truststore.jks");

                                toClient.write(Files.readAllBytes(f.toPath()));

                                logger.warn(fromClient.readUTF());

                                fromClient.close();
                                toClient.close();
                            }
                            catch (IOException exception)
                            {
                                logger.error("Unable to read input/output of the request", exception, genericsLogs, serverCallsLogs);
                            }

                            IProcessRequest.super.process(clientSocket, logger, genericsLogs, serverCallsLogs);
                        }
                    }
                )
                .commandsHandler(new CommandsHandler())
                .build();

        serverSocket.start();
    }
}
