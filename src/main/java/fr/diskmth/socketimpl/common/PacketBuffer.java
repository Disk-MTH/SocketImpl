package fr.diskmth.socketimpl.common;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

public abstract class PacketBuffer
{
    protected ByteBuffer byteBuffer;

    protected PacketBuffer(ByteBuffer byteBuffer)
    {
        this.byteBuffer = byteBuffer;
    }

    public byte[] getBytes()
    {
        return byteBuffer.array();
    }

    public int getSize()
    {
        return getBytes().length;
    }

    public void flush()
    {
        byteBuffer.clear();
    }

    public static class Reader extends PacketBuffer
    {
        public Reader(byte... bytes)
        {
            super(ByteBuffer.wrap(bytes));
        }

        public void reassign(byte... bytes)
        {
            byteBuffer = ByteBuffer.wrap(bytes);
        }

        public boolean available()
        {
            return this.byteBuffer.hasRemaining();
        }

        public byte readByte()
        {
            return this.byteBuffer.get();
        }

        public boolean readBoolean()
        {
            return this.readByte() == 1;
        }

        public short readShort()
        {
            return this.byteBuffer.getShort();
        }

        public int readInt()
        {
            return this.byteBuffer.getInt();
        }

        public long readLong()
        {
            return this.byteBuffer.getLong();
        }

        public float readFloat()
        {
            return this.byteBuffer.getFloat();
        }

        public double readDouble()
        {
            return this.byteBuffer.getDouble();
        }

        public char readChar()
        {
            return (char) this.readByte();
        }

        public String readString()
        {
            final int length = this.readInt();
            final byte[] bytes = new byte[length];
            this.byteBuffer.get(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }

        public UUID readUUID()
        {
            return UUID.fromString(this.readString());
        }
    }

    public static class Writer extends PacketBuffer
    {
        public Writer(int size)
        {
            super(ByteBuffer.allocate(size));
        }

        public void writeByte(byte value)
        {
            this.byteBuffer.put(value);
        }

        public void writeBoolean(boolean value)
        {
            this.writeByte((byte) (value ? 1 : 0));
        }

        public void writeShort(short value)
        {
            this.byteBuffer.putShort(value);
        }

        public void writeInt(int value)
        {
            this.byteBuffer.putInt(value);
        }

        public void writeLong(long value)
        {
            this.byteBuffer.putLong(value);
        }

        public void writeFloat(float value)
        {
            this.byteBuffer.putFloat(value);
        }

        public void writeDouble(double value)
        {
            this.byteBuffer.putDouble(value);
        }

        public void writeChar(char value)
        {
            this.writeByte((byte) value);
        }

        public void writeString(String value)
        {
            Objects.requireNonNull(value);
            final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            this.writeInt(bytes.length);
            for (byte b : bytes)
            {
                this.writeByte(b);
            }
        }

        public void writeUUID(UUID value)
        {
            this.writeString(Objects.requireNonNull(value.toString()));
        }
    }
}
