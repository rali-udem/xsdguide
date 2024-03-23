/**
 * (c) RALI - Recherche appliquee en linguistique informatique.
 *     http://rali.iro.umontreal.ca
 */
package ca.umontreal.rali.xsdguide.demo;

import java.util.Comparator;

import javax.xml.namespace.QName;

/**
 * @author Fabrizio Gotti - gottif
 *
 */
public class QNameComparator implements Comparator<QName> {

    @Override
    public int compare(QName o1, QName o2) {
        int result = 0;
        
        String ns1 = o1.getNamespaceURI();
        String ns2 = o2.getNamespaceURI();
        
        if (ns1.equals(ns2)) {
            result = o1.getLocalPart().compareTo(o2.getLocalPart());
        } else {
            result = ns1.compareTo(ns2);
        }
        
        return result;
    }

}
