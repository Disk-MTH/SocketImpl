package fr.diskmth.socketimpl;

import java.util.UUID;

public interface IConnexion
{
    default UUID getUUID()
    {
        return UUID.randomUUID();
    }

    void init();
    void close();
}
