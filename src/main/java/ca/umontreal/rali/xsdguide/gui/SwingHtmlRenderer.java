/**
 * (c) RALI - Recherche appliquee en linguistique informatique.
 *     http://rali.iro.umontreal.ca
 */
package ca.umontreal.rali.xsdguide.gui;

import javax.swing.JEditorPane;

import ca.umontreal.rali.xsdguide.guides.UIGuideNode;

/**
 * Renders a {@linkplain UIGuideNode} in Swing.
 * 
 * @author Fabrizio Gotti - gottif
 *
 */
public class SwingHtmlRenderer {
    
    Renderer renderer = new HtmlRenderer();
    
    public void render(UIGuideNode rootNode, JEditorPane htmlPane) {
        htmlPane.setText(renderer.render(rootNode));
    }

}
