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
	private boolean[][] deadSquares;
	
	@Override
	protected List<EDirection> think(BoardCompact board) {
		this.board = board;
		searchedNodes = 0;
		this.result = new ArrayList<>();
		this.targets = new HashSet<>();
		this.deadSquares = DeadSquareDetector.detect(board);

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

						if(deadSquares[currentState.boardState.playerX + 2*dir.dX][currentState.boardState.playerY + 2*dir.dY]) continue;

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

class DeadSquareDetector {

	/**
	 * Finds all the dead squares on the board
	 *
	 * @param board The board that we play on
	 * @return a boolean[][] where true entries are dead tiles, and false entries are live tiles
	 */
	public static boolean[][] detect(BoardCompact board) {
		boolean[][] tiles = new boolean[board.width()][board.height()];
		for (int i = 0; i < board.width(); i++) {
			Arrays.fill(tiles[i], true);
		}

		// This is to retrieve the target for when we are using the DeadSquareTest. In our final own
		// implementation this part will not be necessary
		List<Pair> targets = new ArrayList<>();
		for (int x = 0; x < board.width(); x++) {
			for (int y = 0; y < board.height(); y++) {
				if(CTile.forBox(1, board.tiles[x][y])) {
					targets.add(new Pair(x,y));
				}
			}
		}

		for (Pair coord : targets) {
		// End of code required for DeadSquareTest

//		for (Pair coord : MyAgent.targets) {
			tiles[coord.x][coord.y] = false; //false is alive, true is dead
			Queue<Pair> q = new LinkedList<>();
			addNeighbours(coord, q, board);

			while (!q.isEmpty()) {
				Pair cur = q.poll();
				if (!tiles[cur.x][cur.y]) continue; //if alive, continue.

				if (CTile.isWall(board.tile(cur.x, cur.y))) continue; //Tile is not walkable, hence dead

				if (canMoveToLivingSpot(cur, board, tiles)) { //If the tile can reach a living spot, set to alive and add neighbours
					tiles[cur.x][cur.y] = false;
					addNeighbours(cur, q, board);
				}
			}
		}
		return tiles;
	}

	/**
	 * Tries to add the neighbours of a tile to the queue. Does so if said neighbours are not out of bound
	 *
	 * @param cur The current Pair whose neighbours we are adding
	 * @param q The queue we are adding to
	 * @param board The board we are playing on
	 */
	public static void addNeighbours(Pair cur, Queue<Pair> q, BoardCompact board) {
		if (cur.x+1 < board.width()) q.add(new Pair(cur.x+1, cur.y));
		if (cur.x-1 >= 0) q.add(new Pair(cur.x-1, cur.y));
		if (cur.y+1 < board.height()) q.add(new Pair(cur.x, cur.y+1));
		if (cur.y-1 >= 0) q.add(new Pair(cur.x, cur.y-1));
	}

	/**
	 * Method checks whether a box on a given square could be moved to a neighbouring live spot
	 *
	 * @param coord	The coordinate where the box would be
	 * @param board The board we use
	 * @param tiles The list of alive and dead tiles
	 * @return boolean whether a box could be moved to a living space
	 */
	public static boolean canMoveToLivingSpot(Pair coord, BoardCompact board, boolean[][] tiles) {
		//check above and below
		if (coord.y != 0 && coord.y != board.height()-1) {
			if (!tiles[coord.x][coord.y-1] && CTile.isWalkable(board.tile(coord.x, coord.y+1))
					|| (!tiles[coord.x][coord.y+1] && CTile.isWalkable(board.tile(coord.x, coord.y-1))))
				return true;
		}
		//check left and right
		if (coord.x != 0 && coord.x != board.width()-1)
            if (!tiles[coord.x-1][coord.y] && CTile.isWalkable(board.tile(coord.x+1, coord.y))
                    || (!tiles[coord.x+1][coord.y] && CTile.isWalkable(board.tile(coord.x-1, coord.y))))
                return true;
		return false;
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
