package com.github.tL975IvfMhs4.security;

import com.github.tL975IvfMhs4.serveur.Server;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.Clock;

import static com.github.tL975IvfMhs4.constant.SecurityConstants.*;

public class SecurityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityService.class);

    private final Clock clock;

    public SecurityService(Clock clock) {
        this.clock = clock;
    }

    // Le dossier doit contenir
    //     - une CA
    //         - une clé CA.key (paire RSA/ECDSA)
    //         - un certificat de CA CA.crt valable 100 ans
    //     - le certificat serveur
    //         - une clé server.key (paire RSA/ECDSA)
    //         - un certificat avec le nom stable en SAN
    //         - un fichier p12 pour regrouper tout ça
    //     - un .mobileconfig pour ios
    public void loadCertificates() {
        createDirectoryIfNotExists();
        final SecurityBundle ca = loadCA();
        final SecurityBundle serveur = loadServerCertificates(ca);
        loadServerCertificateInKeyStore(serveur);
    }

    private void createDirectoryIfNotExists() {
        // Vérification de l’existence du dossier de certificats
        if (Files.notExists(CERTIFICATE_FOLDER_ABSOLUTE_PATH)) {
            LOGGER.info("Le dossier de certificats n’existe pas, on le crée : {}", CERTIFICATE_FOLDER_ABSOLUTE_PATH);
            try {
                Files.createDirectory(CERTIFICATE_FOLDER_ABSOLUTE_PATH);
                LOGGER.info("Dossier de certificats créé : {}", CERTIFICATE_FOLDER_ABSOLUTE_PATH);
            } catch (IOException e) {
                Server.crash(() -> LOGGER.error("Impossible de créer le dossier de certificats «{}», le serveur ne peut pas démarrer", CERTIFICATE_FOLDER_ABSOLUTE_PATH, e));
            }
        } else {
            LOGGER.info("Dossier de certificats trouvé : {}", CERTIFICATE_FOLDER_ABSOLUTE_PATH);
        }
    }

    private SecurityBundle loadCA() {
        // Vérifier l’existence du certificat et de la clé de la CA
        // Si un des deux est manquant, on vide tout le dossier de certificats et on le régénère
        if (Files.notExists(CA_CERTIFICATE_FILE_PATH) || Files.notExists(CA_KEY_FILE_PATH)) {
            LOGGER.info("Impossible de trouver la clé ou le certificat de la CA, on vide tout le dossier de certificat et on régénère tout (clé : {}, certificat : {})",
                CA_KEY_FILE_PATH, CA_CERTIFICATE_FILE_PATH);
            emptyCertificatesFolder();
            LOGGER.info("Génération d’une CA complète");
            try {
                return generateCA();
            } catch (TLSSecurityException e) {
                Server.crash(() -> LOGGER.error("Erreur de chargement des certificats -> génération de la CA impossible : {}", e.getMessage(), e));
            }
            LOGGER.info("CA générée, certificat écrit ici «{}» et clé privée ici «{}». Il ne faut JAMAIS partager la clé privée.", CA_CERTIFICATE_FILE_PATH, CA_KEY_FILE_PATH);
        } else {
            LOGGER.info("Clé de la CA trouvée : {}", CA_KEY_FILE_PATH);
            LOGGER.info("Certificat de la CA trouvé : {}", CA_CERTIFICATE_FILE_PATH);

            // TODO : lire les fichiers et les charger dans le bundle
            return null;
        }
        // Pour faire plaisir au compilateur qui ne voit pas que le catch dans le if mène à un arrêt de la jvm
        // Si on passe ici, on a un problème de logique
        LOGGER.warn("Passage dans une branche normalement inatteignable lors de la génération de la CA, attention aux fautes de logique dans le serveur");
        return null;
    }

    private void emptyCertificatesFolder() {
        try {
            Files.delete(CERTIFICATE_FOLDER_ABSOLUTE_PATH);
            Files.createDirectory(CERTIFICATE_FOLDER_ABSOLUTE_PATH);
            LOGGER.info("Dossier de certificats vidé et recréé");
        } catch (IOException e) {
            Server.crash(() -> LOGGER.error("Impossible de vider le dossier de certificats, le serveur ne peut pas démarrer. Il faut aller vérifier manuellement ce qui se passe dans ce dossier : {}", CERTIFICATE_FOLDER_ABSOLUTE_PATH,  e));
        }
    }

    // Excellente ressource 10/10 : https://docs.keyfactor.com/bouncycastle/latest/how-to-generate-certificates-and-crls
    private SecurityBundle generateCA() {
        // Utiliser le JcaPEMWriter pour clé et certificat : https://www.javathinking.com/blog/self-signed-x509-certificate-with-bouncy-castle-in-java/#step-6-convert-and-save-as-pem
        final KeyPair clesCA = SecurityGenerator.generateECDSAKeyPair();
        final X509Certificate certificatCA = SecurityGenerator.generateCACertificate(clesCA, clock);
        try(JcaPEMWriter pemWriter = new JcaPEMWriter(new FileWriter(CA_CERTIFICATE_FILE_PATH.toString()))) {
            pemWriter.writeObject(certificatCA);
        } catch (IOException e) {
            LOGGER.error("Écriture du certificat CA sur le disque impossible, on vide le dossier de certificats", e);
            emptyCertificatesFolder();
            throw new TLSSecurityException("", e);
        }
        try(JcaPEMWriter pemWriter = new JcaPEMWriter(new FileWriter(CA_KEY_FILE_PATH.toString()))) {
            pemWriter.writeObject(clesCA.getPrivate());
        } catch (IOException e) {
            LOGGER.error("Écriture de la clé privée CA sur le disque impossible, on vide le dossier de certificats", e);
            emptyCertificatesFolder();
            throw new TLSSecurityException("", e);
        }
        return new SecurityBundle(certificatCA, clesCA.getPrivate(), null);
    }

    private SecurityBundle loadServerCertificates(SecurityBundle ca) {
        // Vérifier l’existence du certificat, de la clé privée serveur, et du fichier p12
        // Si un des trois est manquant, on vide tout le dossier de certificats et on le régénère
        if (Files.notExists(SERVEUR_CERTIFICATE_FILE_PATH) || Files.notExists(SERVEUR_KEY_FILE_PATH) || Files.notExists(SERVEUR_P12_FILE_PATH)) {
            LOGGER.info("Impossible de trouver la clé, le certificat ou le p12 du serveur, on vide tout le dossier de certificat et on régénère tout (clé : {}, certificat : {}, p12 : {})",
                SERVEUR_CERTIFICATE_FILE_PATH, SERVEUR_KEY_FILE_PATH, SERVEUR_P12_FILE_PATH);
            emptyCertificatesFolder();
            LOGGER.info("Génération des certificats serveur");
            try {
                return generateServerCertificates(ca);
            } catch (TLSSecurityException e) {
                Server.crash(() -> LOGGER.error("Erreur de chargement des certificats -> génération des certificats serveur impossible : {}", e.getMessage(), e));
            }
            LOGGER.info("Certificats serveur générés, certificat écrit ici «{}», clé privée ici «{}», p12 ici «{}». Il ne faut JAMAIS partager la clé privée ou le p12.", SERVEUR_CERTIFICATE_FILE_PATH, SERVEUR_KEY_FILE_PATH, SERVEUR_P12_FILE_PATH);
        } else {
            LOGGER.info("Clé privée serveur trouvée : {}", SERVEUR_CERTIFICATE_FILE_PATH);
            LOGGER.info("Certificat serveur trouvé : {}", SERVEUR_KEY_FILE_PATH);
            LOGGER.info("P12 serveur trouvé : {}", SERVEUR_P12_FILE_PATH);

            // TODO : lire les fichiers et les charger dans le bundle
            return null;
        }
        // Pour faire plaisir au compilateur qui ne voit pas que le catch dans le if mène à un arrêt de la jvm
        // Si on passe ici, on a un problème de logique
        LOGGER.warn("Passage dans une branche normalement inatteignable lors de la génération de la CA, attention aux fautes de logique dans le serveur");
        return null;
    }

    // TODO : le .mobileconfig pour ios, et trouver un truc similaire pour android
    private SecurityBundle generateServerCertificates(SecurityBundle ca) {
        // Génération et écriture sur disque du certificat serveur + clés
        // Écrire le p12 correspondant pour faciliter le chargement (KeyStore.getInstance("PKCS12"))
        final KeyPair clesServeur = SecurityGenerator.generateECDSAKeyPair();
        final X509Certificate certificatServeur = SecurityGenerator.generateServerCertificate(clesServeur.getPublic(), ca.clePrivee(), clock, ca.certificat());
        try(JcaPEMWriter pemWriter = new JcaPEMWriter(new FileWriter(SERVEUR_CERTIFICATE_FILE_PATH.toString()))) {
            pemWriter.writeObject(certificatServeur);
        } catch (IOException e) {
            LOGGER.error("Écriture du certificat serveur sur le disque impossible, on vide le dossier de certificats", e);
            emptyCertificatesFolder();
            throw new TLSSecurityException("", e);
        }
        try(JcaPEMWriter pemWriter = new JcaPEMWriter(new FileWriter(SERVEUR_KEY_FILE_PATH.toString()))) {
            pemWriter.writeObject(clesServeur.getPrivate());
        } catch (IOException e) {
            LOGGER.error("Écriture de la clé privée serveur sur le disque impossible, on vide le dossier de certificats", e);
            emptyCertificatesFolder();
            throw new TLSSecurityException("", e);
        }
        try {
            final KeyStore keyStoreP12 = SecurityGenerator.generateP12KeyStore("pwas-server", ca.clePrivee(), "bépoè^vdljzw", ca.certificat());
            final ByteArrayOutputStream outputStream = SecurityGenerator.p12ToOutputStream(keyStoreP12, "bépoè^vdljzw");
            final byte[] p12 = outputStream.toByteArray();
            Files.write(SERVEUR_P12_FILE_PATH, p12);
            return new SecurityBundle(certificatServeur, clesServeur.getPrivate(), keyStoreP12);
        } catch (IOException e) {
            LOGGER.error("Écriture du p12 serveur sur le disque impossible, on vide le dossier de certificats", e);
            emptyCertificatesFolder();
            throw new TLSSecurityException("", e);
        }
    }

    private void loadServerCertificateInKeyStore(SecurityBundle serveur) {
        // TODO pour charger le p12 dans undertow, créer un SSLContext à partir d’un KeyManagerFactory, lui-même créé à partir d’un keystore qui lit le p12 et le déchiffre à l’aide du mot de passe
        //      le mot de passe du serveur pourra être écrit en dur dans le code parce qu’on s’en fout ? ou alors, on le demande au premier démarrage du serveur dans le terminal, et on l’enregistre chiffré dans un fichier, à voir
    }
}
