package app.peer;

import app.Models.Payloads.CreateFilePayload;
import app.Models.Payloads.EncryptedPayload;
import app.Models.Payloads.Payload;
import app.Models.Payloads.Peer.UpdateKeyPayload;
import app.Models.Payloads.ResponsePayload;
import app.Models.PeerInfo;
import app.utils.AES;
import app.utils.CObject;
import app.utils.RSA;

import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Properties;

import static app.constants.Constants.TerminalColors.*;

class ClientHandler implements Runnable {
    private Socket clientSocket;
    private String PEER_ID;
    private ObjectInputStream clientReader;
    private ObjectOutputStream clientWriter;
    private SecretKey peerSecretKey;
    private SecretKey peerLocalSecretKey;
    private String peerStorageBucketPath;
    private Properties properties;

    public ClientHandler(Socket clientSocket, String PEER_ID, SecretKey peerSecretKey, SecretKey peerLocalSecretKey, Properties properties) {
        this.clientSocket = clientSocket;
        this.PEER_ID = PEER_ID;
        this.peerSecretKey = peerSecretKey;
        this.peerLocalSecretKey = peerLocalSecretKey;
        this.peerStorageBucketPath = "./src/main/resources/" + PEER_ID;
        this.properties = properties;
    }

    @Override
    public void run() {
        try {
            System.out.println(ANSI_BLUE + "Thread started: " + Thread.currentThread() + ANSI_RESET);

            clientReader = new ObjectInputStream(clientSocket.getInputStream());
            clientWriter = new ObjectOutputStream(clientSocket.getOutputStream());

            Object clientInput;
            while ((clientInput = clientReader.readObject()) != null) {
                Payload payload = null;
                if (clientInput instanceof EncryptedPayload encryptedPayload) {
                    byte[] decryptedData = AES.decrypt(peerSecretKey, encryptedPayload.getData());
                    payload = (Payload) CObject.bytesToObject(decryptedData);
                } else if (clientInput instanceof Payload) {
                    payload = (Payload) clientInput;
                }

                if (payload != null) {
                    ResponsePayload response = processInput(payload);
                    clientWriter.writeObject(response);
                    clientWriter.flush();
                }
            }
        } catch (IOException e) {
            System.out.println(ANSI_RED + "IOException: " + e.getMessage() + ANSI_RESET);
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.out.println(ANSI_RED + "ClassNotFoundException: " + e.getMessage() + ANSI_RESET);
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println(ANSI_RED + "Exception: " + e.getMessage() + ANSI_RESET);
            e.printStackTrace();
        } finally {
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }
                if (clientReader != null) {
                    clientReader.close();
                }
                if (clientWriter != null) {
                    clientWriter.close();
                }
            } catch (IOException e) {
                System.out.println(ANSI_RED + "IOException: Error closing client socket: " + e.getMessage() + ANSI_RESET);
                e.printStackTrace();
            }
        }
    }

    private ResponsePayload processInput(Payload clientPayload) throws Exception {
        PeerInfo peerInfo = clientPayload.getPeerInfo();
        String peer_id = peerInfo.getPeer_id();
        System.out.println(ANSI_BLUE + "Serving Peer: " + peer_id);
        System.out.println("Executing: " + clientPayload.getCommand() + ANSI_RESET);

        ResponsePayload responsePayload = null;
        String command = clientPayload.getCommand();
        String message;

        switch (command) {
            // @deprecated
            case "mkdir":
                CreateFilePayload createFilePayload = (CreateFilePayload) clientPayload;
                byte[] encryptedFileName = AES.encrypt(peerLocalSecretKey, createFilePayload.getFileName().getBytes());
                File folder = new File(Paths.get(peerStorageBucketPath, encryptedFileName.toString()).toString());
                folder.mkdir();

                message = String.format("Folder created successfully %s", createFilePayload.getFileName());
                responsePayload = new ResponsePayload.Builder()
                    .setStatusCode(201)
                    .setMessage(message)
                    .build();
                break;
            case "updateKey":
                UpdateKeyPayload updateKeyPayload = (UpdateKeyPayload) clientPayload;
                byte[] CAPublicKeyBytes = Base64.getDecoder().decode(properties.getProperty("CA_PBK"));
                SecretKey secretKey = AES.getSecretKey(RSA.decrypt(updateKeyPayload.getKey(), RSA.getPublicKey(CAPublicKeyBytes)));

                PeersSecretKeyCache.setPeersSecretKey(updateKeyPayload.getPeerInfo().getPeer_id(), secretKey);

                message = String.format("%s ACK: %s key updated!", PEER_ID, updateKeyPayload.getPeerInfo().getPeer_id());
                responsePayload = new ResponsePayload.Builder()
                    .setStatusCode(200)
                    .setMessage(message)
                    .build();
                break;
            default:
                responsePayload = new ResponsePayload.Builder()
                    .setStatusCode(400)
                    .setMessage("Peer: Command handler not found")
                    .build();
                System.out.println(ANSI_YELLOW + "Invalid command issued: " + ANSI_RESET);
        }

        return responsePayload;
    }
}
