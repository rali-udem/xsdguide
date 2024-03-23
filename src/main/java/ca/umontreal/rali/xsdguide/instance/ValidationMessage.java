/**
 * (c) RALI - Recherche appliquee en linguistique informatique.
 *     http://rali.iro.umontreal.ca
 */
package ca.umontreal.rali.xsdguide.instance;

/**
 * @author Fabrizio Gotti - gottif
 *
 */
public class ValidationMessage {
    private String message = null;

    public ValidationMessage(String msg) {
        setMessage(msg);
    }
    
    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }
}
