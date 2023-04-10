package com.peer;

import com.payloads.Payload;

import java.io.*;
import java.net.Socket;

import static com.constants.Constants.TerminalColors.*;

public class Menu implements Runnable {
    private static Socket socket = null;
    private static String IP_ADDRESS = "127.0.0.1";
    private static int FDS_PORT = 8080;
    private int port_no;
    private String peer_ID = null;

    public Menu(String PEER_ID, int PORT_NO) {
        this.peer_ID = PEER_ID;
        this.port_no = PORT_NO;
    }

    @Override
    public void run() {
        try {
            socket = new Socket(IP_ADDRESS, FDS_PORT);
            System.out.println(ANSI_BLUE + "Connected to server" + ANSI_RESET);

            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            DataInputStream serverReader = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            ObjectOutputStream serverWriter = new ObjectOutputStream(socket.getOutputStream());

            String userInput = "init";
            while (true) {
                if (userInput == null || userInput.equalsIgnoreCase("exit")) {
                    break;
                }
                Payload payload = new Payload.Builder()
                        .setCommand(userInput)
                        .setPeerId(peer_ID)
                        .setPortNo(port_no)
                        .build();
                serverWriter.writeObject(payload);
                serverWriter.flush();

                socket.setSoTimeout(2000);
                String serverResponse = serverReader.readUTF();
                System.out.println(ANSI_GREEN + serverResponse + ANSI_RESET);

                System.out.print("> ");
                userInput = consoleReader.readLine();
            }
        } catch (IOException e) {
            System.out.println(ANSI_RED + "Error: " + e.getMessage() + ANSI_RESET);
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                System.out.println(ANSI_RED + "IOException: Error closing socket: " + e.getMessage() + ANSI_RESET);
            }
        }
    }
}
