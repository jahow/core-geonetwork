package org.fao.geonet.inspireatom.util;

import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.fao.geonet.kernel.DataManager;
import org.fao.geonet.utils.Xml;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class InspireAtomUtilTest {

    @Before
    public void setUp() {
        // Using saxon for XSL transformations
        System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");    
    }
    
    private DataManager getDataManager() {
        DataManager dm = Mockito.mock(DataManager.class);
        Mockito.when(dm.getSchemaDir(Mockito.anyString())).thenReturn(Paths.get("../schemas/iso19139/src/main/plugin/iso19139/"));
        return dm;
    }

    private String testGlobalTransform(boolean local) throws Exception {
        URL testSceMd = this.getClass().getResource("serviceMd.xml");
        URL testDataMd = this.getClass().getResource("dataMd.xml");
        
        assertTrue("file serviceMd.xml not found", testSceMd != null);
        assertTrue("file dataMd.xml not found", testDataMd != null);

        SAXBuilder builder = new SAXBuilder();
        Document sceDoc = builder.build(testSceMd);
        Element rootSceElem = sceDoc.getRootElement();
        rootSceElem.detach();
        Document dataDoc = builder.build(testDataMd);
        Element rootDataElem = dataDoc.getRootElement();
        rootDataElem.detach();
        Document docToTransform = new Document(new Element("root"));
        docToTransform.getRootElement().addContent(new Element("service").addContent(rootSceElem));
        docToTransform.getRootElement().addContent(new Element("dataset").addContent(rootDataElem));

        String ret = InspireAtomUtil.convertIso19119ToAtomFeed("iso19139", docToTransform.getRootElement(), getDataManager(), local);

        // Should contain the expected title
        assertTrue("Expected title not found",
                ret.contains("<atom:title>INSPIRE Download Service Atom feed for my ATOM service</atom:title>"));
        // Should contain a title for the sub-ATOM feed (dataset WMS1)
        assertTrue("Expected a title for the dataset, not found",
                ret.contains("<atom:title>INSPIRE Dataset Atom feed for WMS 1</atom:title>"));
        // should contain an URL composed of the parameters for the sub-ATOM feed
        assertTrue("Expected sub-ATOM feed parameters not found (spatial_dataset_identifier_code, spatial_dataset_identifier_namespace)",
                ret.contains("spatial_dataset_identifier_code=f7d22d90-7651-11e0-a1f0-0800200c9a63&amp;spatial_dataset_identifier_namespace=http://www.cultureelerfgoed.nl"));
        
        return ret;
    }
    
    /**
     * Tests the remote version of the trasnformation (default).
     * @throws Exception
     */
    @Test
    public void testRemoteTransform() throws Exception {
        String result = testGlobalTransform(false);
        // Should contain
        
        // Should not contain any traces of local.decribe
        assertTrue("transformed result contains unexpected local services urls",
                ! result.contains("atom.local.describe?spatial_dataset_identifier_code"));
    }

    /**
     * Tests the local version of the transformation for service MDs. This one is implied by the Spring 
     * service AtomFeed, @see AtomFeed.java.
     *
     * @throws Exception
     */
    @Test
    public void testLocalTransform() throws Exception {
        String result = testGlobalTransform(true);

        // Should contain a reference to local.decribe
        assertTrue("transformed result does not contain URL to atom.local.describe",
                result.contains("<atom:id>/atom.local.describe?spatial_dataset_identifier_code=" +
                        "f7d22d90-7651-11e0-a1f0-0800200c9a63&amp;spatial_dataset_identifier_namespace=" +
                        "http://www.cultureelerfgoed.nl</atom:id>"));
    }
    
    /**
     * Tests the local version of the transformation for the dataset MDs. This one is implied by the Spring 
     * service AtomFeed, @see AtomFeed.java.
     *
     * @throws Exception
     */
    @Test
    public void testLocalDatasetTransform() throws Exception {
        URL testDataMd = this.getClass().getResource("dataMd.xml");
        assertTrue("file dataMd.xml not found", testDataMd != null);
        SAXBuilder builder = new SAXBuilder();
        Document dataDoc = builder.build(testDataMd);
        Element rootDataElem = dataDoc.getRootElement();

        Document doc = new Document();
        doc.addContent(new Element("root").addContent(new Element("dataset").addContent(rootDataElem.detach())));
        doc.getRootElement().getChild("dataset").addContent(new Element("serviceIdentifier").setText("bed3dfd3-b3db-48d1-8a4d-edf16201dc2c"));
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("isLocal", true);
        // TODO: This could be included as a /root/serviceMd/.../ instead
        params.put("serviceFeedTitle", "PIGMA service Metadata");

        Element ret = InspireAtomUtil.convertDatasetMdToAtom("iso19139", doc.getRootElement(), getDataManager(), params);
        
        String atomStr = Xml.getString(ret);
        // Should contain a link to the atom.local.describe service
        assertTrue("Unable to find the URL to the ATOM local dataset service",
                atomStr.contains("<atom:id>/atom.local.describe?spatial_dataset_identifier_code=f7d22d90-7651-11e0-a1f0-0800200c9a63&amp;"+
                          "spatial_dataset_identifier_namespace=http://www.cultureelerfgoed.nl</atom:id>"));
        
        // Should contain a link to the atom.service
        assertTrue("Missing link to the MD ATOM service",
                atomStr.contains("href=\"//atom.service/bed3dfd3-b3db-48d1-8a4d-edf16201dc2c\""));
        
        // Should contain a link to the current ZIP file
        assertTrue("Missing link to the attached file",
                atomStr.contains("link title=\"PLU ville de Bidule\" rel=\"alternate\" type=\"application/x-compressed\" "+
                        "href=\"http://localhost:8080/geonetwork/srv/eng/resources.get"));
    }

}
