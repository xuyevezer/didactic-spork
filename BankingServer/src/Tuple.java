/**
 * Helper class for tuples.
 * 
 * @param <X>
 *            The first tuple type.
 * @param <Y>
 *            The second tuple type.
 */
public class Tuple<X, Y>
{
    /**
     * The first value.
     */
    public final X x;

    /**
     * The second value.
     */
    public final Y y;

    /**
     * Creates a new tuple with the given values.
     * 
     * @param x
     *            The first value.
     * @param y
     *            The second value.
     */
    public Tuple(X x, Y y)
    {
        this.x = x;
        this.y = y;
    }
}