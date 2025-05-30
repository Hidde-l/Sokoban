package TenetAgent;

public class Pair {
    int x, y;

    Pair(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Pair pair = (Pair) o;
        return x == pair.x && y == pair.y;
    }

    /**
     * Minimised hashcode for speed
     *
     * @return a hash
     */
    @Override
    public int hashCode() {
        return 31*x + y;
    }
    public int distanceTo(Pair p) {
        return Math.abs(x - p.x) + Math.abs(y - p.y);
    }
}
