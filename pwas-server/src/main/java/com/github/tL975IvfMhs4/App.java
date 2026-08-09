package com.github.tL975IvfMhs4;

import com.github.tL975IvfMhs4.serveur.Server;

import java.time.Clock;

/**
 * Hello world!
 */
public class App {
    static void main(String[] args) {
        final Clock clock = Clock.systemUTC();
        Server.run(clock);
    }
}
