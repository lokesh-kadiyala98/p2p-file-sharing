package app.peer;

import app.constants.Constants;
import app.constants.KeyManager;

import java.net.*;
import java.io.*;

import static app.constants.Constants.TerminalColors.*;

public class Peer {
    private static String IP_ADDRESS = "127.0.0.1";
    private static int FDS_PORT = 8080;
    private static String PEER_ID = null;

    public static void serverSocket(int PORT) throws IOException {

        try {
            ServerSocket serverSocket = new ServerSocket(PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();

                ClientHandler clientHandler = new ClientHandler(clientSocket, PEER_ID);
                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        } catch (IOException e) {
            System.out.println(ANSI_RED+ "IOException: " + e.getMessage() + ANSI_RESET);
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Incorrect number of arguments\n");
            System.exit(1);
        }

        PEER_ID = args[0];
        int PORT_NO = Integer.parseInt(args[1]);

        String peerFolderName = "./src/main/resources/" + PEER_ID;
        File folder = new File(peerFolderName);
        folder.mkdir();
        folder = new File(peerFolderName + "/FDS");
        folder.mkdir();

        Menu menu = new Menu(PEER_ID, PORT_NO);
        Thread thread = new Thread(menu);thread.start();

        serverSocket(PORT_NO);
    }
}

