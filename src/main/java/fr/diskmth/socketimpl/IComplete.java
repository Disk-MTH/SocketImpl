package fr.diskmth.socketimpl;

public interface IComplete<T>
{
    T result();

    void onComplete(Runnable onComplete);
}
