package tasks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import javafx.concurrent.Task;

public class ReceiveFile extends Task<File> {
    
    private String destination;
    
    public ReceiveFile(String destination)
    {
        this.destination = destination;
    }

    @Override
    protected File call() throws Exception {
        byte[] buffer = new byte[1024];
        int bw = 0;
        File img=null;
        BufferedReader in;
        String fileName;

        try {          
            ServerSocket server = new ServerSocket(2009); //on prépare le serveur
            updateMessage("Attente de connexion");
            Socket clientSocket = server.accept(); //on attend un client

            updateMessage("Attente nom du fichier");
            //on récupère le nom du fichier
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            fileName = in.readLine();
            //in.close();

            img = new File(destination + "/" + fileName); //fichier de l'image

            updateMessage("Réception de l'image");
            //on récupère l'image
            FileOutputStream fos = new FileOutputStream(img);
            InputStream fis = clientSocket.getInputStream();

            while ((bw = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, bw);
            }
            
            in.close();
            fos.flush();
            fos.close();
            fis.close();
            clientSocket.close(); //on ferme le socket client
            server.close(); //on ferme notre socket
            updateMessage("Fichier téléchargé");
        } catch (IOException ex) {
            updateMessage(ex.getMessage());
        }
        return img;
    }
}
