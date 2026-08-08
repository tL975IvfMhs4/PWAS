package com.github.tL975IvfMhs4.security;

import com.github.tL975IvfMhs4.serveur.Server;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.security.Security;

import static com.github.tL975IvfMhs4.constant.SecurityConstants.*;

public class SecurityService {

//    static {
//        Security.addProvider(new BouncyCastleProvider());
//    }

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityService.class);

    // Le dossier doit contenir
    //     - une CA
    //         - une clé CA.key (paire RSA/ECDSA)
    //         - un certificat de CA CA.crt valable 100 ans
    //     - le certificat serveur
    //         - une clé server.key (paire RSA/ECDSA)
    //         - un certificat avec le nom stable en SAN
    //         - un fichier p12 pour regrouper tout ça
    //     - un .mobileconfig pour ios
    //     - TODO trouver un truc similaire pour android ?
    public void manageCertificates() {
        createDirectoryIfNotExists();
        checkCA();
        checkServerCertificates();
    }

    private void createDirectoryIfNotExists() {
        // Vérification de l’existence du dossier de certificats
        if (Files.notExists(CERTIFICATE_FOLDER_ABSOLUTE_PATH)) {
            LOGGER.info("Le dossier de certificats n’existe pas, on le crée : {}", CERTIFICATE_FOLDER_ABSOLUTE_PATH);
            try {
                Files.createDirectory(CERTIFICATE_FOLDER_ABSOLUTE_PATH);
                LOGGER.info("Dossier de certificats créé : {}", CERTIFICATE_FOLDER_ABSOLUTE_PATH);
            } catch (IOException e) {
                Server.crash(() -> LOGGER.error("Impossible de créer le dossier de certificats «{}», le serveur ne peut pas fonctionner", CERTIFICATE_FOLDER_ABSOLUTE_PATH, e));
            }
        } else {
            LOGGER.info("Dossier de certificats trouvé : {}", CERTIFICATE_FOLDER_ABSOLUTE_PATH);
        }
    }

    private void checkCA() {
        // Vérifier l’existence du certificat et de la clé de la CA
        // Si un des deux est manquant, on vide tout le dossier de certificats et on le régénère
        if (Files.notExists(CA_CERTIFICATE_FILE_PATH) || Files.notExists(CA_KEY_FILE_PATH)) {
            LOGGER.info("Impossible de trouver la clé ou le certificat de la CA, on vide tout le dossier de certificat et on régénère tout (clé : {}, certificat : {})",
                CA_KEY_FILE_PATH, CA_CERTIFICATE_FILE_PATH);
            try {
                Files.delete(CERTIFICATE_FOLDER_ABSOLUTE_PATH);
                Files.createDirectory(CERTIFICATE_FOLDER_ABSOLUTE_PATH);
                LOGGER.info("Dossier de certificats vidé et recréé");
            } catch (IOException e) {
                Server.crash(() -> LOGGER.error("Impossible de vider le dossier de certificats «{}», le serveur ne peut pas fonctionner", CERTIFICATE_FOLDER_ABSOLUTE_PATH,  e));
            }
            generateCA();
        } else {
            LOGGER.info("Clé de la CA trouvée : {}", CA_KEY_FILE_PATH);
            LOGGER.info("Certificat de la CA trouvé : {}", CA_CERTIFICATE_FILE_PATH);
        }
    }

    // https://docs.keyfactor.com/bouncycastle/latest/how-to-generate-certificates-and-crls
    private void generateCA() {
        // TODO avec bouncy castle
    }

    private void checkServerCertificates() {
        // TODO avec bouncy castle
    }
}
