/**
 * (c) RALI - Recherche appliquee en linguistique informatique.
 *     http://rali.iro.umontreal.ca
 */
package ca.umontreal.rali.xsdguide;

import java.io.File;

import javax.xml.namespace.QName;

import ca.umontreal.rali.xsdguide.gui.HtmlRenderer;
import ca.umontreal.rali.xsdguide.guides.UIGuide;
import ca.umontreal.rali.xsdguide.tools.XsdTools;

/**
 * This entry point allows one to load an XML Schema file and produce
 * corresponding html files.
 * 
 * @author Fabrizio Gotti - gottif
 *
 */
public class FGenerateHtmlForms {

    /**
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
        
        if (args.length != 3) {
            System.err.println("Usage: prog schemafile.xsd rootElementQName outputdir");
            System.exit(1);
        }

        int argNum = 0;
        final String schemaFileName = args[argNum++];
        final String rootElementQName = args[argNum++];
        final String outDirName = args[argNum++];
        
        QName qualifiedName = XsdTools.parseQName(rootElementQName);
        
        File schemaFile = new File(schemaFileName);
        File outDir = new File(outDirName);
        
        if (!outDir.exists()) {
            System.err.println("Creating " + outDir);
            outDir.mkdir();
        }
        
        HtmlRenderer renderer = new HtmlRenderer();
        System.err.print("Loading schema... ");
        UIGuide guide = new UIGuide(schemaFile);
        System.err.println("done.");
        
        renderer.renderAll(guide, qualifiedName, outDir);
       
        System.err.println("Complete.");
    }

}
