/**
 * (c) RALI - Recherche appliquee en linguistique informatique.
 *     http://rali.iro.umontreal.ca
 */
package ca.umontreal.rali.xsdguide.instance;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ca.umontreal.rali.xsdguide.gui.Renderer;
import ca.umontreal.rali.xsdguide.guides.GuideNodeException;
import ca.umontreal.rali.xsdguide.guides.UIGuide;
import ca.umontreal.rali.xsdguide.guides.UIGuideNode;
import ca.umontreal.rali.xsdguide.tools.CatalogTools;



/**
 * @author Fabrizio Gotti - gottif
 *
 */
public class GuidedXmlDoc {
    
    private UIGuide uiGuide;
    private GuidedElement rootElement;
    private Validator validator;
    private Document xmlDoc;
    private Map<String, GuidedElement> elementMap = new HashMap<>();
    public static final String IJIS_ISESAR_1_1_ROOTEL = "SuspiciousActivityReport";
    
    public static final String HTTP_IJIS_ISESAR_1_1_NS = "http://ijis-isesar/1.1";

    public GuidedXmlDoc(File schemaFile, QName rootElementName) throws GuideNodeException {
        
        if (rootElementName == null) {
            throw new IllegalArgumentException("Root element name must be specified");
        }

        try {
            uiGuide = new UIGuide(schemaFile);

            // schema 
            SchemaFactory schemaFactory = null;
            
            if (CatalogTools.isInitialized()) {
                schemaFactory = CatalogTools.getSchemaFactory();
            } else {
                schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            }
            
            Schema schema = schemaFactory.newSchema(new StreamSource(schemaFile));
            
            validator = schema.newValidator();
            validator.setErrorHandler(new InstanceErrorHandler());
            
            // document
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            xmlDoc = builder.newDocument();
            
            rootElement = createElement(xmlDoc, null, rootElementName);
            
            rootElement.getXmlElement().setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            setSchemaLocation(schemaFile.getName());
            
        } catch (ClassNotFoundException | InstantiationException
                | IllegalAccessException | ClassCastException | SAXException
                | ParserConfigurationException e) {
            throw new GuideNodeException("Could not build guided xml doc "
                    + e.getMessage());
        }
        
    }

    public void setSchemaLocation(String uri) {
        rootElement.getXmlElement().setAttributeNS(
                "http://www.w3.org/2001/XMLSchema-instance",
                "xsi:schemaLocation",
                rootElement.getQName().getNamespaceURI() + " " + uri);
    }

    private GuidedElement createElement(Node xmlParent, Node previousSibling, 
            QName elName) 
            throws GuideNodeException {
        
        UIGuideNode guideNode = uiGuide.getGuideNodeForElementName(elName);
        return createElement(xmlParent, previousSibling, elName, guideNode);
    }

    /**
     * @param xmlParent
     * @param elName
     * @param nextSibling If null, means the last child.
     * @param guideNode
     * @return
     */
    private GuidedElement createElement(Node xmlParent, Node nextSibling, 
            QName elName, UIGuideNode guideNode) {
        Element el = xmlDoc.createElementNS(elName.getNamespaceURI(),
                                            elName.getLocalPart());
        
        if (nextSibling == null) {
            xmlParent.appendChild(el);
        } else {
            xmlParent.insertBefore(el, nextSibling);
        }
        
        GuidedElement element = new GuidedElement(guideNode, el);
        
        elementMap.put(element.getId(), element);

        return element;
    }
    
    public List<ValidationMessage> validate() throws SAXException, IOException {
        ForgivingErrorHandler lenient = new ForgivingErrorHandler();
        validator.setErrorHandler(lenient);
        
        DOMResult validationResult = new DOMResult();
        validator.validate(new DOMSource(xmlDoc), validationResult);
        
        List<ValidationMessage> result = new ArrayList<ValidationMessage>();
        final List<String> validationMessages = lenient.getValidationMessages();
        for (String validationMessage : validationMessages) {
            result.add(new ValidationMessage(validationMessage));
        }
        
        return result;
    }
    
    public void save(OutputStream outStream) throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        Result output = new StreamResult(outStream);
        Source input = new DOMSource(xmlDoc);

        transformer.transform(input, output);
    }

    /**
     * Returns the id of the root element.
     */
    public String getRootElementId() {
        return rootElement.getId();
    }

    public void render(String elementId, Renderer renderer) {
        
        GuidedElement el = elementMap.get(elementId);
        renderer.render(el);
        
    }

    /**
     * Creates a new element.
     * @param parentId The element id of the parent.
     * @param elName The fully qualified id of the element to be created.
     * @return The id of the newly created element.
     * @throws GuideNodeException 
     */
    public String createElement(String parentId, QName elName) throws GuideNodeException {
        
        return createElement(parentId, elName, "last");
        
    }

    /**
     * 
     * @param parentElId
     * @param valueOf
     * @param nextSiblingId The new element will be inserted before the element
     *                      whose id is specified. To insert first, use special value
     *                      "first". To have the schema find the proper place, use
     *                      "last".
     * TODO: The 'last' option guesses the proper place. Can we improve this?   
     * @return
     * @throws GuideNodeException 
     */
    public String createElement(String parentId, QName elName, 
                                String nextSiblingId) throws GuideNodeException {
        
        GuidedElement parentGuidedEl = getGuidedElementById(parentId);
        UIGuideNode elGuideNode = uiGuide.getCompleteGuideNode(parentGuidedEl.getGuideNode().getSubElement(elName.getNamespaceURI(), elName.getLocalPart()));
        
        // now find sibling node
        Node nextSibling = null;
        if (nextSiblingId.equalsIgnoreCase("last")) {
            // this is an educated guess, unfortunately
            nextSibling = guessNextSibling(elName, parentGuidedEl);
            
        } else if (nextSiblingId.equalsIgnoreCase("first")) {
            nextSibling = parentGuidedEl.getXmlElement().getFirstChild();
        } else {
            nextSibling = getGuidedElementById(nextSiblingId).getXmlElement();
        }
        
        GuidedElement guidedEl = createElement(parentGuidedEl.getXmlElement(), nextSibling, elName, elGuideNode);
        
        return guidedEl.getId();        
    }

    /**
     * @param elName
     * @param parentGuidedEl
     * @param nextSibling
     * @return <code>null</code> if not found.
     */
    private Element guessNextSibling(QName elName, GuidedElement parentGuidedEl) {
        Element result = null;
        
        final UIGuideNode parentGuideNode = parentGuidedEl.getGuideNode();
        List<UIGuideNode> descendantElements = parentGuideNode.getSubElements();
        
        // what is the position of the new node in this list of descendants?
        int newNodePos = -1;
        for (int i = 0; i < descendantElements.size() && newNodePos < 0; ++i) {
            if (descendantElements.get(i).getQName().equals(elName)) {
                newNodePos = i;
            }
        }
        
        if (newNodePos >= 0) {
            // among the following possible descendants, what is the first
            // one for which we have an actual instance?
            int nextActualSiblingPos = newNodePos + 1;
            Element nextActualSibling = null;
            boolean siblingFound = false;
            for (; nextActualSiblingPos < descendantElements.size() && !siblingFound; ++nextActualSiblingPos) {
                QName nextSiblingName = descendantElements.get(nextActualSiblingPos).getQName();
                NodeList actualSiblingList = parentGuidedEl.getXmlElement().getElementsByTagNameNS(nextSiblingName.getNamespaceURI(), nextSiblingName.getLocalPart());
                if (actualSiblingList.getLength() != 0) {
                    // got it!
                    siblingFound = true;
                    nextActualSibling = (Element) actualSiblingList.item(0);
                }
            }
            
            if (siblingFound) {
                result = nextActualSibling;
            }
            
        }
        
        return result;
    }

    /**
     * @param elementId
     * @return
     */
    private GuidedElement getGuidedElementById(String elementId) {
        if (!elementMap.containsKey(elementId)) {
            throw new IllegalArgumentException("Parent id is invalid, for " + elementId);
        }
        
        GuidedElement guidedElement = elementMap.get(elementId);
        return guidedElement;
    }
    
    /**
     * Sets values for a given element.
     * @param id The id of the element
     * @param valueSet
     */
    public void setValues(String id, ValueSet valueSet) {
        if (valueSet == null) {
            throw new IllegalArgumentException("Value set cannot be null");
        }
        
        GuidedElement guidedElement = getGuidedElementById(id);
        guidedElement.setValues(valueSet);
    }
    
    @Override
    public String toString() {
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        String result = null;
        try {
            save(outputStream);
            outputStream.close();
        } catch (Exception e) {
            result = e.getMessage();
        }
        
        if (result == null) {
            result = new String(outputStream.toByteArray(), Charset.forName("UTF-8"));
        }
        
        return result;
    }

    public void setElementTextValue( String inputId, String value) {
        GuidedElement guidedElement = getGuidedElementById(inputId);
        guidedElement.setSingleValue(value);
    }

    public void removeElement(String elementId) {
        GuidedElement guidedElement = getGuidedElementById(elementId);
        guidedElement.seppuku();
        elementMap.remove(elementId);
    }

    public void removeAttribute(String parentElementId, String attributeId) {
        GuidedElement parentElement = getGuidedElementById(parentElementId);
        QName attQname = parentElement.getGuideNode().getActualContentName(attributeId);        
        parentElement.removeAttribute(attQname);
    }

    public void setAttributeValue(String parentElementId, String attributeId,
            String value) {
        
        GuidedElement parentElement = getGuidedElementById(parentElementId);
        QName attQname = parentElement.getGuideNode().getActualContentName(attributeId);
        parentElement.setAttributeValue(attQname, value);

        
    }

}
