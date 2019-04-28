import java.io.*;
import java.net.*;
import java.util.*;
class Connect4Client {

  static final String YOUR_MOVE = "YOUR_MOVE";
  static final String YOUR_NAME = "YOUR_NAME";
  static final String DUPLICATE_NAME = "DUPLICATE_NAME";
  static final String WAIT_FOR_OPPONENT = "WAIT_FOR_OPPONENT";
  static final String INVALID_MOVE = "INVALID_MOVE";
  static final String END_GAME = "END_GAME";
  static final String HUMAN_COMP = "HUMAN_COMP";
  static final String BOT_LEVEL = "BOT_LEVEL";
  static final String DISCONNECT = "DISCONNECT";
  static final String SERVER_IP_ADDRESS = "172.18.24.33";

  public static void main(String argv[]) throws Exception
  {
    
    BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

    Socket clientSocket = new Socket(SERVER_IP_ADDRESS, 6789);

    DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());

    BufferedReader inFromServer =new BufferedReader(new InputStreamReader(
      clientSocket.getInputStream()));

    while (true) {
      String sentence = inFromServer.readLine();
      if (sentence.equals(HUMAN_COMP)) {
        System.out.println("Which mode do you want to play? (Type 1 or 2)");
        System.out.println("1: Play with another person");
        System.out.println("2: Play with Connecto-bot");
        String choice = inFromUser.readLine().trim();
        while (!choice.equals("1") && !choice.equals("2")) {
          System.out.println("Please only type 1 or 2:");
          choice = inFromUser.readLine().trim();
        }
        outToServer.writeBytes(choice + "\n");
      } else if (sentence.equals(BOT_LEVEL)) {
        System.out.println("Pick a difficulty.");
        System.out.println("1: Connecto-bot's little brother : He'll be nice to you.");
        System.out.println("2: Connecto-bot's friend ' The Connectinator' : She's a little bit tougher.");
        System.out.println("3: Connecto-bot : You will probably lose.");
        System.out.println("4: Connecto-bot 2.0 : If you win, Quan will connect you with $4.");
        System.out.println("5: Connecto-bot's FINAL FORM : You should probably just give up...");
        String level = inFromUser.readLine().trim();
        ArrayList<String> levels = new ArrayList<String>(Arrays.asList("1", "2", "3", "4", "5"));
        while (!levels.contains(level)) {
          System.out.println("Please only choose level 1 - 5:");
          level = inFromUser.readLine().trim();
        }
        outToServer.writeBytes(level + "\n");
      } else if (sentence.equals(YOUR_NAME)) {
        System.out.println("What is your name?");
        String name = inFromUser.readLine();
        name = name.trim();
        outToServer.writeBytes(name + "\n");
      } else if (sentence.equals(DUPLICATE_NAME)) { 
        System.out.println("Your name has been used by your opponent.");
      } else if (sentence.equals(YOUR_MOVE)) {
        System.out.println("It is your turn. Pick any available column from 1 to 7:");
        String column = inFromUser.readLine();
        column = column.trim();
        outToServer.writeBytes(column + "\n");
      } else if (sentence.equals(WAIT_FOR_OPPONENT)) {
        System.out.println("Waiting for your opponent to make a move...");
      } else if (sentence.equals(INVALID_MOVE)) {
        System.out.println("Please only enter a number between 1 and 7:");
        String column = inFromUser.readLine();
        column = column.trim();
        outToServer.writeBytes(column + "\n");
      } else if (sentence.equals(END_GAME)) { 
        System.out.println("The game ends. Disconnecting...");
        break;
      } else if (sentence.equals(DISCONNECT)) {
        System.out.println("Your opponent has disconnected. Ending the game...");
        break;
      } else {
        System.out.println(sentence);
      } 

    }

    clientSocket.close(); 
    outToServer.close();
    inFromServer.close();


  }
}


