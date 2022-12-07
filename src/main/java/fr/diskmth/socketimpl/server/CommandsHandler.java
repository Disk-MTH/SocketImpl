package fr.diskmth.socketimpl.server;

import fr.diskmth.socketimpl.server.ICommand;
import fr.diskmth.socketimpl.server.Server;

import java.util.HashMap;
import java.util.Scanner;

public class CommandsHandler extends Thread
{
    public static final HashMap<String, ICommand> DEFAULT_COMMANDS = new HashMap<>()
    {
        {
            put("start", Server::asyncListen);
            put("stop", (server) -> server.isListening = false);
            put("pause", (server) -> server.pause(true));
            put("resume", (server) -> server.pause(false));
            put("close", Server::close);
            put("help", (server) ->
            {
                System.out.println("Available commands are:");
                DEFAULT_COMMANDS.forEach((syntax, command) -> System.out.println(" - "  + syntax));
            });
        }
    };

    private final HashMap<String, ICommand> commands;
    private final boolean ignoreCase;

    private Server server;

    public CommandsHandler(HashMap<String, ICommand> commands, boolean ignoreCase)
    {
        this.commands = commands;
        this.ignoreCase = ignoreCase;
    }

    protected void init(Server server)
    {
        this.server = server;
        start();
    }

    @Override
    public void run()
    {
        server.logger.log("Commands are enabled", server.genericsLogs);

        final Scanner commandsListener = new Scanner(System.in);

        while (server.isInit)
        {
            final String input = commandsListener.next();

            if (commands != null && !server.areCommandsPaused)
            {
                commands.forEach((syntax, command) ->
                {
                    if ((ignoreCase && input.equalsIgnoreCase(syntax)) || input.equals(syntax))
                    {
                        command.execute(server);
                    }
                });
            }
        }

        server.logger.log("Commands are disabled", server.genericsLogs);
    }
}
