/**
 * (c) RALI - Recherche appliquee en linguistique informatique.
 *     http://rali.iro.umontreal.ca
 */
package ca.umontreal.rali.xsdguide.instance;

import java.util.HashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

/**
 * @author Fabrizio Gotti - gottif
 *
 */
public class ValueSet {
    
    private static final Matcher NS_MATCHER = Pattern.compile("\\{([^}]+)}(.*)").matcher("");
    
    private HashMap<String, String> dict = new HashMap<>();

    public Set<String> getKeys() {
        return dict.keySet();
    }

    public String getValue(String key) {
        return dict.get(key);
    }

    public void setValue(QName qName, String value) {
        String fullKey = qName.toString();
        setValue(fullKey, value);
    }

    public void setValue(String key, String value) {
        dict.put(key, value);
    }

    /**
     * Converts string in the format <code>{http://ijis-isesar/1.1}SARMetadata}</code>
     * to {@linkplain QName}. 
     * 
     * @param stringVal
     * @return
     */
    public static QName string2NS(String stringVal) {
        NS_MATCHER.reset(stringVal);
        
        if (!NS_MATCHER.matches()) {
            throw new IllegalArgumentException("Invalid ns spec " + stringVal);
        }
        
        return new QName(NS_MATCHER.group(1), NS_MATCHER.group(2));
    }

}
