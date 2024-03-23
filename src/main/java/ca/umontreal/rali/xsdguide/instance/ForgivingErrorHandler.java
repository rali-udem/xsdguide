/**
 * (c) RALI - Recherche appliquee en linguistique informatique.
 *     http://rali.iro.umontreal.ca
 */
package ca.umontreal.rali.xsdguide.instance;

/**
 * Adapted from http://www.ibm.com/developerworks/library/x-javaxmlvalidapi/
 * 
 * @author Fabrizio Gotti - gottif
 *
 */
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class ForgivingErrorHandler implements ErrorHandler {
    
    private List<String> errMsgs = new ArrayList<String>();

    @Override
    public void warning(SAXParseException ex) {
        errMsgs.add(ex.getMessage());
    }

    @Override
    public void error(SAXParseException ex) {
        errMsgs.add(ex.getMessage());
    }

    @Override
    public void fatalError(SAXParseException ex) throws SAXException {
        errMsgs.add(ex.getMessage());
    }
    
    public List<String> getValidationMessages() {
        return Collections.unmodifiableList(errMsgs);
    }
    
    public void clearMessages() {
        errMsgs.clear();
    }

}