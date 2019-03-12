package edu.hawaii.its.api.service;

import edu.hawaii.its.api.configuration.SpringBootWebApplication;
import edu.hawaii.its.api.repository.GroupRepository;
import edu.hawaii.its.api.repository.GroupingRepository;
import edu.hawaii.its.api.repository.MembershipRepository;
import edu.hawaii.its.api.repository.PersonRepository;
import edu.hawaii.its.api.type.AdminListsHolder;
import edu.hawaii.its.api.type.Group;
import edu.hawaii.its.api.type.Grouping;
import edu.hawaii.its.api.type.GroupingAssignment;
import edu.hawaii.its.api.type.Person;
import edu.internet2.middleware.grouperClient.ws.beans.WsGetMembersResult;
import edu.internet2.middleware.grouperClient.ws.beans.WsGetMembersResults;
import edu.internet2.middleware.grouperClient.ws.beans.WsGroup;
import edu.internet2.middleware.grouperClient.ws.beans.WsSubject;
import edu.internet2.middleware.grouperClient.ws.beans.WsSubjectLookup;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.coyote.http11.Constants.a;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThat;

import static org.hamcrest.CoreMatchers.*;

@ActiveProfiles("localTest")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { SpringBootWebApplication.class })
@WebAppConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class GroupingAssignmentServiceTest {

    @Value("${groupings.api.grouping_admins}")
    private String GROUPING_ADMINS;

    @Value("${groupings.api.grouping_apps}")
    private String GROUPING_APPS;

    @Value("${groupings.api.test.username}")
    private String USERNAME;

    @Value("${groupings.api.test.name}")
    private String NAME;

    @Value("${groupings.api.test.uuid}")
    private String UUID;

    @Value("${groupings.api.person_attributes.uuid}")
    private String UUID_KEY;

    @Value("${groupings.api.person_attributes.username}")
    private String UID_KEY;

    @Value("${groupings.api.person_attributes.first_name}")
    private String FIRST_NAME_KEY;

    @Value("${groupings.api.person_attributes.last_name}")
    private String LAST_NAME_KEY;

    @Value("${groupings.api.person_attributes.composite_name}")
    private String COMPOSITE_NAME_KEY;

    private static final String PATH_ROOT = "path:to:grouping";

    private static final String BASIS = ":basis";

    private static final String GROUPING_0_PATH = PATH_ROOT + 0;
    private static final String GROUPING_1_PATH = PATH_ROOT + 1;
    private static final String GROUPING_3_PATH = PATH_ROOT + 3;
    private static final String GROUPING_3_BASIS_PATH = GROUPING_3_PATH + BASIS;

    private static final String ADMIN_USER = "admin";
    private static final Person ADMIN_PERSON = new Person(ADMIN_USER, ADMIN_USER, ADMIN_USER);
    private List<Person> admins = new ArrayList<>();
    private Group adminGroup = new Group();

    private static final String APP_USER = "app";
    private static final Person APP_PERSON = new Person(APP_USER, APP_USER, APP_USER);
    private List<Person> apps = new ArrayList<>();
    private Group appGroup = new Group();

    private List<Person> users = new ArrayList<>();
    private List<WsSubjectLookup> lookups = new ArrayList<>();

    @Autowired
    private GroupingRepository groupingRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private GroupingAssignmentService groupingAssignmentService;

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private GrouperFactoryService grouperFS;

    @Autowired
    private DatabaseSetupService databaseSetupService;

    @Before
    public void setup() {
        databaseSetupService.initialize(users, lookups, admins, adminGroup, appGroup);
    }

    @Test
    public void construction() {
        //autowired
        assertNotNull(groupingAssignmentService);
    }

    @Test
    public void getGroupingTest() {
        Grouping groupingRandom = groupingAssignmentService.getGrouping(GROUPING_0_PATH, users.get(1).getUsername());
        Grouping groupingOwner = groupingAssignmentService.getGrouping(GROUPING_0_PATH, users.get(0).getUsername());
        Grouping groupingAdmin = groupingAssignmentService.getGrouping(GROUPING_0_PATH, ADMIN_USER);

        assertEquals(0, groupingRandom.getComposite().getMembers().size());
        assertEquals(0, groupingRandom.getInclude().getMembers().size());
        assertEquals(0, groupingRandom.getExclude().getMembers().size());
        assertEquals(0, groupingRandom.getBasis().getMembers().size());
        assertEquals(0, groupingRandom.getOwners().getMembers().size());

        assertTrue(groupingOwner.getComposite().getNames().contains(users.get(0).getName()));
        assertTrue(groupingOwner.getComposite().getUsernames().contains(users.get(0).getUsername()));
        assertTrue(groupingOwner.getComposite().getUuids().contains(users.get(0).getUuid()));
        assertTrue(groupingOwner.getInclude().getNames().contains(users.get(5).getName()));
        assertTrue(groupingOwner.getExclude().getNames().contains(users.get(2).getName()));
        assertTrue(groupingOwner.getBasis().getNames().contains(users.get(4).getName()));
        assertTrue(groupingOwner.getOwners().getNames().contains(users.get(0).getName()));

        assertTrue(groupingAdmin.getComposite().getNames().contains(users.get(0).getName()));
        assertTrue(groupingAdmin.getComposite().getUsernames().contains(users.get(0).getUsername()));
        assertTrue(groupingAdmin.getComposite().getUuids().contains(users.get(0).getUuid()));
        assertTrue(groupingAdmin.getInclude().getNames().contains(users.get(5).getName()));
        assertTrue(groupingAdmin.getExclude().getNames().contains(users.get(2).getName()));
        assertTrue(groupingAdmin.getBasis().getNames().contains(users.get(4).getName()));
        assertTrue(groupingAdmin.getOwners().getNames().contains(users.get(0).getName()));
    }

    @Test
    public void getPaginatedGroupingTest() {
        Grouping groupingRandom = groupingAssignmentService
                .getPaginatedGrouping(GROUPING_0_PATH, users.get(1).getUsername(), 1, 4, "name", true);
        Grouping groupingOwner = groupingAssignmentService
                .getPaginatedGrouping(GROUPING_0_PATH, users.get(0).getUsername(), 1, 4, "name", true);
        Grouping groupingAdmin = groupingAssignmentService
                .getPaginatedGrouping(GROUPING_0_PATH, ADMIN_USER, 1, 4, "name", false);
        Grouping groupingNull = groupingAssignmentService
                .getPaginatedGrouping(GROUPING_0_PATH, users.get(0).getUsername(), null, null, null, null);

        assertThat(groupingRandom.getComposite().getMembers().size(), equalTo(0));
        assertThat(groupingRandom.getBasis().getMembers().size(), equalTo(0));
        assertThat(groupingRandom.getInclude().getMembers().size(), equalTo(0));
        assertThat(groupingRandom.getExclude().getMembers().size(), equalTo(0));
        assertThat(groupingRandom.getOwners().getMembers().size(), equalTo(0));

        assertTrue(groupingOwner.getComposite().getNames().contains(users.get(0).getName()));
        assertTrue(groupingOwner.getComposite().getUsernames().contains(users.get(0).getUsername()));
        assertTrue(groupingOwner.getComposite().getUuids().contains(users.get(0).getUuid()));
        assertFalse(groupingOwner.getComposite().getNames().contains(users.get(7).getName()));
        assertFalse(groupingOwner.getComposite().getUsernames().contains(users.get(7).getUsername()));
        assertFalse(groupingOwner.getComposite().getUuids().contains(users.get(7).getUuid()));

        assertTrue(groupingAdmin.getComposite().getNames().contains(users.get(7).getName()));
        assertTrue(groupingAdmin.getComposite().getUsernames().contains(users.get(7).getUsername()));
        assertTrue(groupingAdmin.getComposite().getUuids().contains(users.get(7).getUuid()));
        assertFalse(groupingAdmin.getComposite().getNames().contains(users.get(0).getName()));
        assertFalse(groupingAdmin.getComposite().getUsernames().contains(users.get(0).getUsername()));
        assertFalse(groupingAdmin.getComposite().getUuids().contains(users.get(0).getUuid()));

        assertTrue(groupingNull.getComposite().getNames().contains(users.get(0).getName()));
        assertTrue(groupingNull.getComposite().getUsernames().contains(users.get(0).getUsername()));
        assertTrue(groupingNull.getComposite().getUuids().contains(users.get(0).getUuid()));
        assertTrue(groupingNull.getComposite().getNames().contains(users.get(7).getName()));
        assertTrue(groupingNull.getComposite().getUsernames().contains(users.get(7).getUsername()));
        assertTrue(groupingNull.getComposite().getUuids().contains(users.get(7).getUuid()));
    }

    @Test
    public void getMyGroupingsTest() {
        GroupingAssignment myGroupings = groupingAssignmentService.getGroupingAssignment(users.get(1).getUsername());

        assertEquals(0, myGroupings.getGroupingsOwned().size());
        assertEquals(5, myGroupings.getGroupingsIn().size());
        assertEquals(0, myGroupings.getGroupingsOptedInTo().size());
        assertEquals(0, myGroupings.getGroupingsOptedOutOf().size());
        assertEquals(0, myGroupings.getGroupingsToOptInTo().size());
        assertEquals(2, myGroupings.getGroupingsToOptOutOf().size());

    }

    @Test
    public void groupingsToOptTest() {
        GroupingAssignment myGroupings = groupingAssignmentService.getGroupingAssignment(users.get(1).getUsername());
        //todo finish

    }

    @Test
    public void groupingsInTest() {

        Iterable<Group> groupsIn = groupRepository.findByMembersUsername(users.get(6).getUsername());
        List<String> groupPaths = new ArrayList<>();
        List<String> supposedGroupings = new ArrayList<>();

        for (Group group : groupsIn) {
            groupPaths.add(group.getPath());
        }
        supposedGroupings
                .addAll(groupPaths.stream().filter(groupPath -> groupPath.matches("[a-zA-Z0-9:]*grouping[0-9]*"))
                        .collect(Collectors.toList()));

        List<Grouping> groupingsIn = groupingAssignmentService.groupingsIn(groupPaths);
        List<String> groupingPaths = groupingsIn.stream().map(Grouping::getPath).collect(Collectors.toList());

        for (String path : supposedGroupings) {
            assertTrue(groupingPaths.contains(path));
        }
        for (Grouping grouping : groupingsIn) {
            assertTrue(supposedGroupings.contains(grouping.getPath()));
        }
    }

    @Test
    public void groupingsOwnedTest() {
        Iterable<Group> groupsIn = groupRepository.findByMembersUsername(users.get(0).getUsername());
        List<String> groupPaths = new ArrayList<>();

        for (Group group : groupsIn) {
            groupPaths.add(group.getPath());
        }

        List<Grouping> groupingsOwned = groupingAssignmentService.groupingsOwned(groupPaths);

        for (int i = 0; i < groupingsOwned.size(); i++) {
            assertTrue(groupingsOwned.get(i).getPath().equals(PATH_ROOT + i));
        }
    }

    @Test
    public void groupingsOptedIntoTest() {
        String user5 = users.get(5).getUsername();

        Iterable<Group> groups = groupRepository.findByMembersUsername(user5);
        List<String> groupPaths = new ArrayList<>();
        for (Group group : groups) {
            groupPaths.add(group.getPath());
        }

        List<Grouping> groupingsOptedInto = groupingAssignmentService.groupingsOptedInto(user5, groupPaths);

        //starts with no groupings opted into
        assertEquals(0, groupingsOptedInto.size());

        //opt into a grouping
        membershipService.optIn(user5, GROUPING_1_PATH);
        groupingsOptedInto = groupingAssignmentService.groupingsOptedInto(user5, groupPaths);
        assertEquals(1, groupingsOptedInto.size());

        //opt into another grouping
        membershipService.optIn(user5, GROUPING_3_PATH);
        groupingsOptedInto = groupingAssignmentService.groupingsOptedInto(user5, groupPaths);
        assertEquals(2, groupingsOptedInto.size());

        //opt out of a grouping
        membershipService.optOut(user5, GROUPING_3_PATH);
        groupingsOptedInto = groupingAssignmentService.groupingsOptedInto(user5, groupPaths);
        assertEquals(1, groupingsOptedInto.size());

        //opt out of another grouping
        membershipService.optOut(user5, GROUPING_1_PATH);
        groupingsOptedInto = groupingAssignmentService.groupingsOptedInto(user5, groupPaths);
        assertEquals(0, groupingsOptedInto.size());
    }

    @Test
    public void groupingsOptedOutOfTest() {
        String user1 = users.get(1).getUsername();

        Iterable<Group> groups = groupRepository.findByMembersUsername(user1);
        List<String> groupPaths = new ArrayList<>();
        for (Group group : groups) {
            groupPaths.add(group.getPath());
        }

        List<Grouping> groupingsOptedOutOf = groupingAssignmentService.groupingsOptedOutOf(user1, groupPaths);

        //starts with no groupings out of
        assertEquals(0, groupingsOptedOutOf.size());

        //opt out of a grouping
        membershipService.optOut(user1, GROUPING_1_PATH);
        groups = groupRepository.findByMembersUsername(user1);
        groupPaths = new ArrayList<>();
        for (Group group : groups) {
            groupPaths.add(group.getPath());
        }
        groupingsOptedOutOf = groupingAssignmentService.groupingsOptedOutOf(user1, groupPaths);
        assertEquals(1, groupingsOptedOutOf.size());

        //opt out of another grouping
        membershipService.optOut(user1, GROUPING_3_PATH);
        groups = groupRepository.findByMembersUsername(user1);
        groupPaths = new ArrayList<>();
        for (Group group : groups) {
            groupPaths.add(group.getPath());
        }
        groupingsOptedOutOf = groupingAssignmentService.groupingsOptedOutOf(user1, groupPaths);
        assertEquals(2, groupingsOptedOutOf.size());

        //opt into a grouping
        membershipService.optIn(user1, GROUPING_3_PATH);
        groups = groupRepository.findByMembersUsername(user1);
        groupPaths = new ArrayList<>();
        for (Group group : groups) {
            groupPaths.add(group.getPath());
        }
        groupingsOptedOutOf = groupingAssignmentService.groupingsOptedOutOf(user1, groupPaths);
        assertEquals(1, groupingsOptedOutOf.size());

        //opt into another grouping
        membershipService.optIn(user1, GROUPING_1_PATH);
        groups = groupRepository.findByMembersUsername(user1);
        groupPaths = new ArrayList<>();
        for (Group group : groups) {
            groupPaths.add(group.getPath());
        }
        groupingsOptedOutOf = groupingAssignmentService.groupingsOptedOutOf(user1, groupPaths);
        assertEquals(0, groupingsOptedOutOf.size());
    }

    @Test
    public void adminListsTest() {
        AdminListsHolder adminListsHolder = groupingAssignmentService.adminLists(ADMIN_USER);
        AdminListsHolder emptyAdminListHolder = groupingAssignmentService.adminLists(users.get(1).getUsername());

        assertEquals(adminListsHolder.getAllGroupings().size(), 5);
        assertEquals(adminListsHolder.getAdminGroup().getMembers().size(), 1);

        assertEquals(emptyAdminListHolder.getAllGroupings().size(), 0);
        assertEquals(emptyAdminListHolder.getAdminGroup().getMembers().size(), 0);
    }

    /////////////////////////////////////////////////////
    // non-mocked tests//////////////////////////////////
    /////////////////////////////////////////////////////

    @Test
    public void extractGroupPaths() {
        List<String> groupNames = groupingAssignmentService.extractGroupPaths(null);
        assertEquals(0, groupNames.size());

        List<WsGroup> groups = new ArrayList<>();
        final int size = 300;

        for (int i = 0; i < size; i++) {
            WsGroup w = new WsGroup();
            w.setName("testName_" + i);
            groups.add(w);
        }
        assertEquals(size, groups.size());

        groupNames = groupingAssignmentService.extractGroupPaths(groups);
        for (int i = 0; i < size; i++) {
            assertTrue(groupNames.contains("testName_" + i));
        }
        assertEquals(size, groupNames.size());

        // Create some duplicates.
        groups = new ArrayList<>();
        for (int j = 0; j < 3; j++) {
            for (int i = 0; i < size; i++) {
                WsGroup w = new WsGroup();
                w.setName("testName_" + i);
                groups.add(w);
            }
        }
        assertEquals(size * 3, groups.size());

        // Duplicates should not be in groupNames list.
        groupNames = groupingAssignmentService.extractGroupPaths(groups);
        assertEquals(size, groupNames.size());
        for (int i = 0; i < size; i++) {
            assertTrue(groupNames.contains("testName_" + i));
        }
    }

    @Test
    public void getMembershipAssignmentTest() {
        String username = users.get(0).getUsername();
        // username should already be in GROUPING_0_PATH
        List<String> groupingsIn = groupingAssignmentService
                .getMembershipAssignment(username, username)
                .getGroupingsIn()
                .stream()
                .map(Grouping::getPath)
                .collect(Collectors.toList());

        List<String> groupingsToOptInto = groupingAssignmentService
                .getMembershipAssignment(username, username)
                .getGroupingsToOptInTo()
                .stream()
                .map(Grouping::getPath)
                .collect(Collectors.toList());

        assertTrue(groupingsIn.contains(GROUPING_0_PATH));
        assertFalse(groupingsToOptInto.contains(GROUPING_0_PATH));

        // take username[1] out of GROUPING
        membershipService.deleteGroupingMemberByUsername(username, GROUPING_0_PATH, username);

        // GROUPING has OPT-IN turned on, so username[1] should be able to opt back into GROUPING
        groupingsIn = groupingAssignmentService
                .getMembershipAssignment(username, username)
                .getGroupingsIn()
                .stream()
                .map(Grouping::getPath)
                .collect(Collectors.toList());

        groupingsToOptInto = groupingAssignmentService
                .getMembershipAssignment(username, username)
                .getGroupingsToOptInTo()
                .stream()
                .map(Grouping::getPath)
                .collect(Collectors.toList());

        assertFalse(groupingsIn.contains(GROUPING_0_PATH));
        assertTrue(groupingsToOptInto.contains(GROUPING_0_PATH));
    }

    @Test
    public void makeGroup() {
        WsGetMembersResults getMembersResults = new WsGetMembersResults();
        WsGetMembersResult[] getMembersResult = new WsGetMembersResult[1];
        WsGetMembersResult getMembersResult1 = new WsGetMembersResult();
        WsSubject[] subjects = new WsSubject[0];
        getMembersResult1.setWsSubjects(subjects);
        getMembersResult[0] = getMembersResult1;
        getMembersResults.setResults(getMembersResult);
        assertNotNull(groupingAssignmentService.makeGroup(getMembersResults));

        subjects = new WsSubject[1];
        getMembersResults.getResults()[0].setWsSubjects(subjects);
        assertNotNull(groupingAssignmentService.makeGroup(getMembersResults));

        subjects[0] = new WsSubject();
        getMembersResults.getResults()[0].setWsSubjects(subjects);
        assertNotNull(groupingAssignmentService.makeGroup(getMembersResults));

    }

    @Test
    public void makeGroupTest() {
        WsGetMembersResults getMembersResults = new WsGetMembersResults();
        WsGetMembersResult[] getMembersResult = new WsGetMembersResult[1];
        WsGetMembersResult getMembersResult1 = new WsGetMembersResult();

        WsSubject[] list = new WsSubject[3];
        for (int i = 0; i < 3; i++) {
            list[i] = new WsSubject();
            list[i].setName("testSubject_" + i);
            list[i].setId("testSubject_uuid_" + i);
            list[i].setAttributeValues(new String[] { "testSubject_username_" + i });
        }

        getMembersResult1.setWsSubjects(list);
        getMembersResult[0] = getMembersResult1;
        getMembersResults.setResults(getMembersResult);

        Group group = groupingAssignmentService.makeGroup(getMembersResults);

        for (int i = 0; i < group.getMembers().size(); i++) {
            assertTrue(group.getMembers().get(i).getName().equals("testSubject_" + i));
            assertTrue(group.getNames().contains("testSubject_" + i));
            assertTrue(group.getMembers().get(i).getUuid().equals("testSubject_uuid_" + i));
            assertTrue(group.getUuids().contains("testSubject_uuid_" + i));
            assertTrue(group.getMembers().get(i).getUsername().equals("testSubject_username_" + i));
            assertTrue(group.getUsernames().contains("testSubject_username_" + i));
        }
    }

    @Test
    public void makePerson() {
        String name = "name";
        String id = "uuid";
        String identifier = "username";
        String[] attributeNames = new String[] { UID_KEY, UUID_KEY, LAST_NAME_KEY, COMPOSITE_NAME_KEY, FIRST_NAME_KEY };
        String[] attributeValues = new String[] { identifier, id, null, name, null };

        WsSubject subject = new WsSubject();
        subject.setName(name);
        subject.setId(id);
        subject.setAttributeValues(attributeValues);

        Person person = groupingAssignmentService.makePerson(subject, attributeNames);

        assertTrue(person.getName().equals(name));
        assertTrue(person.getUuid().equals(id));
        assertTrue(person.getUsername().equals(identifier));

        assertNotNull(groupingAssignmentService.makePerson(new WsSubject(), new String[] {}));
    }

    //todo Finish this test for setGroupingAttributes
    @Test
    public void setGroupingAttributesTest() {
        Grouping grouping = new Grouping();
        grouping = groupingAssignmentService.getGrouping(GROUPING_3_PATH, users.get(0).getUsername());
    }

    //todo
    @Test
    public void makeBasisGroupTest() {
        //groupingAssignmentService.getMembers(users.get(0).getUsername(), GROUPING_3_BASIS_PATH);

//        WsSubject garbageSubject = new WsSubject();
//        garbageSubject.setSourceId("g:gsa");
//        String id = garbageSubject.getId();
//        WsSubjectLookup lookup = grouperFS.makeWsSubjectLookup(garbageSubject.getName());
//        membershipService.addGroupMemberByUsername(users.get(0).getUsername(), GROUPING_3_BASIS_PATH, garbageSubject.getName());
//
//        WsGetMembersResults results = grouperFS.makeWsGetMembersResults(id, lookup, GROUPING_3_BASIS_PATH);
//        Group members = groupingAssignmentService.makeBasisGroup(results);
//        assertTrue(members.getMembers().size() == 0);
    }

}

