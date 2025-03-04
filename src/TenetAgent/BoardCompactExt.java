package TenetAgent;

import game.actions.EDirection;

import java.util.HashSet;

public class BoardCompactExt {
    HashSet<Pair> boxes;
    Pair player;
    int hashcode;

    public BoardCompactExt(HashSet<Pair> boxes, Pair player, int hashcode) {
        this.boxes = boxes;
        this.player = player;
        this.hashcode = hashcode;
    }

    /**
     * Calculate the hashcode of a different board given an action that is applied
     *
     * @param hashValues the list of hash values initially generated
     * @param action the action being made to move away from this board
     * @param width the width of the board
     * @return the new hashcode
     */
    public int calcNewHashcode(int[][] hashValues, TAction action, int width) {
        int h = hashcode;
        EDirection dir = action.getDirection();

        if(action.pull) { // if we are pushing, remove old position of box and add new one
            h = h ^ hashValues[(player.x - dir.dX -1) + (player.y - dir.dY -1)*width][0];
            h = h ^ hashValues[(player.x -1) + (player.y -1)*width][0];
        }
        // move the player
        h = h ^ hashValues[(player.x-1) + (player.y-1)*width][1];
        return h ^ hashValues[(player.x + dir.dX -1) + (player.y + dir.dY -1)*width][1];
    }

    /**
     * Creates a clone of the BoardCompactExt, performs an action on it, and returns that new board
     *
     * @param action the action to be performed on a board
     * @return a new BoardCompactExt
     */
    public BoardCompactExt perform(TAction action) {
        HashSet<Pair> clonedSet = new HashSet<>();

        BoardCompactExt cloned = new BoardCompactExt(clonedSet, new Pair(player.x, player.y), hashcode);
        EDirection dir = action.getDirection();

        cloned.player = new Pair(this.player.x + dir.dX, this.player.y + dir.dY);

        if (action.pull) {
            for(Pair p : boxes) cloned.boxes.add(new Pair(p.x, p.y));
            // update the location of the box
            cloned.boxes.remove(new Pair(this.player.x-action.direction.dX, this.player.y-action.direction.dY));
            cloned.boxes.add(new Pair(this.player.x, this.player.y));
        } else {
            cloned.boxes = boxes;
        }

        return cloned;
    }

    /**
     * Checks if a board is in a victory state
     *
     * @param targets The targets where a board needs to be situated
     * @return a boolean value on whether the board is in a victory state or not
     */
    public boolean isVictory(HashSet<Pair> targets) {
        for (Pair box : boxes) if(!targets.contains(box)) return false;
        return true;
    }

    @Override
    public boolean equals(Object o) {
        return hashcode == o.hashCode();
    }

    @Override
    public int hashCode() {
        return hashcode;
    }
}