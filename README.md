# PWAS

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 22.1.2.

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Vitest](https://vitest.dev/) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.


## Trucs à faire, idées en vrac

### Apps
Toutes les PWA à servir, schema est le dossier contenant l’appli blanche. Utiliser la commande `npm run create <app>` pour
dupliquer le dossier schema avec le bon nom, et tout de renseigné correctement à partir du nom de l’appli.

### Build
Les scripts et ressources pour construire l’appli finale. Toutes les pwa seront construites dans un dossier dist, puis deux choses.
Le jar ira voir s’il a les pwa en interne et dans ce cas il les servira (release), sinon il ira servir le contenu de dist (dev).

Génère un petit fichier manifeste consommé par le index.html pour lister toutes les applis..

À voir si on met le index.html ici ou dans launcher, probablement ici.

À terme on aura une commande build qui génère le contenu du dist et le manifeste, et une commande release qui ira construire
un gros jar intégrant le contenu du dist, le index.html et compagnie en interne.

### Launcher
Contient le jar qui va servir les pwa avec undertow, et le index.html. Les certificats pour undertow seront générés une première fois
si on ne les trouve pas dans un dossier quelconque (par exemple C:\ProgramData\PWAS\certificats) puis conservés, pour ne pas avoir à les
regénérer et donc les réinstaller systématiquement sur le mobile. Au démarrage, le jar ouvre le navigateur par défaut sur index.html.

Il faudra exposer deux endpoints supplémentaires reliés chacun à un bouton dans le index.html, un «Arrêter le serveur»
pour arrêter le jar, et un autre «Supprimer les certificats serveur» qui ira nettoyer les certificats (supprimer le dossier
C:\ProgramData\PWAS\certificats).

Il faudra également que ce soit le jar qui construise les codes qr (un par pwa, et un pour le certificat .crt).
Soit on les construit au démarrage mais il faut les placer quelque part (et donc potentiellement les supprimer à l’arrêt),
soit on les sert directement sur /qr?url=... qui est mis dans l’attribut src d’une balise img, ce qui implique
un endpoint supplémentaire. On va probablement partir sur la deuxième solution.

### Dist
Il contiendra tout le build des pwas et le index.html, et sera servi par le jar de dev (build) et inséré dans le jar de release.
Structure de dossiers potentielle :
```
dist/
├── index.html
├── apps.json
│
├── courses/
│   ├── index.html
│   ├── manifest.webmanifest
│   └── assets/
│
└── echeances/
    ├── index.html
    ├── manifest.webmanifest
    └── assets/
```


## Découpage (un peu en vrac)

- mise en place du jar avec le pom.xml, les dépendances et la structure de dossier requises
- écrire le contenu du jar
  - génération des certificats au premier démarrage (placer dans C:\ProgramData\PWAS\certificats)
  - découverte de l’ip
  - vérifier si on a un dist interne à servir, sinon se rabattre sur ./dist/ ou ../dist/ (dossier exact à déterminer quand on y sera)
  - générer les url pour undertow ?
  - écrire les 3 endpoints
    - nettoyage des certificats
    - arrêt du serveur
    - génération de code qr
- écriture du index.html
  - partir sur un premier fichier apps.json factice
  - afficher le code qr du certificat
  - afficher un code qr par pwa
  - documenter la marche à suivre (pour enregistrer certificat et pwa sur le téléphone)
  - les boutons
- scripts js
  - build
    - construction du dist avec la structure donnée plus haut
  - package?
    - build
    - mvn package pour un petit jar de dev
  - release
    - build
    - construction du gros jar de release contenant le dist, donc il faudra certainement rajouter une partie de copie dans le pom.xml
- enfin, passer aux pwas elles-mêmes
  - installer @angular/pwa et voir la conf des service workers
  - installer ionic
  - dev les pwa
