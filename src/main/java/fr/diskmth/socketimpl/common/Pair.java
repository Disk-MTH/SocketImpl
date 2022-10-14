package fr.diskmth.socketimpl.common;

public class Pair<F, S>
{
    private F first;
    private S second;

    public Pair(F first, S second)
    {
        this.first = first;
        this.second = second;
    }

    public F getFirst()
    {
        return this.first;
    }

    public Pair<F, S> setFirst(F first)
    {
        this.first = first;
        return this;
    }

    public S getSecond()
    {
        return this.second;
    }

    public Pair<F, S> setSecond(S second)
    {
        this.second = second;
        return this;
    }

    @Override
    public String toString()
    {
        return "(" + first + ", " + second + ")";
    }

    public static <F, S> Pair<F, S> of(final F first, final S second)
    {
        return new Pair<>(first, second);
    }
}
