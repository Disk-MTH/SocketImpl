package fr.diskmth.socketimpl.server;

import java.util.HashMap;
import java.util.Scanner;

public final class CommandsHandler extends Thread
{
    private boolean isRunning = true;
    private final HashMap<String, Runnable> commands;
    private final boolean ignoreCase;

    public CommandsHandler(HashMap<String, Runnable> commands, boolean ignoreCase)
    {
        this.commands = commands;
        this.ignoreCase = ignoreCase;
    }

    public void close()
    {
        isRunning = false;
    }

    @Override
    public void run()
    {
        final Scanner commandsListener = new Scanner(System.in);

        while(isRunning)
        {
            final String input = commandsListener.next();

            for (String command : commands.keySet())
            {
                if ((ignoreCase && input.equalsIgnoreCase(command)) || (!ignoreCase && input.equals(command)))
                {
                    commands.get(command).run();
                }
            }
        }
    }
}
