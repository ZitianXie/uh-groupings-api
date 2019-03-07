package edu.hawaii.its.api.service;

import edu.hawaii.its.api.type.GroupingsServiceResult;
import edu.internet2.middleware.grouperClient.ws.beans.WsAssignAttributesResults;
import edu.internet2.middleware.grouperClient.ws.beans.WsAssignGrouperPrivilegesLiteResult;
import edu.internet2.middleware.grouperClient.ws.beans.WsAttributeAssign;
import edu.internet2.middleware.grouperClient.ws.beans.WsGetAttributeAssignmentsResults;
import edu.internet2.middleware.grouperClient.ws.beans.WsSubjectLookup;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("groupAttributeService")
public class GroupAttributeServiceImpl implements GroupAttributeService {

    @Value("${groupings.api.settings}")
    private String SETTINGS;

    @Value("${groupings.api.grouping_admins}")
    private String GROUPING_ADMINS;

    @Value("${groupings.api.grouping_apps}")
    private String GROUPING_APPS;

    @Value("${groupings.api.grouping_owners}")
    private String GROUPING_OWNERS;

    @Value("${groupings.api.grouping_superusers}")
    private String GROUPING_SUPERUSERS;

    @Value("${groupings.api.attributes}")
    private String ATTRIBUTES;

    @Value("${groupings.api.for_groups}")
    private String FOR_GROUPS;

    @Value("${groupings.api.for_memberships}")
    private String FOR_MEMBERSHIPS;

    @Value("${groupings.api.last_modified}")
    private String LAST_MODIFIED;

    @Value("${groupings.api.yyyymmddThhmm}")
    private String YYYYMMDDTHHMM;

    @Value("${groupings.api.uhgrouping}")
    private String UHGROUPING;

    @Value("${groupings.api.destinations}")
    private String DESTINATIONS;

    @Value("${groupings.api.listserv}")
    private String LISTSERV;

    @Value("${groupings.api.releasedgrouping}")
    private String RELEASED_GROUPING;

    @Value("${groupings.api.trio}")
    private String TRIO;

    @Value("${groupings.api.purge_grouping}")
    private String PURGE_GROUPING;

    @Value("${groupings.api.self_opted}")
    private String SELF_OPTED;

    @Value("${groupings.api.anyone_can}")
    private String ANYONE_CAN;

    @Value("${groupings.api.opt_in}")
    private String OPT_IN;

    @Value("${groupings.api.opt_out}")
    private String OPT_OUT;

    @Value("${groupings.api.basis}")
    private String BASIS;

    @Value("${groupings.api.basis_plus_include}")
    private String BASIS_PLUS_INCLUDE;

    @Value("${groupings.api.exclude}")
    private String EXCLUDE;

    @Value("${groupings.api.include}")
    private String INCLUDE;

    @Value("${groupings.api.owners}")
    private String OWNERS;

    @Value("${groupings.api.assign_type_group}")
    private String ASSIGN_TYPE_GROUP;

    @Value("${groupings.api.assign_type_immediate_membership}")
    private String ASSIGN_TYPE_IMMEDIATE_MEMBERSHIP;

    @Value("${groupings.api.subject_attribute_name_uuid}")
    private String SUBJECT_ATTRIBUTE_NAME_UID;

    @Value("${groupings.api.operation_assign_attribute}")
    private String OPERATION_ASSIGN_ATTRIBUTE;

    @Value("${groupings.api.operation_remove_attribute}")
    private String OPERATION_REMOVE_ATTRIBUTE;

    @Value("${groupings.api.operation_replace_values}")
    private String OPERATION_REPLACE_VALUES;

    @Value("${groupings.api.privilege_opt_out}")
    private String PRIVILEGE_OPT_OUT;

    @Value("${groupings.api.privilege_opt_in}")
    private String PRIVILEGE_OPT_IN;

    @Value("${groupings.api.every_entity}")
    private String EVERY_ENTITY;

    @Value("${groupings.api.is_member}")
    private String IS_MEMBER;

    @Value("${groupings.api.success}")
    private String SUCCESS;

    @Value("${groupings.api.failure}")
    private String FAILURE;

    @Value("${groupings.api.success_allowed}")
    private String SUCCESS_ALLOWED;

    @Value("${groupings.api.stem}")
    private String STEM;

    @Value("${groupings.api.person_attributes.uuid}")
    private String UUID;

    @Value("${groupings.api.person_attributes.username}")
    private String UID;

    @Value("${groupings.api.person_attributes.first_name}")
    private String FIRST_NAME;

    @Value("${groupings.api.person_attributes.last_name}")
    private String LAST_NAME;

    @Value("${groupings.api.person_attributes.composite_name}")
    private String COMPOSITE_NAME;

    public static final Log logger = LogFactory.getLog(GroupAttributeServiceImpl.class);

    @Autowired
    private GrouperFactoryService grouperFactoryService;

    @Autowired
    private HelperService helperService;

    @Autowired
    private MemberAttributeService memberAttributeService;

    @Autowired
    private MembershipService membershipService;

    //turn the listserv for a grouping on or off
    @Override
    public GroupingsServiceResult changeListservStatus(String groupingPath, String owenerUsername, boolean isListservOn) {
        return changeGroupAttributeStatus(groupingPath, owenerUsername, LISTSERV, isListservOn);
    }

    //turn the releasedGrouping attribute for a grouping on or off
    @Override
    public GroupingsServiceResult changeReleasedGroupingStatus(String groupingPath, String ownerUsername, boolean isReleasedGroupingOn) {
        return changeGroupAttributeStatus(groupingPath, ownerUsername, RELEASED_GROUPING, isReleasedGroupingOn);
    }

    //turn the ability for users to opt-in to a grouping on or off
    @Override
    public List<GroupingsServiceResult> changeOptInStatus(String groupingPath, String ownerUsername, boolean isOptInOn) {
        List<GroupingsServiceResult> results = new ArrayList<>();
        if (memberAttributeService.isOwner(groupingPath, ownerUsername) || memberAttributeService.isSuperuser(ownerUsername)) {
            results.add(assignGrouperPrivilege(EVERY_ENTITY, PRIVILEGE_OPT_IN, groupingPath + INCLUDE, isOptInOn));
            results.add(assignGrouperPrivilege(EVERY_ENTITY, PRIVILEGE_OPT_OUT, groupingPath + EXCLUDE, isOptInOn));
            results.add(changeGroupAttributeStatus(groupingPath, ownerUsername, OPT_IN, isOptInOn));
        } else {

            // adding FAILURE to the beginning of the result will turn it into an error that will be handled
            helperService.makeGroupingsServiceResult(
                    FAILURE + ", " + ownerUsername + " does not own " + groupingPath,
                    "change opt in status for " + groupingPath + " to " + isOptInOn);
        }
        return results;
    }

    //turn the ability for users to opt-out of a grouping on or off
    @Override
    public List<GroupingsServiceResult> changeOptOutStatus(String groupingPath, String ownerUsername,
                                                           boolean isOptOutOn) {
        List<GroupingsServiceResult> results = new ArrayList<>();
        if (memberAttributeService.isOwner(groupingPath, ownerUsername) || memberAttributeService.isSuperuser(ownerUsername)) {
            results.add(assignGrouperPrivilege(EVERY_ENTITY, PRIVILEGE_OPT_IN, groupingPath + EXCLUDE, isOptOutOn));
            results.add(assignGrouperPrivilege(EVERY_ENTITY, PRIVILEGE_OPT_OUT, groupingPath + INCLUDE, isOptOutOn));
            results.add(changeGroupAttributeStatus(groupingPath, ownerUsername, OPT_OUT, isOptOutOn));
        } else {

            // adding FAILURE to the beginning of the result will turn it into an error that will be handled
            helperService.makeGroupingsServiceResult(
                    FAILURE + ", " + ownerUsername + " does not own " + groupingPath,
                    "change opt out status for " + groupingPath + " to " + isOptOutOn);
        }
        return results;
    }

    //returns true if the grouping has a listserv, false otherwise
    @Override
    public boolean isContainingListserv(String groupingPath) {
        return isGroupHasAttribute(groupingPath, LISTSERV);
    }

    //returns true if the grouping has LDAP, false otherwise
    @Override
    public boolean isContainingReleasedGrouping(String groupingPath) {
        return isGroupHasAttribute(groupingPath, RELEASED_GROUPING);
    }

    //returns true if the grouping allows the user to opt out, false otherwise
    @Override
    public boolean isOptOutPossible(String groupingPath) {
        return isGroupHasAttribute(groupingPath, OPT_OUT);
    }

    //returns true if the grouping allows the user to opt in, false otherwise
    @Override
    public boolean isOptInPossible(String groupingPath) {
        return isGroupHasAttribute(groupingPath, OPT_IN);
    }

    //turns the attribute on or off in a group
    public GroupingsServiceResult changeGroupAttributeStatus(String groupPath, String ownerUsername,
                                                             String attributeName, boolean isAttributeOn) {
        GroupingsServiceResult gsr;

        String verb = "removed from ";
        if (isAttributeOn) {
            verb = "added to ";
        }
        String action = attributeName + " has been " + verb + groupPath + " by " + ownerUsername;

        if (memberAttributeService.isOwner(groupPath, ownerUsername) || memberAttributeService.isSuperuser(ownerUsername)) {
            boolean isHasAttribute = isGroupHasAttribute(groupPath, attributeName);
            if (isAttributeOn) {
                if (!isHasAttribute) {
                    assignGroupAttributes(attributeName, OPERATION_ASSIGN_ATTRIBUTE, groupPath);

                    gsr = helperService.makeGroupingsServiceResult(SUCCESS, action);

                    //todo should we return this result as well?
                    membershipService.updateLastModified(groupPath);
                } else {
                    gsr = helperService.makeGroupingsServiceResult(SUCCESS + ", " + attributeName + " already existed", action);
                }
            } else {
                if (isHasAttribute) {
                    assignGroupAttributes(attributeName, OPERATION_REMOVE_ATTRIBUTE, groupPath);

                    gsr = helperService.makeGroupingsServiceResult(SUCCESS, action);

                    //todo should we return this result as well?
                    membershipService.updateLastModified(groupPath);
                } else {
                    gsr = helperService.makeGroupingsServiceResult(SUCCESS + ", " + attributeName + " did not exist", action);
                }
            }
        } else {
            gsr = helperService.makeGroupingsServiceResult(
                    FAILURE + ", " + ownerUsername + "does not have permission to set " + attributeName
                            + " because " + ownerUsername + " does not own " + groupPath, action);
        }

        return gsr;
    }

    //returns true if the group has the attribute with that name
    public boolean isGroupHasAttribute(String groupPath, String attributeName) {
        WsGetAttributeAssignmentsResults wsGetAttributeAssignmentsResults = attributeAssignmentsResults(
                ASSIGN_TYPE_GROUP,
                groupPath,
                attributeName);

        if (wsGetAttributeAssignmentsResults.getWsAttributeAssigns() != null) {
            for (WsAttributeAssign attribute : wsGetAttributeAssignmentsResults.getWsAttributeAssigns()) {
                if (attribute.getAttributeDefNameName() != null && attribute.getAttributeDefNameName()
                        .equals(attributeName)) {
                    return true;
                }
            }
        }
        return false;
    }

    //checks to see if a group has an attribute of a specific type and returns the list if it does
    @Override
    public WsGetAttributeAssignmentsResults attributeAssignmentsResults(String assignType, String groupPath,
                                                                        String attributeName) {
        logger.info("attributeAssignmentsResults; assignType: "
                + assignType
                + "; group: "
                + groupPath
                + "; nameName: "
                + attributeName
                + ";");

        return grouperFactoryService.makeWsGetAttributeAssignmentsResultsForGroup(assignType, attributeName, groupPath);
    }

    //adds, removes, updates (operationName) the attribute for the group
    public GroupingsServiceResult assignGroupAttributes(String attributeName, String attributeOperation,
                                                        String groupPath) {
        logger.info("assignGroupAttributes; "
                + "; attributeName: "
                + attributeName
                + "; attributeOperation: "
                + attributeOperation
                + "; group: "
                + groupPath
                + ";");

        WsAssignAttributesResults attributesResults = grouperFactoryService.makeWsAssignAttributesResultsForGroup(
                ASSIGN_TYPE_GROUP,
                attributeOperation,
                attributeName,
                groupPath);

        return helperService.makeGroupingsServiceResult(attributesResults,
                "assign " + attributeName + " attribute to " + groupPath);
    }

    //gives the user the privilege for that group
    public GroupingsServiceResult assignGrouperPrivilege(
            String username,
            String privilegeName,
            String groupPath,
            boolean isSet) {

        logger.info("assignGrouperPrivilege; username: "
                + username
                + "; group: "
                + groupPath
                + "; privilegeName: "
                + privilegeName
                + " set: "
                + isSet
                + ";");

        WsSubjectLookup lookup = grouperFactoryService.makeWsSubjectLookup(username);
        String action = "set " + privilegeName + " " + isSet + " for " + username + " in " + groupPath;

        WsAssignGrouperPrivilegesLiteResult grouperPrivilegesLiteResult =
                grouperFactoryService.makeWsAssignGrouperPrivilegesLiteResult(
                        groupPath,
                        privilegeName,
                        lookup,
                        isSet);

        return helperService.makeGroupingsServiceResult(grouperPrivilegesLiteResult, action);
    }

}
