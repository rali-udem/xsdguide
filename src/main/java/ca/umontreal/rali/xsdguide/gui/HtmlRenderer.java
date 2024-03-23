/**
 * (c) RALI - Recherche appliquee en linguistique informatique.
 *     http://rali.iro.umontreal.ca
 */
package ca.umontreal.rali.xsdguide.gui;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.xerces.xs.XSConstants;
import org.apache.xerces.xs.XSSimpleTypeDefinition;

import ca.umontreal.rali.xsdguide.guides.UIGuide;
import ca.umontreal.rali.xsdguide.guides.UIGuideNode;
import ca.umontreal.rali.xsdguide.guides.UIGuideNode.XmlDomContentType;
import ca.umontreal.rali.xsdguide.instance.GuidedElement;
import ca.umontreal.rali.xsdguide.instance.ValueSet;
import ca.umontreal.rali.xsdguide.tools.XsdTools;

import com.google.common.io.Files;

/**
 * Renders {@link UIGuideNode}s in Html. This class has suffered from 
 * changing requirements as our research progressed.
 * 
 * @author Fabrizio Gotti - gottif
 *
 */
public class HtmlRenderer extends Renderer {

    public static final String XMLATTRIBUTE_PREFIX = "xmlattribute-";
    private static final int MAX_SELECT_LABEL_LENGTH = 50;
    private String host;

    public HtmlRenderer() {
        this("http://localhost:8080");
    }
    
    public HtmlRenderer(String hostURL) {
        this.host = hostURL;
    }

    @Override
    public String render(UIGuideNode node) {
        StringBuilder result = new StringBuilder();
        result.append(getHtmlPrefix());
        toString(node, result, true);
        result.append(getHtmlPostfix());
        
        setLastResult(result.toString());
        
        return getLastResult();
    }

    private String getHtmlPostfix() {
        return "<p><input type='submit' value='Submit' /></p></form></body></html>";
    }
    
    public void toFragmentString(UIGuideNode el,
                                 ValueSet values,
                                 StringBuilder finalResult, 
                                 boolean rootElement) {
        
        final String requiredClass = el.isRequired() ? "required" : "optional";
        String finalResultDivType = "";
        
        StringBuilder result = new StringBuilder();
        
        switch (el.getWidgetType()) {
        
        case ELEMENT_TYPE:
            
            StringBuilder innerContent = new StringBuilder();
            
            if (rootElement) {
                innerContent.append(tag("div", "el_title", tag("span", "el_name", el.getName())));
                innerContent.append(tag("div", "el_help", el.getHelpString()));
            }
            
            // child elements
            List<UIGuideNode> childNodes = el.getChildNodes();
            for (UIGuideNode childNode : childNodes) {
                toFragmentString(childNode, values, innerContent, false);
            }
            
            if (rootElement) {
                
                StringBuilder childNodeTargetDivs = new StringBuilder();
                List<UIGuideNode> subElements = el.getSubElements();
                
                for (UIGuideNode childNode : subElements) {
                    childNodeTargetDivs.append(tag("div", "sub_el", "", 
                            new String[]{ "data-target", childNode.getQName().toString() }));
                }
                
                innerContent.append(tag("div", "el_subelements", childNodeTargetDivs.toString()));
                finalResultDivType = "element complex";
            } else {
                finalResultDivType = "element";
            }
                
            result.append(innerContent);
            
            break;
            
        case TEXT_TYPE:
            
            String tagId = values.getValue("id");
            final String validatingRegexp = el.getValidatingRegexp();
            String javaScriptValidator = "";
            
            if (validatingRegexp != null) {
                javaScriptValidator = " data-regexpvalidator=" + validatingRegexp;
            }
            
            String schemaType = "";
            String typeString = XsdTools.toTypeString(el.getXmlSchemaBuiltinKind());
            if (!typeString.equalsIgnoreCase(XsdTools.toTypeString(XSConstants.STRING_DT))) {
                schemaType = "data-schema-type='" + XsdTools.toTypeString(el.getXmlSchemaBuiltinKind()) + "'";
            }
            
            String helpString = el.getHelpString();
            
            String facets = getFacets(el);
            
            result.append(tag("div", "el_text" + " " + requiredClass, 
                    tag("span", null, 
                            tag("label", null, makeLabel(el, false), "for='" + tagId + "'")) +
                    tag("input", null, "", "type='text'" + 
                                           makeIdName(tagId, el.isRequired()) + 
                                           javaScriptValidator + 
                                           " title='" + escapeString(helpString) + "'" +
                                           schemaType + " " + 
                                           facets)
                    ));
           
            finalResultDivType = el.getContentType() == XmlDomContentType.ELEMENT ? "element" : "attribute";
            
            break;
            
        case SUB_ELEMENT_LINK:
            
            result.append(tag(
                    "div",
                    requiredClass,
                    tag("a", "sub_element",
                            "Add new " + el.getName()
                                    + (el.isRequired() ? String.format(" (at least %d required)", el.getMinOccurs()) : " (optional)"),
                            " href='#' data-elname='" + el.getQName().toString() + "'" )));
            
            finalResultDivType = "sub_link";
            
            break;
            
        case SELECT:
            
            String tagSelectId = values.getValue("id");
            String selectHelpString = el.getHelpString();
            
            StringBuilder select = new StringBuilder();
            select.append(tag("option", null, "", "value='' selected")); // empty
            for (final String[] valueTitlePair : el.getSelectValues()) {
                String value = valueTitlePair[0];
                String label = valueTitlePair[1];
                
                if (label == null || label.isEmpty()) {
                    label = value;
                }
                
                String title = "";
                if (label.length() > MAX_SELECT_LABEL_LENGTH) {
                    title = " title=\" " + label + "\" ";
                    label = label.substring(0, MAX_SELECT_LABEL_LENGTH) + "...";
                }
                
                select.append(tag("option", null, label, "value=\'" + value + "\'" + title));
            }
            
            wrapWithTag("select", select, (el.isRequired() ? "required" : null), 
                                  makeIdName(tagSelectId, el.isRequired()) + 
                                  " title='" + selectHelpString + 
                                  "'");
            
            result.append(tag("div", "el_text " + requiredClass, 
                    tag("label", null, makeLabel(el, false), "for='" + tagSelectId + "'") +
                    select.toString()
                    ));
            
            finalResultDivType = el.getContentType() == XmlDomContentType.ELEMENT ? "element" : "attribute";
            
            break;
            
        case CHOICE:
            
            StringBuilder choiceBuilder = new StringBuilder();
            choiceBuilder.append(tag("legend", null, "Choose one of:" + (el.isRequired() ? " (required)" : "")));
            
            // child elements
            List<UIGuideNode> childChoiceNodes = el.getChildNodes();
            for (UIGuideNode childNode : childChoiceNodes) {
                StringBuilder currentChoice = new StringBuilder();
                toFragmentString(childNode, values, currentChoice, false);
                choiceBuilder.append(tag("div", "choice_option", currentChoice.toString(), new String[]{ "data-option-label", childNode.getName() } ));
            }
            
            wrapWithTag("fieldset", choiceBuilder, requiredClass, null);
            result.append(choiceBuilder);
            
            finalResultDivType = "choice";
            
            break;
            
        case BOOLEAN:
            
            String booleanTagId = el.getId();
            
            result.append(tag("div", "el_text " + requiredClass, 
                    tag("label", null, makeLabel(el, false), "for='" + booleanTagId + "'") +
                    tag("label", null, tag("input", null, "", "type='checkbox' id='" + booleanTagId + "' name='" + booleanTagId + "'") + "Tick to set to true")
                    ));
            
            finalResultDivType = el.getContentType() == XmlDomContentType.ELEMENT ? "element" : "attribute";            
            
            break;
            
        case DATE:
            
            String dateTagId = el.getId();
            
            result.append(tag("div", "el_text date " + requiredClass, 
                    tag("label", null, makeLabel(el, false), "for='" + dateTagId + "'") +
                    tag("input", null, "", "type='text' " +
                            "data-date='true' " + 
                            makeIdName(dateTagId, el.isRequired()) + 
                            " title='" + escapeString(el.getHelpString()) + 
                            "'"))
                    );
            
            finalResultDivType = el.getContentType() == XmlDomContentType.ELEMENT ? "element" : "attribute";            
            
            break;
            
        case DATE_TIME:
            
            String dateTimeTagId = el.getId();
            
            result.append(tag("div", "el_text datetime " + requiredClass, 
                    tag("label", null, makeLabel(el, false), "for='" + dateTimeTagId + "'") +
                    tag("input", null, "", "type='text' " +
                                           "data-datetime='true' " + 
                                           makeIdName(dateTimeTagId, el.isRequired()) + 
                                           " title='" + escapeString(el.getHelpString()) + 
                                           "'"))
                    );
            
            finalResultDivType = el.getContentType() == XmlDomContentType.ELEMENT ? "element" : "attribute";            
            
            break;
            
        case UNKNOWN:
            result.append(tag("p", null, "Unknown widget for " + 
                    el.getNamespace() + ":" + el.getName()));
            break;
            
        default:
            throw new RuntimeException("Type not implemented");
        }

        finalResult.append(tag("div", 
                                finalResultDivType, 
                                result.toString(), 
                                values.getValue("id") != null ? 
                                        new String[]{"data-id", getDataId(el, values),
                                                     "data-minoccurs", Integer.toString(el.getMinOccurs()),
                                                     "data-maxoccurs", (el.isMaxOccursUnbounded() ? "unbounded" : Integer.toString(el.getMaxOccurs())) } :
                                        null ));
        
    }

    /**
     * Returns attribute string like "data-minexclusive='32' data-nbfractiondigits='4'"
     * 
     * @param el
     * @return
     */
    private String getFacets(UIGuideNode el) {
        StringBuilder result = new StringBuilder();
        
        if (el.hasFacet(XSSimpleTypeDefinition.FACET_MININCLUSIVE)) {
            result.append(" data-mininclusive='" + el.getMinValue() + "' ");
        } 
        
        if (el.hasFacet(XSSimpleTypeDefinition.FACET_MINEXCLUSIVE)) {
            result.append(" data-minexclusive='" + el.getMinValue() + "' ");
        } 
        
        if (el.hasFacet(XSSimpleTypeDefinition.FACET_MAXINCLUSIVE)) {
            result.append(" data-maxinclusive='" + el.getMaxValue() + "' ");
        } 
        
        if (el.hasFacet(XSSimpleTypeDefinition.FACET_MAXEXCLUSIVE)) {
            result.append(" data-maxexclusive='" + el.getMaxValue() + "' ");
        } 
        
        if (el.hasFacet(XSSimpleTypeDefinition.FACET_FRACTIONDIGITS)) {
            result.append(" data-fractiondigits='" + el.getNbFractionDigits() + "' ");
        } 
        
        return result.toString();
    }

    private String getDataId(UIGuideNode el, ValueSet values) {
        String result = null;
        
        XmlDomContentType type = el.getContentType();
        if (type == null || type == XmlDomContentType.ELEMENT) {
            result = values.getValue("id");
        } else if (type == XmlDomContentType.ATTRIBUTE) {
            result = el.getId();
        } else {
            throw new RuntimeException("Invalid type " + el.getContentType());
        }
        
        return result;
    }

    /**
     * Replaces single and double quotes by their corresponding entities.
     * @param helpString
     * @return
     */
    private String escapeString(String helpString) {
        String result = helpString.replaceAll("'", "&#39;");
        result = result.replaceAll("\"", "&#34;");
        
        return result;
    }

    private Object tag(String htmlTagName, String className, String content,
            String[] attrs) {
        
        StringBuilder attrRender = new StringBuilder();
        for (int i = 0; i < attrs.length;  i+= 2) {
            attrRender.append(String.format("%s=\"%s\"", attrs[i], attrs[i + 1]));
            attrRender.append(' ');
        }

        return tag(htmlTagName, className, content, attrRender.toString());
    }

    void toString(UIGuideNode node, StringBuilder result, boolean rootElement) {
        
        final String requiredClass = node.isRequired() ? "required" : "optional";

        if (rootElement) {
            // title
            result.append(tag("p", "el_title", node.getName()));
            
            // help
            result.append(tag("p", "el_help", node.getHelpString()));
            
            // hidden fields with el name and namespace
            result.append(
                    tag("input", null, "", "type='hidden' id='el_ns' name='el_ns' value='" + node.getNamespace() + "'")                     
                    );
            result.append(
                    tag("input", null, "", "type='hidden' id='el_name' name='el_name' value='" + node.getName() + "'")                     
                    );
            // hidden field with parent type (for demo)
            if (node.getParentQName() != null) {
                result.append(
                        tag("input", null, "", "type='hidden' id='parent_qname' name='parent_qname' value='" + node.getParentQName().toString() + "'")                     
                        );
            }
        }
        
        switch (node.getWidgetType()) {
        case ELEMENT_TYPE:
            
            // child elements
            List<UIGuideNode> childNodes = node.getChildNodes();
            for (UIGuideNode childNode : childNodes) {
                toString(childNode, result, false);
            }
            
            // simple/advanced toggle
            result.append(tag("a", "toggler", "Switch to simple view", "href='javascript:toggleView();' id='toggler' "));
            
            break;
        case TEXT_TYPE:
            
            String tagId = node.getId();
            final String validatingRegexp = node.getValidatingRegexp();
            String javaScriptValidator = "";
            
            if (validatingRegexp != null) {
                javaScriptValidator = " onblur='/" + validatingRegexp + "/.test(this.value) ? this.style.borderColor = \"Green\"  : this.style.borderColor = \"Red\"  '";                
            }
            
            String helpString = node.getHelpString();
            
            result.append(tag("p", "el_text" + " " + requiredClass, 
                    tag("span", null, 
                            tag("label", null, makeLabel(node, true), "for='" + tagId + "'"), "title=\"" + helpString + "\"") +
                    tag("input", null, "", "type='text'" + makeIdName(tagId, node.isRequired()) + javaScriptValidator)
                    ));
            
            break;
        case SUB_ELEMENT_LINK:
            result.append(tag(
                    "p",
                    requiredClass,
                    tag("a", null,
                            "Add new " + node.getName()
                                    + (node.isRequired() ? " (required)" : ""),
                            "target='_blank' href='" + getEscapedQName(node, true)
                                    + ".html'")));
            break;
        case SELECT:
            
            String tagSelectId = node.getId();
            
            StringBuilder select = new StringBuilder();
            for (final String[] valueTitlePair : node.getSelectValues()) {
                String option = valueTitlePair[0];
                String title = valueTitlePair[1];
                
                if (title == null) {
                    title = option;
                }
                select.append(tag("option", null, title, "value=" + option));
            }
            
            wrapWithTag("select", select, (node.isRequired() ? "required" : null), makeIdName(tagSelectId, node.isRequired()));
            
            result.append(tag("p", "el_select " + requiredClass, 
                    tag("label", null, makeLabel(node, true), "for='" + tagSelectId + "'") +
                    select.toString()
                    ));
            
            break;
            
        case CHOICE:
            
            StringBuilder choiceBuilder = new StringBuilder();
            choiceBuilder.append(tag("legend", null, "Choose one of:" + (node.isRequired() ? " (required)" : "")));
            
            // child elements
            List<UIGuideNode> childChoiceNodes = node.getChildNodes();
            for (UIGuideNode childNode : childChoiceNodes) {
                toString(childNode, choiceBuilder, false);
            }
            
            wrapWithTag("fieldset", choiceBuilder, requiredClass, null);
            result.append(choiceBuilder);
            
            break;
        case BOOLEAN:
            
            String booleanTagId = node.getId();
            
            result.append(tag("p", "el_text " + requiredClass, 
                    tag("label", null, makeLabel(node, true), "for='" + booleanTagId + "'") +
                    tag("input", null, "", "type='checkbox' id='" + booleanTagId + "' name='" + booleanTagId + "'")
                    ));
            
            break;
        case DATE:
            
            String dateTagId = node.getId();
            
            result.append("<script src='http://atour.iro.umontreal.ca/sem/datepicker.js'></script>");
            addToHead(result, "<link rel=\"stylesheet\" href=\"http://atour.iro.umontreal.ca/sem/datepicker.css\" type=\"text/css\" />");
            
            result.append(tag("p", "el_text" + requiredClass, 
                    tag("label", null, makeLabel(node, true), "for='" + dateTagId + "'") +
                    tag("input", null, "", "type='text' " + makeIdName(dateTagId, node.isRequired())) +
                    String.format("<input class='datepickerbtn' value=\"📅 \" onclick=\"displayDatePicker('%s', false, 'dmy', '/');\" type=\"button\">", dateTagId)
                    ));
            
            break;
        case DATE_TIME:
            
            String dateTimeTagId = node.getId();
            
            result.append("<script src='http://atour.iro.umontreal.ca/sem/datepicker.js'></script>");
            addToHead(result, "<link rel=\"stylesheet\" href=\"http://atour.iro.umontreal.ca/sem/datepicker.css\" type=\"text/css\" />");
            
            result.append(tag("p", "el_text " + requiredClass, 
                    tag("label", null, makeLabel(node, true), "for='" + dateTimeTagId + "'") +
                    tag("input", null, "", "type='text' " + makeIdName(dateTimeTagId, node.isRequired())) +
                    String.format("<input class='datepickerbtn' value=\"📅 \" onclick=\"displayDatePicker('%s', false, 'dmy', '/');\" type=\"button\">", dateTimeTagId)
                    ));
            
            break;
        case UNKNOWN:
            result.append(tag("p", null, "Unknown widget for " + 
                    node.getNamespace() + ":" + node.getName()));
            break;
        default:
            throw new RuntimeException("type not implemented");
        }
    }

    private String makeIdName(String tagId, boolean required) {
        String result = " id='" + tagId + "' name='" + tagId + "' " + (required ? " data-required='true' " : "");
        return result;
    }

    private void wrapWithTag(String tag, StringBuilder content, String className, String moreData) {
        content.insert(0, "<" + tag + (className == null ? "" : " class=\"" + className + "\"" ) +
                                      (moreData == null ? "" : " " + moreData + " ") + ">");
        content.append("</" + tag + ">\n");
    }

    /**
     * @param node
     * @return
     */
    private String makeLabel(UIGuideNode node, boolean addLabel) {
        
        String result = node.getName();
        
        if (addLabel && node.getLabel() != null) {
            result += " (" + node.getLabel() + ")";
        }
        
        if (node.isRequired()) {
            result += " <span title='Required'>*</span>";
        }
        
        return result;
    }

    private void addToHead(StringBuilder result, String addition) {
        int endHead = result.indexOf("</head>");
        result.insert(endHead, addition);
    }

    private String tag(String htmlTagName, String className, 
            String content, String moreAttributes) {
        
        return String.format("<%s class='%s' %s>%s</%s>\n", htmlTagName, 
                className == null ? "" : className, 
                moreAttributes == null ? "" : moreAttributes,
                content, htmlTagName);
        
    }

    /*
    private String makeId() {
        return "id_" + ((int) (Math.random() * 1000));
    }
    */

    private String tag(String htmlTagName, String className, String content) {
        return String.format("<%s class='%s'>%s</%s>\n", htmlTagName, 
                                                       className == null ? "" : className, 
                                                       content, htmlTagName);
    }

    private String getHtmlPrefix() {
        return "<html><head> <meta charset=\"UTF-8\"><style>" + getCss() + "</style><script>\n" +
                getJs () + "\n</script></head><body><form method='GET' action='" + host + "'>";
    }

    private String getJs() {
        String result = "";
        try {
            File testf = new File(this.getClass().getResource("/html/guidejs.js").toURI());
            result = Files.toString(testf, Charset.forName("UTF-8"));
        } catch ( NullPointerException | URISyntaxException | IOException e) {
            throw new RuntimeException("Can't find JS");
        }
        return result;
    }

    private String getCss() {
        String result = "";
        try {
            File testf = new File(this.getClass().getResource("/html/guidenode.css").toURI());
            result = Files.toString(testf, Charset.forName("UTF-8"));
        } catch ( NullPointerException | URISyntaxException | IOException e) {
            throw new RuntimeException("Can't find CSS");
        }
        return result;
    }

    /**
     * Renders all elements in specified name, starting with the root element
     * specified. The files are written in outDir.
     * 
     * @param guide
     * @param qualifiedName
     * @param outDir
     * @throws Exception 
     */
    public void renderAll(UIGuide guide, QName rootName, File outDir) throws Exception {
        writeElementRecursive(guide.getGuideNodeForElementName(rootName), guide, outDir);
    }
    
    private void writeElementRecursive(UIGuideNode node, UIGuide guide, File outDir)
            throws Exception {
        
        System.out.println("Producing html for element " + node.getName());        
        writeElement(node, outDir);
        
        List<UIGuideNode> subElements = node.getSubElements();
        
        for (UIGuideNode childNode : subElements) {
            // UIGuideNode childGuideNode = guide.getGuideNode(childNode.getNamespace(), childNode.getName());
            UIGuideNode childGuideNode = guide.getCompleteGuideNode(childNode);
            childGuideNode.setParentQName(node.getQName());
            writeElementRecursive(childGuideNode, guide, outDir);
        }
    }
    
    private void writeElement(UIGuideNode node, File outDir) throws IOException {
        String html = render(node);
        final String name = getEscapedQName(node, false);
        Files.write(html, new File(outDir, name + ".html"), Charset.forName("UTF-8"));
    }
    
    /**
     * Returns a version of the node's name that is appropriate for file
     * names and url links. Some fully qualified names may contain inappropriate
     * characters for links or file names, so this method is provided.
     * 
     * @param node The node a name will be extracted from.
     * @param encodeForUrl true iff the result will be used in a url, false 
     *                     otherwise (e.g. for filename)
     * @return A string appropriate for file names and urls.
     */
    public static String getEscapedQName(UIGuideNode node, boolean encodeForUrl) {
        String result = (node.getNamespace() == null ? "" : node.getNamespace() + ":")
                        + node.getName();
        
        try {
            result = URLEncoder.encode(result, "UTF-8");
            
            if (encodeForUrl) {
                result = URLEncoder.encode(result, "UTF-8");
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        
        return result;
    }

    @Override
    public String render(GuidedElement el) {
        StringBuilder result = new StringBuilder();;
        toString(el.getGuideNode(), result, true);
        setLastResult(result.toString());
        return getLastResult();
    }

}
