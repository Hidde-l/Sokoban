package TenetAgent;

import game.board.compact.BoardCompact;

import java.util.HashSet;
import java.util.Objects;

class Node implements Comparable<Node> {
    BoardCompactExt boardState;
    HashSet<Pair> tenetBoxes;
    TAction action;
    double estimate;
    double distance;
    Node parent;

    public Node(BoardCompact board, double estimate, HashSet<Pair> tenetBoxes, int[][] hashValues, Pair player) {
        this.action = null;
        this.estimate = estimate;
        this.distance = 0.0;
        this.parent = null;
        this.tenetBoxes = tenetBoxes;

        // calculate the hashcode by XORing the player position and all positions of the boxes
        int h = hashValues[(player.x-1) + (player.y-1)*board.width()][1];
        for (Pair p : tenetBoxes) h = h ^ hashValues[(p.x-1) + (p.y-1)* board.width()][0];

        this.boardState = new BoardCompactExt(tenetBoxes, new Pair(player.x, player.y), h);
    }

    public Node(BoardCompactExt board, TAction action, double estimate, double distance, Node parent) {
        this.boardState = board;
        this.action = action;
        this.estimate = estimate;
        this.distance = distance;
        this.parent = parent;
    }

    @Override
    public int compareTo(Node o) {
        return Double.compare(this.estimate + this.distance, o.estimate + o.distance);
    }

}


