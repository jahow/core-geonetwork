package org.fao.geonet.services.inspireatom;

import java.util.HashMap;
import java.util.Map;

import javassist.NotFoundException;

import javax.servlet.http.HttpServletRequest;

import jeeves.server.ServiceConfig;
import jeeves.server.context.ServiceContext;
import jeeves.server.dispatchers.ServiceManager;

import org.apache.commons.lang.StringUtils;
import org.fao.geonet.ApplicationContextHolder;
import org.fao.geonet.constants.Geonet;
import org.fao.geonet.domain.Metadata;
import org.fao.geonet.domain.ReservedOperation;
import org.fao.geonet.exceptions.BadParameterEx;
import org.fao.geonet.exceptions.MetadataNotFoundEx;
import org.fao.geonet.exceptions.ObjectNotFoundEx;
import org.fao.geonet.exceptions.UnAuthorizedException;
import org.fao.geonet.inspireatom.util.InspireAtomUtil;
import org.fao.geonet.kernel.DataManager;
import org.fao.geonet.kernel.search.MetaSearcher;
import org.fao.geonet.kernel.search.SearchManager;
import org.fao.geonet.kernel.search.SearcherType;
import org.fao.geonet.kernel.setting.SettingManager;
import org.fao.geonet.lib.Lib;
import org.fao.geonet.repository.MetadataRepository;
import org.fao.geonet.utils.Log;
import org.fao.geonet.utils.Xml;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Text;
import org.jdom.xpath.XPath;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.NativeWebRequest;


/**
 * Created by fgravin on 7/15/15.
 */

@Controller
public class AtomFeed {

    @RequestMapping(value = "/{uiLang}/atom.service/{uuid}")
    @ResponseBody
    public HttpEntity<byte[]> localServiceDescribe(
            @PathVariable String uiLang,
            @PathVariable String uuid,
            NativeWebRequest webRequest) throws Exception {

        ServiceContext context = createServiceContext(uiLang, webRequest.getNativeRequest(HttpServletRequest.class));
        Element feed = getServiceFeed(context, uuid);

        return writeOutResponse(Xml.getString(feed));
    }

    /**
     * Main entry point for local dataset ATOM sub-feeds description.
     *
     * @param uiLang the language parameter
     * @param webRequest
     * @return the HTTP response corresponding to the expected ATOM feed
     * @throws Exception
     */
    @RequestMapping(value = "/{uiLang}/atom.dataset")
    @ResponseBody
    public HttpEntity<byte[]> localDatasetDescribe(
            @PathVariable String uiLang,
            @RequestParam("spatial_dataset_identifier_code") String spIdentifier,
            @RequestParam("spatial_dataset_identifier_namespace") String spNamespace,
            NativeWebRequest webRequest) throws Exception {

        ServiceContext context = createServiceContext(uiLang, webRequest.getNativeRequest(HttpServletRequest.class));
        Element feed = getDatasetFeed(context, spIdentifier);
        return writeOutResponse(Xml.getString(feed));
    }

    private Element getDatasetFeed(ServiceContext context, final String spIdentifier) throws Exception {

        MetadataRepository repo = context.getBean(MetadataRepository.class);
        Metadata datasetMd = repo.findOneByUuid(spIdentifier);

        if (datasetMd == null) {
            throw new ObjectNotFoundEx("metadata " + spIdentifier + " not found");
        }
        // check user's rights
        try {
            Lib.resource.checkPrivilege(context, ""+datasetMd.getId(), ReservedOperation.view);
        } catch (Exception e) {
            // This does not return a 403 as expected Oo
            throw new UnAuthorizedException("Acces denied to metadata " +datasetMd.getUuid(), e);
        }
        String serviceMdUuid = null;
        Document luceneParamSearch = new Document(new Element("request").
                addContent(new Element("operatesOn").setText(datasetMd.getUuid())).
                addContent(new Element("from").setText("1")).
                addContent(new Element("to").setText("1000")).
                addContent(new Element("fast").setText("index")));
        ServiceConfig config = new ServiceConfig();
        SearchManager searchMan = context.getBean(SearchManager.class);
        try (MetaSearcher searcher = searchMan.newSearcher(SearcherType.LUCENE, Geonet.File.SEARCH_LUCENE)) {
            searcher.search(context, luceneParamSearch.getRootElement(), config);
            Element searchResult = searcher.present(context, luceneParamSearch.getRootElement(), config);

            XPath xp = XPath.newInstance("//response/metadata/geonet:info/uuid/text()");
            xp.addNamespace("geonet", "http://www.fao.org/geonetwork");

            Text uuidTxt = (Text) xp.selectSingleNode(new Document(searchResult));
            serviceMdUuid = uuidTxt.getText();
        } finally {
            if (serviceMdUuid == null) {
                throw new ObjectNotFoundEx("No related service metadata found");
            }
        }

        Map<String, Object> params = new HashMap<String,Object>();
        params.put("isLocal", true);
        // TODO: This could be included as a /root/serviceMd/.../ instead
        params.put("serviceFeedTitle", "PIGMA service Metadata");

        DataManager dm = context.getBean(DataManager.class);
        SettingManager sm = context.getBean(SettingManager.class);
        Element inputDoc = InspireAtomUtil.prepareDatasetFeedEltBeforeTransform(datasetMd.getXmlData(false),
                serviceMdUuid, getBaseURL(sm, context), context.getLanguage());

        Element transformed = InspireAtomUtil.convertDatasetMdToAtom("iso19139", inputDoc, dm, params);
        return transformed;
    }


    private Element getServiceFeed(ServiceContext context, final String uuid) throws Exception {

        SettingManager sm = context.getBean(SettingManager.class);
        DataManager dm = context.getBean(DataManager.class);

        boolean inspireEnable = sm.getValueAsBool("system/inspire/enable");

        if (!inspireEnable) {
            Log.info(Geonet.ATOM, "Inspire is disabled");
            throw new BadParameterEx("system/inspire/enable. Please activate INSPIRE before trying to use the service.", inspireEnable);
        }

        // Check if metadata exists
        String id = dm.getMetadataId(uuid);
        if (StringUtils.isEmpty(id)) throw new MetadataNotFoundEx(uuid);

        // Check if it is a service metadata

        // TODO: NO ACLs checks are currently done, any user can call this service
        // to get potential restricted infos.
        //
        // One has to call Lib.resource.checkPrivilege(context, id, ReservedOperation.view);

        Element md = dm.getMetadata(id);
        String schema = dm.getMetadataSchema(id);
        if (!InspireAtomUtil.isServiceMetadata(dm, schema, md)) {
            throw new NotFoundException("No service metadata found with uuid:" + uuid);
        }

        Element inputDoc = InspireAtomUtil.prepareServiceFeedEltBeforeTransform(schema, md, dm,
                getBaseURL(sm, context), context.getLanguage());

        Element transformed = InspireAtomUtil.convertDatasetMdToAtom("iso19139", inputDoc, dm, null);
        return transformed;
    }


    private String getBaseURL(SettingManager settingManager, ServiceContext context) {
        String baseUrl = settingManager.getSiteURL(context);
        return baseUrl.substring(0, baseUrl.length()-5);
    }

    private ServiceContext createServiceContext(String lang, HttpServletRequest request) {
        final ServiceManager serviceManager = ApplicationContextHolder.get().getBean(ServiceManager.class);
        return serviceManager.createServiceContext("atom.service", lang, request);
    }

    private HttpEntity<byte[]> writeOutResponse(String content) throws Exception {
        byte[] documentBody = content.getBytes();

        HttpHeaders header = new HttpHeaders();
        // TODO: character-set encoding ?
        header.setContentType(new MediaType("application", "xml"));
        header.setContentLength(documentBody.length);
        return new HttpEntity<byte[]>(documentBody, header);
    }

}
