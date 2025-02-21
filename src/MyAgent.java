import static java.lang.System.out;

import java.util.*;

import agents.ArtificialAgent;
import game.actions.EDirection;
import game.actions.compact.*;
import game.board.compact.BoardCompact;
import game.board.compact.CTile;


public class MyAgent extends ArtificialAgent {
	protected BoardCompact board;
	protected int searchedNodes;
	protected List<EDirection> result;
	
	private HashSet<Pair> targets;
	private boolean[][] deadSquares;
	private int[][] hashValues;
	
	@Override
	protected List<EDirection> think(BoardCompact board) {
		// initialize everything
		this.board = board;
		searchedNodes = 0;
		this.result = new ArrayList<>();
		this.targets = new HashSet<>();
		HashSet<Pair> boxes = new HashSet<>();
		findBoxes(boxes);
		this.deadSquares = DeadSquareDetector.detect(board);

		// Construct the hash values for different states
		// first column: the hash values of having a box at a certain square (row ind)
		// second column: the hash value of having a person at a certain square (row ind)
		// take a look at https://en.wikipedia.org/wiki/Zobrist_hashing
		this.hashValues = new int[(board.width() - 1) * (board.height() - 1)][2];
		for(int i = 0; i < hashValues.length; i++) {
			hashValues[i][0] = new Random().nextInt();
			hashValues[i][1] = new Random().nextInt();
		}

		// actually search
		long searchStartMillis = System.currentTimeMillis();
		search(boxes);
		long searchTime = System.currentTimeMillis() - searchStartMillis;
        
        if (verbose) {
            out.println("Nodes visited: " + searchedNodes);
            out.printf("Performance: %.1f nodes/sec\n",
                        ((double)searchedNodes / (double)searchTime * 1000));
        }

		return result.isEmpty() ? null : result;
	}

	/**
	 * Standard A* algorithm.
	 *
	 * @param boxes the initial placement of boxes on the board
	 */
	private void search(HashSet<Pair> boxes) {
		PriorityQueue<Node> queue = new PriorityQueue<>();
		HashSet<Node> visited = new HashSet<>();

		Node init = new Node(board, null, estimate(boxes), 0.0, null, boxes, hashValues);
		queue.add(init);

		while (!queue.isEmpty()) {
			Node currentState = queue.poll();
			if(visited.contains(currentState)) continue;

			visited.add(currentState);

			// if we found a solution, construct the list of actions taken and return
			if(currentState.boardState.isVictory()) {
				construct(currentState);
				return;
			}

            List<CAction> allActions = new ArrayList<>(CMove.getActions());
			allActions.addAll(CPush.getActions());

			// loop over all the possible actions
			for(CAction action : allActions) {
				// if the action is possible then apply it to a cloned board
				if (!action.isPossible(currentState.boardState)) continue;
				BoardCompact newState = currentState.boardState.clone();
				action.perform(newState);
				int newHash = currentState.calcNewHashcode(hashValues, action); // the hash value of the new state

				// if we haven't been at this board state before, explore it further
				if (!visited.contains(new Node(newHash))) {
					HashSet<Pair> newBoxes = cloneSet(currentState.boxes);
					if (action.getClass() == CPush.class) {
						EDirection dir = action.getDirection();

						// if the place you push to is a dead square, don't search this further
						if (deadSquares[newState.playerX + dir.dX][newState.playerY + dir.dY]) continue;

						// update the location of the box
						newBoxes.remove(new Pair(newState.playerX, newState.playerY));
						newBoxes.add(new Pair(newState.playerX + dir.dX, newState.playerY + dir.dY));
					}

					queue.add(new Node(newState, action, estimate(newBoxes), currentState.distance + 1, currentState, newBoxes, newHash));

				}
			}
		}
	}

	/**
	 * Clone the set passed in.
	 *
	 * @param set the set to be cloned
	 * @return a new set with all the same, cloned elements
	 */
	private HashSet<Pair> cloneSet(HashSet<Pair> set) {
		HashSet<Pair> clone = new HashSet<>();
		for(Pair p : set) clone.add(new Pair(p.x, p.y));
		return clone;
	}

	/**
	 * Construct the final solution.
	 *
	 * @param currentState the final state the A* search found
	 */
	private void construct(Node currentState) {
		Node current = currentState;
		while (current.parent != null) {
			result.add(0, current.action.getDirection());
			current = current.parent;
		}
	}

	/**
	 * Find all the boxes and targets of these boxes on a board.
	 *
	 * @param boxes the set to which found boxes should be added
	 */
	private void findBoxes(Set<Pair> boxes) {
		for (int x = 0; x < board.width(); x++) {
			for (int y = 0; y < board.height(); y++) {
				if(CTile.forBox(1, board.tiles[x][y])) this.targets.add(new Pair(x, y));
				if(CTile.isSomeBox(board.tiles[x][y])) boxes.add(new Pair(x, y));
			}
		}
	}

	/**
	 * Heuristic function to estimate cost.
	 * Calculates hamming distance of box to nearest target on the board.
	 *
	 * @param boxes the hashset of box locations
	 */
	private int estimate(HashSet<Pair> boxes) {
		int count = 0;
		for(Pair box : boxes) {
			int min = Integer.MAX_VALUE;
			for(Pair tar : targets) {
				min = Math.min(min, box.distanceTo(tar));
			}
			count += min;
		}
		return count;
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
		// construct board and set all tiles to be dead
		boolean[][] tiles = new boolean[board.width()][board.height()];
		for (int i = 0; i < board.width(); i++) {
			Arrays.fill(tiles[i], true);
		}

		// find all boxes and targets on the board
		HashSet<Pair> targetsAndBoxes = new HashSet<>();
		for (int x = 0; x < board.width(); x++) {
			for (int y = 0; y < board.height(); y++) {
				if(CTile.forBox(1, board.tiles[x][y])) targetsAndBoxes.add(new Pair(x, y));
				if(CTile.isSomeBox(board.tiles[x][y])) targetsAndBoxes.add(new Pair(x, y));
			}
		}

		// mark all boxes and targets as alive squares
		for(Pair box : targetsAndBoxes) tiles[box.x][box.y] = false;

		// for each box/target, expand outward and find the live neighboring squares
		for (Pair coord : targetsAndBoxes) {
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
					|| (!tiles[coord.x][coord.y+1] && CTile.isWalkable(board.tile(coord.x, coord.y-1)))
					|| (!tiles[coord.x][coord.y+1] && !tiles[coord.x][coord.y-1]))
				return true;
		}
		//check left and right
		if (coord.x != 0 && coord.x != board.width()-1)
			if (!tiles[coord.x-1][coord.y] && CTile.isWalkable(board.tile(coord.x+1, coord.y))
					|| !tiles[coord.x+1][coord.y] && CTile.isWalkable(board.tile(coord.x-1, coord.y))
					|| (!tiles[coord.x+1][coord.y] && !tiles[coord.x-1][coord.y]))
				return true;
		return false;
	}
}

class Node implements Comparable<Node> {
	BoardCompact boardState;
	HashSet<Pair> boxes;
	int hashcode;

	CAction action;
	double estimate;
	double distance;
	Node parent;

	public Node(BoardCompact board, CAction action, double estimate, double distance, Node parent, HashSet<Pair> boxes, int[][] hashValues) {
		this.boardState = board.clone();
		this.action = action;
		this.estimate = estimate;
		this.distance = distance;
		this.parent = parent;
		this.boxes = boxes;

		// calculate the hashcode by XORing the player position and all positions of the boxes
		int h = hashValues[(boardState.playerX-1) + (boardState.playerY-1)*boardState.width()][1];
		for (Pair p : boxes) h = h | hashValues[(p.x-1) + (p.y-1)* boardState.width()][0];
		this.hashcode = h;
	}

	public Node(BoardCompact board, CAction action, double estimate, double distance, Node parent, HashSet<Pair> boxes, int hashcode) {
		this.boardState = board.clone();
		this.action = action;
		this.estimate = estimate;
		this.distance = distance;
		this.parent = parent;
		this.boxes = boxes;
		this.hashcode = hashcode;
	}

	// just used for comparison
	public Node(int hashcode){
		this.hashcode = hashcode;
	}

	/**
	 * Calculate the hashcode of a different board given an action that is applied
	 * @param hashValues the list of hash values initially generated
	 * @param action the action being made to move away from this board
	 * @return the new hashcode
	 */
	public int calcNewHashcode(int[][] hashValues, CAction action) {
		int h = hashcode;
		EDirection dir = action.getDirection();

		if(action.getClass() == CPush.class) { // if we are pushing, remove old position of box and add new one
			h = h | hashValues[(boardState.playerX + dir.dX - 1) + (boardState.playerY + dir.dY -1)*boardState.width()][0];
			h = h | hashValues[(boardState.playerX + 2*dir.dX -1) + (boardState.playerY + 2*dir.dY -1)*boardState.width()][0];
		}
		// move the player
		h = h | hashValues[(boardState.playerX-1) + (boardState.playerY-1)*boardState.width()][1];
		return h | hashValues[(boardState.playerX + dir.dX - 1) + (boardState.playerY + dir.dY - 1)*boardState.width()][1];
	}

	@Override
	public int compareTo(Node o) {
		return Double.compare(this.estimate + this.distance, o.estimate + o.distance);
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		Node node = (Node) o;
		return hashcode == node.hashcode && Double.compare(estimate, node.estimate) == 0 && Double.compare(distance, node.distance) == 0 && Objects.equals(boardState, node.boardState) && Objects.equals(boxes, node.boxes) && Objects.equals(action, node.action) && Objects.equals(parent, node.parent);
	}

	@Override
	public int hashCode() {
		return hashcode;
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
