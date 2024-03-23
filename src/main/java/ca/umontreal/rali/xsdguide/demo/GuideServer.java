/**
 * (c) RALI - Recherche appliquee en linguistique informatique.
 *     http://rali.iro.umontreal.ca
 */
package ca.umontreal.rali.xsdguide.demo;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.io.FileUtils;
import org.apache.xerces.xs.XSConstants;
import org.apache.xerces.xs.XSLoader;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSNamedMap;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.session.HashSessionIdManager;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.xml.sax.SAXException;

import ca.umontreal.rali.xsdguide.demo.ServerSchemaManager.SchemaDef;
import ca.umontreal.rali.xsdguide.gui.HtmlFragmentRenderer;
import ca.umontreal.rali.xsdguide.gui.HtmlRenderer;
import ca.umontreal.rali.xsdguide.gui.Renderer;
import ca.umontreal.rali.xsdguide.guides.GuideNodeException;
import ca.umontreal.rali.xsdguide.instance.ForgivingErrorHandler;
import ca.umontreal.rali.xsdguide.instance.GuidedXmlDoc;
import ca.umontreal.rali.xsdguide.instance.ValidationMessage;
import ca.umontreal.rali.xsdguide.tools.CatalogTools;
import ca.umontreal.rali.xsdguide.tools.XsdTools;

import com.google.common.base.Joiner;
import com.google.common.io.Files;

/**
 * No error handling - very fast prototyping.
 * 
 * @author Fabrizio Gotti - gottif
 *
 */
public class GuideServer extends AbstractHandler {

    private static final String USAGE_PROG = "Usage: prog static-content-dir port schema-dir catalog-file";
    private static final String NEWXSD_SOURCE = "NEWXSD_SOURCE";
    private static final String MULTIPART_TMPDIR = System.getProperty("java.io.tmpdir");
    private static final String STATIC_ROOT_DIR = "R:\\proj\\masas\\src\\xsdguideclient\\content";
    private static final int SERVER_PORT = 8080;
    private static final String DEFAULT_SCHEMA_DIR = "R:\\proj\\masas\\src\\xsdguide\\xsdguide\\src\\main\\resources\\schemata";
    private static final MultipartConfigElement MULTI_PART_CONFIG = new MultipartConfigElement(MULTIPART_TMPDIR);
    private static final String FULL_SCHEMA = "If your schema file refers to other schema files (e.g. through import statements), create a zip file containing all necessary files and upload the zip file instead.";
    private static final String USER_ID = "user_id";
    private static final String DEFAULT_CATALOG_FILE = "R:\\proj\\masas\\data\\catalog\\catalogonly\\catalog.xml";
    
    private static int docId = 1;
    private static int userId = 1;
    private final JSONParser jsonParser = new JSONParser();
    private Renderer htmlRenderer = new HtmlFragmentRenderer();
    private String schemaDir;
    private ServerSchemaManager schemaManager;
    private XSLoader loader;
    private Validator xsdValidator;
    private SchemaFactory schemaFactory = null;

    /**
     * Null schema dir means look inside the archive for the resource.
     * @param schemaDir
     * @throws IOException 
     * @throws ClassCastException 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     * @throws ClassNotFoundException 
     * @throws SAXException 
     */
    public GuideServer(String schemaDir) throws IOException,
            ClassNotFoundException, InstantiationException,
            IllegalAccessException, ClassCastException, SAXException {
        this.schemaDir = schemaDir;
        
        schemaFactory = CatalogTools.getSchemaFactory();
        
        this.schemaManager = new ServerSchemaManager(new File(schemaDir));
        
        loader = CatalogTools.getLoader();
        initializeValidator();
    }

    private void initializeValidator() throws MalformedURLException, SAXException {
        Schema schema = schemaFactory.newSchema(new URL("http://www.w3.org/2001/XMLSchema.xsd"));
        xsdValidator = schema.newValidator();
    }

    @Override
    public synchronized void handle(String target, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        
        try {
        
            if (baseRequest.getPathInfo().equalsIgnoreCase("/doc/new")) {
                handleNewReport(target, baseRequest, request, response);
            } else if (baseRequest.getPathInfo().equalsIgnoreCase("/doc/getel")) {
                handleGetElement(target, baseRequest, request, response);
            } else if (baseRequest.getPathInfo().equalsIgnoreCase("/doc/addel")) {
                handleAddElement(target, baseRequest, request, response);
            } else if (baseRequest.getPathInfo().equalsIgnoreCase("/doc/save")) {
                handleSaveDocument(target, baseRequest, request, response);
            } else if (baseRequest.getPathInfo().equalsIgnoreCase("/doc/updatevals")) {
                handleUpdateVals(target, baseRequest, request, response);
            } else if (baseRequest.getPathInfo().equalsIgnoreCase("/doc/delel")) {
                handleDeleteElement(target, baseRequest, request, response);
            } else if (baseRequest.getPathInfo().equalsIgnoreCase("/doc/validate")) {
                handleValidateDocument(target, baseRequest, request, response);
            } else if (baseRequest.getPathInfo().startsWith("/schema/")) {
                handleGetSchema(target, baseRequest, request, response);
            } else if (baseRequest.getPathInfo().startsWith("/xsd/upload")) {
                handleUploadSchema(target, baseRequest, request, response);
            } else if (baseRequest.getPathInfo().startsWith("/xsd/getrootelements")) {
                handleGetSchemaRootElements(target, baseRequest, request, response);
            } else if (baseRequest.getPathInfo().startsWith("/xsd/getschemachoices")) {
                handleGetSchemaChoices(target, baseRequest, request, response);
            } else if (baseRequest.getPathInfo().startsWith("/xsd/addfileroot")) {
                handleInstallSchema(target, baseRequest, request, response);
            }
        
        } catch (Exception e) {
            
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            System.err.println(e.getMessage());
            e.printStackTrace();
            
        }

    }

    private void handleInstallSchema(@SuppressWarnings("unused") String target, Request baseRequest,
            @SuppressWarnings("unused") HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {
        
        String errMsg = null;
        
        HttpSession currentSession = baseRequest.getSession(true);
        baseRequest.setHandled(true);
        
        final String xsdFile = baseRequest.getParameter("xsdFile");
        final String rootElement = baseRequest.getParameter("rootElement");
        final File xsdSource = (File) currentSession.getAttribute(NEWXSD_SOURCE);
        SchemaDef newDef = null;
        
        try {
            newDef = schemaManager.installSchemaDir(xsdSource, xsdFile, rootElement);
            // cleanup
            currentSession.removeAttribute(NEWXSD_SOURCE);
            xsdSource.delete();
        } catch (Exception e) {
            errMsg = e.getMessage();
        }
        
        String[] results = {"status", errMsg == null ? "ok" : "err", 
                             "msg" , errMsg == null && newDef != null ? (newDef.schemaPath + " » " + newDef.rootElName) : errMsg };
        
        returnJson(results, response);
    }

    private void handleGetSchemaChoices(@SuppressWarnings("unused") String target, @SuppressWarnings("unused") Request baseRequest,
            @SuppressWarnings("unused") HttpServletRequest request, HttpServletResponse response) throws IOException {
        
        List<SchemaDef> listSchemas = schemaManager.getAvailableSchemas();
        List<String> schemaRepresentations = new ArrayList<String>();
        
        for (SchemaDef schemaDef : listSchemas) {
            schemaRepresentations.add(schemaDef.schemaPath + " » " + schemaDef.rootElName);
        }

        returnJson(new String[] { "results", Joiner.on(" | ").join(schemaRepresentations) }, response);
    }

    private void handleGetSchemaRootElements(@SuppressWarnings("unused") String target,
            Request baseRequest, @SuppressWarnings("unused") HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        HttpSession currentSession = baseRequest.getSession(true);        
        baseRequest.setHandled(true);
        
        final String xsdFile = baseRequest.getParameter("xsd");
        String userId = (String) currentSession.getAttribute(USER_ID);
        
        List<String> rootElements = findRootElements(new File(MULTIPART_TMPDIR + File.separator + userId, xsdFile));
        
        returnJson(new String[]{"rootelements", Joiner.on(">").join(rootElements) }, response);
    }

    private List<String> findRootElements(File schemaFile) {
        List<String> result = new ArrayList<String>();
        String uri = schemaFile.toURI().toString();
        XSModel xsModel = loader.loadURI(uri);
        
        XSNamedMap elDecls = xsModel.getComponents(XSConstants.ELEMENT_DECLARATION);
        @SuppressWarnings("unchecked")
        Collection<QName> qnames = elDecls.keySet();
        
        List<QName> sortedQnames = new ArrayList<QName>();
        sortedQnames.addAll(qnames);
        Collections.sort(sortedQnames, new QNameComparator());
        
        for (QName curName : sortedQnames) {
            result.add(XsdTools.writeQName(curName));
        }
        
        return result;
    }

    private void handleUploadSchema(@SuppressWarnings("unused") String target, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response) throws IOException  {
        
        if (request.getContentType() != null && request.getContentType().startsWith("multipart/form-data")) {
            baseRequest.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, MULTI_PART_CONFIG);
        }

        HttpSession currentSession = baseRequest.getSession(true);        
        baseRequest.setHandled(true);
        
        // set user id if necessary first
        String userId = (String) currentSession.getAttribute(USER_ID);

        if (userId == null) {
            userId = getNewUserId();
            currentSession.setAttribute(USER_ID, userId);
        }
        
        File userDir = new File(MULTIPART_TMPDIR + File.separator + userId);
        userDir.mkdir();
        
        String errMsg = null;
        List<String> xsdFileList = new ArrayList<String>();
        Part part = null;
        
        try {
            part = request.getPart("uploadxsd");
            String fileName = part.getSubmittedFileName();
            
            if (fileName.toLowerCase().endsWith(".xsd") || fileName.toLowerCase().endsWith(".zip")) {
                
                boolean isXsdFile = fileName.toLowerCase().endsWith(".xsd");
                File partFile = new File(MULTIPART_TMPDIR, part.getSubmittedFileName());
                
                if (partFile.exists()) {
                    partFile.delete();
                }
                
                part.write(partFile.getName()); // this renames the temp file, doesn't just copies it
                // move it to user dir
                File newPartFile = new File(userDir, partFile.getName());
                Files.move(partFile, newPartFile);
                partFile = newPartFile;
                
                if (!partFile.exists()) {
                    throw new RuntimeException("Cannot write file " + fileName);
                }
                
                if (isXsdFile) {
                    validateFile(partFile);
                    xsdFileList.add(partFile.getName());
                    currentSession.setAttribute(NEWXSD_SOURCE, partFile.getParentFile());
                } else {
                    File unzippedDir = new File(userDir, removeExtension(partFile.getName()));
                    if (unzippedDir.exists()) {
                        FileUtils.deleteDirectory(unzippedDir);
                    }

                    unzip(partFile, unzippedDir);
                    partFile.delete();
                    validateDir(unzippedDir);
                    findXsdFiles(unzippedDir, unzippedDir.getName(), xsdFileList);
                    
                    if (xsdFileList.isEmpty()) {
                        errMsg = "No schema files (*.xsd files) found in uploaded archive.";
                    } else {
                        currentSession.setAttribute(NEWXSD_SOURCE, unzippedDir.getParentFile());
                    }
                }
                
            } else {
                errMsg = "Invalid file type.";
            }
            
            
        } catch (IOException | ServletException | RuntimeException e) {
            errMsg = e.getMessage();
        }
        
        String[] results = {"status", errMsg == null ? "ok" : "err", 
                            "msg" , errMsg == null ? "ok" : errMsg, 
                            "files", Joiner.on(">").join(xsdFileList) };
        
        returnJson(results, response); 
        
    }

    private List<String> findXsdFiles(File srcDir, String filePrefix, List<String> xsdFiles) {
        
        File[] entries = srcDir.listFiles(new FileFilter() {
            
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory() || 
                       pathname.getName().toLowerCase().endsWith(".xsd") && !pathname.getName().startsWith(".");
            }
        });
        
        for (File curEntry : entries) {
            if (curEntry.isFile()) {
                xsdFiles.add(filePrefix + File.separator + curEntry.getName());
            } else {
                findXsdFiles(curEntry, filePrefix + File.separator + curEntry.getName(), xsdFiles);
            }
        }

        return xsdFiles;
    }

    private void validateDir(File srcDir) throws RuntimeException {
        
        File[] entries = srcDir.listFiles((FileFilter) null);
        for (File curEntry : entries) {
            if (curEntry.isFile()) {
                validateFile(curEntry);
            } else {
                validateDir(curEntry);
            }
        }
        
    }

    private void unzip(File partFile, File unzippedDir) throws IOException {
        XsdUnzipUtility unzipper = new XsdUnzipUtility();
        unzipper.unzip(partFile.getPath(), unzippedDir.getPath());
    }

    private String removeExtension(String name) {
        String result = name;
        int lastDotPos = result.lastIndexOf('.');
        if (lastDotPos > 0) {
            result = result.substring(0, lastDotPos);
        }
        
        return result;
    }

    private void validateFile(File schemaFile) throws RuntimeException {
        
        // don't validate hidden files
        if (schemaFile.getName().startsWith(".")) {
            return;
        }
        
        System.err.println("Validating " + schemaFile);
        
        boolean classicOk = false;
        
        try {
            // 1) classic validate
            ForgivingErrorHandler handler = new ForgivingErrorHandler();
            xsdValidator.setErrorHandler(handler);
            xsdValidator.validate(new StreamSource(schemaFile), null);
            
            List<String> msgs = handler.getValidationMessages();
            if (!msgs.isEmpty()) {
                throw new SAXException(Joiner.on(" ").join(msgs));
            }

            classicOk = true;
            
            // 2) schema factory
            schemaFactory.newSchema(new StreamSource(schemaFile));
        } catch (SAXException e) {
            throw new RuntimeException(
                    "The following file is not a valid XML schema file: "
                            + schemaFile.getName()
                            + ", for the following reason(s): " + e.getMessage() + 
                            (classicOk ? " " + FULL_SCHEMA : ""));
        } catch (IOException e) {
            throw new RuntimeException("Error reading file " + schemaFile.getName());
        }
    }

    private void handleGetSchema(@SuppressWarnings("unused") String target, Request baseRequest,
            @SuppressWarnings("unused") HttpServletRequest request, HttpServletResponse response) throws IOException, URISyntaxException {

        final String schemaPath = baseRequest.getPathInfo().substring("/schema/".length());
        File xsdFile = getSchemaFile(schemaPath);
        
        response.setContentType("application/xml; charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().println(Files.toString(xsdFile, Charset.forName("UTF-8")));
        
        response.getWriter().flush(); // crucial
    }

    private void handleValidateDocument(@SuppressWarnings("unused") String target, Request baseRequest,
            @SuppressWarnings("unused") HttpServletRequest request, HttpServletResponse response) throws SAXException, IOException {

        final GuidedXmlDoc gDoc = getGuidedDoc(baseRequest);
        
        List<ValidationMessage> validationMessages = gDoc.validate();
        
        String[] results = new String[validationMessages.size() * 2];
        
        for (int i = 0; i < validationMessages.size(); ++i) {
            results[i * 2] = Integer.toString(i);
            results[i * 2 + 1] = validationMessages.get(i).getMessage();
        }
        
        returnJson(results, response);
    }

    @SuppressWarnings("unused")
    private void handleDeleteElement(String target, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        
        final GuidedXmlDoc gDoc = getGuidedDoc(baseRequest);
        
        final String inputId = baseRequest.getParameter("inputId");
        final String parentElementId = baseRequest.getParameter("parentElId");
        
        if (inputId.startsWith(HtmlRenderer.XMLATTRIBUTE_PREFIX)) {
            gDoc.removeAttribute(parentElementId, inputId);
        } else {
            gDoc.removeElement(inputId);
        }

        
        returnJson(new String[]{ "return", "ok" }, response);
    }

    private void handleUpdateVals(@SuppressWarnings("unused") String target, Request baseRequest,
            @SuppressWarnings("unused") HttpServletRequest request, HttpServletResponse response) throws ParseException, IOException {

        final GuidedXmlDoc gDoc = getGuidedDoc(baseRequest);
        
        final String newValueString = baseRequest.getParameter("vals");
        
        JSONArray newValues = (JSONArray) jsonParser.parse(newValueString);
        
        for (int i = 0; i < newValues.size(); ++i) {
            JSONArray valueTriplet = (JSONArray) jsonParser.parse(newValues.get(i).toString());

            final String inputId = valueTriplet.get(0).toString();
            final String parentElementId = valueTriplet.get(1).toString();
            final String value = valueTriplet.get(2).toString();

            if (inputId.startsWith(HtmlRenderer.XMLATTRIBUTE_PREFIX)) {
                gDoc.setAttributeValue(parentElementId, inputId, value);
            } else {
                gDoc.setElementTextValue(inputId, value);
            }
            
        }
        
        returnJson(new String[]{ "return", "ok" }, response);
    }

    /**
     * @param baseRequest
     * @return
     */
    private GuidedXmlDoc getGuidedDoc(Request baseRequest) {
        HttpSession currentSession = baseRequest.getSession(true);
        baseRequest.setHandled(true);
        
        final String docId = baseRequest.getParameter("docId");
        GuidedXmlDoc gDoc = (GuidedXmlDoc) currentSession.getAttribute(docId);
        
        return gDoc;
    }

    @SuppressWarnings("unused")
    private void handleSaveDocument(String target, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        
        final GuidedXmlDoc gDoc = getGuidedDoc(baseRequest);

        response.setContentType("text/xml;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);

        response.getWriter().println(gDoc.toString());
    }

    @SuppressWarnings("unused")
    private void handleAddElement(String target, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response) throws GuideNodeException, IOException {
        
        final GuidedXmlDoc gDoc = getGuidedDoc(baseRequest);
        
        final String parentElId = baseRequest.getParameter("parentElId");
        final String elementQname = baseRequest.getParameter("elementQname");
        final String nextSiblingId = baseRequest.getParameter("nextSiblingId");
        
        String elementId = gDoc.createElement(parentElId, QName.valueOf(elementQname), nextSiblingId);
        
        gDoc.render(elementId, htmlRenderer);
        String htmlRendering = htmlRenderer.getLastResult();

        returnJson(new String[]{ "html", htmlRendering }, response);
    }

    @SuppressWarnings("unused")
    private void handleGetElement(String target, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        
        final GuidedXmlDoc gDoc = getGuidedDoc(baseRequest);
        
        final String elementId = baseRequest.getParameter("elementId");
        
        gDoc.render(elementId, htmlRenderer);
        String htmlRendering = htmlRenderer.getLastResult();

        returnJson(new String[]{ "html", htmlRendering }, response);
    }

    @SuppressWarnings("unused")
    private void handleNewReport(String target, Request baseRequest,
            HttpServletRequest request, HttpServletResponse response)
            throws GuideNodeException, URISyntaxException, IOException {
        
        HttpSession currentSession = baseRequest.getSession(true);
        baseRequest.setHandled(true);
        
        String xsdName = baseRequest.getParameter("xsd");
        String rootElementName = baseRequest.getParameter("rootElement");

        File xsdFile = getSchemaFile(xsdName);
        
        GuidedXmlDoc gDoc = new GuidedXmlDoc(xsdFile, XsdTools.parseQName(rootElementName));
        String schemaUrl = request.getScheme() + "://" + 
                           request.getServerName() + ":" + 
                           request.getServerPort() + "/schema/" + 
                           xsdName.replace('\\', '/');
        
        gDoc.setSchemaLocation(schemaUrl);
        
        final String newDocId = getNewDocId();
        final String rootElId = gDoc.getRootElementId();
        
        currentSession.setAttribute(newDocId, gDoc);

        String[] retDict = new String[] {
                "docId", newDocId,
                "rootElId", rootElId,
        };
        
        returnJson(retDict, response);
    }

    private File getSchemaFile(String xsdName) throws URISyntaxException {
        File result = null;
        
        if (schemaDir == null) {        
            result = new File(this.getClass().getResource("/schemata/" + xsdName).toURI());
        } else {
            //result = new File(schemaDir, xsdName);
            result = schemaManager.getSchemaFile(xsdName);
        }
        
        return result;
    }

    @SuppressWarnings("unchecked")
    private void returnJson(String[] retDict, HttpServletResponse response) throws IOException {
        
        // return json
        response.setContentType("application/json; charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        
        // serialize array
        JSONObject jsonObj = new JSONObject();
        for (int i = 0; i < retDict.length; i += 2) {
            jsonObj.put(retDict[i], retDict[i + 1]);
        }
        
        response.getWriter().println(jsonObj.toJSONString());
        response.getWriter().flush();
    }

    private synchronized String getNewDocId() {
        return "d" + docId++;
    }
    
    private synchronized String getNewUserId() {
        return "u" + userId++;
    }

    /**
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
        String staticContentDir = STATIC_ROOT_DIR;
        int port = SERVER_PORT;
        String schemaDir = DEFAULT_SCHEMA_DIR;
        String catalogFile = DEFAULT_CATALOG_FILE;
        
        if (args.length == 0) {
            System.err.println(USAGE_PROG);            
        } else if (args.length != 4) {
            System.err.println(USAGE_PROG);
            System.exit(1);
        } else if (args.length == 4) {
            staticContentDir = args[0];
            port = Integer.parseInt(args[1]);
            schemaDir = args[2];
            catalogFile = args[3];
        }
        
        System.err.println("Root dir is " + staticContentDir);
        System.err.println("Schema dir is " + (schemaDir == null ? "internal to archive" : schemaDir));
        System.err.println("Port is " + port);
        System.err.println("Catalog file is " + catalogFile);
        System.err.println("Temporary directory is " + MULTIPART_TMPDIR);
        
        CatalogTools.initializeCatalog(new File(catalogFile));
        
        GuideServer guideServer = new GuideServer(schemaDir);
        
        Server server = new Server(port);
        
        // Specify the Session ID Manager
        HashSessionIdManager idmanager = new HashSessionIdManager();
        server.setSessionIdManager(idmanager);
        
        // Sessions are bound to a context.
        ContextHandler sarOperationContext = new ContextHandler("/");
        
        // Create the SessionHandler (wrapper) to handle the sessions
        HashSessionManager manager = new HashSessionManager();
        SessionHandler sessions = new SessionHandler(manager);
        sarOperationContext.setHandler(sessions);        
        sessions.setHandler(guideServer);

        ResourceHandler rh = new ResourceHandler();
        rh.setBaseResource(Resource.newResource(staticContentDir));
        ContextHandler fileContext = new ContextHandler("/content/");
        fileContext.setHandler(rh);
        
        ContextHandlerCollection contexts = new ContextHandlerCollection();
        contexts.setHandlers(new Handler[] { sarOperationContext, fileContext });
        
        server.setHandler(contexts);        
        
        server.start();
        server.join();
    }

}
