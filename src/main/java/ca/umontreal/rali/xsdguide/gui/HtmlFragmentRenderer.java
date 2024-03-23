/**
 * (c) RALI - Recherche appliquee en linguistique informatique.
 *     http://rali.iro.umontreal.ca
 */
package ca.umontreal.rali.xsdguide.gui;

import ca.umontreal.rali.xsdguide.guides.UIGuideNode;
import ca.umontreal.rali.xsdguide.instance.GuidedElement;

/**
 * Creates html fragments (not complete pages from an element). Relies on 
 * {@link HtmlRenderer}.
 * 
 * @author Fabrizio Gotti - gottif
 *
 */
public class HtmlFragmentRenderer extends Renderer {

    private HtmlRenderer renderer = new HtmlRenderer();
    
    @Override
    public String render(UIGuideNode node) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public String render(GuidedElement el) {
        StringBuilder result = new StringBuilder();
        renderer.toFragmentString(el.getGuideNode(), el.getValues(), result, true);
        setLastResult(result.toString());
        return getLastResult();
    }

}
