package steganov11;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Slider;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import java.io.File;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.PasswordField;
import javafx.util.StringConverter;

/**
 *
 * @author Thibault
 */
public class FXMLDocumentController implements Initializable {

    @FXML
    private TextArea textToHide;
    @FXML
    private Button coder;
    @FXML
    private Button decoder;
    @FXML
    private Button parcourir;
    @FXML
    private ImageView previsualisation;
    @FXML
    private Slider degradation;
    @FXML
    private AnchorPane mainPane;
    @FXML
    private TabPane tabPane;
    @FXML
    private Label tailleTexteMax;
    @FXML
    private Label tailleTexteActuel;
    @FXML
    private Button buttonEnvoyer;
    @FXML
    private AnchorPane spRecevoir;
    @FXML
    private TextField ipRecevoir;
    @FXML
    private TextField portRecevoir;
    @FXML
    private Button buttonRecevoir;
    @FXML
    private Button choixDossier;
    @FXML
    private ImageView visualisationImage;
    @FXML
    private Label destination;
    @FXML
    private Label messageTask;
    @FXML
    private PasswordField passwordCoder;
    @FXML
    private TextArea textDecode;
    @FXML
    private PasswordField passwordDecoder;

    private steganov11.Image image;
    private String cheminImage;

    public void setImage(String path) {
        try {
            image = new steganov11.Image(path, null);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        coder.setDisable(true);
        decoder.setDisable(true);
        degradation.setDisable(true);
        textToHide.setDisable(true);

        degradation.setMin(0);
        degradation.setMax(3);
        degradation.setLabelFormatter(new StringConverter<Double>() {
            @Override
            public String toString(Double x) {
                return String.format(
                        "%1$.0f",
                        Math.pow(2, x)
                );
            }

            @Override
            public Double fromString(String s) {
                return null;
            }
        });
        degradation.setValue(0);
        degradation.setShowTickLabels(true);

        degradation.valueProperty().addListener((ObservableValue<? extends Number> obs, Number oldValue, Number newValue) -> {
            //on limite les valeurs à des entiers
            if (newValue.doubleValue() - Math.floor(newValue.doubleValue()) < 0.5) //on est avant la moitié
            {
                degradation.setValue(Math.floor(newValue.doubleValue()));
            } else {
                degradation.setValue(Math.ceil(newValue.doubleValue()));
            }
            coder.setDisable(!isCodable());
            setTailleTexteActuel();
        });

        textToHide.textProperty().addListener((final ObservableValue<? extends String> observable, final String oldValue, final String newValue) -> {
            //à la modification du texte
            coder.setDisable(!isCodable());
            setTailleTexteActuel();
        });
    }

    @FXML
    private void handleButtonCoder(ActionEvent event) {
        image.setDegradation((int) Math.pow(2, degradation.getValue())); //on récupère l'info de dégradation
        image.setAdresseDebut(image.genRandomAdress(textToHide.getLength())); //on génère une adresse de début
        String fichierSortie;

        File selectedDirectory = chooseDirectory();
        if (selectedDirectory != null) {
            fichierSortie = selectedDirectory.getAbsolutePath() + cheminImage.substring(cheminImage.lastIndexOf(File.separator));

            image.setFichierSortie(fichierSortie);

            String texteACoder = textToHide.getText(); //on prépare le texte à enregistrer dans l'image
            if (!passwordCoder.getText().equals("")) {
                texteACoder = Text.encrypt(texteACoder, passwordCoder.getText()); //cryptage si nécessaire
            }
            image.coderImage(texteACoder); //on enregistre le texte dans l'image
            previsualisation.setImage(new Image(new File(fichierSortie).toURI().toString())); //on affiche l'image modifiée
            try {
                image = new steganov11.Image(fichierSortie, null);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @FXML
    private void handleButtonDecoder(ActionEvent event) {
        String s = image.decoderImage(); //on récupère le message de l'image

        if (!passwordDecoder.getText().equals("")) {
            s = Text.decrypt(s, passwordDecoder.getText()); //décryptage si nécessaire
        }
        textDecode.setText(s); //on affiche le texte
    }

    private boolean isCodable() {
        int nbOctets = image.getNbOctets() - image.getLengthEntete() - 1 * (int) (8 / Math.pow(2, degradation.getValue())); //nombre octets disponible pour le codage
        int nbOctetsNecessaires = (textToHide.getText().length()) * (int) (8 / Math.pow(2, degradation.getValue())); //nombre d'octets nécessaires

        return !textToHide.getText().equals("") && previsualisation.getImage() != null && nbOctetsNecessaires <= nbOctets;
    }

    private boolean isDecodable() {
        return previsualisation.getImage() != null; //on ne peut appeler decoderImage que si l'on a spécifié une image
    }

    private void setTailleTexteActuel() {
        tailleTexteActuel.setText(String.valueOf((textToHide.getText().length() * (int) (8 / Math.pow(2, degradation.getValue()))))); //taille en octets que prendra le texte dans l'image
    }

    @FXML
    private void handleButtonParcourir(ActionEvent event) {
        File selectedFile = chooseImageFile(); //on sélectionne une image
        if (selectedFile != null) { //si on a bien sélectionné une image
            previsualisation.setImage(new Image(selectedFile.toURI().toString())); //on affiche l'image
            cheminImage = selectedFile.getAbsolutePath();
            this.setImage(cheminImage);

            degradation.setDisable(false);
            textToHide.setDisable(false);
            coder.setDisable(!isCodable());
            decoder.setDisable(!isDecodable());
            tailleTexteMax.setText(String.valueOf(image.getNbOctets() - image.getLengthEntete() - 1 * (int) (8 / Math.pow(2, degradation.getValue())))); //nombre d'octets modifiables
        }
    }

    private File chooseImageFile() {
        Window mainStage = mainPane.getScene().getWindow();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choissisez une image");
        fileChooser.getExtensionFilters().add(
                new ExtensionFilter("Image", "*.bmp")
        );
        File selectedFile = fileChooser.showOpenDialog(mainStage);

        return selectedFile;
    }

    private File chooseDirectory() {
        Window mainStage = mainPane.getScene().getWindow();
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Dossier d'enregistrement");

        File selectedFolder = dc.showDialog(mainStage);
        return selectedFolder;
    }

    /*TAB RECEVOIR/ENVOYER*/
    @FXML
    private void handleChoixDestination(ActionEvent event) {
        File selectedFolder = chooseDirectory();
        if (selectedFolder != null) {
            destination.setText(selectedFolder.getAbsolutePath());
        }
    }

    @FXML
    private void handleEnvoiImage(ActionEvent event) {
        if (!ipRecevoir.getText().equals("") && !portRecevoir.getText().equals("")) {

            File selectedFile;

            selectedFile = chooseImageFile(); //on commence par demander l'image à envoyer

            if (selectedFile != null) { //si un fichier a été sélectionné
                visualisationImage.setImage(new Image(selectedFile.toURI().toString())); //on affiche l'image
                tasks.SendFile sf = new tasks.SendFile(selectedFile, ipRecevoir.getText(), Integer.valueOf(portRecevoir.getText())); //on prépare l'envoi de l'image
                sf.messageProperty().addListener((obs, oldMsg, newMsg) -> {
                    messageTask.setText(newMsg);
                });
                new Thread(sf).start();
            }
        }
    }

    @FXML
    private void handleRecevoirFichier(ActionEvent event) {
        String dossier = destination.getText(); //on récupère le dossier d'enregistrement

        if (dossier.equals("")) { //si il n'y a pas de dossier spécifié
            dossier = System.getProperty("user.home"); //on en met un par défaut (répertoire home)
        }

        tasks.ReceiveFile rf = new tasks.ReceiveFile(dossier); //on se prépare à recevoir l'image
        rf.messageProperty().addListener((obs, oldMsg, newMsg) -> {
            messageTask.setText(newMsg); //on lie le message d'état de la tâche à un label
            //lorsque la tâche appellera 
        });

        rf.setOnSucceeded(new EventHandler<WorkerStateEvent>() { //quand le transfert se finit
            @Override
            public void handle(WorkerStateEvent t) {
                File img = rf.getValue();
                if (img != null) {
                    visualisationImage.setImage(new Image(img.toURI().toString()));
                    previsualisation.setImage(new Image(img.toURI().toString()));

                    setImage(img.getAbsolutePath());

                    degradation.setDisable(false);
                    coder.setDisable(!isCodable());
                    decoder.setDisable(!isDecodable());
                    tailleTexteMax.setText(String.valueOf(image.getNbOctets() - image.getLengthEntete() - 1 * (int) (8 / Math.pow(2, degradation.getValue())))); //nombre d'octets modifiables
                }
            }
        });
        new Thread(rf).start();
    }
}
