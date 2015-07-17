package org.fao.geonet.services.inspireatom;

import java.util.Map;

import javassist.NotFoundException;

import javax.servlet.http.HttpServletRequest;

import jeeves.server.context.ServiceContext;
import jeeves.server.dispatchers.ServiceManager;

import org.apache.commons.lang.StringUtils;
import org.fao.geonet.ApplicationContextHolder;
import org.fao.geonet.Logger;
import org.fao.geonet.constants.Geonet;
import org.fao.geonet.domain.Metadata;
import org.fao.geonet.exceptions.BadParameterEx;
import org.fao.geonet.exceptions.MetadataNotFoundEx;
import org.fao.geonet.inspireatom.util.InspireAtomUtil;
import org.fao.geonet.kernel.DataManager;
import org.fao.geonet.kernel.search.SearchManager;
import org.fao.geonet.kernel.setting.SettingManager;
import org.fao.geonet.utils.Log;
import org.jdom.Element;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.NativeWebRequest;

import com.google.common.collect.Maps;

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
            throw new BadParameterEx("system/inspire/enable. Please activate INSPIRE before trying to use the service.", inspireEnable);
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
            throw new NotFoundException("No service metadata found with uuid:" + uuid);
        }

        String baseUrl = sm.getSiteURL(context);
        baseUrl = baseUrl.substring(0, baseUrl.length()-5);
        String lang = context.getLanguage();
        Element inputDoc = InspireAtomUtil.createInputElement(schema, md, dm, baseUrl, lang);
        String atomFeed = InspireAtomUtil.convertIso19119ToAtomFeed(schema, inputDoc, dm, true);
        
        return writeOutResponse(atomFeed);
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
