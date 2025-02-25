package agents;

import game.actions.EDirection;
import game.actions.compact.CAction;
import game.actions.compact.CMove;
import game.actions.compact.CPush;
import game.board.compact.BoardCompact;
import game.board.compact.CTile;

import java.util.*;

import static java.lang.System.out;

public class TenetAgent extends ArtificialAgent {
    protected BoardCompact board;
    protected TenetBoard tboard;
    protected int searchedNodes;
    protected List<EDirection> result;

    private HashSet<Pair> targets;
    private HashSet<Pair> boxes;


    @Override
    protected List<EDirection> think(BoardCompact board) {
        // initialize everything
        this.board = board;
        searchedNodes = 0;
        this.result = new ArrayList<>();
        this.targets = new HashSet<>();
        this.boxes = new HashSet<>();
        this.tboard = new TenetBoard(board);
        findBoxesAndTargets(this.targets, this.boxes);

        // actually search
        long searchStartMillis = System.currentTimeMillis();
        search(boxes);
        long searchTime = System.currentTimeMillis() - searchStartMillis;

        if (verbose) {
            out.println("Nodes visited: " + searchedNodes);
            out.printf("Performance: %.1f nodes/sec\n",
                    ((double)searchedNodes / (double)searchTime * 1000));
        }

        return result.isEmpty() ? null : result;
    }

    private void search(HashSet<Pair> boxes) {
        PriorityQueue<TenetAgent.Node> queue = new PriorityQueue<>();
        Set<TenetBoard> visited = new HashSet<>();

        // Make a new board class where the target and the box are swapped?
        TenetAgent.Node init = new TenetAgent.Node(tboard, boxes, null, estimate(boxes), 0.0, null);
        queue.add(init);

        while (!queue.isEmpty()) {
            TenetAgent.Node currentState = queue.poll();
            visited.add(currentState.boardState);

            // if we found a solution, construct the list of actions taken and return
            if(currentState.boardState.isVictory()) {
//                construct(currentState);
                return;
            }

            List<CAction> allActions = new ArrayList<>(CMove.getActions());
            allActions.addAll(CPush.getActions());

            // loop over all the possible actions
            for(CAction action : allActions){
                // if the action is possible then apply it to a cloned board
                if(!action.isPossible(currentState.boardState)) continue;
                TenetBoard newState = currentState.boardState.clone();
                action.perform(newState);

                // if we haven't been at this board state before, explore it further
                if(!visited.contains(newState)) {
                    HashSet<Pair> newBoxes = cloneSet(currentState.boxes);
                    if(action.getClass() == CPush.class) {
                        EDirection dir = action.getDirection();

                        // if the place you push to is a dead square, don't search this further
                        if(deadSquares[newState.playerX + dir.dX][newState.playerY + dir.dY]) continue;

                        // update the location of the box
                        newBoxes.remove(new Pair(newState.playerX, newState.playerY));
                        newBoxes.add(new Pair(newState.playerX + dir.dX, newState.playerY + dir.dY));
                    }
                    queue.add(new TenetAgent.Node(newState, newBoxes, action, estimate(newBoxes),currentState.distance + 1, currentState));
                }
            }
        }
    }

    private double estimate(HashSet<Pair> boxes) {
        return 0.0;
    }

    private void findBoxesAndTargets(Set<Pair> targets, Set<Pair> boxes) {
        for (int x = 0; x < board.width(); x++) {
            for (int y = 0; y < board.height(); y++) {
                if(CTile.forBox(1, board.tiles[x][y])) targets.add(new Pair(x, y));
                if(CTile.isSomeBox(board.tiles[x][y])) boxes.add(new Pair(x, y));
            }
        }
    }

    class Node implements Comparable<TenetAgent.Node> {
        TenetBoard boardState;
        HashSet<Pair> boxes;
        CAction action;
        double estimate;
        double distance;
        TenetAgent.Node parent;

        public Node(TenetBoard tboard, HashSet<Pair> boxes, CAction action, double estimate, double distance, TenetAgent.Node parent) {
            this.boardState = tboard.clone();
            this.boxes = boxes;
            this.action = action;
            this.estimate = estimate;
            this.distance = distance;
            this.parent = parent;
        }

        @Override
        public int compareTo(TenetAgent.Node o) {
            return Double.compare(this.estimate + this.distance, o.estimate + o.distance);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TenetAgent.Node node = (TenetAgent.Node) o;
            return Double.compare(estimate, node.estimate) == 0 && Double.compare(distance, node.distance) == 0 && Objects.equals(boardState, node.boardState) && Objects.equals(action, node.action) && Objects.equals(parent, node.parent);
        }

        @Override
        public int hashCode() {
            return Objects.hash(boardState, action, estimate, distance, parent);
        }
    }
}

class Pair {

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

class TenetBoard {
    public int[][] tiles; // Value 0: wall. Value 1: Free space; Value 2: TenetBox; Value 3: TenetTarget, Value 4: Player
    public int width; //Make TenetBox value 1 so that any value >1 is walkable
    public int height;
    public int boxCount;
    public int boxInPlaceCount;
    public int playerX;
    public int playerY;
    public Set<Pair> tenetTargets; //Note that these are the boxes in the original board
    public Set<Pair> tenetBoxes; //and these are the targets of the original board

    /**
     * Constructor from a BoardCompact
     *
     * @param board the CompactBoard of this level
     */
    public TenetBoard(BoardCompact board) {
        this.width = board.width();
        this.height = board.height();
        this.tiles = new int[this.width][this.height];
        this.boxCount = board.boxCount;
        this.boxInPlaceCount = board.boxInPlaceCount;
        this.playerX = board.playerX;
        this.playerY = board.playerY;
        tenetTargets = new HashSet<>();
        tenetBoxes = new HashSet<>();

        for (int x = 0; x < this.width; x++) {
            for (int y = 0; y < this.height; y++) {
                if (CTile.isWalkable(board.tiles[x][y])) tiles[x][y] = 1;
                if (CTile.isPlayer(board.tiles[x][y])) tiles[x][y] = 4;
                if (CTile.forBox(1, board.tiles[x][y])) { //This is a target, hence a TenetBox
                    tenetBoxes.add(new Pair(x, y));
                    tiles[x][y] = 2;
                }
                if (CTile.isSomeBox(board.tiles[x][y])) { //This is a box, hence a TenetTarget
                    tenetTargets.add(new Pair(x, y));
                    tiles[x][y] = 3;
                }
            }
        }
    }

    /**
     * Constructor from a different TenetBoard
     * @param board
     */
    public TenetBoard(TenetBoard board) {
        this.width = board.width;
        this.height = board.height;
        this.tiles = new int[this.width][this.height];
        this.boxCount = board.boxCount;
        this.boxInPlaceCount = board.boxInPlaceCount;
        this.playerX = board.playerX;
        this.playerY = board.playerY;
        this.tenetTargets = board.tenetTargets;
        this.tenetBoxes = board.tenetBoxes;


        for (int x = 0; x < this.width; x++) {
            for (int y = 0; y < this.height; y++) {
                this.tiles[x][y] = board.tiles[x][y];
            }
        }
    }

    /**
     * Checks whether the board is currently in a win-state
     *
     * @return boolean for whether each box is in place
     */
    public boolean isVictory() {
        return (boxCount == boxInPlaceCount);
    }

    @Override
    public TenetBoard clone() {
        return new TenetBoard(this);
    }

    public boolean actionPossible(TAction action, TenetBoard tboard) {

    }
}

abstract class TAction {
    public abstract boolean isPossible(TenetBoard board);
}

class TPull extends TAction {
    public int[] up = {0, -1};
    public int[] right = {1, 0};
    public int[] down = {0, 1};
    public int[] left = {-1, 0};
    public int[] direction;

    public TPull(int[] direction) {
        this.direction = direction;
    }

    public boolean isPossible(TenetBoard tboard) {
        if (tboard.tiles[tboard.playerX+direction[0]] == )
    }

}

class TMove extends TAction {
    int x;
    int y;

    public boolean isPossible(TenetBoard tboard) {
        return (tboard.tiles[x][y] > 0);
    }

}
