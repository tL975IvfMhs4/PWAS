package com.github.tL975IvfMhs4.constant;

import java.nio.file.Path;

import static com.github.tL975IvfMhs4.constant.ServerConstants.SERVER_FOLDER_ABSOLUTE;

public final class CertificateConstants {

    /**
     * Dossier des certificats tls du serveur. Il est recommandé de nettoyer ces certificats le moins souvent possible
     * puisqu’ils seront importés sur les mobiles pour faire fonctionner les PWA, donc un changement de certificat
     * invaliderait intégralement la PWA et forcerait sa réinstallation.
     */
    public static final String CERTIFICATE_FOLDER = "certificats";

    public static final Path CERTIFICATE_FOLDER_ABSOLUTE_PATH = Path.of(SERVER_FOLDER_ABSOLUTE, CERTIFICATE_FOLDER);

    public static final String EXTENSION_CERTIFICATE = ".crt";
    public static final String EXTENSION_KEY = ".key";
    public static final String EXTENSION_P12 = ".p12";
    public static final String EXTENSION_MOBILECONFIG = ".mobileconfig";

    /**
     * Nom des fichiers de la CA (clé + certificat)
     */
    public static final String NOM_FICHIERS_CA = "CA";

    public static final String CA_CERTIFICATE_FILE_NAME = NOM_FICHIERS_CA + EXTENSION_CERTIFICATE;

    public static final Path CA_CERTIFICATE_FILE_PATH = Path.of(CERTIFICATE_FOLDER_ABSOLUTE_PATH.toString(), CA_CERTIFICATE_FILE_NAME);

    public static final String CA_KEY_FILE_NAME = NOM_FICHIERS_CA + EXTENSION_KEY;

    public static final Path CA_KEY_FILE_PATH = Path.of(CERTIFICATE_FOLDER_ABSOLUTE_PATH.toString(), CA_KEY_FILE_NAME);

    /**
     * Nom des fichiers du serveur (clé, certificat, p12, mobileconfig)
     */
    public static final String NOMS_FICHIERS_SERVEUR = "server";

    public static final String SERVEUR_CERTIFICATE_FILE_NAME = NOMS_FICHIERS_SERVEUR + EXTENSION_CERTIFICATE;

    public static final Path SERVEUR_CERTIFICATE_FILE_PATH = Path.of(CERTIFICATE_FOLDER_ABSOLUTE_PATH.toString(), SERVEUR_CERTIFICATE_FILE_NAME);


    public static final String SERVEUR_KEY_FILE_NAME = NOMS_FICHIERS_SERVEUR + EXTENSION_KEY;

    public static final Path SERVEUR_KEY_FILE_PATH = Path.of(CERTIFICATE_FOLDER_ABSOLUTE_PATH.toString(), SERVEUR_KEY_FILE_NAME);


    public static final String SERVEUR_P12_FILE_NAME = NOMS_FICHIERS_SERVEUR + EXTENSION_P12;
}
