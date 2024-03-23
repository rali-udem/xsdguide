/**
 * (c) RALI - Recherche appliquee en linguistique informatique.
 *     http://rali.iro.umontreal.ca
 */
package ca.umontreal.rali.xsdguide.demo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.google.common.io.Files;

/**
 * @author Fabrizio Gotti - gottif
 *
 */
public class ServerSchemaManager {

    public static final class SchemaDef {
        public String id;
        public String schemaPath;
        public String rootElName;
        
        public SchemaDef(String id, String schemaPath, String rootElName) {
            this.id = id;
            this.schemaPath = schemaPath;
            this.rootElName = rootElName;
        }

        public static SchemaDef parse(String line) {
            String[] args = line.split("\t");
            if (args.length != 3) {
                throw new IllegalArgumentException("Invalid line");
            }
            
            return new SchemaDef(args[0], args[1], args[2]);
        }
        
        @Override
        public String toString() {
            return id + "\t" + schemaPath + "\t" + rootElName;
        }
    }
    
    private static final String SCHEMADEFS_FILENAME = "schemadefs";
    private File schemaDirectory;
    private File schemaDefFile;
    private List<SchemaDef> schemaDefs = new ArrayList<>();
    
    public ServerSchemaManager(File schemaDirectory) throws IOException {
        if (!schemaDirectory.canWrite()) {
            throw new IllegalArgumentException("Invalid schema dir " + schemaDirectory);
        }
        
        this.schemaDirectory = schemaDirectory;
        this.schemaDefFile = new File(schemaDirectory, SCHEMADEFS_FILENAME);
        
        if (!schemaDefFile.exists()) {
            schemaDirectory.createNewFile();
        }
        
        loadSchemaDefinitions();
    }

    private void loadSchemaDefinitions() throws IOException {
        
        schemaDefs.clear();
        
        List<String> lines = Files.readLines(schemaDefFile, Charset.forName("UTF-8"));
        for (String line : lines) {
            if (!line.trim().isEmpty() && !line.trim().startsWith("#")) {
                try {
                    schemaDefs.add(SchemaDef.parse(line));
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
        }
        
    }
    
    /**
     * 
     * @return Sorted results (by string)
     * @throws IOException
     */
    public List<SchemaDef> getAvailableSchemas() throws IOException {
        loadSchemaDefinitions();
        
        List<SchemaDef> newList = new ArrayList<SchemaDef>();
        newList.addAll(schemaDefs);
        Collections.sort(newList, new Comparator<SchemaDef>() {

            @Override
            public int compare(SchemaDef o1, SchemaDef o2) {
                int result = o1.schemaPath.compareToIgnoreCase(o2.schemaPath);
                if (result == 0) {
                    result = o1.rootElName.compareToIgnoreCase(o2.rootElName);
                }
                
                return result;
            }
        });
        
        return newList;
    }

    public File getSchemaFile(String xsdName) {
        return new File(schemaDirectory, xsdName);
    }

    public SchemaDef installSchemaDir(File xsdDir, String xsdFileRelativePath, String rootElement) throws IOException {
        
        if (!xsdDir.isDirectory()) {
            throw new IllegalArgumentException("XSD source is not a directory.");
        }
        
        File xsdFilePath = new File(xsdDir, xsdFileRelativePath);
        if (!xsdFilePath.canRead()) {
            throw new IllegalArgumentException("Cannot find relative file " + xsdFileRelativePath + " in source dir");
        }
        
        File destDir = new File(schemaDirectory, xsdDir.getName());
        
/*        if (destDir.exists()) {
            FileUtils.deleteDirectory(destDir);
        }
*/        
        FileUtils.copyDirectory(xsdDir, destDir);
        SchemaDef newDef = new SchemaDef(nextId(), 
                                         destDir.getName() + File.separator + xsdFileRelativePath, 
                                         rootElement);
        writeSchemaDef(newDef);
        schemaDefs.add(newDef); 
        return newDef;
    }

    @Deprecated
    public void installSchema(File xsdSource, String xsdFile, String rootElement) throws IOException {

        SchemaDef newDef = null;
        
        if (xsdSource.isDirectory()) {
            FileUtils.copyDirectory(xsdSource, new File(schemaDirectory, xsdSource.getName()));
            newDef = new SchemaDef(nextId(), xsdFile, rootElement);
        } else {
            if (!xsdSource.getName().equals(xsdFile)) {
                throw new IllegalArgumentException("Incoherent single-file addition with " + 
                                                    xsdSource.getName() + " and " + xsdFile);
            }
            
            Files.copy(xsdSource, new File(schemaDirectory, xsdSource.getName()));
            newDef = new SchemaDef(nextId(), xsdFile, rootElement);
        }
        
        writeSchemaDef(newDef);
        schemaDefs.add(newDef);        
    }

    private void writeSchemaDef(SchemaDef newDef) throws IOException {
        synchronized (schemaDefFile) {
            Files.append(newDef.toString() + "\n", schemaDefFile, Charset.forName("UTF-8"));
        }
    }

    private String nextId() {
        return "id" + (int) (Math.random() * 100000);
    }
    
}
