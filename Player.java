import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;

/**
* The Player class represents a connection between a server and a client. Each creation
* of this class creates a new Thread, which handles message exchange between server and client.
* 
* @author Quan Chau, Noah Hunt-Isaak
* @version 4/28/19
*/

public class Player extends Thread {
	static final String HUMAN_COMP = "HUMAN_COMP\n";
	static final String BOT_LEVEL = "BOT_LEVEL\n";
	static final int HUMAN = 1;
	static final int COMP = 2;
	static final String WAIT_FOR_OPPONENT = "WAIT_FOR_OPPONENT\n";
	static final String YOUR_NAME = "YOUR_NAME\n";
	static final String DUPLICATE_NAME = "DUPLICATE_NAME\n";
	static final String YOUR_MOVE = "YOUR_MOVE\n";
	static final String INVALID_MOVE = "INVALID_MOVE\n";
	static final String END_GAME = "END_GAME\n";
	static final String DUMMY_NAME = "1309ureh32id09iwd#&41";
	static final String DISCONNECT = "DISCONNECT\n";

	Socket socket; 
	BufferedReader inFromClient;
	DataOutputStream outToClient;
	Player opponent;
	String name;
	boolean firstPlayer;
	Game game;

	Player(Socket initSocket, boolean isFirstPlayer)
			throws IOException {
		socket = initSocket;
		inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		outToClient = new DataOutputStream(socket.getOutputStream());
		firstPlayer = isFirstPlayer;
		name = "";
	}
	
	Player(){
		//dummy constructor
		name = DUMMY_NAME;
	}

	/**
	* When start() is called, the thread runs this method to activate the player
	* It will ask the client to choose the game mode, enter the name and start the game.
	*/
	@Override
	public void run() {
		try {
			requestGameMode();
			requestPlayerName();

			// Send the initial boardstring to both players, and ask the first player to move
			String boardString = game.boardToString();
			outToClient.writeBytes(boardString);
			if (firstPlayer) {
				outToClient.writeBytes(YOUR_MOVE);
			} else {
				outToClient.writeBytes(WAIT_FOR_OPPONENT);
			}

			while (true) {
				// Read the move entered my the player and make the move if it is their turn
				String move = inFromClient.readLine();
				synchronized (game) {
					if (game.currentPlayer == this) {
						move = move.trim();
						try {
							int moveCol = Integer.parseInt(move);
							// Check if the entered column value is valid
							if (moveCol < 1 || moveCol > 7)
								throw new NumberFormatException();
								int height = game.heights[moveCol - 1];
								if (height < 6) {
									game.placeToken(moveCol - 1);
									boardString = game.boardToString();

									if (game.isWin(moveCol - 1)) {
										// check if this is the winning move of the current player
										outToClient.writeBytes(boardString);

										// send the winning message to both players and end the game
										outToClient.writeBytes(name + " wins!\n");
										outToClient.writeBytes(END_GAME);

										if (!game.botEnabled) {
											opponent.outToClient.writeBytes(boardString);
											opponent.outToClient.writeBytes(name + " wins!\n");
											opponent.outToClient.writeBytes(END_GAME);
										}
										game.endGame = true;
										socket.close();
										opponent.socket.close();
										break;

									} else if (game.isTie()) {
										// Check if the game is tie after this move (all of the columns are full but no one wins)
										outToClient.writeBytes(boardString);

										// send the tie message to both players and end the game
										outToClient.writeBytes("It's a tie!\n");
										outToClient.writeBytes(END_GAME);

										if (!game.botEnabled) {
											opponent.outToClient.writeBytes(boardString);
											opponent.outToClient.writeBytes("It's a tie!\n");
											opponent.outToClient.writeBytes(END_GAME);
										}

										game.endGame = true;
										socket.close();
										opponent.socket.close();
										break;
									}

								} else {
									throw new ColumnFullException(moveCol);
								}

							// If the game has not ended, notify the opponent/the bot to move
							if (!game.endGame) {
								game.currentPlayer = opponent;
								boardString = game.boardToString();
								if (game.botEnabled) {
									// Call the method in Game class to make the bot move
									game.botMove();
								} else {
									outToClient.writeBytes(boardString);
									outToClient.writeBytes(WAIT_FOR_OPPONENT);
									opponent.outToClient.writeBytes(boardString);
									opponent.outToClient.writeBytes(YOUR_MOVE);
								}
							}
						} catch (NumberFormatException nfe) {
							// This error indicates the entered move is not a number 1 - 7
							outToClient.writeBytes(INVALID_MOVE);

						} catch (ColumnFullException e) {
							// This error indicates the entered column is already full,
							// asking the player to enter another move
							System.out.println(e);
							outToClient.writeBytes(e.toString());
							outToClient.writeBytes(YOUR_MOVE);
						}
					} else {
						outToClient.writeBytes(WAIT_FOR_OPPONENT);
					}
				}
			}
		} catch (Exception e) {
			System.out.println(e);
			if (opponent != null) {
				// Disconnect another player if the current player disconnects/raises an uncaught error
				try {
					opponent.outToClient.writeBytes(DISCONNECT);
				} catch (IOException e2) {
					System.out.println(e2);
				}
			}
		} 
	}

	/**
	* Request the client the game mode they want to client (with another player
	* or with the AI Connecto-bot)
	*/
	public void requestGameMode() throws IOException, InterruptedException {
		outToClient.writeBytes(HUMAN_COMP);
		int choice = Integer.parseInt(inFromClient.readLine());
		if (choice == HUMAN) {
			synchronized(Game.waitingPlayer) {
				if (!Game.waitingPlayer.name.equals(DUMMY_NAME)) {
					// If there is another player waiting, pair them together and start the game
					game = Game.waitingPlayer.game;
					synchronized(game) {
						game.player2 = this;
						Game.waitingPlayer.opponent = this;
						opponent = Game.waitingPlayer;
						game.start(false);
						Game.waitingPlayer = Game.dummy;
					}
				} else {
					// If waitingPlayer is the dummy player, replace the waiting player and wait
					// for another player to connect
					game = new Game();
					game.player1 = this;
					Game.waitingPlayer = this;
					firstPlayer = true;
					outToClient.writeBytes("Waiting for another player to connect...\n");
					while (game.player2 == null) {
						sleep(1000);
					}
				}
			}
		} else {
			// Start the game with the AI Connecto-bot
			game = new Game();
			firstPlayer = true;
			game.player1 = this;
			outToClient.writeBytes(BOT_LEVEL);
			int level = Integer.parseInt(inFromClient.readLine());
			game.botLevel = level;
			game.start(true);
		}	
	}

	/**
	* Ask the player to enter their name. If the entered name is an empty string,
	* set the default name (Player 1 or Player 2)
	*/
	public void requestPlayerName() throws IOException, InterruptedException  {
		// Requesting the player's name
		while (true) {
			outToClient.writeBytes(YOUR_NAME);
			String inputName = inFromClient.readLine();
			if (game.botEnabled) {
				name = inputName;
				break;
			}
			// Request for another name if the name has been used by another player
			synchronized (opponent.name) {
				if (inputName.length() > 0 && inputName.equals(opponent.name)) {
					outToClient.writeBytes(DUPLICATE_NAME);
				} else {
					// Set default name
					name = inputName;
					if (name.length() == 0) {
						name = firstPlayer ? "Player 1" : "Player 2";
					}
					break;
				}
			}
		}
		if (!game.botEnabled) {
			// Wait for opponent to enter his/her name
			outToClient.writeBytes("Waiting for opponent to get ready...\n");
			while (opponent.name.equals("")) {
				sleep(1000);
			}
			outToClient.writeBytes("Your opponent is " + opponent.name + "\n");
		}
	}
	

	/**
	* This exception indicates the player chooses a column
	* that is already full.
	*/
	public class ColumnFullException extends Exception {
		int col;

		public ColumnFullException(int initCol) {
			col = initCol;

		}

		public String toString() {
			return "Column " + col + " is full.\n";
		}
	}
}
