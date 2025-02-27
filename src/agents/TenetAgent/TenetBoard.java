package agents.TenetAgent;

import game.board.compact.BoardCompact;
import game.board.compact.CTile;

import java.util.HashSet;
import java.util.Set;

public class TenetBoard {
    public int[][] tiles; // Value 0: wall || Value 1: Free Space
    public int boxInPlaceCount;
    public int playerX;
    public int playerY;
    public HashSet<Pair> tenetBoxes; //and these are the targets of the original board

    /**
     * Constructor from a BoardCompact
     *
     * @param board the CompactBoard of this level
     */
    public TenetBoard(BoardCompact board) {
        this.tiles = new int[board.width()][board.height()];
        this.boxInPlaceCount = board.boxInPlaceCount;
        this.playerX = board.playerX;
        this.playerY = board.playerY;
        tenetBoxes = new HashSet<>();

        for (int x = 0; x < board.width(); x++) {
            for (int y = 0; y < board.height(); y++) {
                if (!CTile.isWall(board.tiles[x][y])) tiles[x][y] = 1;
            }
        }
    }

    /**
     * Constructor from a different TenetBoard
     * @param board
     */
    public TenetBoard(TenetBoard board) {
        this.tiles = new int[tiles.length][tiles[0].length];
        this.boxInPlaceCount = board.boxInPlaceCount;
        this.playerX = board.playerX;
        this.playerY = board.playerY;
        this.tenetBoxes = board.tenetBoxes;


        for (int x = 0; x < this.tiles.length; x++) {
            for (int y = 0; y < this.tiles[0].length; y++) {
                this.tiles[x][y] = board.tiles[x][y];
            }
        }
    }

    /**
     * Constructor for a TenetBoard given a board, a set of tenetBoxes, and a player location
     * @param board another TenetBoard
     * @param tenetBoxes A set of Pairs of tenetBoxes
     * @param playerX x location of the player
     * @param playerY y location of the player
     */
    public TenetBoard(TenetBoard board, HashSet<Pair> tenetBoxes, int playerX, int playerY) {
        this.tiles = new int[board.tiles.length][board.tiles[0].length];
        this.boxInPlaceCount = board.boxInPlaceCount;
        this.playerX = playerX;
        this.playerY = playerY;
        this.tenetBoxes = tenetBoxes;

        for (int x = 0; x < this.tiles.length; x++) {
            for (int y = 0; y < this.tiles[0].length; y++) {
                if (!CTile.isWall(board.tiles[x][y])) tiles[x][y] = 1;
            }
        }
    }

    /**
     * Checks whether the board is currently in a win-state
     *
     * @return boolean for whether each box is in place
     */
    public boolean isVictory(Set<Pair> boxes, Set<Pair> targets) {
        for (Pair box : boxes) if(!targets.contains(box)) return false;
        return true;
    }

    /**
     * Clones the entire board (WARNING: This may be very unoptimised)
     *
     * @return a new, identical TenetBoard
     */
    @Override
    public TenetBoard clone() {
        return new TenetBoard(this);
    }

    /**
     * Performs an action on a board. Changes the state of the board!
     *
     * @param action The action you want to peform. NOTE that it is assumed this action is possible!
     */
    public TenetBoard perform(TAction action) {
        //Set up a new TenetBoxes HashSet for the new TenetBoard
        HashSet<Pair> clonedTenetBoxes = new HashSet<>();
        for(Pair p : tenetBoxes) clonedTenetBoxes.add(new Pair(p.x, p.y));
        if (action.pull) {
            clonedTenetBoxes.remove(new Pair(playerX-action.direction.dX, playerY-action.direction.dY));
            clonedTenetBoxes.add(new Pair(playerX, playerY));
        }

        return new TenetBoard(this, clonedTenetBoxes, playerX+action.direction.dX, playerY+action.direction.dY);
    }
}
