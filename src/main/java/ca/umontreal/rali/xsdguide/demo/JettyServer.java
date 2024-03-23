/**
 * (c) RALI - Recherche appliquee en linguistique informatique.
 *     http://rali.iro.umontreal.ca
 */
package ca.umontreal.rali.xsdguide.demo;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.xml.sax.SAXException;

import ca.umontreal.rali.xsdguide.guides.GuideNodeException;
import ca.umontreal.rali.xsdguide.instance.GuidedXmlDoc;
import ca.umontreal.rali.xsdguide.instance.ValueSet;

/**
 * @author Fabrizio Gotti - gottif
 * 
 */
public class JettyServer extends AbstractHandler {
    private GuidedXmlDoc gDoc;
    
    // only for demo
    private Map<String, String> qName2ElId = new HashMap<>();

    private String xsdFile;

    public JettyServer(String xsdFile) throws Exception {
        this.xsdFile = xsdFile;
        reset();
    }

    /**
     * @param schemaFile
     * @throws GuideNodeException
     * @throws URISyntaxException 
     */
    private void reset() throws GuideNodeException, URISyntaxException {
        File schemaFile = xsdFile != null ? new File(xsdFile) :
            new File(this.getClass().getResource("/schemata/isefs200/SAR 1.1.1 - Detailed/xsd/sari/sari/1.1/sari.xsd").toURI());
        final QName rootElementName = new QName(GuidedXmlDoc.HTTP_IJIS_ISESAR_1_1_NS, GuidedXmlDoc.IJIS_ISESAR_1_1_ROOTEL);
        gDoc = new GuidedXmlDoc(schemaFile, rootElementName);
        qName2ElId.clear();
        qName2ElId.put(rootElementName.toString(), gDoc.getRootElementId());
    }
    
    @Override
    public void handle(String target, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        response.setContentType("text/xml;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);

        baseRequest.setHandled(true);

        System.err.println(">> Handle started");
        
        if (baseRequest.getParameter("reset") != null) {
            try {
                reset();
            } catch (GuideNodeException | URISyntaxException e) {
                e.printStackTrace();
            }
            response.getWriter().println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><op>Reset ok</op>");
            return;
        }
        
        String localPart = baseRequest.getParameter("el_name");
        String namespaceURI = baseRequest.getParameter("el_ns");
        
        if (localPart == null || namespaceURI == null) {
            System.err.println("Invalid request " + baseRequest);
            return;
        }
        
        String parentId = gDoc.getRootElementId();
        String parentQNameString = baseRequest.getParameter("parent_qname");
        if (parentQNameString != null) {
            if (qName2ElId.containsKey(parentQNameString)) {
                parentId = qName2ElId.get(parentQNameString);
            }
        }
        
        try {
            QName newElementQName = new QName(namespaceURI, localPart);
            String id = gDoc.createElement(parentId, newElementQName);
            qName2ElId.put(newElementQName.toString(), id);

            ValueSet valueSet = new ValueSet();
            
            for (Enumeration<String> paramNames = baseRequest.getParameterNames(); paramNames.hasMoreElements(); ) {
                String curParamName = paramNames.nextElement();
                if (curParamName.startsWith("id_")) {
                    valueSet.setValue(curParamName, baseRequest.getParameterValues(curParamName)[0]);
                }
            }
            
            gDoc.setValues(id, valueSet);
            gDoc.validate();
            
        } catch (GuideNodeException | SAXException e) {
            e.printStackTrace();
        }
        
        response.getWriter().println(gDoc.toString());
    }
    
    public static void main(String[] args) throws Exception {
        String file = null;
        int port = 8080;
        
        if (args.length != 0) {
            if (args.length != 2) {
                System.err.println("Usage: prog schemafile.xsd portnumber");
                System.exit(1);
            }
            
            file = args[0];
            port = Integer.parseInt(args[1]);
        }
        
        
        Server server = new Server(port);
        server.setHandler(new JettyServer(file));

        server.start();
        server.join();
    }
}
