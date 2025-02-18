import static java.lang.System.out;

import java.sql.Array;
import java.util.*;

import agents.ArtificialAgent;
import game.actions.EDirection;
import game.actions.compact.*;
import game.board.compact.BoardCompact;
import game.board.compact.CTile;
import game.board.oop.Board;

/**
 * The simplest Tree-DFS agent.
 * @author Jimmy
 */
public class MyAgent extends ArtificialAgent {
	protected BoardCompact board;
	protected int searchedNodes;
	protected List<EDirection> result;

	private HashSet<Pair> targets;
	private HashSet<Pair> boxes;
	
	@Override
	protected List<EDirection> think(BoardCompact board) {
		this.board = board;
		searchedNodes = 0;
		findBoxes();

		long searchStartMillis = System.currentTimeMillis();
		
		this.result = new ArrayList<EDirection>();
		search(); // the number marks how deep we will search (the longest plan we will consider)

		long searchTime = System.currentTimeMillis() - searchStartMillis;
        
        if (verbose) {
            out.println("Nodes visited: " + searchedNodes);
            out.printf("Performance: %.1f nodes/sec\n",
                        ((double)searchedNodes / (double)searchTime * 1000));
        }
		
		return result.isEmpty() ? null : result;
	}

	private boolean search() {
		PriorityQueue<Node> queue = new PriorityQueue<>();

		Node init = new Node(board, null, estimate(board), 0.0, null);
		queue.add(init);

		Set<BoardCompact> visited = new HashSet<>();

		while (!queue.isEmpty()) {
			Node currentState = queue.poll();
			visited.add(currentState.boardState);

			if(currentState.boardState.isVictory()) {
				construct(currentState);
				return true;
			}

            List<CAction> allActions = new ArrayList<>(CMove.getActions());
			allActions.addAll(CPush.getActions());

			for(CAction action : allActions){
				if(!action.isPossible(currentState.boardState)) continue;
				BoardCompact newState = currentState.boardState.clone();
				action.perform(newState);



				if(!visited.contains(newState)) {
					queue.add(new Node(newState, action, estimate(newState),
							currentState.distance + 1, currentState));
				}
			}
		}
		return false;
	}

	private void construct(Node currentState) {
		Node current = currentState;
		while (current.parent != null) {
			result.add(0, current.action.getDirection());
			current = current.parent;
		}
	}

	private void findBoxes() {
		for (int x = 0; x < board.width(); x++) {
			for (int y = 0; y < board.height(); y++) {
				if(CTile.forBox(1, board.tiles[x][y])) targets.add(new Pair(x, y));
				if(CTile.isSomeBox(board.tiles[x][y])) boxes.add(new Pair(x, y));
			}
		}
	}

	/**
	 * Currently loop over entire board
	 * @param board loop over entire thing
	 */
	private int estimate(BoardCompact board) {
		List<List<Integer>> boxes = new ArrayList<>();
		List<List<Integer>> targets = new ArrayList<>();

		for (int x = 0; x < board.width(); x++) {
			for (int y = 0; y < board.height(); y++) {
				if(CTile.forBox(1, board.tiles[x][y])) targets.add(Arrays.asList(x, y));
				if(CTile.isSomeBox(board.tiles[x][y])) boxes.add(Arrays.asList(x, y));
			}
		}

		int count = 0;
//		for(int i = 0; i < boxes.size(); i++) {
//			count += distance(boxes.get(i), targets.get(i));
//		}
		for (int i = 0; i < boxes.size(); i++) {
			int tempCount = 0;
			for(int j = 0; j < boxes.size(); j++) {
				tempCount += distance(boxes.get(j), targets.get(j+i % boxes.size()));
			}

			count = Math.min(count, tempCount);
		}

		return count;
	}

	private int distance(List<Integer> box, List<Integer> target) {
		return Math.abs(box.get(0) - target.get(0)) + Math.abs(box.get(1) - target.get(1));
	}
}

class Pair {
	Integer x, y;

	Pair(Integer x, Integer y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		Pair pair = (Pair) o;
		return Objects.equals(x, pair.x) && Objects.equals(y, pair.y);
	}

	@Override
	public int hashCode() {
		return Objects.hash(x, y);
	}
}

class Node implements Comparable<Node> {
	BoardCompact boardState;
	CAction action;
	double estimate;
	double distance;
	Node parent;

	public Node(BoardCompact board, CAction action, double estimate, double distance, Node parent) {
		this.boardState = board.clone();
		this.action = action;
		this.estimate = estimate;
		this.distance = distance;
		this.parent = parent;
	}

	@Override
	public int compareTo(Node o) {
		return Double.compare(this.estimate + this.distance, o.estimate + o.distance);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Node node = (Node) o;
		return Double.compare(estimate, node.estimate) == 0 && Double.compare(distance, node.distance) == 0 && Objects.equals(boardState, node.boardState) && Objects.equals(action, node.action) && Objects.equals(parent, node.parent);
	}

	@Override
	public int hashCode() {
		return Objects.hash(boardState, action, estimate, distance, parent);
	}
}
