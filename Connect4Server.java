import java.io.*;
import java.net.*;

class Connect4Server {

	public static void main(String argv[]) throws Exception {

		ServerSocket welcomeSocket = new ServerSocket(6789);

		System.out.println("Waiting for incoming connection Request...");

		int clientsConnected = 0;

		while (true) {
			Socket connectionSocket = welcomeSocket.accept();
			clientsConnected++;
			System.out.println("New client connected with IP: " + connectionSocket.getInetAddress());
			System.out.println("Total number of clients connected: " + clientsConnected);
			
			// create a new player, initially set to move second
			Player currentPlayer = new Player(connectionSocket, false);
			currentPlayer.start();
		}
	}
}
