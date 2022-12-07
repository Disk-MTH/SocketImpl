package fr.diskmth.socketimpl.server;

import fr.diskmth.loggy.Logger;
import fr.diskmth.loggy.LogsFile;
import fr.diskmth.socketimpl.common.Packet;
import fr.diskmth.socketimpl.common.PacketBuffer;
import fr.diskmth.socketimpl.common.PacketContext;
import fr.diskmth.socketimpl.common.PacketRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.util.Collections;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.UUID;

public class ClientProcess implements Runnable
{
    public final UUID clientId = UUID.randomUUID();

    private final Socket clientSocket;
    private final Logger logger;
    private final LogsFile genericsLogs;
    private final LogsFile serverCallsLogs;

    private final Queue<Packet.S2C> packetsToSend = new PriorityQueue<>();

    protected ClientProcess(Socket clientSocket, Logger logger, LogsFile genericsLogs, LogsFile serverCallsLogs)
    {
        this.clientSocket = clientSocket;
        this.logger = logger;
        this.genericsLogs = genericsLogs;
        this.serverCallsLogs = serverCallsLogs;
    }

    @Override
    public void run()
    {
        try
        {
            final InputStream fromClient = clientSocket.getInputStream();
            final OutputStream toClient = clientSocket.getOutputStream();

            while (!clientSocket.isClosed())
            {
                final PacketBuffer.Reader reader = new PacketBuffer.Reader(fromClient.readAllBytes());

                if(reader.available())
                {
                    PacketRegistry.get(reader.readInt()).getConstructor((Class<?>) null).newInstance().receive(reader, new PacketContext.Server(logger, genericsLogs, serverCallsLogs));
                }

                if(!packetsToSend.isEmpty())
                {
                    final Packet.S2C packetToSend = packetsToSend.remove();
                    final PacketBuffer.Writer writer = new PacketBuffer.Writer(packetToSend.toString().getBytes().length);

                    writer.writeInt(PacketRegistry.indexOf(packetToSend.getClass()));
                    packetToSend.send(writer, new PacketContext.Server(logger, genericsLogs, serverCallsLogs));
                    toClient.write(writer.getBytes());
                }
            }
        }
        catch (IOException | InvocationTargetException | InstantiationException | IllegalAccessException |
               NoSuchMethodException exception)
        {
            //todo
        }
    }

    public void sendPacket(Packet.S2C packet)
    {
        packetsToSend.add(packet);
    }

    public void sendPackets(Packet.S2C... packets)
    {
        Collections.addAll(packetsToSend, packets);
    }
}
