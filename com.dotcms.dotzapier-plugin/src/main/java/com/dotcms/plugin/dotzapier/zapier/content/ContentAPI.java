package com.dotcms.plugin.dotzapier.zapier.content;

import com.dotcms.contenttype.model.field.Field;
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
import com.liferay.portal.model.User;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ContentAPI {

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
     * Get all content types
     * @return Map
     * @throws DotDataException
     */
    public Map<String, String> types () throws DotDataException {

        return APILocator.getContentTypeAPI(APILocator.systemUser()).findAll()
                .stream().collect(Collectors.toMap(ContentType::variable, ContentType::name));
    }

    /**
     * Retrieve the first 10 contents
     * @return List
     * @throws DotDataException
     */
    public List<Contentlet> contents() throws DotDataException {

        return APILocator.getContentletAPI().findAllContent(0, 10);
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
        final Contentlet contentlet = new Contentlet();
        contentlet.setContentType(type);
        final WorkflowAPI.SystemAction systemAction      = this.findSystemAction(actionName);
        final Optional<WorkflowAction> workflowActionOpt = // ask to see if there is any default action by content type or scheme
                APILocator.getWorkflowAPI().findActionMappedBySystemActionContentlet
                        (contentlet, systemAction, user);

        this.populateContent(contentlet, dotCMSContent);
        if (workflowActionOpt.isPresent()) {

            final ContentletDependencies contentletDependencies = new ContentletDependencies.Builder()
                    .modUser(user).respectAnonymousPermissions(false).workflowActionId(workflowActionOpt.get().getId()).build();
            return APILocator.getWorkflowAPI().fireContentWorkflow(contentlet, contentletDependencies);
        }

        return APILocator.getContentletAPI().checkin(contentlet, user, false);
    }

    private void populateContent(final Contentlet contentlet,
                                    final Map<String, Object> dotCMSContent) throws JSONException {

        this.mapToContentletPopulator.populate(contentlet, dotCMSContent);

        if (dotCMSContent.containsKey("id")) {

            contentlet.setIdentifier(dotCMSContent.get("id").toString());
        }
    }

    private WorkflowAPI.SystemAction findSystemAction(final String actionName) {

        return this.actionNameSystemActionMap.getOrDefault(actionName, WorkflowAPI.SystemAction.NEW);
    }
}
