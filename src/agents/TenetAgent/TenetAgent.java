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
    protected List<EDirection> result;


    @Override
    protected List<EDirection> think(BoardCompact board) {
        // initialize everything
        this.tboard = new TenetBoard(board);
        this.tenetTargets = new HashSet<>();
        HashSet<Pair> tenetBoxes = new HashSet<>();
        findTenetTargetsAndBoxes(tenetBoxes);
        generateAllActions();
        searchedNodes = 0;
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

    private void search(HashSet<Pair> tenetBoxes) {
        PriorityQueue<Node> queue = new PriorityQueue<>();
        HashSet<TenetBoard> visited = new HashSet<>(150);

        Node init = new Node(tboard, tenetBoxes, null, estimate(tenetBoxes), 0.0, null);
        queue.add(init);

        while (!queue.isEmpty()) {
            Node currentState = queue.poll();
            visited.add(currentState.boardState);

            // if we found a solution, construct the list of actions taken and return
            if(currentState.boardState.isVictory(tenetBoxes, tenetTargets)) {
                construct(currentState);
                return;
            }

            // loop over all the possible actions
            for(TAction action : allActions){
                // if the action is possible then apply it to a cloned board
                if(!action.isPossible(currentState.boardState)) continue;
                TenetBoard newState = currentState.boardState.clone();
                newState.perform(action);

                // if we haven't been at this board state before, explore it further
                if(!visited.contains(newState)) {
                    queue.add(new Node(newState, newState.tenetBoxes, action, estimate(newState.tenetBoxes),currentState.distance + 1, currentState));
                }
            }
        }
    }

    private double estimate(HashSet<Pair> boxes) {
        return 0.0;
    }

    /**
     * Construct the final solution.
     *
     * @param currentState the final state the A* search found
     */
    private void construct(Node currentState) {
        Node current = currentState;
        while (current.parent != null) {
            this.result.add(0, current.action.getDirection());
            current = current.parent;
        }
    }

    private void findTenetTargetsAndBoxes(Set<Pair> tenetBoxes) {
        for (int x = 0; x < board.width(); x++) {
            for (int y = 0; y < board.height(); y++) {
                if(CTile.forBox(1, board.tiles[x][y])) tenetBoxes.add(new Pair(x, y));
                if(CTile.isSomeBox(board.tiles[x][y])) this.tenetTargets.add(new Pair(x, y));
            }
        }
    }

    private void generateAllActions() {
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
