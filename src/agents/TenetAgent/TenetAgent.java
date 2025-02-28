package agents.TenetAgent;

import agents.ArtificialAgent;
import game.actions.EDirection;
import game.board.compact.BoardCompact;
import game.board.compact.CTile;

import java.util.*;

import static java.lang.System.out;

public class TenetAgent extends ArtificialAgent {
    protected BoardCompact board;
    protected TenetBoard tboard;
    protected HashSet<Pair> tenetTargets;
    protected List<TAction> allActions;
    protected int searchedNodes;
    protected Pair tenetEndLocation;
    protected List<EDirection> result;


    @Override
    protected List<EDirection> think(BoardCompact board) {
        // initialize everything
        this.board = board;
        this.tboard = new TenetBoard(board);
        this.tenetTargets = new HashSet<>();
        HashSet<Pair> tenetBoxes = new HashSet<>();
        findTenetTargetsAndBoxes(tenetBoxes);
        generateAllActions();
        searchedNodes = 0;
        this.tenetEndLocation = new Pair(board.playerX, board.playerY);
        this.result = new ArrayList<>();

        // actually search
        long searchStartMillis = System.currentTimeMillis();
        search(tenetBoxes);
        long searchTime = System.currentTimeMillis() - searchStartMillis;

        if (verbose) {
            out.println("Nodes visited: " + searchedNodes);
            out.printf("Performance: %.1f nodes/sec\n",
                    ((double)searchedNodes / (double)searchTime * 1000));
        }

        return result.isEmpty() ? null : result;
    }

    /**
     * A* algorithm with as catch that we now pull boxes instead of pushing them.
     * This requires us to have swapped the boxes and targets (so we pull targets) and Sokoban starts on all free
     * positions next to TenetBoxes
     *
     * @param tenetBoxes a list of the TenetBoxes at the start of the level
     */
    private void search(HashSet<Pair> tenetBoxes) {
        PriorityQueue<Node> queue = new PriorityQueue<>();
        HashSet<TenetBoard> visited = new HashSet<>(150);

        //Add starting positions to the queue
        for (Pair p : tenetBoxes) {
            if (tboard.tiles[p.x + 1][p.y] == 1 && !tenetBoxes.contains(new Pair(p.x+1, p.y)))
                queue.add(new Node(new TenetBoard(tboard, tenetBoxes, p.x+1, p.y), tenetBoxes, null, estimate(tenetBoxes), 0.0, null));
            if (tboard.tiles[p.x - 1][p.y] == 1 && !tenetBoxes.contains(new Pair(p.x-1, p.y)))
                queue.add(new Node(new TenetBoard(tboard, tenetBoxes, p.x-1, p.y), tenetBoxes, null, estimate(tenetBoxes), 0.0, null));
            if (tboard.tiles[p.x][p.y + 1] == 1 && !tenetBoxes.contains(new Pair(p.x, p.y+1)))
                queue.add(new Node(new TenetBoard(tboard, tenetBoxes, p.x, p.y+1), tenetBoxes, null, estimate(tenetBoxes), 0.0, null));
            if (tboard.tiles[p.x][p.y - 1] == 1 && !tenetBoxes.contains(new Pair(p.x, p.y-1)))
                queue.add(new Node(new TenetBoard(tboard, tenetBoxes, p.x, p.y-1), tenetBoxes, null, estimate(tenetBoxes), 0.0, null));
        }

        while (!queue.isEmpty()) {
            Node currentState = queue.poll();
            visited.add(currentState.boardState);

            // if we found a solution, construct the list of actions taken and return
            if(currentState.boardState.isVictory(currentState.tenetBoxes, tenetTargets)
                && currentState.boardState.playerX == tenetEndLocation.x && currentState.boardState.playerY == tenetEndLocation.y) {
                construct(currentState, currentState.boardState);
                return;
            }

            // loop over all the possible actions
            for(TAction action : allActions){
                // if the action is possible then apply it to a cloned board
                if(!action.isPossible(currentState.boardState)) continue;
                TenetBoard newState = currentState.boardState.perform(action);

                // if we haven't been at this board state before, explore it further
                if(!visited.contains(newState)) {
                    queue.add(new Node(newState, newState.tenetBoxes, action, estimate(newState.tenetBoxes),currentState.distance + 1, currentState));
                }
            }
        }
    }

    /**
     * Simplest heuristic possible: Number of boxes on a target
     *
     * @param boxes List of box locations
     * @return number of boxes - number of boxes on a target
     */
    private double estimate(HashSet<Pair> boxes) {
        double r = board.boxCount;
        for (Pair box : boxes) if (tenetTargets.contains(box)) r -= 1;
        return r;
    }

    /**
     * Construct the results list.
     *
     * @param currentState the final state the A* search found
     */
    private void construct(Node currentState, TenetBoard tboard) {
        Node current = currentState;
        while (current.parent != null) {
            this.result.add(current.action.getReverseDirection());
            current = current.parent;
        }
    }

    /**
     * Finds all the tenetTargets and tenetBoxes given the global board
     *
     * @param tenetBoxes a list of pairs that we fill with tenetBox locations
     */
    private void findTenetTargetsAndBoxes(Set<Pair> tenetBoxes) {
        for (int x = 0; x < board.width(); x++) {
            for (int y = 0; y < board.height(); y++) {
                if(CTile.forBox(1, board.tiles[x][y])) tenetBoxes.add(new Pair(x, y));
                if(CTile.isSomeBox(board.tiles[x][y])) this.tenetTargets.add(new Pair(x, y));
            }
        }
    }

    /**
     * Fills the allActions list with all possible actions, for faster access later on
     */
    private void generateAllActions() {
        this.allActions = new ArrayList<TAction>();
        this.allActions.add(new TAction(EDirection.UP, false));
        this.allActions.add(new TAction(EDirection.RIGHT, false));
        this.allActions.add(new TAction(EDirection.DOWN, false));
        this.allActions.add(new TAction(EDirection.LEFT, false));
        this.allActions.add(new TAction(EDirection.UP, true));
        this.allActions.add(new TAction(EDirection.RIGHT, true));
        this.allActions.add(new TAction(EDirection.DOWN, true));
        this.allActions.add(new TAction(EDirection.LEFT, true));
    }
}
