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
            && tboard.playerY+direction.dY < tboard.tiles[0].length && tboard.playerY+direction.dY >= 0) { //Is destination in bounds?
            return (tboard.tiles[tboard.playerX+direction.dX][tboard.playerY+direction.dY] == 1); //Is destination not occupied?
        }
        return false;
    }

    public EDirection getDirection() {
        return this.direction;
    }
}
