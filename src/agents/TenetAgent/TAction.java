package agents.TenetAgent;

import game.actions.EDirection;
import game.actions.compact.CAction;
import game.actions.compact.CPush;
import game.board.compact.CTile;

public class TAction {
    public EDirection direction;
    public boolean pull;

    public TAction(EDirection direction, boolean pull) {
        this.direction = direction;
        this.pull = pull;
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
