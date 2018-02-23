package tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import javafx.concurrent.Task;

public class SendFile extends Task<Void> {

    private final File img;
    private String ipServeur;
    private int portServeur;

    public SendFile(File img, String ipServeur, int portServeur) {
        this.img = img;
        this.ipServeur = ipServeur;
        this.portServeur = portServeur;
    }

    @Override
    protected Void call() throws Exception {
        try {
            byte[] buffer = new byte[1024];
            int bw = 0;
            InputStream fis;
            PrintWriter printWriterOut;
            OutputStream fos;
            
            updateMessage("Connexion");
            Socket client = new Socket(ipServeur, portServeur); //on se connecte au serveur
            
            fos = client.getOutputStream();
            
            //on commence par envoyer le nom du fichier au serveur
            printWriterOut = new PrintWriter(fos);
            printWriterOut.println(img.getName());
            printWriterOut.flush();
            
            updateMessage("Nom du fichier envoyé");
            
            //puis l'image
            fis = new FileInputStream(img); //on ouvre le fichier image
            while ((bw = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, bw); //on envoie l'image
            }
            updateMessage("Image envoyé");
            
            fos.flush();
            fos.close();
            fis.close();
            client.close();
        } catch (IOException e) {
            updateMessage(e.getMessage());
        }

        return null;
    }
}
