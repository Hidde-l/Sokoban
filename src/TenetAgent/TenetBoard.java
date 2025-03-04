package TenetAgent;

import game.board.compact.BoardCompact;
import game.board.compact.CTile;

import java.util.HashSet;
import java.util.Objects;
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
                if (CTile.forBox(1, board.tiles[x][y])) tenetBoxes.add(new Pair(x, y));
            }
        }
    }

    /**
     * Constructor from a different TenetBoard
     * @param tboard
     */
    public TenetBoard(TenetBoard tboard) {
        this.tiles = new int[tboard.tiles.length][tboard.tiles[0].length];
        this.boxInPlaceCount = tboard.boxInPlaceCount;
        this.playerX = tboard.playerX;
        this.playerY = tboard.playerY;
        this.tenetBoxes = new HashSet<Pair>();
        for (Pair p : tboard.tenetBoxes) {
            this.tenetBoxes.add(new Pair(p.x, p.y));
        }

        for (int x = 0; x < this.tiles.length; x++) {
            for (int y = 0; y < this.tiles[0].length; y++) {
                this.tiles[x][y] = tboard.tiles[x][y];
            }
        }
    }

    /**
     * Constructor for a TenetBoard given a board, a set of tenetBoxes, and a player location
     *
     * @param tboard another TenetBoard
     * @param tenetBoxes A set of Pairs of tenetBoxes
     * @param playerX x location of the player
     * @param playerY y location of the player
     */
    public TenetBoard(TenetBoard tboard, HashSet<Pair> tenetBoxes, int playerX, int playerY) {
        this.tiles = new int[tboard.tiles.length][tboard.tiles[0].length];
        this.boxInPlaceCount = tboard.boxInPlaceCount;
        this.playerX = playerX;
        this.playerY = playerY;
        this.tenetBoxes = new HashSet<Pair>();
        for (Pair p : tenetBoxes) {
            this.tenetBoxes.add(new Pair(p.x, p.y));
        }

        for (int x = 0; x < this.tiles.length; x++) {
            for (int y = 0; y < this.tiles[0].length; y++) {
                this.tiles[x][y] = tboard.tiles[x][y];
            }
        }
    }

    /**
     * Checks whether the board is currently in a win-state
     *
     * @return boolean for whether each box is in place
     */
    public boolean isVictory(Set<Pair> boxes, Set<Pair> targets) {
        for (Pair box : boxes) if (!targets.contains(box)) return false;
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
     * Returns a copy of the given board, with an action performed
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TenetBoard that = (TenetBoard) o;
        return playerX == that.playerX && playerY == that.playerY && Objects.equals(tenetBoxes, that.tenetBoxes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerX, playerY, tenetBoxes);
    }
}
