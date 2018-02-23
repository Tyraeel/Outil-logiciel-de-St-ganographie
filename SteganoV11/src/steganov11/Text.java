package steganov11;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

public class Text {

    public static final String END_OF_STRING = "00000000";

    public static String[] getParts(String s, int partitionSize) //découpe une chaine de caractères en chaines de partitionSize caractères
    {
        String[] groupes = new String[s.length() / partitionSize];

        for (int i = 0; i < s.length(); i += partitionSize) {
            groupes[i / partitionSize] = new String(s.substring(i, Math.min(s.length(), i + partitionSize))); //on extrait la chaine caractère suivante de taille partitionSize
        }
        return groupes;
    }

    public static String decToBin(int dec, int length) //dec : la valeur décimale à coder en binaire
    //length : la taille de la chaine de caractère (on complète à gauche par des 0)
    //si le nombre doit etre codé sur plus de length bits, alors length est ignoré
    {
        String bin = "";
        while (dec != 0) {
            bin = String.valueOf(dec % 2) + bin;
            dec = dec / 2;
        } //transformation par division euclidienne

        while (bin.length() < length) //ajout de 0 à gauche 
        {
            bin = "0" + bin;
        }

        return bin;
    }

    public static int binToDec(String s) //s : représentation binaire du nombre décimal
    //retourne le nombre décimal
    {
        int res = 0;
        for (int i = s.length() - 1; i >= 0; i--) {
            res += Math.pow(2.0, (double) (s.length() - 1 - i)) * Character.getNumericValue(s.charAt(i));
        }

        return res;
    }

    public static String stringToBin(String s) //s : texte à transformer en binaire
    //retourne la représentation des caractères de s en binaire
    {
        String binary = "";
        byte[] infoBin;

        try {
            infoBin = s.getBytes("US-ASCII");
            for (byte b : infoBin) {
                String bin = Integer.toBinaryString(b);
                while (bin.length() < 8) {
                    bin = "0" + bin;
                }
                binary += bin;
            }
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(Text.class.getName()).log(Level.SEVERE, null, ex);
        }

        return binary;
    }

    public static String replaceCharAt(String s, int pos, char c) {
        //remplace le caractère à la position pos dans s par c
        return s.substring(0, pos) + c + s.substring(pos + 1);
    }

    public static String binToString(String s) {
        //transforme une représentation binaire d'un texte en une chaîne de caractères
        String output = "";
        for (int i = 0; i <= s.length() - 8; i += 8) //tous les 8 bits, car une lettre est codée sur 8 bits
        {
            int k = binToDec(s.substring(i, i + 8)); //on extrait la valeur décimale de la lettre
            output += (char) k; //on ajoute la lettre à la chaîne
        }

        return output;
    }

    public static String encrypt(String s, String cle) {
        try {
            byte[] key = cle.getBytes(); //Conversion de la clé en octets et création du vecteur d'initialisation
            String IV = "c!Q62Q4:";

            SecretKeySpec keySpec = new SecretKeySpec(key, "Blowfish");
            Cipher cipher = Cipher.getInstance("Blowfish/CBC/PKCS5Padding"); //Création de la clé et du Cipher utilisant Blowfish

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new javax.crypto.spec.IvParameterSpec(IV.getBytes()));
            byte[] encoding = cipher.doFinal(s.getBytes());

            String crypted = DatatypeConverter.printBase64Binary(encoding);

            return crypted;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
            Logger.getLogger(Text.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public static String decrypt(String crypted, String cle) {
        try {
            byte[] ciphertext = DatatypeConverter.parseBase64Binary(crypted);
            byte[] key = cle.getBytes(); //Conversion de la clé en octets et création du vecteur d'initialisation
            String IV = "c!Q62Q4:";

            SecretKeySpec keySpec = new SecretKeySpec(key, "Blowfish");
            Cipher cipher = Cipher.getInstance("Blowfish/CBC/PKCS5Padding"); //Création de la clé et du Cipher utilisant Blowfish

            cipher.init(Cipher.DECRYPT_MODE, keySpec, new javax.crypto.spec.IvParameterSpec(IV.getBytes()));
            byte[] message = cipher.doFinal(ciphertext);

            return new String(message);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException ex) {
            return "Mot de passe erroné";
        }
    }
}
