package fr.diskmth.socketimpl.server;

import fr.diskmth.socketimpl.common.Pair;

import java.util.List;
import java.util.Scanner;

public final class CommandsHandler extends Thread
{
    private boolean isRunning = true;
    private final List<Pair<String, Runnable>> commands;
    private final boolean ignoreCase;
    private ServerSocketImpl server;

    public CommandsHandler(List<Pair<String, Runnable>> commands, boolean ignoreCase)
    {
        this.commands = commands;
        this.ignoreCase = ignoreCase;
    }

    public CommandsHandler()
    {
        this(null, false);
    }

    public synchronized void init(ServerSocketImpl server)
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
                commands.forEach((command) ->
                {

                    if ((ignoreCase && input.equalsIgnoreCase(command.getFirst())) || (!ignoreCase && input.equals(command.getFirst())))
                    {
                        command.getSecond().run();
                    }
                });
            }
        }
    }
}
