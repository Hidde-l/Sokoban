package TenetAgent;

import agents.ArtificialAgent;
import game.actions.EDirection;
import game.board.compact.BoardCompact;
import game.board.compact.CTile;

import java.util.*;

import static java.lang.System.out;

public class TenetAgent extends ArtificialAgent {
    protected BoardCompact board;

    protected HashSet<Pair> tenetTargets;
    protected List<TAction> allActions;
    protected int searchedNodes;
    protected Pair tenetEndLocation;
    protected List<EDirection> result;
    private int[][] hashValues;


    @Override
    protected List<EDirection> think(BoardCompact board) {
        long searchStartMillis = System.currentTimeMillis();
        // initialize everything
        this.board = board;
        this.tenetTargets = new HashSet<>();
        HashSet<Pair> tenetBoxes = new HashSet<>();
        findTenetTargetsAndBoxes(tenetBoxes);
        generateAllActions();
        searchedNodes = 0;
        this.tenetEndLocation = new Pair(board.playerX, board.playerY);
        this.result = new ArrayList<>();

        // Construct the hash values for different states
        // first column: the hash values of having a box at a certain square (row ind)
        // second column: the hash value of having a person at a certain square (row ind)
        // take a look at https://en.wikipedia.org/wiki/Zobrist_hashing
        this.hashValues = new int[(board.width() - 1) * (board.height() - 1)][2];
        for(int i = 0; i < hashValues.length; i++) {
            hashValues[i][0] = new Random().nextInt();
            hashValues[i][1] = new Random().nextInt();
        }

        // actually search
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
        HashSet<BoardCompactExt> visited = new HashSet<>();

        //Add starting positions to the queue
        for (Pair p : tenetBoxes) {
            if (!CTile.isWall(board.tiles[p.x + 1][p.y]) && !tenetBoxes.contains(new Pair(p.x+1, p.y)))
                queue.add(new Node(board, estimate(tenetBoxes),tenetBoxes, hashValues, new Pair(p.x+1, p.y)));
            if (!CTile.isWall(board.tiles[p.x - 1][p.y]) && !tenetBoxes.contains(new Pair(p.x-1, p.y)))
                queue.add(new Node(board, estimate(tenetBoxes),tenetBoxes, hashValues, new Pair(p.x-1, p.y)));
            if (!CTile.isWall(board.tiles[p.x][p.y + 1]) && !tenetBoxes.contains(new Pair(p.x, p.y+1)))
                queue.add(new Node(board, estimate(tenetBoxes),tenetBoxes, hashValues, new Pair(p.x, p.y+1)));
            if (!CTile.isWall(board.tiles[p.x][p.y - 1]) && !tenetBoxes.contains(new Pair(p.x, p.y-1)))
                queue.add(new Node(board, estimate(tenetBoxes),tenetBoxes, hashValues, new Pair(p.x, p.y-1)));
        }

        while (!queue.isEmpty()) {
            Node currentState = queue.poll();
            visited.add(currentState.boardState);

            // if we found a solution, construct the list of actions taken and return
            if(currentState.boardState.player.x == tenetEndLocation.x && currentState.boardState.player.y == tenetEndLocation.y
                && currentState.boardState.isVictory(tenetTargets)) {
                construct(currentState);
                return;
            }

            // loop over all the possible actions
            for(TAction action : allActions) {
                // if the action is possible then apply it to a cloned board
                if(!isPossible(action, currentState.boardState)) continue;
                BoardCompactExt newState = currentState.boardState.perform(action);
                newState.hashcode = currentState.boardState.calcNewHashcode(hashValues, action, board.width());

                // if we haven't been at this board state before, explore it further
                if(!visited.contains(newState)) {
                    queue.add(new Node(newState, action, estimate(newState.boxes), currentState.distance + 1, currentState));
                }
            }
        }
    }

    /**
     * Checks whether an action is possible on a given board
     *
     * @param action The action to be performed
     * @param boardState The board to perform an action on
     * @return a boolean value indicating whether an action is possible or not
     */
    private boolean isPossible(TAction action, BoardCompactExt boardState) {
        EDirection dir = action.getDirection();
        Pair target = new Pair(boardState.player.x + dir.dX, boardState.player.y + dir.dY);

        // Check that moving does not put Sokoban outside the map, in a wall, or in a box
        if(target.x < 0 || target.x >= board.width() || target.y < 0 || target.y >= board.height()
                || CTile.isWall(board.tiles[target.x][target.y])
                || playerOnBox(boardState, action)) return false;

        // Checks that for all pull actions there is a box to pull
        if (action.pull) {
            return boardState.boxes.contains(new Pair(boardState.player.x-dir.dX, boardState.player.y-dir.dY));
        }

        return true;
    }

    /**
     * Checks whether a player is on a box
     *
     * @param board the board we are using
     * @return boolean whether the player x and y are the same as any box's x and y
     */
    public boolean playerOnBox(BoardCompactExt board, TAction action) {
        return board.boxes.contains(new Pair(board.player.x+action.direction.dX, board.player.y+action.direction.dY));
    }

    /**
     * A relatively simple heuristic which checks the smallest manhattan distance of each box to a unique board
     *
     * @param boxes List of box locations
     * @return number of boxes - number of boxes on a target
     */
    private int estimate(HashSet<Pair> boxes) {
        int count = 0;
        for(Pair box : boxes) {
            int min = Integer.MAX_VALUE;
            for(Pair tar : tenetTargets) {
                min = Math.min(min, box.distanceTo(tar));
            }
            count += min;
        }
        return count;
    }

    /**
     * Construct the results list.
     *
     * @param currentState the final state the A* search found
     */
    private void construct(Node currentState) {
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
        this.allActions = new ArrayList<>();
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
