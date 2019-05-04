import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class Game {

	static final String YOUR_MOVE = "YOUR_MOVE\n";
	static final String END_GAME = "END_GAME\n";

	static final int WIN = 5120;
	static final int LOSS = -5120;

	Player player1;
	Player player2;
	Player currentPlayer;
	int[][] board;
	int[] heights;
	boolean endGame;
	boolean botEnabled;
	int botLevel;

	static Player dummy = new Player();
	static Player waitingPlayer = dummy;

	/**
	 * Start a new game.
	 * @param initBotEnabled true if playing a bot
	 */
	public void start(boolean initBotEnabled) {
		currentPlayer = player1;
		botEnabled = initBotEnabled;
		board = new int[6][7];
		heights = new int[7];
	}

	/**
	 * Convert the game board into a string representation for printing
	 * @return String representation of the board
	 */
	public String boardToString() {
		String str = "";
		for (int i = 0; i < 6; i++) {
			str += "     | ";
			for (int j = 0; j < 7; j++) {
				String token = board[i][j] == 0 ? " " : board[i][j] == 1 ? "X" : "O";
				str += token + " | ";
			}
			str += "\n";
		}
		str += "     +---+---+---+---+---+---+---+\n";
		str += "       1   2   3   4   5   6   7  \n";

		return str;
	}

	/**
	 * Evaluate the score of a length 4 segment based on number of tokens.
	 */
	public int evalSegment(int[] segment) {
		int XCount = 0;
		int OCount = 0;
		// count the number of each type of token in segment
		for (int token : segment) {
			if (token == 1)
				XCount++;
			else if (token == 2)
				OCount++;
		}

		if (XCount > 0 && OCount > 0)
			// segments with both type of token don't affect score
			return 0; 
		if (XCount == 4)
			return WIN;
		if (OCount == 4)
			return LOSS;

		if (XCount == 0) {
			if (OCount == 3)
				return -500;
			else if (OCount == 2)
				return -100;
			else if (OCount == 1)
				return -10;
			else
				return 0;
		}

		else {
			if (XCount == 3)
				return 500;
			else if (XCount == 2)
				return 100;
			else
				return 10;
		}
	}

	/**
	 * Return a static evaluation score of the current board state
	 */
	public int staticEval() {
		int value = 0;

		// evaluate columns
		for (int i = 0; i < 7; i++) {
			for (int j = 0; j < 3; j++) {
				int evalSegment = evalSegment(
						new int[] { board[j][i], board[j + 1][i], board[j + 2][i], board[j + 3][i] });
				if (evalSegment == WIN || evalSegment == LOSS) {
					return evalSegment;
				} else {
					value += evalSegment;
				}
			}
		}

		// evaluate rows
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 4; j++) {
				int evalSegment = evalSegment(
						new int[] { board[i][j], board[i][j + 1], board[i][j + 2], board[i][j + 3] });
				if (evalSegment == WIN || evalSegment == LOSS) {
					return evalSegment;
				} else {
					value += evalSegment;
				}
			}
		}
		
		// save all diagonals as lists
		HashMap<Integer, ArrayList<Integer>> diagMap = new HashMap<>();
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 7; j++) {
				int sum = i + j;
				if (!diagMap.containsKey(sum)) diagMap.put(sum, new ArrayList<Integer>());
				diagMap.get(sum).add(board[i][j]);
			}
		}
		
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 7; j++) {
				int diff = i - j + 30;
				if (!diagMap.containsKey(diff)) diagMap.put(diff, new ArrayList<Integer>());
				diagMap.get(diff).add(board[i][j]);
			}
		}
		
		// evaluate diagonals
		for (ArrayList<Integer> diag : diagMap.values()) {
			if (diag.size() > 3) {
				for (int i = 0; i < diag.size()-3; i++){
					int evalSegment = evalSegment(new int[] {diag.get(i), diag.get(i+1), diag.get(i+2), diag.get(i+3)});
					if (evalSegment == WIN || evalSegment == LOSS) {
						return evalSegment;
					} else {
						value += evalSegment;
					}
				}
			}
		}
		
		return value;
	}

	/**
	 * Implementation of the miniMax algorithm for min player
	 * @param depth specifies how many turns to look into the future
	 */
	public int[] minMax(int depth) {
		int val = staticEval();
		if (val == WIN || val == LOSS || depth == 0)
			return new int[] { val, -1 };

		// get all valid moves
		ArrayList<Integer> columns = getValidCols();

		if (columns.size() == 0)
			return new int[] { val, -1 };
		
		val = 100000;
		int move = -1;

		// for each valid move, recursively call maxMin
		for (int col : columns) {
			placeToken(col);
			// switch the active player for the next move
			currentPlayer = player1;
			int value = maxMin(depth - 1)[0];
			if (value < val) {
				move = col;
				val = value;
			}
			// undo the move
			removeToken(col);
			currentPlayer = null;
		}

		return new int[] { val, move };
	}

	/**
	 * Implementation of the miniMax algorithm for max player
	 * @param depth specifies how many turns to look into the future
	 */
	public int[] maxMin(int depth) {
		int val = staticEval();
		if (val == WIN || val == LOSS || depth == 0)
			return new int[] { val, -1 };

		ArrayList<Integer> columns = getValidCols();

		if (columns.size() == 0)
			return new int[] { val, -1 };
		
		val = -100000;
		int move = -1;

		for (int col : columns) {
			placeToken(col);
			currentPlayer = null;
			int value = minMax(depth - 1)[0];
			if (value > val) {
				move = col;
				val = value;
			}
			removeToken(col);
			currentPlayer = player1;
		}

		return new int[] { val, move };
	}

	/**
	 * Connecto-bot move logic
	 */
	public void botMove() throws IOException {
		int col = -1;
		int res[] = new int[] {-1, -1};
		if (botLevel == 1) col = getRandomMove(); // choose moves at random
		else if (botLevel == 2) res = minMax(2); // run minMax with depth 2
		else if (botLevel == 3) res = minMax(3); // run minMax with depth 3 
		else if (botLevel == 4) res = minMax(4); // run minMax with depth 4
		else if (botLevel == 5) res = minMax(5); // run minMax with depth 5
		
		if (botLevel > 1) {
			col = res[1];
			if (res[0] == LOSS) {
				player1.outToClient.writeBytes("I'm going to win... You can just give up now. :)\n"); // bot guaranteed to win, brag about it
			}
		}
		
		currentPlayer = null;
		if (col >= 0) {
			placeToken(col);
		}

		String boardString = boardToString();

		// check if won
		if (isWin(col)) {
			boardString = boardToString();

			// send final board state
			player1.outToClient.writeBytes(boardString);

			// send game over messages
			player1.outToClient.writeBytes("I win! HAHAHAHAHA\n");
			player1.outToClient.writeBytes(END_GAME);
			endGame = true;
		} else if (isTie()) {
			// send final board state
			player1.outToClient.writeBytes(boardString);

			// send game over messages
			player1.outToClient.writeBytes("It's a tie!\n");
			player1.outToClient.writeBytes(END_GAME);
			endGame = true;
		}

		currentPlayer = player1;
		player1.outToClient.writeBytes(boardString);
		player1.outToClient.writeBytes(YOUR_MOVE);
	}

	/**
	 * Get a random valid column.
	 * @return
	 */
	public int getRandomMove() {
		ArrayList<Integer> columns = getValidCols();
		Random rand = new Random();
		int col = columns.get(rand.nextInt(columns.size()));
		return col;
	}

	/**
	 * Place a token in a column on the board.
	 * @param col Column to place a token in
	 */
	public void placeToken(int col) {
		int height = heights[col];
		int row = 5 - height;
		board[row][col] = currentPlayer == player1 ? 1 : 2;
		heights[col]++;
	}

	/**
	 * Remove a token from the board. This is used for recursive backtracking in minMax.
	 * @param col
	 */
	public void removeToken(int col) {
		heights[col]--;
		int height = heights[col];
		int row = 5 - height;
		board[row][col] = 0;
	}

	/**
	 * Retrieve all columns which are not full.
	 * @return ArrayList<Integer> ArrayList of valid columns.
	 */
	public ArrayList<Integer> getValidCols() {
		ArrayList<Integer> columns = new ArrayList<Integer>();
		for (int i = 0; i < heights.length; i++) {
			if (heights[i] < 6) {
				columns.add(i);
			}
		}
		return columns;
	}

	/**
	 * Check if the board is full.
	 * @return true if the board is full, false otherwise.
	 */
	public boolean isTie() {
		for (int i = 0; i < 7; i++) {
			if (board[0][i] == 0) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Determine if the last move (indicated by col) resulted in a win.
	 * @param col the column of the last move played
	 * @return
	 */
	public boolean isWin(int col) {
		int height = heights[col];
		int row = 5 - height + 1;
		int token = board[row][col];

		int downRow = row + 1;
		int upRow = row - 1;
		int sumRow = 1;
		while (downRow < 6 && board[downRow][col] == token) {
			downRow++;
			sumRow++;
		}

		while (upRow >= 0 && board[upRow][col] == token) {
			upRow--;
			sumRow++;
		}

		if (sumRow >= 4)
			return true;

		int leftCol = col - 1;
		int rightCol = col + 1;
		int sumCol = 1;
		while (leftCol >= 0 && board[row][leftCol] == token) {
			leftCol--;
			sumCol++;
		}

		while (rightCol < 7 && board[row][rightCol] == token) {
			rightCol++;
			sumCol++;
		}

		if (sumCol >= 4)
			return true;

		leftCol = col - 1;
		downRow = row + 1;
		int sumPosDiagonal = 1;
		while (leftCol >= 0 && downRow < 6 && board[downRow][leftCol] == token) {
			leftCol--;
			downRow++;
			sumPosDiagonal++;
		}

		rightCol = col + 1;
		upRow = row - 1;
		while (rightCol < 7 && upRow >= 0 && board[upRow][rightCol] == token) {
			rightCol++;
			upRow--;
			sumPosDiagonal++;
		}
		if (sumPosDiagonal >= 4)
			return true;

		// Negative Diagonal
		leftCol = col - 1;
		upRow = row - 1;
		int sumNegDiagonal = 1;
		while (leftCol >= 0 && upRow >= 0 && board[upRow][leftCol] == token) {
			leftCol--;
			upRow--;
			sumNegDiagonal++;
		}

		rightCol = col + 1;
		downRow = row + 1;
		while (rightCol < 7 && downRow < 6 && board[downRow][rightCol] == token) {
			rightCol++;
			downRow++;
			sumNegDiagonal++;
		}
		if (sumNegDiagonal >= 4)
			return true;

		return false;
	}
}

