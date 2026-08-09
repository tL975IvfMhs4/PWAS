package com.github.tL975IvfMhs4.security;

import com.github.tL975IvfMhs4.constant.DNSConstants;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Date;

import static com.github.tL975IvfMhs4.constant.DNSConstants.LOCAL_DNS_NAME;
import static com.github.tL975IvfMhs4.constant.SecurityConstants.*;

public final class SecurityGenerator {

    /**
     * Génération d’une paire de clés ECDSA P-256 (sha2)
     * @return
     */
    public static KeyPair generateECDSAKeyPair() {
        try {
            final KeyPairGenerator generateur = KeyPairGenerator.getInstance("EC", SECURITY_PROVIDER);
            generateur.initialize(new ECGenParameterSpec("secp256r1"));
            return generateur.generateKeyPair();
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new TLSSecurityException("Impossible de construire le générateur de clés de chiffrement ECDSA avec le provider BC (bouncy castle), le serveur ne peut pas continuer de fonctionner", e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new TLSSecurityException("Impossible d’initialiser le générateur ECDSA avec le curve secp256r1", e);
        }
    }

    /**
     * Génération d’un certificat de CA auto-signé.
     * @param clesECDSA
     * @param clock
     * @return
     */
    public static X509Certificate generateCACertificate(KeyPair clesECDSA, Clock clock) {
        // Le certificat CA est auto-signé donc la clé privée et la clé publique proviennent de la même paire
        return generateCertificate(clesECDSA.getPublic(), clesECDSA.getPrivate(), clock, null);
    }

    /**
     * Génération d’un certificat serveur X509 signé par le certificat de CA donné. La clé privée est celle de la CA.
     * @param cleECDSAPubliqueServeur
     * @param cleECDSAPriveeCA
     * @param clock
     * @param caCertificate
     * @return
     */
    public static X509Certificate generateServerCertificate(PublicKey cleECDSAPubliqueServeur, PrivateKey cleECDSAPriveeCA, Clock clock, X509Certificate caCertificate) {
        // Le certificat serveur est signé par la clé privée de la CA donc il faut faire la distinction
        return generateCertificate(cleECDSAPubliqueServeur, cleECDSAPriveeCA, clock, caCertificate);
    }

    public static KeyStore generateP12KeyStore(String alias, PrivateKey clePrivee, String mdp, X509Certificate... chaineCertification) {
        try {
            final char[] mdpTableau = mdp.toCharArray();
            final KeyStore keyStoreP12 = KeyStore.getInstance("PKCS12");
            keyStoreP12.setKeyEntry(alias, clePrivee, mdpTableau, chaineCertification);

            return keyStoreP12;
        } catch (KeyStoreException e) {
            throw new TLSSecurityException("Impossible de récupérer le keystore PKCS12", e);
        }
    }

    // Génère un fichier .p12 contenant le certificat et la clé privée donnés en paramètre.
    // Le fichier est retourné dans un stream au format binaire, pour laisser le choix sur la façon de consommer le fichier (chargement, écriture, ...)
    public static ByteArrayOutputStream p12ToOutputStream(KeyStore keyStoreP12, String mdp) {
        try {
            final char[] mdpTableau = mdp.toCharArray();
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            keyStoreP12.store(outputStream, mdpTableau);
            return outputStream;
        } catch (KeyStoreException e) {
            throw new TLSSecurityException("Le keystore PKCS12 n’a pas été initialisé", e);
        } catch (CertificateException e) {
            throw new TLSSecurityException("Un des certificats de la chaîne à persister est invalide, donc soit le serveur génère des mauvais certificats," +
                "soit le dossier de certificats contient des fichiers malformés ou corrompus, dans ce cas il est conseillé de vider le dossier et de supprimer le certificat enregistré sur le téléphone", e);
        } catch (IOException e) {
            throw new TLSSecurityException("Erreur IO pendant l’écriture du .p12", e);
        } catch (NoSuchAlgorithmException e) {
            throw new TLSSecurityException("Un des algorithmes utilisés est manquant (on utilise seulement ECDSA normalement), merci de vérifier que la distribution de java utilisée est correcte", e);
        }
    }

    /**
     * Génération d’un certificat X509 pour les valeurs données. Le certificat en paramètre est un certificat de CA, et sa présence indique qu’on s’en
     * servira pour signer un certificat serveur.<p>
     * Si on génère un certificat de CA (certificat en paramètre null), il sera auto-signé donc la clé privée et la clé publique doivent provenir de la même paire.<p>
     * Sinon, la clé publique sera celle du serveur, et la clé privée celle de la CA.<p>
     * Les clés DOIVENT être générées pour l’algo ECDSA P-256 (sha2). Il y aura une exception si la clé est autre (par exemple RSA).
     * @param cleECDSAPublique Clé publique du certificat à générer.
     * @param cleECDSAPrivee Clé privée liée à la clé publique si on doit générer une CA, clé privée de la CA sinon
     * @param clock
     * @param caCertificate Éventuel certificat de la CA si on doit générer et signer un certificat serveur.
     * @return
     */
    private static X509Certificate generateCertificate(PublicKey cleECDSAPublique, PrivateKey cleECDSAPrivee, Clock clock, X509Certificate caCertificate) {

        final boolean generatingCA = caCertificate == null;

        // --------------------------------------
        // DONNÉES DE BASE DU CERTIFICAT
        // --------------------------------------

        // Sujet du certificat, on met un peu ce qu’on veut sauf pour le CN où on met le nom de domaine avec CA pour que la chose soit bien claire (rien pour le serveur)
        final X500Name sujet = new X500NameBuilder()
            .addRDN(BCStyle.C, "FR")
            .addRDN(BCStyle.L, "Maison")
            .addRDN(BCStyle.O, "PWAS")
            .addRDN(BCStyle.CN, LOCAL_DNS_NAME + (generatingCA ? " CA" : ""))
            .build();

        // On utilise le timestamp comme numéro de série pour éviter tout risque de conflit si on doit régénérer les certificats plus tard
        final BigInteger serialNumber = BigInteger.valueOf(clock.millis());

        // Le certificat commence sa période de validité 10 secondes avant le démarrage du serveur, et périme dans 100 ans
        final Date notBefore = Date.from(clock.instant().minusSeconds(10));
        final Date notAfter = Date.from(OffsetDateTime.now(clock).plusYears(100).toInstant());

        // Si on construit un certificat de CA, il est auto-signé donc le sujet est en premier paramètre à la place de l’issuer
        // Si on construit un certificat serveur, on doit le signer avec le certificat de la CA
        final JcaX509v3CertificateBuilder certificateBuilder;
        if (generatingCA) {
            certificateBuilder = new JcaX509v3CertificateBuilder(
                sujet,
                serialNumber,
                notBefore,
                notAfter,
                sujet,
                cleECDSAPublique
            );
        } else {
            certificateBuilder = new JcaX509v3CertificateBuilder(
                caCertificate,
                serialNumber,
                notBefore,
                notAfter,
                sujet,
                cleECDSAPublique
            );
        }


        // --------------------------------------
        // EXTENSIONS
        // --------------------------------------

        // On rajoute des SAN pour les normes de certificat récentes
        // Pour l’instant on a que le nom stable local
        // Attention à ne pas mettre localhost ou 127.0.0.1 (ou toute IP locale), puisqu’à terme
        // la CA sera importée sur un téléphone donc localhost y sera une faiblesse
        final GeneralNames subjectAltNames = new GeneralNames(new GeneralName[]{
            new GeneralName(GeneralName.dNSName, DNSConstants.LOCAL_DNS_NAME)
        });
        try {
            certificateBuilder.addExtension(Extension.subjectAlternativeName, false, subjectAltNames);
        } catch (CertIOException e) {
            throw new TLSSecurityException("Impossible de rajouter un SAN dans le certificat", e);
        }

        // Si on est une CA, on a le droit de signer des certificats et des CRL (listes noires)
        // Si on est le serveur, on a le droit de signer la clé de chiffrement symétrique, rien de plus (on utilise des clés ECDSA donc pas besoin de keyEncipherment comme pour RSA)
        try {
            certificateBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(generatingCA ? (KeyUsage.cRLSign | KeyUsage.keyCertSign) : KeyUsage.digitalSignature));
        } catch (CertIOException e) {
            throw new TLSSecurityException("Impossible de renseigner les usages de clé dans le certificat", e);
        }

        // Pas de extendedKeyUsage pour la CA
        // Le certificat serveur utilisera serverAuth
        if (!generatingCA) {
            try {
                certificateBuilder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth));
            } catch (CertIOException e) {
                throw new TLSSecurityException("Impossible de renseigner l’usage étendu de clé serverAuth dans le certificat");
            }
        }

        // Basic constraints : on génère une CA donc CA:true, ou alors false pour le serveur
        // Dans tous les cas, on garde l’extension maximum à sa valeur par défaut 0, ainsi une éventuelle CA signée par cette CA ne pourra pas signer d’autres certificats
        try {
            certificateBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(generatingCA));
        } catch (CertIOException e) {
            throw new TLSSecurityException("Impossible de renseigner les contraintes de base du certificat", e);
        }

        // Signature du certificat, on utilise ECDSA P-256
        try {
            final ContentSigner contentSigner = new JcaContentSignerBuilder(CA_KEY_ALGORITHM).setProvider(SECURITY_PROVIDER).build(cleECDSAPrivee);

            final X509CertificateHolder holder = certificateBuilder.build(contentSigner);
            final X509Certificate certificat = new JcaX509CertificateConverter().setProvider(SECURITY_PROVIDER).getCertificate(holder);

            certificat.checkValidity();
            certificat.verify(cleECDSAPublique);

            return certificat;

        } catch (OperatorCreationException e) {
            throw new TLSSecurityException("Impossible de générer la signature du certificat", e);
        } catch (CertificateExpiredException e) {
            throw new TLSSecurityException("Échec de validation du certificat généré : déjà expiré", e);
        } catch (CertificateNotYetValidException e) {
            throw new TLSSecurityException("Échec de validation du certificat généré : pas encore rentré dans sa période de validité qui aurait dû commencer", e);
        } catch (CertificateException e) {
            throw new TLSSecurityException("Impossible de générer le X509Certificate depuis le holder", e);
        } catch (NoSuchAlgorithmException e) {
            throw new TLSSecurityException("Échec de validation du certificat généré : algorithme de la signature invalide", e);
        } catch (SignatureException e) {
            throw new TLSSecurityException("Échec de validation du certificat généré : erreur de signature", e);
        } catch (InvalidKeyException e) {
            throw new TLSSecurityException("Échec de validation du certificat généré : clé invalide", e);
        } catch (NoSuchProviderException e) {
            throw new TLSSecurityException("Échec de validation du certificat généré : le provider bouncycastle n’a pas été trouvé", e);
        }
    }
}
