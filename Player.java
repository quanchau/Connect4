import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;


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

		@Override
		public void run() {
			try {
				// Ask the client 1 or 2 -> Client enter 1 -> Check player static field in Game to see if there is another player ready -> if yes, join the game of player 1 & set the opponent of that game to player 2
				// (don't run in Game constructor). 
				// If no, run the while loop to keep checking of the field opponent is still null. If not null, run the thread as normal.
				outToClient.writeBytes(HUMAN_COMP);
				int choice = Integer.parseInt(inFromClient.readLine());
				if (choice == HUMAN) {
					synchronized(Game.waitingPlayer) {
						if (!Game.waitingPlayer.name.equals(DUMMY_NAME)) {
							game = Game.waitingPlayer.game;
							synchronized(game) {
								game.player2 = this;
								Game.waitingPlayer.opponent = this;
								opponent = Game.waitingPlayer;
								game.start(false);
								Game.waitingPlayer = Game.dummy;
							}
						} else {
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
					game = new Game();
					firstPlayer = true;
					game.player1 = this;
					outToClient.writeBytes(BOT_LEVEL);
					int level = Integer.parseInt(inFromClient.readLine());
					game.botLevel = level;
					game.start(true);
				}
				
				// Requesting player's name
				while (true) {
					outToClient.writeBytes(YOUR_NAME);
					String inputName = inFromClient.readLine();
					if (game.botEnabled) {
						name = inputName;
						break;
					}

					// Handle duplicate names
					synchronized (opponent.name) {
						if (inputName.equals(opponent.name)) {
							outToClient.writeBytes(DUPLICATE_NAME);
						} else {
							name = inputName;
							if (name.length() == 0) {
								name = firstPlayer ? "Player 1" : "Player 2";
							}
							break;
						}
					}
				}

				if (!game.botEnabled) {
					outToClient.writeBytes("Waiting for opponent to get ready...\n");
					// Wait for opponent to enter his/her name
					while (opponent.name.equals("")) {
						sleep(1000);
					}
					outToClient.writeBytes("Your opponent is " + opponent.name + "\n");
				}
				
				String boardString = game.boardToString();
				outToClient.writeBytes(boardString);
				if (firstPlayer) {
					outToClient.writeBytes(YOUR_MOVE);
				} else {
					outToClient.writeBytes(WAIT_FOR_OPPONENT);
				}

				while (true) {
					String move = inFromClient.readLine();
					// TODO move should not be read/saved when it is not your turn
					synchronized (game) {
						if (game.currentPlayer == this) {
							move = move.trim();
							try {
								int moveCol = Integer.parseInt(move);
								if (moveCol < 1 || moveCol > 7)
									throw new NumberFormatException();
								// synchronized (game) {
									int height = game.heights[moveCol - 1];
									if (height < 6) {
										game.placeToken(moveCol - 1);
										boardString = game.boardToString();

										// check if won
										if (game.isWin(moveCol - 1)) {
											// send final board state
											outToClient.writeBytes(boardString);

											// send game over messages
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
											// send final board state
											outToClient.writeBytes(boardString);

											// send game over messages
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
//							}

								if (!game.endGame) {
									game.currentPlayer = opponent;
									boardString = game.boardToString();
									if (game.botEnabled) {
										game.botMove();
									} else {
										outToClient.writeBytes(boardString);
										outToClient.writeBytes(WAIT_FOR_OPPONENT);
										opponent.outToClient.writeBytes(boardString);
										opponent.outToClient.writeBytes(YOUR_MOVE);
									}
								}
							} catch (NumberFormatException nfe) {
								outToClient.writeBytes(INVALID_MOVE);

							} catch (ColumnFullException e) {
								System.out.println(e.getMessage());
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
					try {
						opponent.outToClient.writeBytes(DISCONNECT);
					} catch (IOException e2) {
						System.out.println(e2);
					}
				}
			} 
		}
		
		public class ColumnFullException extends Exception {
			int col;

			public ColumnFullException(int initCol) {
				col = initCol;

			}

			public String toString() {
				return "Column " + col + " is full.";
			}
		}
}
