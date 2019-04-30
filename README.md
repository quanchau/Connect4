
# Instruction on how to run the programs for Mac Users:

Running the server:

-	First, make sure that Connect4Server.java, Game.java, and Player.java are in the same folder.
-	On the terminal, go to the folder containing all the above files and compile the Connect4Server class by running the following command:

    ```sh
    $ javac Connect4Server.java
    ```

- After that, we start the server by running the following command:

    ```sh
    $ javac Connect4Server.java
    ```

- At this point, the server will start receiving connection requests from the clients. If the client chooses to play with the AI Connecto-bot, the server will start the game immediately after connection is successful. Otherwise, the server will wait for the next client to connect to start the game between two clients.

Running the client (for players):

- First, open the Connect4Client.java file and change the SERVER_IP_ADDRESS constant value (line 15) to the IP address of the machine that is running the server. 

    - To find the IP address of the server machine, go to the Terminal and run the following command:

    ```sh
    $ ipconfig getifaddr en0
    ```

    - After that, open the Terminal and direct to the folder containing the Connect4Client.java file. Then compile the file by running the following command:

    ```sh
    $ javac Connect4Client.java
    ```

    - Now, we can run the client and start playing the game with the following command:

    ```sh
    $ java Connect4Client
    ```

    - The client (player) can choose to play with either an AI Connecto-bot or another player. If the player chooses to play with the AI bot, they can start the game immediately. Otherwise, they have to wait for another player to connect to play the game.
    
- Playing with the AI Connecto-bot: There are 5 different levels for the player to choose. Higher level means it is harder to win against the bot.
    - While playing, if one of the players disconnects, the game will end and the other player is also disconnected.

For Windows Users, most of the steps are similar except for the followings:
- On Windows, Terminal means Command Prompt
- On Windows Command Prompt, running ipconfig shows all the information about the IP address of the machine’s network interface card.
