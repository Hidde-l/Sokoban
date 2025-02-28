package agents.TenetAgent;

import game.actions.EDirection;

public class TAction {
    public EDirection direction;
    public boolean pull;

    public TAction(EDirection direction, boolean pull) {
        this.direction = direction;
        this.pull = pull;
    }

    /**
     * Checks whether an action is possible given a TenetBoard -> Checks whether the action would leave the player inside
     * bounds and on a walkable tile.
     *
     * @param tboard the TenetBoard to test the action against
     * @return boolean whether action is possible or not
     */
    public boolean isPossible(TenetBoard tboard) {
        if (tboard.playerX+direction.dX < tboard.tiles.length && tboard.playerX+direction.dX >= 0
            && tboard.playerY+direction.dY < tboard.tiles[0].length && tboard.playerY+direction.dY >= 0 //Destination in bounds
            && tboard.tiles[tboard.playerX+direction.dX][tboard.playerY+direction.dY] == 1 //Is destination not occupied?
            && !playerOnBox(tboard)) { //Player would not stand on a box if you did this
            if (this.pull) { //Is there a box to pull in this direction?
                return tboard.tenetBoxes.contains(new Pair(tboard.playerX-this.direction.dX, tboard.playerY-this.direction.dY));
            }
            return true;
        }
        return false;
    }

    /**
     * Checks whether a player is on a box
     *
     * @param tboard the board we are using
     * @return boolean whether the player x and y are the same as any TenetBox's x and y
     */
    public boolean playerOnBox(TenetBoard tboard) {
        for (Pair p : tboard.tenetBoxes) {
            if (tboard.playerX+direction.dX == p.x && tboard.playerY+direction.dY == p.y) return true;
        }
        return false;
    }

    public EDirection getDirection() {
        return this.direction;
    }

    /**
     * Returns the reverse direction of the current action. This is used to reverse the path at the end of TenetSearch
     *
     * @return the EDirection opposite of this action
     */
    public EDirection getReverseDirection() {
        if (this.direction == EDirection.UP) return EDirection.DOWN;
        if (this.direction == EDirection.DOWN) return EDirection.UP;
        if (this.direction == EDirection.LEFT) return EDirection.RIGHT;
        return EDirection.LEFT;
    }
}
