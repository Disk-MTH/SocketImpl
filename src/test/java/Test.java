import fr.diskmth.loggy.Logger;
import fr.diskmth.socketimpl.server.CommandsHandler;
import fr.diskmth.socketimpl.server.ServerSocketImpl;

public class Test
{
    public static void main(String[] args)
    {
        final ServerSocketImpl serverSocket = new ServerSocketImpl.Builder(
                new Logger("Test"), (socket, logger, genericsLogs, serversCallsLogs) -> System.out.println("Test"))
                .commandsHandler(new CommandsHandler())
                .build();

        serverSocket.start();
    }
}
