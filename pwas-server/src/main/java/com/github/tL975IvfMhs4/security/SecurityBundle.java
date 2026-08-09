package com.github.tL975IvfMhs4.security;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public record SecurityBundle(X509Certificate certificat, PrivateKey clePrivee, KeyStore keyStoreP12) {}
