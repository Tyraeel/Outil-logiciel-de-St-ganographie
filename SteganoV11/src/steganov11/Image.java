package steganov11;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Image {

    private RandomAccessFile fichierEntree; //descripteur du fichier d'entrée
    private RandomAccessFile fichierSortie; //descripteur du fichier de sortie
    private int degradation; //taux de dégradation utilisé pour coder le texte dans le fichier de sortie
    private int largeur; //largeur en pixels de l'image
    private int hauteur; //hauteur en pixels de l'image
    private int adresseDebut; //nombre d'octets à ignorer pour atteindre le début du texte

    public Image(String cheminEntree, String cheminSortie, int deg) throws FileNotFoundException {
        this.fichierEntree = new RandomAccessFile(cheminEntree, "r"); //on ouvre le fichier d'entrée en lecture
        if (cheminSortie != null) { //si le chemin a été spécifié
            this.fichierSortie = new RandomAccessFile(cheminSortie, "rw"); //on l'ouvre en lecture (pas de mode "w" uniquement)
        }
        this.degradation = deg;
        this.setDimension(); //on initialise largeur et hateur
    }

    public Image(String cheminEntree, String cheminSortie) throws FileNotFoundException {
        this(cheminEntree, cheminSortie, 1);
    }

    public int getLargeur() {
        return this.largeur;
    }

    public int getHauteur() {
        return this.hauteur;
    }

    public int getNbOctets() {
        return this.hauteur * this.largeur * 3; //sur un pixel il y a 3 composantes (RVB) chacune codée sur un octet.
                                                //or il y a hauteur*largeur pixels
    }

    public void close() {
        try {
            this.fichierEntree.close(); //on ferme les deux descripteurs
            this.fichierSortie.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setDimension() {
        try {
            fichierEntree.seek(0); //on se place au début du fichier
            fichierEntree.skipBytes(18); //on se déplace jusqu'à l'information de la taille
            String s = new String();
            for (int i = 0; i < 4; i++) //largeur sur 4 octets
            {
                s = Text.decToBin(fichierEntree.read(), 8) + s; //on construit la représentation binaire de la largeur
            }
            this.largeur = Text.binToDec(s);

            s = new String();
            for (int i = 0; i < 4; i++) //hauteur sur 4 octets
            {
                s = Text.decToBin(fichierEntree.read(), 8) + s;
            }
            this.hauteur = Text.binToDec(s);
        } catch (IOException ex) {
            Logger.getLogger(Image.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void setAdresseDebut(int adr) {
        this.adresseDebut = adr;
    }

    public void setDegradation(int value) {
        this.degradation = value;
    }

    public int getDegradation() {
        return this.degradation;
    }

    public void setFichierSortie(String path) {
        try {
            this.fichierSortie = new RandomAccessFile(path, "rw");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Image.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void creerEntete() {
        String binDeg = Text.decToBin(this.degradation, 4); //on crée une représentation du taux de dégradation. Au maximum 8 donc le codage se fait sur 4 bits
        try {
            this.setAtBeginningBMP(); //on se place après l'entete de l'image
            String[] groupes = Text.getParts(binDeg, 2); // on decoupe 2 bit par 2 bit. Le taux de dégradation de l'entete est de 2
            for (int i = 0; i < groupes.length; i++) {
                String b = Text.decToBin(this.fichierEntree.read(), 8); //on récupère la représentation binaire de l'octet suivant
                b = Text.replaceCharAt(b, b.length() - 2, groupes[i].charAt(0)); // on change les deux derniers bits (car taux de dégradation à 2)
                b = Text.replaceCharAt(b, b.length() - 1, groupes[i].charAt(1));

                this.fichierSortie.write(Text.binToDec(b)); //on écrit dans le fichier de sortie
            }//on a écrit la dégradation

            String adresseDebutBin = Text.decToBin(this.adresseDebut, (this.getLengthEntete() - 2)*2);
            
            groupes = Text.getParts(adresseDebutBin, 2);

            for (int i = 0; i < groupes.length; i++) {
                String b = Text.decToBin(this.fichierEntree.read(), 8);
                b = Text.replaceCharAt(b, b.length() - 2, groupes[i].charAt(0)); // on change les deux derniers bit
                b = Text.replaceCharAt(b, b.length() - 1, groupes[i].charAt(1));

                this.fichierSortie.write(Text.binToDec(b));
            }//on a écrit l'adresse de début

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void coderImage(String texteACoder) {
        try {
            int offset = this.getOffsetBMP();
            this.fichierEntree.seek(0); //on se place au début des deux fichiers
            this.fichierSortie.seek(0);

            for (int i = 0; i < offset; i++) {
                this.fichierSortie.write(this.fichierEntree.read()); //on copie l'entete de l'image (offset octets)
            }
            this.creerEntete(); //on créé l'entete du texte

            for (int i = 0; i < this.adresseDebut; i++) {
                this.fichierSortie.write(this.fichierEntree.read()); //on copie les pixels non modifiés
            }
            String texteBinaire = Text.stringToBin(texteACoder); //on prépare le texte à écrire
            texteBinaire += Text.END_OF_STRING; //on ajoute le caractère fin de chaine '\0'
            String[] groupes = Text.getParts(texteBinaire, this.degradation); //on découpe en groupe de {this.degradation} (puissance de 2) bits

            for (int i = 0; i < groupes.length; i++)//pour chaque groupe de 1/2/4/8 bits (this.degradation)
            {
                String b = Text.decToBin(this.fichierEntree.read(), 8); //on récupère l'octet suivant
                for (int j = 0; j < this.degradation; j++) {
                    b = Text.replaceCharAt(b, b.length() - (j + 1), groupes[i].charAt(this.degradation - j - 1)); //on remplace autant de bits que nécessaire
                }

                this.fichierSortie.write(Text.binToDec(b)); //on y inscrit la nouvelle valeur
            }

            int octet;
            while ((octet = this.fichierEntree.read()) != -1) {
                this.fichierSortie.write(octet);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.close();
    }

    public String decoderImage() {
        try {
            setAtBeginningBMP(); //on se déplace au début de l'entete
            String deg = "";
            for (int i = 0; i < 2; i++) {
                String s = Text.decToBin(fichierEntree.read(), 8);
                deg += s.substring(s.length() - 2); //on récupère que les deux derniers bits (car le taux de dégradation est de 2)
            } //on lit l'info sur le taux de dégradation
            this.degradation = Text.binToDec(deg);
            
            String adresseDebut = "";
            
            for (int i = 0; i < this.getLengthEntete() - 2; i++) {
                String s = Text.decToBin(fichierEntree.read(), 8);
                adresseDebut += s.substring(s.length() - 2); //de même on isole les deux derniers bits
            } //on lit l'adresse de début (qui correspond au nombre d'octets à ignorer à partir de la fin de l'entete)
            this.adresseDebut = Text.binToDec(adresseDebut);
            
            this.fichierEntree.skipBytes(this.adresseDebut); //on se place au début du message
            String msg = "";
            String curr_char = readByteBMP(); //on lit un caractère du message
            while (!curr_char.equals(Text.binToString(Text.END_OF_STRING))) { //tant que le caractère actuel ne correspond pas à une fin de chaine
                msg += curr_char; //on ajoute ce caracètre à la chaîne
                curr_char = readByteBMP(); //on lit le caractère suivant
            }
            return msg;
        } catch (IOException e) {
            return "";
        }
    }

    public String readByteBMP() {
        try {
            String s = "";
            for (int i = 0; i < 8 / this.degradation; i++)//on lit par groupe de {this.degradation} bits.
            {
                String value = Text.decToBin(this.fichierEntree.read(), 8); //on lit un octet
                s += value.substring(value.length()-this.degradation); //on extrait les 1/2/4/8 derniers bits de la chaine value
            }
            return Text.binToString(s);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public int getOffsetBMP() {
        try {
            long current_pos = fichierEntree.getFilePointer();
            fichierEntree.seek(0); //on se place au début du fichier
            this.fichierEntree.skipBytes(10); //on se place à l'endroit où il y a l'info 

            String s = "";

            for (int i = 0; i < 4; i++) { //on lit l'info sur le début de l'image
                s = Text.decToBin(this.fichierEntree.read(), 8) + s;
            }

            fichierEntree.seek(current_pos);
            return Text.binToDec(s);
        } catch (IOException e) {
            return -1;
        }
    }

    public void setAtBeginningBMP() {
        //déplace le pointeur de fichier au début de l'entete du texte
        try {
            this.fichierEntree.seek(this.getOffsetBMP());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getLengthEntete() //retourne la taille en octets de l'entête
    {
        int lengthEntete = 2; //taux de dégradation
        int nbPixels = this.getHauteur() * this.getLargeur() * 3;
        int nbBits = 0;

        while (Math.pow(2, nbBits) < nbPixels) {
            nbBits++;
        } //on cherche le nombre de bits nécessaires pour coder nbPixels
        if(nbBits % 2 == 1)
            nbBits++; //on veut une entete paire, on change les deux derniers bits de chaque octet.
        lengthEntete += nbBits/2; //car 2 bits par octet

        return lengthEntete;
    }

    public int genRandomAdress(int tailleTexte) {
        //génère une adresse de départ pour le texte
        int nbOctets = this.getNbOctets() - this.getLengthEntete(); //nombre d'octets modifiables dans l'image
        int nbOctetsNecessairesTexte = (tailleTexte + 1) * (int) (8 / Math.pow(2, this.degradation)); //nombre d'octets nécessaires pour enregistrer le texte (+1 correspond au caractère \0)
        int adresseMax = nbOctets - nbOctetsNecessairesTexte; //adresse maximum que l'on veut générer

        Random adresse = new Random();
        return adresse.nextInt(adresseMax);
    }
}
