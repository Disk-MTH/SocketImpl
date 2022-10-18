package fr.diskmth.socketimpl.server;

import java.util.HashMap;
import java.util.Scanner;

public class CommandsHandler extends Thread
{
    private final HashMap<String, ICommand> commands;
    private final boolean ignoreCase;

    private boolean isRunning = true;
    private Server server;

    public CommandsHandler(HashMap<String, ICommand> commands, boolean ignoreCase)
    {
        this.commands = commands;
        this.ignoreCase = ignoreCase;
    }

    public synchronized void init(Server server)
    {
        this.server = server;
        start();
    }

    @Override
    public void run()
    {
        final Scanner commandsListener = new Scanner(System.in);

        while(isRunning)
        {
            final String input = commandsListener.next();

            if (input.equalsIgnoreCase("stop"))
            {
                isRunning = false;
                server.stop();
            }
            else if (input.equalsIgnoreCase("pause"))
            {
                server.pause(true);
            }
            else if (input.equalsIgnoreCase("resume"))
            {
                server.pause(false);
            }
            else if (commands != null)
            {
                commands.forEach((syntax, command) ->
                {
                    if ((ignoreCase && input.equalsIgnoreCase(syntax)) || (!ignoreCase && input.equals(syntax)))
                    {
                        command.execute(server);
                    }
                });
            }
        }
    }
}
