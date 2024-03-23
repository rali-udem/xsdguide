/**
 * (c) RALI - Recherche appliquee en linguistique informatique.
 *     http://rali.iro.umontreal.ca
 */
package ca.umontreal.rali.xsdguide.gui;

import ca.umontreal.rali.xsdguide.guides.UIGuideNode;
import ca.umontreal.rali.xsdguide.instance.GuidedElement;

/**
 * @author Fabrizio Gotti - gottif
 *
 */
public abstract class Renderer {

    private String lastResult;

    public abstract String render(UIGuideNode node);

    public String getLastResult() {
        return lastResult;
    }
    
    protected void setLastResult(String lastResult) {
        this.lastResult = lastResult;
    }

    public abstract String render(GuidedElement el);

}
