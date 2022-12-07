package fr.diskmth.socketimpl.common;

public abstract class Packet
{
    public Packet() {}

    public static abstract class C2S extends Packet
    {
        public abstract void send(PacketBuffer.Writer writer, PacketContext.Client context);

        public abstract void receive(PacketBuffer.Reader reader, PacketContext.Client context);
    }

    public static abstract class S2C extends Packet
    {
        public abstract void send(PacketBuffer.Writer writer, PacketContext.Server context);

        public abstract void receive(PacketBuffer.Reader reader, PacketContext.Server context);
    }
}
