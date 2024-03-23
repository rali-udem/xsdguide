/**
 * (c) RALI - Recherche appliquee en linguistique informatique.
 *     http://rali.iro.umontreal.ca
 */
package ca.umontreal.rali.xsdguide.tools;

import java.io.File;

import javax.xml.XMLConstants;
import javax.xml.validation.SchemaFactory;

import org.apache.xerces.util.XMLCatalogResolver;
import org.apache.xerces.xs.XSImplementation;
import org.apache.xerces.xs.XSLoader;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

/**
 * @author Fabrizio Gotti - gottif
 *
 */
public class CatalogTools {

    private static SchemaFactory catalogBackedSchemaFactory;
    private static XMLCatalogResolver resolver;
    private static XSLoader loader;
    private static boolean initialized;

    public static void initializeCatalog(File catalogFile) {
        if (!catalogFile.canRead() || catalogFile.isDirectory()) {
            throw new IllegalArgumentException("Cannot read catalog " + catalogFile);
        }
        
        // resolver
        resolver = new XMLCatalogResolver(new String[] { catalogFile.toURI().toString() });
        
        // schema factory
        catalogBackedSchemaFactory  = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        catalogBackedSchemaFactory.setResourceResolver(resolver);
        
        // xsloader
        DOMImplementationRegistry registry;
        
        try {
            registry = org.w3c.dom.bootstrap.DOMImplementationRegistry.newInstance();
        } catch (ClassNotFoundException | InstantiationException
                | IllegalAccessException | ClassCastException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
        
        XSImplementation implementation = (XSImplementation) registry.getDOMImplementation("XS-Loader");
        loader = implementation.createXSLoader(null);
        loader.getConfig().setParameter("resource-resolver", resolver);
        
        // mark as done
        initialized = true;
    }
    
    public static SchemaFactory getSchemaFactory() {
        if (!initialized) {
            throw new IllegalStateException("Catalog tools not initialized.");
        }
        return catalogBackedSchemaFactory;
    }
    
    public static XSLoader getLoader() {
        if (!initialized) {
            throw new IllegalStateException("Catalog tools not initialized.");
        }
        return loader;
    }

    public static boolean isInitialized() {
        return initialized;
    }
    
}
