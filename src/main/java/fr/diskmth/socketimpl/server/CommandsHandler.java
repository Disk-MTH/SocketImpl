package fr.diskmth.socketimpl.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class CommandsHandler extends Thread
{
    public static final HashMap<String, ICommand> DEFAULT_COMMANDS = new HashMap<>()
    {
        {
            put("start", Server::asyncStart);
            put("stop", Server::stop);
            put("pause", (server) -> server.pause(true));
            put("resume", (server) -> server.pause(false));
            put("close", Server::close);
            put("help", (server) ->
            {
                server.getLogger().log("Available commands are:");
                DEFAULT_COMMANDS.forEach((syntax, command) -> server.getLogger().log(" - " + syntax));
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
        if (server == null)
        {
            throw new IllegalStateException("the server might not be null");
        }

        server.getLogger().log("Commands have been enabled", server.getGenericsLogs());

        try
        {
            while (server.isInit())
            {
                if (System.in.available() <= 0)
                {
                    sleep(500);
                    continue;
                }

                final String input = new Scanner(System.in).next();

                if (commands != null && !server.areCommandsPaused())
                {
                    final AtomicBoolean found = new AtomicBoolean(false);

                    commands.forEach((syntax, command) ->
                    {
                        if ((ignoreCase && input.equalsIgnoreCase(syntax)) || input.equals(syntax))
                        {
                            command.execute(server);
                            found.set(true);
                        }
                    });

                    if (!found.get())
                    {
                        server.getLogger().warn("Unknown command: " + input, server.getGenericsLogs());
                    }
                }
            }
        }
        catch (IOException | InterruptedException exception)
        {
            server.getLogger().error("An error occurred while reading commands", exception, server.getGenericsLogs());
        }

        server.getLogger().log("Commands have been disabled", server.getGenericsLogs());
    }
}
