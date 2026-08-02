import fs from "node:fs";
import path from "node:path";

const [, , appName] = process.argv;

const appFolderName = appName.toLowerCase();

if (!appName) {
  console.error("Usage: npm run create <nom-app>");
  process.exit(1);
}

const root = process.cwd();

const templateDir = path.join(root, "apps", "schema");
const targetDir = path.join(root, "apps", appFolderName);
const angularJsonPath = path.join(root, "angular.json");

if (fs.existsSync(targetDir)) {
  console.error(`L’application "${appName}" existe déjà.`);
  process.exit(1);
}

if (!fs.existsSync(templateDir)) {
  console.error("Le schéma n’existe pas : apps/schema");
  process.exit(1);
}


// -------------------------
// Copie du template
// -------------------------

copyDirectory(templateDir, targetDir);


// -------------------------
// Remplacement des variables
// -------------------------

replaceInDirectory(targetDir, "SCHEMA_TITRE", appName);
replaceInDirectory(targetDir, "SCHEMA", appFolderName);


// -------------------------
// Modification angular.json
// -------------------------

addAngularProject(appFolderName);
console.log(`✔ Application créée : ${appName}`);


// =================================================
// Fonctions utilitaires
// =================================================

function copyDirectory(source, destination) {
  fs.mkdirSync(destination, { recursive: true });

  for (const file of fs.readdirSync(source)) {
    const sourcePath = path.join(source, file);
    const destinationPath = path.join(destination, file);

    const stat = fs.statSync(sourcePath);

    if (stat.isDirectory()) {
      copyDirectory(sourcePath, destinationPath);
    } else {
      fs.copyFileSync(sourcePath, destinationPath);
    }
  }
}


function replaceInDirectory(directory, search, replacement) {
  for (const file of fs.readdirSync(directory)) {
    const filePath = path.join(directory, file);

    const stat = fs.statSync(filePath);

    if (stat.isDirectory()) {
      replaceInDirectory(filePath, search, replacement);
      continue;
    }

    // On évite les fichiers binaires
    if (
      file.endsWith(".png") ||
      file.endsWith(".ico") ||
      file.endsWith(".jpg")
    ) {
      continue;
    }

    let content = fs.readFileSync(filePath, "utf8");

    if (content.includes(search)) {
      content = content.replaceAll(search, replacement);
      fs.writeFileSync(filePath, content);
    }
  }
}


function addAngularProject(name) {
  const confAngular = JSON.parse(fs.readFileSync(angularJsonPath, "utf8"));

  if (confAngular.projects[name]) throw new Error(`Le projet ${name} existe déjà dans angular.json`);

  const confSchema = JSON.parse(JSON.stringify(confAngular.projects.schema));
  replaceObjectValues(confSchema, "schema", name);
  confSchema.prefix = name;
  confAngular.projects[name] = confSchema;

  fs.writeFileSync(angularJsonPath, JSON.stringify(confAngular, null, 2) + "\n");
}


function replaceObjectValues(object, search, replacement) {
  for (const key of Object.keys(object)) {

    if (typeof object[key] === "string") {
      object[key] = object[key].replaceAll(search, replacement);
    }
    else if (typeof object[key] === "object" && object[key] !== null) {
      replaceObjectValues(object[key], search, replacement);
    }
  }
}
