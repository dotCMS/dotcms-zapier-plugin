package com.dotcms.plugin.dotzapier.zapier.content;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.rest.MapToContentletPopulator;
import com.dotcms.util.ConversionUtils;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.util.json.JSONException;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ContentAPI {

    public static final String JSON_FORMAT = "json";
    public static final String CSV_FORMAT  = "csv";

    private final Map<String, WorkflowAPI.SystemAction> actionNameSystemActionMap = getActionNameSystemActionMap();
    private final MapToContentletPopulator mapToContentletPopulator = MapToContentletPopulator.INSTANCE;

    private Map<String, WorkflowAPI.SystemAction> getActionNameSystemActionMap() {

        final LinkedHashMap<String, WorkflowAPI.SystemAction> map = new LinkedHashMap<>();
        map.put("save", WorkflowAPI.SystemAction.NEW);
        map.put("edit", WorkflowAPI.SystemAction.EDIT);
        map.put("publish", WorkflowAPI.SystemAction.PUBLISH);
        map.put("unpublish", WorkflowAPI.SystemAction.UNPUBLISH);
        map.put("archive", WorkflowAPI.SystemAction.ARCHIVE);
        map.put("unarchive", WorkflowAPI.SystemAction.UNARCHIVE);
        map.put("delete", WorkflowAPI.SystemAction.DELETE);
        return map;
    }

    private final Map<String, String> contentInputFormatMap = getContentInputFormatMap();

    private Map<String, String> getContentInputFormatMap() {

        final LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put(JSON_FORMAT, JSON_FORMAT);
        map.put(CSV_FORMAT, CSV_FORMAT);
        return map;
    }

    public Map<String, String> contentInputFormatMap() {

        return this.contentInputFormatMap;
    }

    /**
     * Get the action names
     * @return Map
     */
    public Map<String, String> actionNames () {

        final LinkedHashMap<String, String> actionNames = new LinkedHashMap<>();

        for (final String actionName: this.actionNameSystemActionMap.keySet()) {

            actionNames.put(actionName, actionName);
        }

        return actionNames;
    }

    /**
     * Get all content types based on the parameter allowedContentTypes
     * @param allowedContentTypes {@link Set}
     * @return Map
     * @throws DotDataException
     */
    public Map<String, String> types (final Set<String> allowedContentTypes) throws DotDataException {

        return APILocator.getContentTypeAPI(APILocator.systemUser()).findAll()
                .stream().filter(contentType -> allowedContentTypes.contains(contentType.variable()))
                .collect(Collectors.toMap(ContentType::variable, ContentType::name));
    }

    /**
     * Get all content types based on the parameter
     * @return Map
     * @throws DotDataException
     */
    public Map<String, String> types () throws DotDataException {

        return APILocator.getContentTypeAPI(APILocator.systemUser()).findAll()
                .stream()
                .collect(Collectors.toMap(ContentType::variable, ContentType::name));
    }

    /**
     * Retrieve the first 10 contents
     * @return List
     * @throws DotDataException
     */
    public List<Contentlet> contents() throws DotDataException {

        return APILocator.getContentletAPI().findAllContent(0, 10);
    }

    public List<Contentlet> contents(final Set<String> allowedContentTypes) throws DotDataException, DotSecurityException {

        final List<Contentlet> contentSamples = new ArrayList<>();
        final ContentTypeAPI contentTypeAPI   = APILocator.getContentTypeAPI(APILocator.systemUser());

        for (final String contentTypeVar : allowedContentTypes) {

            final ContentType contentType = contentTypeAPI.find(contentTypeVar);
            if (null != contentType) {
                contentSamples.addAll(APILocator.getContentletAPI()
                        .findByStructure(contentType.inode(), APILocator.systemUser(), false, 1, 0));
            }
        }

        return contentSamples.isEmpty()? this.contents(): contentSamples;
    }

    /**
     * Fire an action over a contentlet
     * @param dotCMSContent
     * @param contentType
     * @param actionName
     * @param user
     * @return Contentlet
     * @throws DotDataException
     * @throws DotSecurityException
     */
    public Contentlet fire (final Map<String, Object> dotCMSContent, final String contentType, final String actionName,
                            final User user) throws DotDataException, DotSecurityException, JSONException {

        final ContentType type      = APILocator.getContentTypeAPI(user).find(contentType);
        if (null == type) {
            throw new DoesNotExistException("The type: " + contentType + ", does not exits");
        }
        Contentlet contentlet = new Contentlet();
        contentlet.setContentType(type);
        contentlet = this.populateContent(contentlet, dotCMSContent, user);

        final WorkflowAPI.SystemAction systemAction      = this.findSystemAction(actionName);
        final Optional<WorkflowAction> workflowActionOpt = // ask to see if there is any default action by content type or scheme
                APILocator.getWorkflowAPI().findActionMappedBySystemActionContentlet
                        (contentlet, systemAction, user);

        if (workflowActionOpt.isPresent()) {

            final ContentletDependencies contentletDependencies = new ContentletDependencies.Builder()
                    .modUser(user).respectAnonymousPermissions(false).workflowActionId(workflowActionOpt.get().getId()).build();
            return APILocator.getWorkflowAPI().fireContentWorkflow(contentlet, contentletDependencies);
        }

        return APILocator.getContentletAPI().checkin(contentlet, user, false);
    }

    private Contentlet populateContent(final Contentlet contentlet,
                                 final Map<String, Object> dotCMSContent,
                                 final User user) throws JSONException, DotDataException, DotSecurityException {

        return dotCMSContent.containsKey("id") || dotCMSContent.containsKey("identifier")?
                this.populateContentExistingOne(contentlet, dotCMSContent, user):
                this.mapToContentletPopulator.populate(contentlet, dotCMSContent);
    }

    private Contentlet populateContentExistingOne(Contentlet localContentlet,
                                       final Map<String, Object> dotCMSContent,
                                       final User user) throws JSONException, DotDataException, DotSecurityException {

        String identifier = null;
        long languageId   = -1;
        if (dotCMSContent.containsKey("id")) {

            localContentlet.setIdentifier(dotCMSContent.get("id").toString());
            identifier = localContentlet.getIdentifier();
        }

        if (dotCMSContent.containsKey("identifier")) {

            localContentlet.setIdentifier(dotCMSContent.get("identifier").toString());
            identifier = localContentlet.getIdentifier();
        }

        if (dotCMSContent.containsKey("languageId")) {

            localContentlet.setLanguageId(ConversionUtils.toLong(dotCMSContent.get("languageId").toString(), -1l));
            languageId = localContentlet.getLanguageId();
        }

        if (null != identifier) {

            final Optional<Contentlet> currentContentlet =  languageId <= 0?
                    Optional.ofNullable(APILocator.getContentletAPI().findContentletByIdentifier(
                            identifier, false, APILocator.getLanguageAPI().getDefaultLanguage().getId(), user, false)):
                    APILocator.getContentletAPI().findContentletByIdentifierOrFallback
                            (identifier, false, languageId, user, false);

            if(currentContentlet.isPresent()) {
                localContentlet = currentContentlet.get();
                final String inode = localContentlet.getInode();
                localContentlet = this.mapToContentletPopulator.populate(localContentlet, dotCMSContent);
                localContentlet.setInode(inode);
                return localContentlet;
            }
        }

        return this.mapToContentletPopulator.populate(localContentlet, dotCMSContent);
    }

    private WorkflowAPI.SystemAction findSystemAction(final String actionName) {

        return this.actionNameSystemActionMap.getOrDefault(actionName, WorkflowAPI.SystemAction.NEW);
    }

    public String generateEditContentletURL (final Contentlet contentlet) {

        String url = "dotAdmin/#/c/content/" + contentlet.getInode();
        final Company company = APILocator.getCompanyAPI().getDefaultCompany();
        if (null != company) {

            String portalUrl = company.getPortalURL();
            if (!portalUrl.trim().endsWith("/")) {
                portalUrl = portalUrl.trim() + "/";
            }

            url = portalUrl + url;
        }

        return url;
    }

    public String createContentKey (final Contentlet contentlet, final String propertyName) {

        final String contentTypeVar = contentlet.getContentType().variable();
        return contentTypeVar + StringPool.UNDERLINE + propertyName;
    }

}
