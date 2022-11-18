package fr.diskmth.socketimpl.common;

public interface IComplete<T>
{
    T result();

    void onComplete(Runnable onComplete);
}
