package agents.TenetAgent;

import java.util.Objects;

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

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
    public int distanceTo(Pair p) {
        return Math.abs(x - p.x) + Math.abs(y - p.y);
    }
}
