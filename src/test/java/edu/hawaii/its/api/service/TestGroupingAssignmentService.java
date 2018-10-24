package edu.hawaii.its.api.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import edu.hawaii.its.api.type.AdminListsHolder;
import edu.hawaii.its.api.type.Group;
import edu.hawaii.its.api.type.Grouping;
import edu.hawaii.its.api.type.GroupingAssignment;
import edu.hawaii.its.api.type.GroupingsHTTPException;
import edu.hawaii.its.api.type.GroupingsServiceResult;
import edu.hawaii.its.api.configuration.SpringBootWebApplication;

import edu.internet2.middleware.grouperClient.api.GcGetAttributeAssignments;
import edu.internet2.middleware.grouperClient.ws.beans.WsAttributeAssign;
import edu.internet2.middleware.grouperClient.ws.beans.WsGetAttributeAssignmentsResults;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.hamcrest.CoreMatchers.*;

@ActiveProfiles("integrationTest")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { SpringBootWebApplication.class })
public class TestGroupingAssignmentService {

    @Value("${groupings.api.test.grouping_many}")
    private String GROUPING;
    @Value("${groupings.api.test.grouping_many_basis}")
    private String GROUPING_BASIS;
    @Value("${groupings.api.test.grouping_many_include}")
    private String GROUPING_INCLUDE;
    @Value("${groupings.api.test.grouping_many_exclude}")
    private String GROUPING_EXCLUDE;
    @Value("${groupings.api.test.grouping_many_owners}")
    private String GROUPING_OWNERS;

    @Value("${groupings.api.test.grouping_store_empty}")
    private String GROUPING_STORE_EMPTY;
    @Value("${groupings.api.test.grouping_store_empty_include}")
    private String GROUPING_STORE_EMPTY_INCLUDE;
    @Value("${groupings.api.test.grouping_store_empty_exclude}")
    private String GROUPING_STORE_EMPTY_EXCLUDE;
    @Value("${groupings.api.test.grouping_store_empty_owners}")
    private String GROUPING_STORE_EMPTY_OWNERS;

    @Value("${groupings.api.test.grouping_true_empty}")
    private String GROUPING_TRUE_EMPTY;
    @Value("${groupings.api.test.grouping_true_empty_include}")
    private String GROUPING_TRUE_EMPTY_INCLUDE;
    @Value("${groupings.api.test.grouping_true_empty_exclude}")
    private String GROUPING_TRUE_EMPTY_EXCLUDE;
    @Value("${groupings.api.test.grouping_true_empty_owners}")
    private String GROUPING_TRUE_EMPTY_OWNERS;

    @Value("${groupings.api.yyyymmddThhmm}")
    private String YYYYMMDDTHHMM;

    @Value("${groupings.api.trio}")
    private String TRIO;

    @Value("${groupings.api.assign_type_group}")
    private String ASSIGN_TYPE_GROUP;

    @Value("${groupings.api.test.admin_user}")
    private String ADMIN;

    @Value("${groupings.api.test.usernames}")
    private String[] username;

    @Autowired
    GroupAttributeService groupAttributeService;

    @Autowired
    GroupingAssignmentService groupingAssignmentService;

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private HelperService helperService;

    @Autowired
    public Environment env; // Just for the settings check.

    @PostConstruct
    public void init() {
        Assert.hasLength(env.getProperty("grouperClient.webService.url"),
                "property 'grouperClient.webService.url' is required");
        Assert.hasLength(env.getProperty("grouperClient.webService.login"),
                "property 'grouperClient.webService.login' is required");
        Assert.hasLength(env.getProperty("grouperClient.webService.password"),
                "property 'grouperClient.webService.password' is required");
    }

    @Before
    public void setUp() {
        groupAttributeService.changeListservStatus(GROUPING, username[0], true);
        groupAttributeService.changeOptInStatus(GROUPING, username[0], true);
        groupAttributeService.changeOptOutStatus(GROUPING, username[0], true);

        //put in include
        membershipService.addGroupingMemberByUsername(username[0], GROUPING, username[0]);
        membershipService.addGroupingMemberByUsername(username[0], GROUPING, username[1]);
        membershipService.addGroupingMemberByUsername(username[0], GROUPING, username[2]);

        //remove from exclude
        membershipService.addGroupingMemberByUsername(username[0], GROUPING, username[4]);
        membershipService.addGroupingMemberByUsername(username[0], GROUPING, username[5]);

        //add to exclude
        membershipService.deleteGroupingMemberByUsername(username[0], GROUPING, username[3]);
    }

    @Test
    public void adminListsTest() {
        //try with non-admin
        AdminListsHolder info = groupingAssignmentService.adminLists(username[0]);
        assertNotNull(info);
        assertEquals(info.getAllGroupings().size(), 0);
        assertEquals(info.getAdminGroup().getMembers().size(), 0);
        assertEquals(info.getAdminGroup().getUsernames().size(), 0);
        assertEquals(info.getAdminGroup().getNames().size(), 0);
        assertEquals(info.getAdminGroup().getUuids().size(), 0);

        //todo What about with admin???
        AdminListsHolder infoAdmin = groupingAssignmentService.adminLists(ADMIN);
        assertNotNull(infoAdmin);
    }

    @Test
    public void updateLastModifiedTest() {
        // Test is accurate to the minute, and if checks to see if the current
        // time gets added to the lastModified attribute of a group if the
        // minute happens to change in between getting the time and setting
        // the time, the test will fail.

        final String group = GROUPING_INCLUDE;

        GroupingsServiceResult gsr = membershipService.updateLastModified(group);
        String dateStr = gsr.getAction().split(" to time ")[1];

        WsGetAttributeAssignmentsResults assignments =
                groupAttributeService.attributeAssignmentsResults(ASSIGN_TYPE_GROUP, group, YYYYMMDDTHHMM);
        String assignedValue = assignments.getWsAttributeAssigns()[0].getWsAttributeAssignValues()[0].getValueSystem();

        assertEquals(dateStr, assignedValue);
    }

    @Test
    public void getGroupingTest() {

        // username[4] does not own grouping, method should return empty grouping
        Grouping grouping = groupingAssignmentService.getGrouping(GROUPING, username[4]);
        assertEquals(grouping.getPath(), "");
        assertEquals(grouping.getName(), "");
        assertEquals(grouping.getOwners().getMembers().size(), 0);
        assertEquals(grouping.getInclude().getMembers().size(), 0);
        assertEquals(grouping.getExclude().getMembers().size(), 0);
        assertEquals(grouping.getBasis().getMembers().size(), 0);
        assertEquals(grouping.getComposite().getMembers().size(), 0);

        grouping = groupingAssignmentService.getGrouping(GROUPING, username[0]);

        assertEquals(grouping.getPath(), GROUPING);

        // Testing for garbage uuid basis bug fix
        // List<String> list = grouping.getBasis().getUuids();

        assertTrue(grouping.getBasis().getUsernames().contains(username[3]));
        assertTrue(grouping.getBasis().getUsernames().contains(username[4]));
        assertTrue(grouping.getBasis().getUsernames().contains(username[5]));

        assertTrue(grouping.getComposite().getUsernames().contains(username[0]));
        assertTrue(grouping.getComposite().getUsernames().contains(username[1]));
        assertTrue(grouping.getComposite().getUsernames().contains(username[2]));
        assertTrue(grouping.getComposite().getUsernames().contains(username[4]));
        assertTrue(grouping.getComposite().getUsernames().contains(username[5]));

        assertTrue(grouping.getExclude().getUsernames().contains(username[3]));

        assertTrue(grouping.getInclude().getUsernames().contains(username[0]));
        assertTrue(grouping.getInclude().getUsernames().contains(username[1]));
        assertTrue(grouping.getInclude().getUsernames().contains(username[2]));

        assertTrue(grouping.getOwners().getUsernames().contains(username[0]));
    }

    @Test
    public void getPaginatedGroupingTest() {

        // Paging starts at 1 D:
        Grouping paginatedGroupingPage1 = groupingAssignmentService.getPaginatedGrouping(GROUPING, username[0], 1, 20);
        Grouping paginatedGroupingPage2 = groupingAssignmentService.getPaginatedGrouping(GROUPING, username[0], 2, 20);

        // Check to see the pages come out the right sizes
        assertThat(paginatedGroupingPage1.getBasis().getMembers().size(), equalTo(20));
        assertThat(paginatedGroupingPage1.getInclude().getMembers().size(), equalTo(10));
        assertThat(paginatedGroupingPage1.getExclude().getMembers().size(), equalTo(1));
        assertThat(paginatedGroupingPage1.getComposite().getMembers().size(), equalTo(20));
        assertThat(paginatedGroupingPage1.getOwners().getMembers().size(), equalTo(2));

        assertThat(paginatedGroupingPage2.getBasis().getMembers().size(), equalTo(20));
        assertThat(paginatedGroupingPage2.getInclude().getMembers().size(), equalTo(0));
        assertThat(paginatedGroupingPage2.getExclude().getMembers().size(), equalTo(0));
        assertThat(paginatedGroupingPage2.getComposite().getMembers().size(), equalTo(20));
        assertThat(paginatedGroupingPage2.getOwners().getMembers().size(), equalTo(0));

        // Both pages should not be the same (assuming no groups are empty)
        assertThat(paginatedGroupingPage1.getBasis(), not(paginatedGroupingPage2.getBasis()));
        assertThat(paginatedGroupingPage1.getInclude(), not(paginatedGroupingPage2.getInclude()));
        assertThat(paginatedGroupingPage1.getExclude(), not(paginatedGroupingPage2.getExclude()));
        assertThat(paginatedGroupingPage1.getComposite(), not(paginatedGroupingPage2.getComposite()));
        assertThat(paginatedGroupingPage1.getOwners(), not(paginatedGroupingPage2.getOwners()));


        // Test paging at the end of the grouping
        Grouping paginatedGroupingPageEnd = groupingAssignmentService.getPaginatedGrouping(GROUPING, username[0], 16, 20);
        assertThat(paginatedGroupingPageEnd.getBasis().getMembers().size(), equalTo(18));

        // Test paging without proper permissions

    }

    @Test
    public void groupingsInTest() {
        GroupingAssignment groupingAssignment = groupingAssignmentService.getGroupingAssignment(username[0]);
        boolean isInGrouping = false;

        for (Grouping grouping : groupingAssignment.getGroupingsIn()) {
            if (grouping.getPath().contains(GROUPING)) {
                isInGrouping = true;
                break;
            }
        }
        assertTrue(isInGrouping);

        isInGrouping = false;
        groupingAssignment = groupingAssignmentService.getGroupingAssignment(username[3]);
        for (Grouping grouping : groupingAssignment.getGroupingsIn()) {
            if (grouping.getPath().contains(GROUPING)) {
                isInGrouping = true;
                break;
            }
        }
        assertFalse(isInGrouping);
    }

    @Test
    public void groupingsOwnedTest() {
        GroupingAssignment groupingAssignment = groupingAssignmentService.getGroupingAssignment(username[0]);
        boolean isGroupingOwner  = false;

        for (Grouping grouping : groupingAssignment.getGroupingsOwned()) {
            if (grouping.getPath().contains(GROUPING)) {
                isGroupingOwner = true;
                break;
            }
        }
        assertTrue(isGroupingOwner);

        isGroupingOwner = false;
        groupingAssignment = groupingAssignmentService.getGroupingAssignment(username[4]);
        for (Grouping grouping : groupingAssignment.getGroupingsOwned()) {
            if (grouping.getPath().contains(GROUPING)) {
                isGroupingOwner = true;
                break;
            }
        }
        assertFalse(isGroupingOwner);
    }

    @Test
    public void groupingsOptedTest() {
        //todo
    }

    @Test
    public void groupingsToOptTest() {
        GroupingAssignment groupingAssignment = groupingAssignmentService.getGroupingAssignment(username[0]);

        boolean isOptInPossible = false;
        for (Grouping grouping : groupingAssignment.getGroupingsToOptInTo()) {
            if (grouping.getPath().contains(GROUPING)) {
                isOptInPossible = true;
                break;
            }
        }
        assertFalse(isOptInPossible);

        boolean isOptOutPossible = false;
        for (Grouping grouping : groupingAssignment.getGroupingsToOptOutOf()) {
            if (grouping.getPath().contains(GROUPING)) {
                isOptOutPossible = true;
                break;
            }
        }
        assertTrue(isOptOutPossible);
    }

    @Test
    public void getMembersTest() {

        // Testing for garbage uuid basis bug fix
        // Group testGroup = groupingAssignmentService.getMembers(username[0], GROUPING_BASIS);

        Group group = groupingAssignmentService.getMembers(username[0], GROUPING);
        List<String> usernames = group.getUsernames();

        assertTrue(usernames.contains(username[0]));
        assertTrue(usernames.contains(username[1]));
        assertTrue(usernames.contains(username[2]));
        assertFalse(usernames.contains(username[3]));
        assertTrue(usernames.contains(username[4]));
        assertTrue(usernames.contains(username[5]));
    }

    @Test
    public void getGroupNamesTest() {
        List<String> groupNames1 = groupingAssignmentService.getGroupPaths(ADMIN, username[1]);
        List<String> groupNames3 = groupingAssignmentService.getGroupPaths(ADMIN, username[3]);

        //username[1] should be in the composite and the include, not basis or exclude
        assertTrue(groupNames1.contains(GROUPING));
        assertTrue(groupNames1.contains(GROUPING_INCLUDE));
        assertFalse(groupNames1.contains(GROUPING_BASIS));
        assertFalse(groupNames1.contains(GROUPING_EXCLUDE));

        //username[3] should be in the basis and exclude, not the composite or include
        assertTrue(groupNames3.contains(GROUPING_BASIS));
        assertTrue(groupNames3.contains(GROUPING_EXCLUDE));
        assertFalse(groupNames3.contains(GROUPING));
        assertFalse(groupNames3.contains(GROUPING_INCLUDE));
    }

    @Test
    public void getGroupNames() {
        List<String> groups = groupingAssignmentService.getGroupPaths(ADMIN, username[0]);

        assertTrue(groups.contains(GROUPING_OWNERS));
        assertTrue(groups.contains(GROUPING_STORE_EMPTY_OWNERS));
        assertTrue(groups.contains(GROUPING_TRUE_EMPTY_OWNERS));

        List<String> groups2 = groupingAssignmentService.getGroupPaths(ADMIN, username[1]);

        assertFalse(groups2.contains(GROUPING_OWNERS));
        assertFalse(groups2.contains(GROUPING_STORE_EMPTY_OWNERS));
        assertFalse(groups2.contains(GROUPING_TRUE_EMPTY_OWNERS));
    }

    @Test
    public void getGroupPathsPermissionsTest(){
        List<String> groups = groupingAssignmentService.getGroupPaths(ADMIN, username[0]);

        assertTrue(groups.contains(GROUPING_OWNERS));
        assertTrue(groups.contains(GROUPING_STORE_EMPTY_OWNERS));
        assertTrue(groups.contains(GROUPING_TRUE_EMPTY_OWNERS));

        List<String> groups2 = groupingAssignmentService.getGroupPaths(username[0], username[0]);

        assertTrue(groups2.contains(GROUPING_OWNERS));
        assertTrue(groups2.contains(GROUPING_STORE_EMPTY_OWNERS));
        assertTrue(groups2.contains(GROUPING_TRUE_EMPTY_OWNERS));

        List<String> groups3 = groupingAssignmentService.getGroupPaths(username[1], username[0]);
        assertThat(groups3.size(), equalTo(0));

//        try{
//            groupingAssignmentService.getGroupPaths(username[1], username[0]);
//            fail("Shouldn't be here");
//        } catch (GroupingsHTTPException ghe) {
//            assertThat(ghe.getStatusCode(), equalTo(403));
//        }
    }

    @Test
    public void grouperTest() {
        List<String> groupPaths = groupingAssignmentService.getGroupPaths(ADMIN, username[0]);

        List<String> groupings = new ArrayList<>();
        List<String> groupings2 = new ArrayList<>();

        if (groupPaths.size() > 0) {

            List<WsAttributeAssign> attributes = new ArrayList<>();

            for (String path : groupPaths) {
                WsGetAttributeAssignmentsResults trioGroups = new GcGetAttributeAssignments()
                        .addAttributeDefNameName(TRIO)
                        .assignAttributeAssignType(ASSIGN_TYPE_GROUP)
                        .addOwnerGroupName(path)
                        .execute();

                if (trioGroups.getWsAttributeAssigns() != null) {
                    Collections.addAll(attributes, trioGroups.getWsAttributeAssigns());
                }
            }

            if (attributes.size() > 0) {
                groupings.addAll(attributes.stream().map(WsAttributeAssign::getOwnerGroupName)
                        .collect(Collectors.toList()));
            }

            assertNotNull(groupings);

            //////////////////////////////////////////////////////////////////////////////////

            GcGetAttributeAssignments trioGroups2 = new GcGetAttributeAssignments()
                    .addAttributeDefNameName(TRIO)
                    .assignAttributeAssignType(ASSIGN_TYPE_GROUP);

            groupPaths.forEach(trioGroups2::addOwnerGroupName);

            WsGetAttributeAssignmentsResults attributeAssignmentsResults2 = trioGroups2.execute();

            assertNotNull(attributeAssignmentsResults2);

            WsAttributeAssign[] wsGroups2 = attributeAssignmentsResults2.getWsAttributeAssigns();

            if (wsGroups2 != null && wsGroups2.length > 0) {
                for (WsAttributeAssign grouping : wsGroups2) {
                    groupings2.add(grouping.getOwnerGroupName());
                }
            }
        }

        assertNotNull(groupings2);

    }

    @Test
    public void makeGroupingsTest() {
        List<String> groupingPaths = new ArrayList<>();
        groupingPaths.add(GROUPING);
        groupingPaths.add(GROUPING_STORE_EMPTY);
        groupingPaths.add(GROUPING_TRUE_EMPTY);

        List<Grouping> groupings = helperService.makeGroupings(groupingPaths);

        assertTrue(groupings.size() == 3);
    }
}
