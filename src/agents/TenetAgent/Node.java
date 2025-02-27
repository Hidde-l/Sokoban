package agents.TenetAgent;

import java.util.HashSet;
import java.util.Objects;

class Node implements Comparable<Node> {
    TenetBoard boardState;
    HashSet<Pair> tenetBoxes;
    TAction action;
    double estimate;
    double distance;
    Node parent;

    public Node(TenetBoard tboard, HashSet<Pair> tenetBoxes, TAction action, double estimate, double distance, Node parent) {
        this.boardState = tboard.clone();
        this.tenetBoxes = tenetBoxes;
        this.action = action;
        this.estimate = estimate;
        this.distance = distance;
        this.parent = parent;
    }

    @Override
    public int compareTo(Node o) {
        return Double.compare(this.estimate + this.distance, o.estimate + o.distance);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return Double.compare(estimate, node.estimate) == 0 && Double.compare(distance, node.distance) == 0 && Objects.equals(boardState, node.boardState) && Objects.equals(action, node.action) && Objects.equals(parent, node.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(boardState, action, estimate, distance, parent);
    }
}
