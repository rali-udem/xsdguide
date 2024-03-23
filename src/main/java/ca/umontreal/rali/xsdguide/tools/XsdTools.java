/**
 * (c) RALI - Recherche appliquee en linguistique informatique.
 *     http://rali.iro.umontreal.ca
 */
package ca.umontreal.rali.xsdguide.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import org.apache.xerces.impl.dv.xs.XSSimpleTypeDecl;
import org.apache.xerces.xs.StringList;
import org.apache.xerces.xs.XSAnnotation;
import org.apache.xerces.xs.XSConstants;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSSimpleTypeDefinition;

/**
 * @author Fabrizio Gotti - gottif
 *
 */
public class XsdTools {
    private static final Pattern TAG_STRIPPER = Pattern.compile("<[^>]*>");
    private static final Pattern NORMALIZER = Pattern.compile("\\s+");
    
    /**
     * Replaces {@linkplain XSAnnotation#getAnnotationString()} with a
     * version stripping the tags from the annotation string. Normalizes
     * space and adds a period when it's missing.
     * @param addPunctuation 
     */
    public static String getAnnotationString(XSAnnotation annotation, boolean addPunctuation) {
        String result = annotation.getAnnotationString();
        
        result = TAG_STRIPPER.matcher(result).replaceAll("");
        result = normalizeString(result);
        if (addPunctuation && !result.endsWith(".")) {
            result += ".";
        }
        
        return result;
    }

    private static String normalizeString(String string) {
        return NORMALIZER.matcher(string).replaceAll(" ").trim();
    }

    /**
     * Converts a {@linkplain StringList} into a simple list.
     * 
     * @param stringlist
     * @return
     */
    public static List<String> asList(StringList stringlist) {
        List<String> result = new ArrayList<>();
        
        for (Object string : stringlist) {
            result.add((String) string);
        }

        return result;
    }

    /**
     * Returns the enumeration annotations if Xerces implementation and
     * enumeration annotation exists. Returns empty list otherwise.
     * 
     * @param simpleType
     * @param addPunctuation 
     * @return
     */
    public static List<String> xercesGetEnumAnnotations(
            XSSimpleTypeDefinition simpleType, boolean addPunctuation) {
        
        List<String> result = new ArrayList<>();
        
        if (simpleType instanceof XSSimpleTypeDecl) {
            XSSimpleTypeDecl xercesSimpleType = (XSSimpleTypeDecl) simpleType;
            /*
            ObjectList objectList = xercesSimpleType.getActualEnumeration();
            for (Object object : objectList) {
                result.add((String) object);
            }
            */
            
            XSObjectList annots = xercesSimpleType.enumerationAnnotations;
            
            if (annots != null && !annots.isEmpty()) {
                for (Object object : annots) {
                    if (object == null) {
                        result.clear();
                        break;
                    }
                    
                    XSAnnotation annot = (XSAnnotation) object;
                    result.add(getAnnotationString(annot, addPunctuation));
                }
            }
            
        }
        
        return result;
    }

    public static QName parseQName(String qualifiedName) {
        QName result = null;
        
        String[] frags = qualifiedName.split(":");
        if (frags.length == 1) {
            result = new QName(frags[0]);
        } else if (frags.length == 2) {
            result = new QName(frags[0], frags[1]);
        } else {
            int lastDelimPos = qualifiedName.lastIndexOf(':');
            result = new QName(qualifiedName.substring(0, lastDelimPos),
                               qualifiedName.substring(lastDelimPos + 1));
        }
        
        return result;
    }

    /**
     * Converts {@link XSConstants} data type to string. Only implemented for
     * a subset of types.
     * 
     * @param xmlSchemaBuiltinKind
     * @return
     */
    public static String toTypeString(short xmlSchemaBuiltinKind) {
        
        String result = "";

        switch (xmlSchemaBuiltinKind) {
        case XSConstants.ID_DT:
            result = "xs:id";
            break;
        case XSConstants.IDREF_DT:
            result = "xs:idref";
            break;
        default:
        case XSConstants.STRING_DT:
            result = "xs:string";
        }
        
        return result;
    }

    public static String writeQName(QName curName) {
        return curName.getNamespaceURI() + ":" + curName.getLocalPart();
    }
    
}
