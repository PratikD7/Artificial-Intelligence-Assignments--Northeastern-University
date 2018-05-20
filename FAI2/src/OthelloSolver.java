// FIRST SUCCESSFUL IMPLEMENTATION //

import java.util.Random;
import java.util.ArrayList;
import java.util.Scanner;

public class OthelloSolver {

	static final int NUM_COLUMNS = 8;
	// We want to keep these enum values so that flipping ownership is just a sign change
	static final int WHITE = 1;
	static final int NOBODY = 0;
	static final int BLACK = -1;
	static final int TIE = 2;

	Random rng = new Random();

	static final float WIN_VAL = 100;
	// int MINIMAX_SEARCH_DEPTH = 11;

	static final boolean WHITE_TO_PLAY = true;
	static final int DEMO_SEARCH_DEPTH = 5;

	// Rather than having you implement a full Othello player, which is hard to fit into the
	// HackerRank paradigm, we're going to just evaluate board positions according to the evaluation
	// function.  This has the nice property of being more specific than the choice of move,
	// helping us make sure the algorithm has been implemented properly.  It should also be
	// robust against changes to the order that moves are explored.
	//
	// The input will be the search depth (in ply, or single moves) and a board position,
	// and the output will be the value of the game in piece difference (+WIN_VAL for a white win,
	// -WIN_VAL for a black win, piece difference otherwise).
	//
	// Some code has been provided for you as well if you want to plug your search into an Othello
	// player; use the command-line argument "play."
	//
	// Take note of the provided functions - the gory details of capturing pieces and so forth
	// have been implemented for you.
	public static void main(String[] args) {
//		if (args.length > 0 && args[0].equals("play")) {
//			play();
//			System.exit(0);
//		}
		Scanner myScanner = new Scanner(System.in);
		int searchDepth = readDepth(myScanner);
		int[][] board = readBoard(myScanner);
		System.out.println(minimax_value(board, WHITE_TO_PLAY, searchDepth, Float.NEGATIVE_INFINITY,
				Float.POSITIVE_INFINITY));
	}

	static int readDepth(Scanner s) {
		try {
			return Integer.parseInt(s.nextLine());
		} catch (Exception e) {
			System.err.println("Recall that first line of the input must be the search depth.");
			System.exit(0);
		}
		// satisfy compiler
		return 0;
	}

	static int[][] readBoard(Scanner s) {
		int [][] board = new int[NUM_COLUMNS][NUM_COLUMNS];
		for (int r = 0; r < NUM_COLUMNS; r++) {
			String line = s.nextLine();
			for (int c = 0; c < NUM_COLUMNS; c++) {
				if (line.charAt(c) == 'W') {
					board[r][c] = WHITE;
				} else if (line.charAt(c) == 'B') {
					board [r][c] = BLACK;
				} else if (line.charAt(c) == '-') {
					board [r][c] = NOBODY;
				} else {
					System.err.println("Badly formatted board; unrecognized token, " + line.charAt(c));
					System.exit(0);
				}
			}
		}
		return board;
	}

	// findWinner assumes the game is over
	static int findWinner(int[][] board) {
		int whiteCount = 0;
		int blackCount = 0;
		for (int row = 0; row < NUM_COLUMNS; row++) {
			for (int col = 0; col < NUM_COLUMNS; col++) {
				if (board[row][col] == WHITE) whiteCount++;
				if (board[row][col] == BLACK) blackCount++;
			}
		}
		if (whiteCount > blackCount) {
			return WHITE;
		} else if (whiteCount < blackCount) {
			return BLACK;
		} else {
			return TIE;
		}
	}

	static class Move {
		int row;
		int col;

		Move(int r, int c) {
			row = r;
			col = c;
		}

		public boolean equals(Object o) {
			if (o == this) {
				return true;
			}

			if (!(o instanceof Move)) {
				return false;
			}
			Move m = (Move) o;
			return (m.row == row && m.col == col);
		}
	}

	static ArrayList<Move> generateLegalMoves(int[][] board, boolean whiteTurn) {
		ArrayList<Move> legalMoves = new ArrayList<Move>();
		for (int row = 0; row < NUM_COLUMNS; row++) {
			for (int col = 0; col < NUM_COLUMNS; col++) {
				if (board[row][col] != NOBODY) {
					continue;  // can't play in occupied space
				}
				// Starting from the upper left ...short-circuit eval makes this not terrible
				if (capturesInDir(board,row,-1,col,-1, whiteTurn) ||
						capturesInDir(board,row,-1,col,0,whiteTurn) ||    // up
						capturesInDir(board,row,-1,col,+1,whiteTurn) ||   // up-right
						capturesInDir(board,row,0,col,+1,whiteTurn) ||    // right
						capturesInDir(board,row,+1,col,+1,whiteTurn) ||   // down-right
						capturesInDir(board,row,+1,col,0,whiteTurn) ||    // down
						capturesInDir(board,row,+1,col,-1,whiteTurn) ||   // down-left
						capturesInDir(board,row,0,col,-1,whiteTurn)) {    // left
					legalMoves.add(new Move(row,col));
				}
			}
		}
		return legalMoves;
	}

	// row_delta and col_delta are the direction of movement of the scan for capture
	static boolean capturesInDir(int[][] board, int row, int row_delta, int col, int col_delta, boolean whiteTurn) {
		// Nothing to capture if we're headed off the board
		if ((row+row_delta < 0) || (row + row_delta >= NUM_COLUMNS)) {
			return false;
		}
		if ((col+col_delta < 0) || (col + col_delta >= NUM_COLUMNS)) {
			return false;
		}
		// Nothing to capture if the neighbor in the right direction isn't of the opposite color
		int enemyColor = (whiteTurn ? BLACK : WHITE);
		if (board[row+row_delta][col+col_delta] != enemyColor) {
			return false;
		}
		// Scan for a friendly piece that could capture -- hitting end of the board
		// or an empty space results in no capture
		int friendlyColor = (whiteTurn ? WHITE : BLACK);
		int scanRow = row + 2*row_delta;
		int scanCol = col + 2*col_delta;
		while ((scanRow >= 0) && (scanRow < NUM_COLUMNS) &&
				(scanCol >= 0) && (scanCol < NUM_COLUMNS) && (board[scanRow][scanCol] != NOBODY)) {
			if (board[scanRow][scanCol] == friendlyColor) {
				return true;
			}
			scanRow += row_delta;
			scanCol += col_delta;
		}
		return false;
	}

	// won't return the board to make clear it's not a copy
	static void capture(int[][] board, int row, int col, boolean whiteTurn) {
		for (int row_delta = -1; row_delta <= 1; row_delta++) {
			for (int col_delta = -1; col_delta <= 1; col_delta++) {
				if ((row_delta == 0) && (col_delta == 0)) {
					// the only combination that isn't a real direction
					continue;
				}
				if (capturesInDir(board, row, row_delta, col, col_delta, whiteTurn)) {
					// All our logic for this being valid just happened -- start flipping
					int flipRow = row + row_delta;
					int flipCol = col + col_delta;
					int enemyColor = (whiteTurn ? BLACK : WHITE);
					// No need to check for board bounds - capturesInDir tells us there's a friendly piece
					while(board[flipRow][flipCol] == enemyColor) {
						// Take advantage of enum values and flip the owner
						board[flipRow][flipCol] = -board[flipRow][flipCol];
						flipRow += row_delta;
						flipCol += col_delta;
					}
				}
			}
		}
	}

	//---------------

	static int[][] play(int[][] board, Move move, boolean whiteTurn) {
		int[][] newBoard = copyBoard(board);
		newBoard[move.row][move.col] = (whiteTurn ? WHITE : BLACK);
		capture(newBoard, move.row, move.col, whiteTurn);
		return newBoard;
	}

	static int[][] copyBoard(int[][] board) {
		int[][] newBoard = new int[NUM_COLUMNS][NUM_COLUMNS];
		for (int i = 0; i < NUM_COLUMNS; i++) {
			for (int j = 0; j < NUM_COLUMNS; j++) {
				newBoard[i][j] = board[i][j];
			}
		}
		return newBoard;
	}

	static float evaluationFunction(int[][] board, boolean end) {
		// TODO Implement this
		int whiteCount = 0;
		int blackCount = 0;
		for (int row = 0; row < NUM_COLUMNS; row++) {
			for (int col = 0; col < NUM_COLUMNS; col++) {
				if (board[row][col] == WHITE) whiteCount++;
				if (board[row][col] == BLACK) blackCount++;
			}
		}
		if (end) {
			int difference = whiteCount - blackCount;
			if (difference > 0)
				return (float) 100.0;
			else if (difference < 0)
				return (float) -100.0;
			else return (float) 0.0;
		}
		else{
			return (float) (whiteCount - blackCount);
		}
	}

	static int checkGameOver(int[][] board) {
		ArrayList<Move> whiteLegalMoves = generateLegalMoves(board, true);
		if (!whiteLegalMoves.isEmpty()) {
			return NOBODY;
		}
		ArrayList<Move> blackLegalMoves = generateLegalMoves(board, false);
		if (!blackLegalMoves.isEmpty()) {
			return NOBODY;
		}
		// No legal moves, so the game is over
		return findWinner(board);
	}
	//-------

	static float minimax_value(int board[][], boolean whiteTurn, int searchDepth, float alpha, float beta) {
//		System.out.println(searchDepth);
		ArrayList<Move> whiteLegalMoves = generateLegalMoves(board, true);
		ArrayList<Move> blackLegalMoves = generateLegalMoves(board, false);
		float maxEval, minEval;
		ArrayList<int[][]> children;
		boolean end = false;

		if ((whiteLegalMoves.isEmpty() && blackLegalMoves.isEmpty())){
			end = true;
			return evaluationFunction(board, end);
		}
		else if (searchDepth == 0){
			return evaluationFunction(board, end);
		}

		if (whiteTurn) {
			maxEval = Float.NEGATIVE_INFINITY;
			float eval;
			children = findAllChildren(board, whiteTurn);

			if (children.isEmpty()) {
				eval = minimax_value(board, false, searchDepth, alpha, beta);
				maxEval = Math.max(eval, maxEval);
				return maxEval;
			}
			else {
				for (int[][] childBoard : children) {
					eval = minimax_value(childBoard, false, searchDepth - 1, alpha, beta);
					maxEval = Math.max(eval, maxEval);
				}
				return maxEval;
			}
		}
		else{
			minEval = Float.POSITIVE_INFINITY;
			float eval;
			children = findAllChildren(board, whiteTurn);

			if (children.isEmpty()){
				eval = minimax_value(board, true, searchDepth, alpha, beta);
				minEval = Math.min(eval, minEval);
				return minEval;
			}
			else {

				for (int[][] childBoard : children) {
					eval = minimax_value(childBoard, true, searchDepth - 1, alpha, beta);
					minEval = Math.min(eval, minEval);
				}
				return minEval;
			}
		}
	}

	private static ArrayList<int[][]> findAllChildren(int[][] board, boolean whiteTurn) {
		ArrayList<int[][]> children = new ArrayList<>();
		ArrayList<Move> legalMoves;
		legalMoves = generateLegalMoves(board, whiteTurn);
		for (Move lm: legalMoves){
			int[][] childBoard = copyBoard(board);
			capture(childBoard, lm.row, lm.col, whiteTurn);
			if (whiteTurn)
				childBoard[lm.row][lm.col] = WHITE;
			else childBoard[lm.row][lm.col] = BLACK;
			children.add(childBoard);
		}

		return children;
	}

	// Handy for debugging!  And used by the interactive player below.
	static void printBoard(int[][] board) {
		for (int r = 0; r < NUM_COLUMNS; r++) {
			for (int c = 0; c < NUM_COLUMNS; c++) {
				if (board[r][c] == WHITE) {
					System.out.print("W");
				} else if (board[r][c] == BLACK) {
					System.out.print("B");
				} else if (board[r][c] == NOBODY) {
					System.out.print("-");
				}
			}
			System.out.println();
		}
	}

	// The rest of the code here is for the interactive Othello player.  You don't need to
	// touch it or use it.
	// ------------------------------------------------------------------------------------
	static void play() {
		int[][] board = new int[NUM_COLUMNS][NUM_COLUMNS];
		board[3][3] = WHITE;
		board[3][4] = BLACK;
		board[4][3] = BLACK;
		board[4][4] = WHITE;
		Scanner myScanner = new Scanner(System.in);
		while(checkGameOver(board) == NOBODY) {
			ArrayList<Move> legalMoves = generateLegalMoves(board, true);
			if (legalMoves.size() > 0) {
				System.out.println("Thinking...");
				float bestVal = Float.NEGATIVE_INFINITY;
				Move bestMove = null;
				for (Move m : legalMoves) {
					int[][] newBoard = play(board, m, true);
					float moveVal = minimax_value(newBoard, true, DEMO_SEARCH_DEPTH,
							Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
					if (moveVal > bestVal) {
						bestMove = m;
						bestVal = moveVal;
					}
				}
				board = play(board, bestMove, true);
				printBoard(board);
				System.out.println();
			} else {
				System.out.println("White has no legal moves; skipping turn...");
			}
			legalMoves = generateLegalMoves(board, false);
			if (legalMoves.size() > 0) {
				Move playerMove = getPlayerMove(board, legalMoves, myScanner);
				board = play(board, playerMove, false);
				printBoard(board);
			} else {
				System.out.println("Black has no legal moves; skipping turn...");
			}

		}
		int winner = findWinner(board);
		if (winner == WHITE) {
			System.out.println("White won!");
		} else if (winner == BLACK) {
			System.out.println("Black won!");
		} else {
			System.out.println("Tie!");
		}
	}

	static Move getPlayerMove(int[][] board, ArrayList<Move> legalMoves, Scanner s) {
		for (int r = 0; r < NUM_COLUMNS; r++) {
			for (int c = 0; c < NUM_COLUMNS; c++) {
				if (board[r][c] == WHITE) {
					System.out.print("W");
				} else if (board[r][c] == BLACK) {
					System.out.print("B");
				} else {
					boolean isValidMove = false;
					for (int i = 0; i < legalMoves.size(); i++) {
						Move m = legalMoves.get(i);
						if (m.row == r && m.col == c) {
							System.out.print(i);
							isValidMove = true;
						}
					}
					if (!isValidMove) {
						System.out.print("-");
					}
				}
			}
			System.out.print("\n");
		}
		System.out.println("Which move do you want to play? [0-" + (legalMoves.size()-1) + "]");
		boolean validChoice = false;
		int moveNum = 0;
		do {
			try {
				moveNum = Integer.parseInt(s.nextLine());
				if (moveNum >= 0 && moveNum < legalMoves.size()) {
					validChoice = true;
				} else {
					System.out.println("Not a real move.  Try again?");
				}
			} catch (Exception e) {
				System.out.println("Didn't quite catch that - what number?");
			}
		} while (!validChoice);
		return legalMoves.get(moveNum);
	}
}
