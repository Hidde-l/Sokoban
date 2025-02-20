import static java.lang.System.out;

import java.sql.Array;
import java.util.*;

import agents.ArtificialAgent;
import game.actions.EDirection;
import game.actions.compact.*;
import game.board.compact.BoardCompact;
import game.board.compact.CTile;
import game.board.oop.Board;


public class MyAgent extends ArtificialAgent {
	protected BoardCompact board;
	protected int searchedNodes;
	protected List<EDirection> result;

	private HashSet<Pair> targets;

	
	@Override
	protected List<EDirection> think(BoardCompact board) {
		this.board = board;
		searchedNodes = 0;
		this.result = new ArrayList<>();
		this.targets = new HashSet<>();

		long searchStartMillis = System.currentTimeMillis();

		search();

		long searchTime = System.currentTimeMillis() - searchStartMillis;
        
        if (verbose) {
            out.println("Nodes visited: " + searchedNodes);
            out.printf("Performance: %.1f nodes/sec\n",
                        ((double)searchedNodes / (double)searchTime * 1000));
        }
		
		return result.isEmpty() ? null : result;
	}

	/**
	 * Classical A* algorithm
	 */
	private void search() {
		PriorityQueue<Node> queue = new PriorityQueue<>();

		HashSet<Pair> boxes = new HashSet<>();
		findBoxes(boxes);

		Node init = new Node(board, null, estimate(boxes), 0.0, null, boxes);
		queue.add(init);

		Set<BoardCompact> visited = new HashSet<>();

		while (!queue.isEmpty()) {
			Node currentState = queue.poll();
			visited.add(currentState.boardState);

			if(currentState.boardState.isVictory()) {
				construct(currentState);
				return;
			}

            List<CAction> allActions = new ArrayList<>(CMove.getActions());
			allActions.addAll(CPush.getActions());

			for(CAction action : allActions){
				if(!action.isPossible(currentState.boardState)) continue;
				BoardCompact newState = currentState.boardState.clone();
				action.perform(newState);

				if(!visited.contains(newState)) {
					HashSet<Pair> newBoxes = cloneSet(currentState.boxes);
					if(action.getClass() == CPush.class) {
						EDirection dir = action.getDirection();
						newBoxes.remove(new Pair(currentState.boardState.playerX + dir.dX, currentState.boardState.playerY + dir.dY));
						newBoxes.add(new Pair(newState.playerX + dir.dX, newState.playerY + dir.dY));
					}
					queue.add(new Node(newState, action, estimate(newBoxes),currentState.distance + 1, currentState, newBoxes));
				}
			}
		}
	}

	private HashSet<Pair> cloneSet(HashSet<Pair> set) {
		HashSet<Pair> clone = new HashSet<>();
		for(Pair p : set) clone.add(new Pair(p.x, p.y));
		return clone;
	}

	private void construct(Node currentState) {
		Node current = currentState;
		while (current.parent != null) {
			result.add(0, current.action.getDirection());
			current = current.parent;
		}
	}

	private void findBoxes(Set<Pair> boxes) {
		for (int x = 0; x < board.width(); x++) {
			for (int y = 0; y < board.height(); y++) {
				if(CTile.forBox(1, board.tiles[x][y])) this.targets.add(new Pair(x, y));
				if(CTile.isSomeBox(board.tiles[x][y])) boxes.add(new Pair(x, y));
			}
		}
	}

	/**
	 * Currently loop over entire board
	 * @param board loop over entire thing
	 */
	private int estimate(HashSet<Pair> boxes) {
		int min = Integer.MAX_VALUE;

        List<Pair> tar = new ArrayList<>(targets);
		List<Pair> box = new ArrayList<>(boxes);

		for (int start = 0; start < tar.size(); start++) {
			int count = 0;
			for (int ind = 0; ind < box.size(); ind++) {
				count += tar.get(ind).distanceTo(box.get((ind + start) % box.size()));
			}
			min = Math.min(min, count);
		}

		return min;
	}
}

class Pair {
	int x, y;

	Pair(int x, int y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		Pair pair = (Pair) o;
		return x == pair.x && y == pair.y;
	}

	@Override
	public int hashCode() {
		return Objects.hash(x, y);
	}

	public int distanceTo(Pair p) {
		return Math.abs(x - p.x) + Math.abs(y - p.y);
	}
}

class Node implements Comparable<Node> {
	BoardCompact boardState;
	HashSet<Pair> boxes;

	CAction action;
	double estimate;
	double distance;
	Node parent;


	public Node(BoardCompact board, CAction action, double estimate, double distance, Node parent, HashSet<Pair> boxes) {
		this.boardState = board.clone();
		this.action = action;
		this.estimate = estimate;
		this.distance = distance;
		this.parent = parent;
		this.boxes = boxes;
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
