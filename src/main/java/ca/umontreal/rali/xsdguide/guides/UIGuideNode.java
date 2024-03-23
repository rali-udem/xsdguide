/**
 * (c) RALI - Recherche appliquee en linguistique informatique.
 *     http://rali.iro.umontreal.ca
 */
package ca.umontreal.rali.xsdguide.guides;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.xerces.xs.XSConstants;
import org.apache.xerces.xs.XSSimpleTypeDefinition;

import ca.umontreal.rali.xsdguide.gui.HtmlRenderer;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;


/**
 * A UI guide node is a ui element associated with some element of an XML
 * schema. It may be mapped to an element, an attribute, a choice, etc. and
 * should be rendered one way or another in order to build a UI linked
 * to a given schema. This class should be decoupled from the rendering logic.
 * <p/>
 * In the future, this should be turned into a shallow class hierarchy.
 * 
 * @author Fabrizio Gotti - gottif
 *
 */
public class UIGuideNode {

    public static enum WidgetType {
        UNKNOWN,
        TEXT_TYPE,
        ELEMENT_TYPE, 
        SUB_ELEMENT_LINK, // a link to a sub element, possibly rendered as a button 
        SELECT, // a drop-down select  
        CHOICE, // a choice between children
        DATE, 
        BOOLEAN, 
        DATE_TIME,
    }
    
    public static enum XmlDomContentType {
        ATTRIBUTE,
        TEXT_CONTENT,
        ELEMENT,
    }

    private static final int INDENT_SIZE = 4;
    public static final int MAX_OCCURS_UNBOUNDED = Integer.MAX_VALUE;
    
    private String name;
    private List<String> helpStrings = new ArrayList<String>();
    private String namespace;
    private List<UIGuideNode> childNodes = new ArrayList<UIGuideNode>();
    private WidgetType widgetType = WidgetType.UNKNOWN;
    private List<String> possibleValues;
    private List<String> possibleValueNames;
    private String label;
    private String validatingRegexp;
    private int minOccurs = 0;
    private int maxOccurs = 1;
    private String id = null;
    private XmlDomContentType xmlDomContentType;
    private int minValue;
    private boolean minValueInclusive;
    private int maxValue;
    private boolean maxValueInclusive;
    private int maxNbFractionDigits;
    private String typeNamespace;
    private String typeName;
    private short xmlSchemaBuiltinKind = XSConstants.STRING_DT;
    private QName parentQName;
    private short facets = 0;

    public UIGuideNode(String namespace, String name, XmlDomContentType xmlDomContentType) {
        setNamespace(namespace);
        setName(name);
        this.xmlDomContentType = xmlDomContentType;
        setId();
    }
    
    public UIGuideNode(WidgetType type) {
        setName(type.toString());
        setNamespace("");
        widgetType = type;
        setId();
    }

    private void setId() {
        String result = "id_";
        
        result += getNamespace() == null ? "0" : Integer.toHexString(getNamespace().hashCode());
        
        result += "_" + Integer.toHexString(getName().hashCode());
        
        if (getContentType() == XmlDomContentType.ATTRIBUTE) {
            result = HtmlRenderer.XMLATTRIBUTE_PREFIX + result;
        }
        
        id = result;
    }
    
    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    public void addHelpText(String helpString) {
        helpStrings.add(helpString);
    }

    /**
     * Returns a help string, or an empty string if no help available.
     */
    public String getHelpString() {
        Joiner joiner = Joiner.on("\n");
        return joiner.join(helpStrings);
    }

    /**
     * @return the namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * @param namespace the namespace to set
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public void addChildNode(UIGuideNode guideNode) {
        if (guideNode == null) {
            throw new IllegalArgumentException("Null guidenode as child.");
        }
        
        childNodes.add(guideNode);
    }
    
    @Override
    public String toString() {
        return toString(0);
    }

    private String toString(int indent) {
        StringBuilder result = new StringBuilder();

        String emptyOffset = "";
        if (indent > 0) {
            emptyOffset = Strings.repeat("─", indent * INDENT_SIZE - 1);
            emptyOffset = "├" + emptyOffset;
        }
        
        String lineOffset = "";
        if (indent > 0) {
            lineOffset = Strings.repeat(" ", indent * INDENT_SIZE - 1);
            lineOffset = "│" + lineOffset;
        }
        
        final String maxOccursFormatted = isMaxOccursUnbounded() ? "∞" : Integer.toString(getMaxOccurs());
        result.append(emptyOffset).append(String.format("[%s:%s] [%d,%s]\n", 
                getNamespace(), getName(), getMinOccurs(), maxOccursFormatted ));
        result.append(lineOffset).append(getHelpString()).append('\n');
        
        for (UIGuideNode childNode : getChildNodes()) {
            result.append(childNode.toString(indent + 1));
        }
        
        return result.toString();
    }

    public List<UIGuideNode> getChildNodes() {
        return childNodes;
    }

    public WidgetType getWidgetType() {
        return widgetType;
    }
    
    public void setWidgetType(WidgetType widgetType) {
        this.widgetType = widgetType;
    }

    /**
     * For select control, sets the possible values as well as (optionally)
     * their user-friendly names. 
     * 
     * @param values An non-null, non-empty list of possible values.
     * @param valueNames A possibly empty list of user-friendly names. When
     *                   non-empty, its size must match that of the values list.
     */
    public void setPossibleValues(List<String> values, List<String> valueNames) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Values list is empty.");
        }
        
        if (!valueNames.isEmpty() && values.size() != valueNames.size()) {
            throw new IllegalArgumentException("Value/names lists have different sizes.");
        }
        
        possibleValues = values;
        possibleValueNames = valueNames;
    }

    /**
     * Returns a list of pairs of strings. For each pair, the first element (0)
     * is the value of the select element, and the second element (1) is the
     * title (documentation) of the value. If the documentation does not exist, 
     * however, the second element will be null. The first element is guaranteed
     * not to be null.
     * 
     * @return A list of pairs of select values.
     */
    public List<String[]> getSelectValues() {
        List<String[]> result = new LinkedList<>();
        
        if (possibleValueNames == null || possibleValueNames.isEmpty()) {
            // only values, not names
            for (String value : possibleValues) {
                result.add(new String[] { value, null });
            }
        } else {
            // values and names
            for (int i = 0; i < possibleValues.size(); ++i) {
                result.add(new String[] { possibleValues.get(i), possibleValueNames.get(i) });
            }
        }
        
        return result ;
    }

    /**
     * Returns a list of {@linkplain UIGuideNode}s that are direct descendants of
     * this node. All the resulting nodes are of widget type 
     * {@linkplain WidgetType#SUB_ELEMENT_LINK}.
     * 
     * @return
     */
    public List<UIGuideNode> getSubElements() {

        List<UIGuideNode> result = new ArrayList<>();
        
        for (UIGuideNode guideNode : getChildNodes()) {
            if (guideNode.getWidgetType() == WidgetType.SUB_ELEMENT_LINK) {
                result.add(guideNode);
            } else if (guideNode.getWidgetType() == WidgetType.CHOICE) {
                result.addAll(guideNode.getSubElements());
            }
        }
        
        return result;
    }
    
    /**
     * Returns an element of {@link #getSubElements()} that matches namespace
     * and name.
     * 
     * @param namespace
     * @param name
     * @return the node to find, or null if not found.
     */
    public UIGuideNode getSubElement(String namespace, String name) {
        UIGuideNode result = null;
        
        QName searchCriterion = new QName(namespace, name);
        
        List<UIGuideNode> allSubelems = getSubElements();
        for (UIGuideNode uiGuideNode : allSubelems) { // not sexy
            // if (uiGuideNode.getNamespace().equals(namespace) && uiGuideNode.getName().equals(name)) {
            if (uiGuideNode.getQName().equals(searchCriterion)) {
                result = uiGuideNode;
                break;
            }
        }
        
        return result;
    }
    

    /**
     * A friendly label that could override the name when displayed to the
     * user.
     * @param label
     */
    public void setLabel(String label) {
        this.label = label;
    }
    
    /**
     * Returns a friendly label for the end user, either from the label set
     * previously with {@linkplain #setLabel(String)} or from the name of 
     * the node.
     * @return 
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets validating regexp for text representation of value. Makes sense
     * only when {@link WidgetType} is {@link WidgetType#TEXT_TYPE}. Regexp
     * is specified in javascript, add <code>^</code> and <code>$</code> 
     * in the pattern if necessary.
     * 
     * @param validatingRegexp
     */
    public void setValidatingRegexp(String validatingRegexp) {
        this.validatingRegexp = validatingRegexp;
    }

    /**
     * Returns validating regexp, or null if not set. The regexp is in 
     * Javascript.
     * 
     * @return
     */
    public String getValidatingRegexp() {
        return this.validatingRegexp;
    }

    /**
     * @return the minOccurs
     */
    public int getMinOccurs() {
        return minOccurs;
    }

    /**
     * @param minOccurs the minOccurs to set
     */
    public void setMinOccurs(int minOccurs) {
        this.minOccurs = minOccurs;
    }

    /**
     * @return the maxOccurs
     */
    public int getMaxOccurs() {
        return maxOccurs;
    }
    
    /**
     * Returns whether the max nb of occurrences is bounded.
     */
    public boolean isMaxOccursUnbounded() {
        return maxOccurs == MAX_OCCURS_UNBOUNDED;
    }

    /**
     * @param maxOccurs the maxOccurs to set
     */
    public void setMaxOccurs(int maxOccurs) {
        this.maxOccurs = maxOccurs;
    }
    
    /**
     * Convenience method setting min occurs and max occurs to the same 
     * value {@code occurs}.
     * @param occurs Positive or null integer.
     */
    public void setOccurs(int occurs) {
        if (occurs < 0) {
            throw new IllegalArgumentException("Negative occurences");
        }
        
        minOccurs = occurs;
        maxOccurs = occurs;
    }
    
    /**
     * @return <code>true</code> iff min occurs > 0.
     */
    public boolean isRequired() {
        return minOccurs > 0;
    }

    /**
     * Returns the fully qualified name corresponding to the id of the
     * child element specified. For instance, if this node has a control whose
     * id is "id_xxx" and this control corresponds to an attribute
     * foo:bar, then this method will return the {@linkplain QName} {foo}bar. 
     * 
     * @param key The id of the control.
     * @return The corresponding {@linkplain QName}, or <code>null</code> if
     *         the key is not found in this element's children.
     */
    public QName getActualContentName(String key) {
        QName result = null;
        
        UIGuideNode childFound = getChildNode(key);
        
        if (childFound != null) {
            result = new QName(childFound.getNamespace(), childFound.getName());
        }
        
        return result;
    }

    /**
     * @param key
     * @return
     */
    private UIGuideNode getChildNode(String key) {
        UIGuideNode childFound = null;
        
        // TODO: for the time being, a brutal loop, hashmap to implement next
        for (UIGuideNode child : getChildNodes()) {
            if (child.getId().equals(key)) {
                childFound = child;
                break;
            }
        }
        return childFound;
    }

    public String getId() {
        return id;
    }

    public XmlDomContentType getContentType(String key) {
        UIGuideNode childFound = getChildNode(key);
        
        if (childFound == null) {
            throw new RuntimeException("No child found for key " + key);
        }
        
        return childFound.xmlDomContentType;
    }
    
    public XmlDomContentType getContentType() {
        return xmlDomContentType;
    }

    public void setOccurs(int minOccurs, int maxOccurs) {
        setMinOccurs(minOccurs);
        setMaxOccurs(maxOccurs);
    }

    public QName getQName() {
        return new QName(getNamespace(), getName());
    }

    public void setParentQName(QName parentQName) {
        this.parentQName = parentQName;
    }

    public QName getParentQName() {
        return parentQName;
    }

    public void setType(String namespace, String name) {
        if (name == null) {
            throw new IllegalArgumentException("Type name cannot be null.");
        }
        
        this.typeNamespace = namespace;
        this.typeName = name;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getTypeNamespace() {
        return typeNamespace;
    }

    public void setMinValue(int minValue, boolean inclusive) {
        this.minValue = minValue;
        this.setMinValueInclusive(inclusive);
    }
    
    private void setMinValueInclusive(boolean inclusive) {
        this.minValueInclusive = inclusive;
        if (inclusive) {
            this.facets |= XSSimpleTypeDefinition.FACET_MININCLUSIVE;
        } else {
            this.facets |= XSSimpleTypeDefinition.FACET_MINEXCLUSIVE;
        }
    }

    public int getMinValue() {
        return minValue;
    }
    
    public void setMaxNbFractionDigits(int maxNbFractionDigits) {
        this.maxNbFractionDigits = maxNbFractionDigits;
        
        this.facets |= XSSimpleTypeDefinition.FACET_FRACTIONDIGITS;
    }
    
    public boolean hasFacet(short test) {
        return (this.facets & test) == test; 
    }
    
    public int getNbFractionDigits() {
        return maxNbFractionDigits;
    }

    public void setMaxValue(int maxValue, boolean inclusive) {
        this.maxValue = maxValue;
        this.setMaxValueInclusive(inclusive);
    }

    /**
     * @return the minValueInclusive
     */
    public boolean isMinValueInclusive() {
        return minValueInclusive;
    }

    /**
     * @return the maxValue
     */
    public int getMaxValue() {
        return maxValue;
    }

    /**
     * @return the maxValueInclusive
     */
    public boolean isMaxValueInclusive() {
        return maxValueInclusive;
    }

    /**
     * @param maxValueInclusive the maxValueInclusive to set
     */
    public void setMaxValueInclusive(boolean maxValueInclusive) {
        this.maxValueInclusive = maxValueInclusive;
        
        if (maxValueInclusive) {
            this.facets |= XSSimpleTypeDefinition.FACET_MAXINCLUSIVE;
        } else {
            this.facets |= XSSimpleTypeDefinition.FACET_MAXEXCLUSIVE;
        }
        
    }

    public QName getTypeQName() {
        return new QName(getTypeNamespace(), getTypeName());
    }

    public void setXmlSchemaBuiltinKind(short builtInKind) {
        xmlSchemaBuiltinKind = builtInKind;
    }

    public short getXmlSchemaBuiltinKind() {
        return xmlSchemaBuiltinKind;
    }

}
