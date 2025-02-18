import static java.lang.System.out;

import java.util.*;

import agents.ArtificialAgent;
import game.actions.EDirection;
import game.actions.compact.*;
import game.board.compact.BoardCompact;

/**
 * The simplest Tree-DFS agent.
 * @author Jimmy
 */
public class MyAgent extends ArtificialAgent {
	protected BoardCompact board;
	protected int searchedNodes;
	
	@Override
	protected List<EDirection> think(BoardCompact board) {
		this.board = board;
		searchedNodes = 0;
		long searchStartMillis = System.currentTimeMillis();
		
		List<EDirection> result = new ArrayList<EDirection>();
//		dfs(50, result); // the number marks how deep we will search (the longest plan we will consider)
		aStar(result);

		long searchTime = System.currentTimeMillis() - searchStartMillis;
        
        if (verbose) {
            out.println("Nodes visited: " + searchedNodes);
            out.printf("Performance: %.1f nodes/sec\n",
                        ((double)searchedNodes / (double)searchTime * 1000));
        }
		
		return result.isEmpty() ? null : result;
	}

	private boolean aStar(List<EDirection> result) {
		PriorityQueue<GameState> pq = new PriorityQueue<>();
		Set<BoardCompact> visitedStates = new HashSet<>();
		pq.add(new GameState(null, 0, 0, null, board));

		while (!pq.isEmpty()) {
			GameState cur = pq.poll();
			if (cur.board.isVictory()) {
				getResult(cur, result);
				return true;
			}
			visitedStates.add(cur.board);

			//get actions
			List<CAction> actions = new ArrayList<>();
			for (CMove move : CMove.getActions()) if (move.isPossible(cur.board)) actions.add(move);
			for (CPush push : CPush.getActions()) if (push.isPossible(cur.board)) actions.add(push);

			for (CAction action : actions) {
				BoardCompact cloneBoard = cur.board.clone();
				action.perform(cloneBoard);
				if (visitedStates.contains(cloneBoard)) continue;
				pq.add(new GameState(cur, cur.pathCost+1, cur.totalCost+1, action, cloneBoard));
				//Currently the totalCost is just the pathCost
//				out.println("pq size: " + pq.size());
			}
		}
        return false;
    }

	public static int calculateDistance(BoardCompact board) {
		int numberOfBoxesInWrongLocation = board.boxCount - board.boxInPlaceCount;

//		for(for (int i : (int[] i : board.tiles))) {
//
//		}

		return 0;
	}

	private static void getResult(GameState gs, List<EDirection> result) {
		while (gs.predecessor != null) {
			result.add(0, gs.action.getDirection());
			gs = gs.predecessor;
		}
	}

class GameState implements Comparable<GameState> {
	public GameState predecessor;
	public int pathCost;
	public int totalCost;
	public CAction action;
	public BoardCompact board;

	public GameState(GameState predecessor, int pathCost, int totalCost, CAction action, BoardCompact board) {
		this.predecessor = predecessor;
		this.pathCost = pathCost;
		this.totalCost = totalCost;
		this.action = action;
		this.board = board;
	}

	@Override
	public int compareTo(GameState o) {
		return Integer.compare(this.totalCost, o.totalCost);
	}
}

	private boolean dfs(int level, List<EDirection> result) {
		if (level <= 0) return false; // DEPTH-LIMITED
		
		++searchedNodes;
		
		// COLLECT POSSIBLE ACTIONS
		
		List<CAction> actions = new ArrayList<CAction>(4);
		
		for (CMove move : CMove.getActions()) {
			if (move.isPossible(board)) {
				actions.add(move);
			}
		}
		for (CPush push : CPush.getActions()) {
			if (push.isPossible(board)) {
				actions.add(push);
			}
		}
		
		// TRY ACTIONS
		for (CAction action : actions) {
			// PERFORM THE ACTION
			result.add(action.getDirection());
			action.perform(board);
			
			// CHECK VICTORY
			if (board.isVictory()) {
				// SOLUTION FOUND!
				return true;
			}
			
			// CONTINUE THE SEARCH
			if (dfs(level - 1, result)) {
				// SOLUTION FOUND!
				return true;
			}
			
			// REVERSE ACTION
			result.remove(result.size()-1);
			action.reverse(board);
		}
		
		return false;
	}
}
