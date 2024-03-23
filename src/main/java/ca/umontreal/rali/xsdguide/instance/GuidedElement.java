/**
 * (c) RALI - Recherche appliquee en linguistique informatique.
 *     http://rali.iro.umontreal.ca
 */
package ca.umontreal.rali.xsdguide.instance;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import ca.umontreal.rali.xsdguide.guides.UIGuideNode;


/**
 * @author Fabrizio Gotti - gottif
 *
 */
public class GuidedElement {
    
    private String id = null;
    private UIGuideNode guideNode;
    private Element element;
    
    private static int idCounter = 0;

    public GuidedElement(UIGuideNode guideNode, Element el) {
        this.guideNode = guideNode;
        this.element = el;
        this.id = generateId();
    }

    private synchronized String generateId() {
        return "id_" + ++idCounter;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    public UIGuideNode getGuideNode() {
        return guideNode;
    }

    public Element getXmlElement() {
        return element;
    }

    public void setValues(ValueSet valueSet) {
        
        for (String key : valueSet.getKeys()) {
            String value = valueSet.getValue(key);
            
            if (!value.isEmpty()) {
                QName actualName = null;
                
                if (key.startsWith("{")) {
                    actualName = ValueSet.string2NS(key);
                } else {
                    actualName = guideNode.getActualContentName(key);
                }
                
                switch (guideNode.getContentType(key)) {
                case ATTRIBUTE:
                    element.setAttributeNS(actualName.getNamespaceURI(), actualName.getLocalPart(), value);
                    break;
                case TEXT_CONTENT:
                    throw new RuntimeException("text content is deprecated");
                    //element.setTextContent(value);
                    //break;
                case ELEMENT:
                    System.err.println("Set value for element not implemented");
                    break;
                default:
                    break;
                }
            }
        }
        
    }
    
    public void setSingleValue(String value) {
        element.setTextContent(value);
    }

    public QName getQName() {
        return guideNode.getQName();
    }

    public ValueSet getValues() {
        // TODO: incomplete, this is a sad, sad kludge
        ValueSet result = new ValueSet();
        result.setValue("id", getId());
        return result;
    }

    public void setAttributeValue(QName attQname, String value) {
        element.setAttributeNS(attQname.getNamespaceURI(), attQname.getLocalPart(), value);
    }

    public void seppuku() {
        element.getParentNode().removeChild(element);
    }

    public void removeAttribute(QName attQname) {
        element.removeAttributeNS(attQname.getNamespaceURI(), attQname.getLocalPart());
    }

}
