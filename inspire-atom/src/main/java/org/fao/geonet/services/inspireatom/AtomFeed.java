package org.fao.geonet.services.inspireatom;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import jeeves.server.context.ServiceContext;
import jeeves.server.dispatchers.ServiceManager;
import org.apache.commons.lang.StringUtils;
import org.fao.geonet.ApplicationContextHolder;
import org.fao.geonet.Constants;
import org.fao.geonet.Logger;
import org.fao.geonet.constants.Geonet;
import org.fao.geonet.domain.Metadata;
import org.fao.geonet.exceptions.MetadataNotFoundEx;
import org.fao.geonet.inspireatom.util.InspireAtomUtil;
import org.fao.geonet.kernel.DataManager;
import org.fao.geonet.kernel.SchemaManager;
import org.fao.geonet.kernel.search.SearchManager;
import org.fao.geonet.kernel.search.facet.ItemConfig;
import org.fao.geonet.kernel.search.facet.SummaryType;
import org.fao.geonet.kernel.search.keyword.XmlParams;
import org.fao.geonet.kernel.setting.SettingManager;
import org.fao.geonet.repository.MetadataRepository;
import org.fao.geonet.services.metadata.format.FormatType;
import org.fao.geonet.utils.Log;
import org.fao.geonet.utils.Xml;
import org.jdom.Element;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.NativeWebRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by fgravin on 7/15/15.
 */

@Controller
public class AtomFeed {

    private Logger logger = Log.createLogger(Geonet.ATOM);

    @RequestMapping(value = "/{uiLang}/atom.service/{uuid}")
    @ResponseBody
    public HttpEntity<byte[]> deprecatedAPI(
            @PathVariable String uiLang,
            @PathVariable String uuid,
            NativeWebRequest webRequest) throws Exception {

        return getAtomFeed(uiLang, uuid, webRequest);
    }

    @RequestMapping(value = "/{uiLang}/atom.service")
    @ResponseBody
    public HttpEntity<byte[]> deprecatedAPI(
            @PathVariable String uiLang,
            NativeWebRequest webRequest) throws Exception {

        return getAtomFeed(uiLang, null, webRequest);
    }

    /**
     *
     * @param uiLang main geonetwork language
     * @param uuid (optional) if set, get the service identified by the uuid
     * @param webRequest
     *
     * @return Map that will be converted in JSON for response
     */
    private HttpEntity<byte[]> getAtomFeed(String uiLang, String uuid, NativeWebRequest webRequest) throws Exception {
        ServiceContext context = createServiceContext(uiLang,
                webRequest.getNativeRequest(HttpServletRequest.class));

        SearchManager searchManager = context.getBean(SearchManager.class);
        SettingManager sm = context.getBean(SettingManager.class);
        DataManager dm = context.getBean(DataManager.class);

        boolean inspireEnable = sm.getValueAsBool("system/inspire/enable");

        if (!inspireEnable) {
            Log.info(Geonet.ATOM, "Inspire is disabled");
            throw new Exception("Inspire is disabled");
        }

        Map<String, Object> results = Maps.newLinkedHashMap();

        Metadata iso19139Metadata;

        // Check if metadata exists
        String id = dm.getMetadataId(uuid);
        if (StringUtils.isEmpty(id)) throw new MetadataNotFoundEx(uuid);

        // Check if it is a service metadata
        Element md = dm.getMetadata(id);
        String schema = dm.getMetadataSchema(id);
        if (!InspireAtomUtil.isServiceMetadata(dm, schema, md)) {
            throw new Exception("No service metadata found with uuid:" + uuid);
        }

        String atomFeed = InspireAtomUtil.convertIso19119ToAtomFeed(schema, md, dm);
        return writeOutResponse(atomFeed);


/*
        if(uuid != null) {
            iso19139Metadata = context.getBean(MetadataRepository.class).findOneByUuid(uuid);
        }



        try {
            logger.info("get ATOM feed " + uuid);
            String atomProtocol = sm.getValue("system/inspire/atomProtocol");

            // Retrieve the SERVICE metadata referencing atom feed documents
            Map<String, String> serviceMetadataWithAtomFeeds =
                    InspireAtomUtil.retrieveServiceMetadataWithAtomFeeds(dataMan, iso19139Metadata, atomProtocol);

            results.put("success", "true");
            results.put("uuids", new ArrayList(serviceMetadataWithAtomFeeds.keySet()));

        }
        catch (Exception x) {
            logger.error("ATOM feed get error: " + x.getMessage());
            results.put("success", "false");
            results.put("msg", x.getMessage());
            x.printStackTrace();
        }

        return results;
*/
    }

    private ServiceContext createServiceContext(String lang, HttpServletRequest request) {
        final ServiceManager serviceManager = ApplicationContextHolder.get().getBean(ServiceManager.class);
        return serviceManager.createServiceContext("atom.service", lang, request);
    }

    private HttpEntity<byte[]> writeOutResponse(String content) throws Exception {
        byte[] documentBody = content.getBytes();

        HttpHeaders header = new HttpHeaders();
        header.setContentType(new MediaType("application", "xml"));
        header.setContentLength(documentBody.length);
        return new HttpEntity<byte[]>(documentBody, header);
    }

}
