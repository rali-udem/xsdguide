/**
 * (c) RALI - Recherche appliquee en linguistique informatique.
 *     http://rali.iro.umontreal.ca
 */
package ca.umontreal.rali.xsdguide.guides;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.xerces.xs.StringList;
import org.apache.xerces.xs.XSAnnotation;
import org.apache.xerces.xs.XSAttributeDeclaration;
import org.apache.xerces.xs.XSAttributeUse;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSConstants;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSFacet;
import org.apache.xerces.xs.XSImplementation;
import org.apache.xerces.xs.XSLoader;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSModelGroup;
import org.apache.xerces.xs.XSMultiValueFacet;
import org.apache.xerces.xs.XSNamedMap;
import org.apache.xerces.xs.XSObject;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSParticle;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import org.apache.xerces.xs.XSTerm;
import org.apache.xerces.xs.XSTypeDefinition;
import org.apache.xerces.xs.XSValue;
import org.apache.xerces.xs.XSWildcard;
import org.apache.xerces.xs.datatypes.ObjectList;
import org.apache.xerces.xs.datatypes.XSDecimal;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

import ca.umontreal.rali.xsdguide.guides.UIGuideNode.WidgetType;
import ca.umontreal.rali.xsdguide.guides.UIGuideNode.XmlDomContentType;
import ca.umontreal.rali.xsdguide.tools.CatalogTools;
import ca.umontreal.rali.xsdguide.tools.XsdTools;

/**
 * A UIGuide loads an XML Schema and produces {@linkplain UIGuideNode} objects
 * for setting up a UI.
 * 
 * @author Fabrizio Gotti - gottif
 *
 */
public class UIGuide {

    private static final String ID_REGEXP = "^[a-zA-Z_][a-zA-Z0-9_\\.-]*$";
    
    private XSModel xsModel;
    private File schemaFile;
    private Map<String, XSTypeDefinition> anonymousTypes = new HashMap<>();
    private int anonymousTypeNum = 1;

    /**
     * Loads a schema from the specified file.
     * 
     * @param schemaFile The file to load.
     */
    public UIGuide(File schemaFile) throws ClassNotFoundException, InstantiationException, IllegalAccessException, ClassCastException {

        XSLoader loader = null;
        
        if (CatalogTools.isInitialized()) {
            loader = CatalogTools.getLoader();
        } else {
            DOMImplementationRegistry registry = org.w3c.dom.bootstrap.DOMImplementationRegistry.newInstance();
            XSImplementation implementation = (XSImplementation) registry.getDOMImplementation("XS-Loader");
            loader = implementation.createXSLoader(null);
        }

        this.schemaFile = schemaFile;
        String uri = schemaFile.toURI().toString();
        xsModel = loader.loadURI(uri);
    }

    public UIGuideNode getGuideNodeForElementName(String namespace, String elementName) throws GuideNodeException {
        XSElementDeclaration element = xsModel.getElementDeclaration(elementName, namespace); // watch out: ns after
        
        if (element == null) {
            XSTypeDefinition typeDef = xsModel.getTypeDefinition(elementName, namespace);
            throw new GuideNodeException("Node element does not exist: " + namespace + ":" + elementName + "\n" +
                                         "and type def for this name is " + typeDef);
        }
        
        UIGuideNode result = getGuideNode(element);
        return result;
    }

    public UIGuideNode getGuideNodeForElementName(QName elementName) throws GuideNodeException {
        return getGuideNodeForElementName(elementName.getNamespaceURI(), elementName.getLocalPart());
    }

    /**
     * Returns a {@link UIGuideNode} for the specified element name and the type
     * definition.
     * 
     * @param elementName
     * @param typeName
     * @return
     * @throws GuideNodeException
     */
    public UIGuideNode getGuideNodeForElementNameWithType(QName elementName,
            QName typeName) throws GuideNodeException {
        
        XSTypeDefinition typeDef = xsModel.getTypeDefinition(typeName.getLocalPart(), typeName.getNamespaceURI());
        
        if (typeDef == null) { // anonymous type
            typeDef = anonymousTypes.get(typeName.getLocalPart());
        }
        
        if (typeDef == null) {
            throw new RuntimeException("Cannot find type " + typeName);
        }
        
        UIGuideNode result = makeGuideNode(elementName.getNamespaceURI(), elementName.getLocalPart(), null, typeDef);
        
        return result;
    }

    /**
     * This method takes a {@link UIGuideNode} that has widget type 
     * {@link WidgetType#SUB_ELEMENT_LINK} and populates a full {@link UIGuideNode}
     * based on the information contained in the input node. This allows to 
     * recurse further. Our very imperfect architecture creates incomplete {@link UIGuideNode} 
     * when they are encountered as children of elements. It is therefore necessary
     * to complete them on demand. This could be corrected by fully parsing
     * the complete schema once, and storing all {@link UIGuideNode}s, completed.
     * 
     * @param subElementNode
     * @return
     * @throws GuideNodeException 
     */
    public UIGuideNode getCompleteGuideNode(UIGuideNode subElementNode) throws GuideNodeException {
        if (subElementNode.getWidgetType() != WidgetType.SUB_ELEMENT_LINK) {
            throw new IllegalArgumentException("To complete a node, it must be a subelement.");
        }
        
        UIGuideNode result = null;
        
        XSElementDeclaration element = xsModel.getElementDeclaration(subElementNode.getName(), subElementNode.getNamespace());
        
        if (element != null) {
            result = getGuideNode(element);
        } else {
            result = getGuideNodeForElementNameWithType(subElementNode.getQName(), subElementNode.getTypeQName());
        }
        
        return result;
    }
    
    private UIGuideNode makeGuideNode(String elementNamespace, 
                                      String elementName,
                                      XSObjectList annotations, 
                                      XSTypeDefinition typeDef) {
        
        // prepare element
        UIGuideNode result = new UIGuideNode(elementNamespace, elementName, XmlDomContentType.ELEMENT);
        result.setWidgetType(WidgetType.ELEMENT_TYPE);
        result.setOccurs(1); // default value

        // sets the type
        setTypeName(result, typeDef);
        
        // annotations
        if (annotations != null) {
            for (Object curAnnotation : annotations) {
                XSAnnotation annotation = (XSAnnotation) curAnnotation;
                result.addHelpText(XsdTools.getAnnotationString(annotation, true));
            }
        }

        // get the type of the element
        final boolean isSimple = typeDef.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE;
        
        // fetch more help
        getHelpFromAnnotations(result, typeDef, isSimple);
        
        if (isSimple) {
            XSSimpleTypeDefinition simpleTypeDef = ((XSSimpleTypeDefinition) typeDef);
            processSimpleType(result, simpleTypeDef);
        } else {
            // find attributes for complex types - none for simple types
            List<XSAttributeUse> attributeUses = getAttributesFromComplexType((XSComplexTypeDefinition) typeDef);
            
            for (XSAttributeUse xsAttributeUse : attributeUses) {
                result.addChildNode(getGuideNode(xsAttributeUse));
            }

            // do the rest
            processComplexType(typeDef, result);
        }
        
        return result;
    }

    /**
     * @param typeDef
     * @param result
     */
    private void processComplexType(XSTypeDefinition typeDef, UIGuideNode result) {
        XSComplexTypeDefinition complexTypeDef = (XSComplexTypeDefinition) typeDef;
        
        // complex type
        switch (complexTypeDef.getContentType()) {
            case XSComplexTypeDefinition.CONTENTTYPE_SIMPLE:
                processComplexTypeContentSimple(result, complexTypeDef);
                break;
            case XSComplexTypeDefinition.CONTENTTYPE_EMPTY:
                processComplexTypeContentEmpty(result, complexTypeDef);
                break;
            case XSComplexTypeDefinition.CONTENTTYPE_ELEMENT:
                processComplexTypeContentMixed(result, complexTypeDef);
                break;
            case XSComplexTypeDefinition.CONTENTTYPE_MIXED:
                System.err.println("GUIDE: treating complex type content mixed as complex type content element");
                processComplexTypeContentMixed(result, complexTypeDef);
                break;
            default:
                throw new RuntimeException("Invalid content type for complex type");
        }
    }

    private void setTypeName(UIGuideNode result, XSTypeDefinition typeDef) {
        String typeName = typeDef.getName();
        String typeNamespace = typeDef.getNamespace();
        
        if (typeDef.getAnonymous()) {
            typeName = getAnonymousTypeName();
            anonymousTypes.put(typeName, typeDef); // remember anonymous type
        }
        
        result.setType(typeNamespace, typeName);        
    }

    /**
     * 
     * @param element
     * @return The {@link UIGuideNode} corresponding to the element specified.
     *         The min and max occurs are set to 1.
     */
    private UIGuideNode getGuideNode(XSElementDeclaration element) {
        if (element == null) {
            throw new IllegalArgumentException("Null element.");
        }
        
        return makeGuideNode(element.getNamespace(), element.getName(),
                element.getAnnotations(), element.getTypeDefinition());
    }

    private void processComplexTypeContentEmpty(UIGuideNode result,
            @SuppressWarnings("unused") XSComplexTypeDefinition complexTypeDef) {
        
        // do we have a substitution group?
        XSElementDeclaration typeElDecl = xsModel.getElementDeclaration(
                result.getName(), result.getNamespace());
        
        if (typeElDecl != null) {
            XSObjectList subGroup = xsModel.getSubstitutionGroup(typeElDecl);
            
            if (!subGroup.isEmpty()) {
            
                UIGuideNode substGuideNode = new UIGuideNode(WidgetType.CHOICE);
                result.addChildNode(substGuideNode);
                
                for (Object substitution : subGroup) {
                    XSElementDeclaration elDecl = (XSElementDeclaration) substitution;
                    UIGuideNode childNode = getGuideNode(elDecl);
                    childNode.setWidgetType(WidgetType.SUB_ELEMENT_LINK);
                    substGuideNode.addChildNode(childNode);
                }
            
            }
        }
        
    }

    /**
     * @param result
     * @param typeDef
     * @param isSimple
     */
    private void getHelpFromAnnotations(UIGuideNode result,
            XSTypeDefinition typeDef, final boolean isSimple) {
        XSObjectList annotations;
        // fetch documentation from element type
        if (isSimple) {
            annotations = ((XSSimpleTypeDefinition) typeDef).getAnnotations();
        } else {
            annotations = ((XSComplexTypeDefinition) typeDef).getAnnotations();
        }
        
        for (Object curAnnotation : annotations) {
            XSAnnotation annotation = (XSAnnotation) curAnnotation;
            result.addHelpText(XsdTools.getAnnotationString(annotation, true));
        }
    }

    private void processComplexTypeContentSimple(UIGuideNode result,
            XSComplexTypeDefinition complexTypeDef) {
        
        switch (complexTypeDef.getDerivationMethod()) {
            case XSConstants.DERIVATION_EXTENSION:
                XSTypeDefinition baseType = complexTypeDef.getBaseType();
                
                if (baseType.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE) {
                    XSSimpleTypeDefinition simpleBaseType = (XSSimpleTypeDefinition) baseType;
                    UIGuideNode childNode = new UIGuideNode(simpleBaseType.getNamespace(), simpleBaseType.getName(), XmlDomContentType.ELEMENT);
                    processSimpleType(childNode, simpleBaseType);
                    childNode.setOccurs(result.getMinOccurs(), result.getMaxOccurs()); // for derivation, this makes sense
                    result.addChildNode(childNode);
                } else {
                    XSComplexTypeDefinition complexBaseType = (XSComplexTypeDefinition) baseType;
                    
                    if (complexBaseType.getContentType() == XSComplexTypeDefinition.CONTENTTYPE_SIMPLE) {
                        processComplexTypeContentSimple(result, complexBaseType);
                    } else {
                        System.err.println("Complex base type with content type != simple, is not implemented for derived type " + complexTypeDef);
                        System.err.println("Base type is " + complexBaseType);
                        return;
                    }
                }
                
                break;
            default:
                // 5 other derivation methods
                throw new RuntimeException("Derivation method not implemented.");
        }
    }

    /**
     * Transforms a simple type into a widget
     * @param result 
     * @param simpleType
     * @return
     */
    private void processSimpleType(UIGuideNode result,
            XSSimpleTypeDefinition simpleType) {
        
        switch (simpleType.getVariety()) {
            case XSSimpleTypeDefinition.VARIETY_ABSENT:
                System.err.println("GUIDE: Variety absent not implemented.");
                break;
            case XSSimpleTypeDefinition.VARIETY_ATOMIC:
                processSimpleTypeAtomic(simpleType, result);
                break;
            case XSSimpleTypeDefinition.VARIETY_LIST:
                // example is idrefs, with id="blah1 blah2 blah3" 
                // result.setMinOccurs(0);
                result.setMaxOccurs(UIGuideNode.MAX_OCCURS_UNBOUNDED);
                XSSimpleTypeDefinition listItemType = simpleType.getItemType();
                System.err.println("GUIDE: Variety list partially implemented.");
                processSimpleTypeAtomic(listItemType, result);
                break;
            case XSSimpleTypeDefinition.VARIETY_UNION: 
                System.err.println("GUIDE: Variety union not implemented.");
                break;
        }

    }

    /**
     * @param simpleType
     * @param result
     */
    private void processSimpleTypeAtomic(XSSimpleTypeDefinition simpleType,
            UIGuideNode result) {
        
        result.setXmlSchemaBuiltinKind(simpleType.getBuiltInKind());
        
        switch (simpleType.getBuiltInKind()) {
            case XSConstants.DATE_DT:
                result.setWidgetType(WidgetType.DATE);
                result.setLabel("Date");
                break;
            case XSConstants.STRING_DT:
            case XSConstants.NCNAME_DT:
            case XSConstants.NMTOKEN_DT:
                result.setWidgetType(WidgetType.TEXT_TYPE);
                result.setLabel("Value");
                break;
            case XSConstants.TOKEN_DT:
                result.setWidgetType(WidgetType.TEXT_TYPE);
                result.setLabel("Value");
                break;
            case XSConstants.BOOLEAN_DT:
                result.setWidgetType(WidgetType.BOOLEAN);
                result.setLabel("Tick to set this element's value to true");
                break;
            case XSConstants.ANYURI_DT:
                result.setWidgetType(WidgetType.TEXT_TYPE);
                result.setLabel("Enter URL");
                break;
            case XSConstants.DECIMAL_DT:
                result.setWidgetType(WidgetType.TEXT_TYPE);
                result.setLabel("Enter floating-point value");
                break;
            case XSConstants.POSITIVEINTEGER_DT:
                result.setWidgetType(WidgetType.TEXT_TYPE);
                result.setLabel("Enter value > 0");
                result.setMinValue(0, false);
                break;
            case XSConstants.NEGATIVEINTEGER_DT:
                result.setWidgetType(WidgetType.TEXT_TYPE);
                result.setLabel("Enter value < 0");
                result.setMaxValue(0, false);
                break;
            case XSConstants.INT_DT:
            case XSConstants.INTEGER_DT:
                result.setWidgetType(WidgetType.TEXT_TYPE);
                result.setLabel("Enter integer");
                break;
            case XSConstants.GYEAR_DT:
                result.setWidgetType(WidgetType.TEXT_TYPE);
                result.setValidatingRegexp("\\d{4}");
                result.setLabel("Enter a year");
                break;
            case XSConstants.GMONTH_DT:
                result.setWidgetType(WidgetType.TEXT_TYPE);
                result.setValidatingRegexp("1|2|3|4|5|6|7|8|9|10|11|12");
                result.setLabel("Enter a month");
                break;
            case XSConstants.DATETIME_DT:
                result.setWidgetType(WidgetType.DATE_TIME);
                // 2015-03-05T14:54:09
                result.setValidatingRegexp("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}");
                result.setLabel("Enter a date and a time");
                break;
            case XSConstants.NONNEGATIVEINTEGER_DT:
                result.setWidgetType(WidgetType.TEXT_TYPE);
                result.setLabel("Enter non-negative integer (>= 0)");
                result.setMinValue(0, true);
                break;
            case XSConstants.NONPOSITIVEINTEGER_DT:
                result.setWidgetType(WidgetType.TEXT_TYPE);
                result.setLabel("Enter non-positive integer (<= 0)");
                result.setMaxValue(0, true);
                break;
            case XSConstants.ID_DT:
                result.setWidgetType(WidgetType.TEXT_TYPE);
                result.setValidatingRegexp(ID_REGEXP);
                result.setLabel("Enter id");
                break;
            case XSConstants.IDREF_DT:
                result.setWidgetType(WidgetType.TEXT_TYPE);
                result.setValidatingRegexp(ID_REGEXP);
                result.setLabel("Enter reference to valid id");
                break;
            default:
                XSSimpleTypeDefinition primitive = simpleType.getPrimitiveType();                
                System.err.println("Primitive type not implemented completely for " + simpleType + ", primitive: " + primitive + ", builtinkind " + simpleType.getBuiltInKind());
        }
        
        getFacets(simpleType, result);
    }

    /**
     * @param simpleType
     * @param result
     */
    private void getFacets(XSSimpleTypeDefinition simpleType, UIGuideNode result) {

        boolean enumerationFound = false;
        
        // handle facets. we could use a mask for the bit field, but what year is this, 1983?
        XSObjectList facets = simpleType.getFacets();
        for (Object curFacetObj : facets) {
            
            final XSFacet facet = (XSFacet) curFacetObj;
            
            switch (facet.getFacetKind()) {
                case XSSimpleTypeDefinition.FACET_MININCLUSIVE:
                    int minValue = ((XSDecimal) facet.getActualFacetValue()).getInt();
                    result.setMinValue(minValue, true);
                    break;
                case XSSimpleTypeDefinition.FACET_FRACTIONDIGITS:
                    result.setMaxNbFractionDigits(facet.getIntFacetValue());
                    break;
                case XSSimpleTypeDefinition.FACET_MINEXCLUSIVE:
                    int minValueEx = ((XSDecimal) facet.getActualFacetValue()).getInt();
                    result.setMinValue(minValueEx, false);
                    break;
                case XSSimpleTypeDefinition.FACET_MAXEXCLUSIVE:
                    int maxValueEx = ((XSDecimal) facet.getActualFacetValue()).getInt();
                    result.setMaxValue(maxValueEx, false);
                    break;
                case XSSimpleTypeDefinition.FACET_MAXINCLUSIVE:
                    int maxValue = ((XSDecimal) facet.getActualFacetValue()).getInt();
                    result.setMaxValue(maxValue, true);
                    break;
                case XSSimpleTypeDefinition.FACET_WHITESPACE:
                    // Only for string and its derived datatypes is whiteSpace not COLLAPSE
                    // We collapse by default. Let's tell when we shouldn't.
                    if (!facet.getActualFacetValue().toString().equalsIgnoreCase("collapse") ) {
                        System.err.println("GUIDE: whitespace facet not implemented, " + facet.getActualFacetValue() + " for base type " + simpleType.getBaseType());
                    }
                    
                    break;
                case XSSimpleTypeDefinition.FACET_LENGTH:
                case XSSimpleTypeDefinition.FACET_MAXLENGTH:
                case XSSimpleTypeDefinition.FACET_MINLENGTH:
                case XSSimpleTypeDefinition.FACET_NONE:
                case XSSimpleTypeDefinition.FACET_TOTALDIGITS:
                default:
                    System.err.println("GUIDE: Facet not implemented "
                            + facet.getFacetKind() + ", value is "
                            + facet.getActualFacetValue() + " for "
                            + simpleType);
                break;
            }
        }

        // getFacets does not work with enumeration and pattern
        
        // enumeration
        if (!enumerationFound) {
            StringList possibleValues = simpleType.getLexicalEnumeration();
            if (possibleValues.getLength() != 0) {
                extractEnumeration(simpleType, result);
            }
        }
        
        // pattern
        StringList patterns = simpleType.getLexicalPattern();
        if (!patterns.isEmpty()) {
            if (patterns.size() > 1) {
                System.err.println("GUIDE: Cannot handle more than one validating pattern.");
                System.err.println("GUIDE: Cannot handle complete xsd regexp language.");
            } else {
                result.setValidatingRegexp((String) patterns.get(0));
            }
            
        }
        
    }

    /**
     * @param simpleType
     * @param result
     * @param possibleValues
     */
    private void extractEnumeration(XSSimpleTypeDefinition simpleType, UIGuideNode result) {
        
        // List<String> enumerationAnnotation = XsdTools.xercesGetEnumAnnotations(simpleType, false);
        List<String> enumerationAnnotation = new ArrayList<String>();        
        List<String> enumerationValues = new ArrayList<String>();
        
        XSObjectList multivalueFacets = simpleType.getMultiValueFacets();
        for (int i = 0; i < multivalueFacets.getLength(); ++i) {
            XSMultiValueFacet curMulti = (XSMultiValueFacet) multivalueFacets.get(i);
            if (curMulti.getFacetKind() == XSSimpleTypeDefinition.FACET_ENUMERATION) {
                XSObjectList annotations = curMulti.getAnnotations();
                ObjectList values = curMulti.getEnumerationValues();
                
                if (annotations.getLength() == values.getLength()) {
                    for (int j = 0; j < annotations.getLength(); ++j) {
                        String curValue = ((XSValue) values.get(j)).getNormalizedValue();
                        Object rawAnnot = annotations.get(j);
                        String curAnnot = rawAnnot == null ? "" : XsdTools.getAnnotationString((XSAnnotation) annotations.get(j), true);
                        
                        enumerationValues.add(curValue);
                        enumerationAnnotation.add(curAnnot);
                    }
                }
            }
            
        }
        
        // TODO: this overwrites the type set previously... (improve)                            
        result.setWidgetType(WidgetType.SELECT);
        result.setLabel("Choose one of:");
        result.setPossibleValues(enumerationValues, enumerationAnnotation);
    }

    /**
     * @param result
     * @param complexTypeDef
     */
    private void processComplexTypeContentMixed(UIGuideNode result,
            XSComplexTypeDefinition complexTypeDef) {
        
        XSParticle particle = complexTypeDef.getParticle();
        if (particle == null) {
            throw new RuntimeException("Not implemented " + complexTypeDef.getName() + 
                    " with content model " + complexTypeDef.getContentType());
        }
        
        if (particle.getMinOccurs() != 1 ||
            particle.getMaxOccursUnbounded() ||
            particle.getMaxOccurs() != 1) {
            System.err.println("GUIDE: particle cardinality other than [1,1] not implemented for type " + complexTypeDef);
        }

/*
 *      // This won't work because it is the content of the particle that is
 *      // important, not the particle itself        
        result.setMinOccurs(particle.getMinOccurs());
        if (particle.getMaxOccursUnbounded()) {
            result.setMaxOccurs(UIGuideNode.MAX_OCCURS_UNBOUNDED);
        } else {
            result.setMaxOccurs(particle.getMaxOccurs());
        }
*/        
        XSTerm term = particle.getTerm();
        
        if (term instanceof XSModelGroup) {
            XSModelGroup group = (XSModelGroup) term;
            
            processModelGroup(result, group);
        } else if (term instanceof XSElementDeclaration) {
            // XSElementDeclaration elementDec = (XSElementDeclaration) term;
            System.err.println("Not yet implemented");
            /*
            GNode newNode = new GNode();
            newNode.setLabel(elementDec.getName());
            newNode.setSchemaObject(elementDec);
            children.add(newNode);
            */
        } else if (term instanceof XSWildcard) {
            System.err.println("Not yet implemented");
        }
    }

    /**
     * @param result
     * @param group
     */
    private void processModelGroup(UIGuideNode result, XSModelGroup group) {
        
        switch (group.getCompositor()) {
            case XSModelGroup.COMPOSITOR_ALL:
                System.err.println("GUIDE: Compositor all treated as sequence.");
                processModelGroupSequence(result, group);
                break;
            case XSModelGroup.COMPOSITOR_CHOICE:
                System.err.println("GUIDE: Compositor choice treated as sequence.");
                processModelGroupChoice(result, group);                
                break;
            case XSModelGroup.COMPOSITOR_SEQUENCE:
                processModelGroupSequence(result, group);
                break;
            default:
                throw new RuntimeException("Invalid compositor type."); 
        }
        
    }

    private void processModelGroupChoice(UIGuideNode result, XSModelGroup group) {
        UIGuideNode substGuideNode = new UIGuideNode(WidgetType.CHOICE);
        result.addChildNode(substGuideNode);
        
        processModelGroupHelper(result.getNamespace(), result.getName(), group, substGuideNode);        
    }

    /**
     * @param result
     * @param group
     * @param resultSubNode
     */
    private void processModelGroupHelper(String resultNamespace, String resultName,
            XSModelGroup group, UIGuideNode resultSubNode) {
        
        XSObjectList parts = group.getParticles();
        
        for (int i = 0; i < parts.getLength(); ++i) {
            XSObject curObj = parts.item(i);
            XSParticle particle = (XSParticle) curObj;
            
            XSTerm t = particle.getTerm();
            
            if (t instanceof XSElementDeclaration) {
                XSElementDeclaration decl = (XSElementDeclaration) t;
                UIGuideNode childNode = new UIGuideNode(decl.getNamespace(),
                        decl.getName(), XmlDomContentType.ELEMENT);
                
                XSTypeDefinition elTypeDef = decl.getTypeDefinition();
                setTypeName(childNode, elTypeDef);
                
                childNode.setWidgetType(WidgetType.SUB_ELEMENT_LINK);
                childNode.setMinOccurs(particle.getMinOccurs());
                childNode.setMaxOccurs(particle.getMaxOccursUnbounded() ? UIGuideNode.MAX_OCCURS_UNBOUNDED : particle.getMaxOccurs());
                resultSubNode.addChildNode(childNode);
            } else if (t instanceof XSWildcard) {
                /*
                 * Wildcards are used in content models to permit
                 * subelements of types not explicitly named to occur at
                 * various points in the content of elements
                 * 
                 * An element type wildcard is represented by an any
                 * element; it is always created as the term of a particle
                 * in a content model.
                 */

                XSWildcard wild = (XSWildcard) t;
                
                if (wild.getConstraintType() != XSWildcard.NSCONSTRAINT_ANY) {
                    throw new RuntimeException("Wildcard: only any implemented");
                }
                
                XSElementDeclaration x = xsModel.getElementDeclaration(resultName, resultNamespace);
                XSObjectList subGroup = xsModel.getSubstitutionGroup(x);
                
                UIGuideNode subSubstGuideNode = new UIGuideNode(WidgetType.CHOICE);
                resultSubNode.addChildNode(subSubstGuideNode);
                
                for (Object substitution : subGroup) {
                    XSElementDeclaration elDecl = (XSElementDeclaration) substitution;

                    UIGuideNode childNode = getGuideNode(elDecl);
                    childNode.setWidgetType(WidgetType.SUB_ELEMENT_LINK);
                    childNode.setMinOccurs(particle.getMinOccurs());
                    childNode.setMaxOccurs(particle.getMaxOccursUnbounded() ? UIGuideNode.MAX_OCCURS_UNBOUNDED : particle.getMaxOccurs());
                    
                    subSubstGuideNode.addChildNode(childNode);
                }
            } else if (t instanceof XSModelGroup) {
                // recurse
                processModelGroup(resultSubNode, (XSModelGroup) t);
            }
        }
    }

    /**
     * @param result
     * @param group
     */
    private void processModelGroupSequence(UIGuideNode result,
            XSModelGroup group) {
        
        processModelGroupHelper(result.getNamespace(), result.getName(), group, result);

    }

    
    private List<XSAttributeUse> getAttributesFromComplexType(XSComplexTypeDefinition typeDef) {
        
        List<XSAttributeUse> result = new ArrayList<XSAttributeUse>();
        
        XSObjectList attributeUses = typeDef.getAttributeUses();
        for (Object curUse : attributeUses) {
            XSAttributeUse attribUse = (XSAttributeUse) curUse;
            result.add(attribUse);
        }
        
        return result;
    }
    
    private UIGuideNode getGuideNode(XSAttributeUse attribUse) {
        XSAttributeDeclaration attrDecl = attribUse.getAttrDeclaration();
        
        UIGuideNode result = new UIGuideNode(attrDecl.getNamespace(),
                attrDecl.getName(), XmlDomContentType.ATTRIBUTE);
        
        processSimpleType(result, attrDecl.getTypeDefinition());
        
        if (attribUse.getRequired()) {
            result.setMinOccurs(1);
        }
        
        XSAnnotation annotation = attrDecl.getAnnotation();
        if (annotation != null) {
            result.addHelpText(XsdTools.getAnnotationString(annotation, true));
        }
        
        return result;
    }

    public File getSchemaFile() {
        return schemaFile;
    }
    
    public List<QName> getAllTypes() {
        List<QName> result = new ArrayList<QName>();
        XSNamedMap map = xsModel.getComponents(XSConstants.TYPE_DEFINITION);
        
        for (Object curKey : map.keySet()) {
            XSObject obj = (XSObject) map.get(curKey);
            result.add(new QName(obj.getNamespace(), obj.getName()));
        }
        
        return result;
    }
    
    private String getAnonymousTypeName() {
        return String.format("AnonType%03d", anonymousTypeNum++);
    }
    
}
