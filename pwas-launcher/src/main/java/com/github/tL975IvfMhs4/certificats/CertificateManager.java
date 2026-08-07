package com.github.tL975IvfMhs4.certificats;

public class CertificateManager {

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
        final String home = System.getProperty("user.home");
    }
}
