package fr.diskmth.socketimpl.common;

import java.util.ArrayList;
import java.util.List;

public class PacketRegistry
{
    private static final List<Class<? extends Packet>> registeredPackets = new ArrayList<>();

    public static int indexOf(Class<? extends Packet> packet)
    {
        return registeredPackets.indexOf(packet);
    }

    public static <T extends Class<? extends Packet>> T get(T packetType, int index)
    {
        return (T) registeredPackets.get(index);
    }

    public static void registerPacket(Class<? extends Packet> packet)
    {
        registeredPackets.add(packet);
    }

    public static void registerPackets(List<Class<? extends Packet>> packets)
    {
        registeredPackets.addAll(packets);
    }
}