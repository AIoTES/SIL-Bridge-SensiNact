package eu.interiot.translators.syntax.sensinact;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Files;

public class SensinactTranslatorDemonstration {

    public static void main(String[] args) throws Exception {

        File resourcesDirectory = new File("src/test/resources/sensinact");

        FilenameFilter filenameFilter = (dir, name) -> {
            String lowercaseName = name.toLowerCase();
            return lowercaseName.endsWith(".test");
        };

        File[] testFiles = resourcesDirectory.listFiles(filenameFilter);

        SensinactTranslator translator = new SensinactTranslator();

        for (File f : testFiles) {
            System.out.println("************************");
            System.out.println("+++ Input file: " + f.getAbsolutePath() + " +++");
            System.out.println();

            String fileContents = new String(Files.readAllBytes(f.toPath()));

            System.out.println(fileContents);

            System.out.println();
            System.out.println("+++ RDF output: +++");
            System.out.println();

            //Translate towards INTER-MW and apply transformers

            Model jenaModel = translator.toJenaModelTransformed(fileContents);

            System.out.println(translator.printJenaModel(jenaModel, Lang.N3));

            System.out.println();
            System.out.println("+++ Reverse translation: +++");

            //Reverse the translation
            String testString = translator.toFormatXTransformed(jenaModel);

            System.out.println();
            System.out.println(testString);
            System.out.println();
        }
    }
}
