package com.webstudio.easybrowser.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.webstudio.easybrowser.models.BrowserState;
import com.webstudio.easybrowser.models.Tab;
import com.webstudio.easybrowser.models.TabGroup;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class BrowserStateStoreTest {
    @Test
    public void addTabSeparatesRegularAndPrivateState() {
        BrowserStateStore store = new BrowserStateStore();
        Tab regular = tab("regular", false);
        Tab privateTab = tab("private", true);

        store.addTab(regular, true);
        store.addTab(privateTab, true);

        BrowserState state = store.snapshot();
        assertEquals(1, state.getRegularTabCount());
        assertEquals(1, state.getPrivateTabCount());
        assertEquals("regular", state.getActiveRegularTabId());
        assertEquals("private", state.getActivePrivateTabId());
        assertEquals("private", state.getActiveTabId());
        assertTrue(state.isPrivateMode());
        assertEquals(1, store.getRegularTabs().size());
    }

    @Test
    public void addRegularTabWithoutActivateKeepsCurrentRegularTab() {
        BrowserStateStore store = new BrowserStateStore();
        store.addTab(tab("active", false), true);
        store.addTab(tab("background", false), false);

        BrowserState state = store.snapshot();
        assertEquals(2, state.getRegularTabCount());
        assertEquals("active", state.getActiveRegularTabId());
        assertEquals("active", store.getCurrentTab().getId());
        assertFalse(state.isPrivateMode());
    }

    @Test
    public void switchToTabChangesActiveModeAndRejectsUnknownTabs() {
        BrowserStateStore store = new BrowserStateStore();
        store.addTab(tab("regular", false), true);
        store.addTab(tab("private", true), true);

        assertTrue(store.switchToTab("regular"));
        assertEquals("regular", store.snapshot().getActiveTabId());
        assertFalse(store.snapshot().isPrivateMode());

        assertTrue(store.switchToTab("private"));
        assertEquals("private", store.snapshot().getActiveTabId());
        assertTrue(store.snapshot().isPrivateMode());

        assertFalse(store.switchToTab("missing"));
        assertEquals("private", store.snapshot().getActiveTabId());
    }

    @Test
    public void closeActiveRegularTabFallsBackToLastRegularTab() {
        BrowserStateStore store = new BrowserStateStore();
        store.addTab(tab("first", false), true);
        store.addTab(tab("active", false), true);
        store.addTab(tab("fallback", false), false);

        Tab closed = store.closeTab("active");

        assertNotNull(closed);
        assertEquals("active", closed.getId());
        assertEquals("fallback", store.snapshot().getActiveRegularTabId());
        assertEquals("fallback", store.getCurrentTab().getId());
    }

    @Test
    public void closeLastPrivateTabExitsPrivateModeAndFallsBackToRegularTab() {
        BrowserStateStore store = new BrowserStateStore();
        store.addTab(tab("regular", false), true);
        store.addTab(tab("private", true), true);

        store.closeTab("private");

        BrowserState state = store.snapshot();
        assertFalse(state.isPrivateMode());
        assertNull(state.getActivePrivateTabId());
        assertEquals("regular", state.getActiveRegularTabId());
        assertEquals("regular", store.getCurrentTab().getId());
    }

    @Test
    public void closeAllSeparatesRegularAndPrivateState() {
        BrowserStateStore store = new BrowserStateStore();
        store.addTab(tab("regular-a", false), true);
        store.addTab(tab("regular-b", false), false);
        store.addTab(tab("private", true), true);

        assertEquals(2, store.closeAll(false).size());

        BrowserState regularClosedState = store.snapshot();
        assertEquals(0, regularClosedState.getRegularTabCount());
        assertEquals(1, regularClosedState.getPrivateTabCount());
        assertTrue(regularClosedState.isPrivateMode());
        assertEquals("private", store.getCurrentTab().getId());

        assertEquals(1, store.closeAll(true).size());

        BrowserState allClosedState = store.snapshot();
        assertEquals(0, allClosedState.getRegularTabCount());
        assertEquals(0, allClosedState.getPrivateTabCount());
        assertFalse(allClosedState.isPrivateMode());
        assertNull(store.getCurrentTab());
    }

    @Test
    public void createGroupRequiresExplicitTabSelection() {
        BrowserStateStore store = new BrowserStateStore();
        store.addTab(tab("a", false), true);
        store.addTab(tab("b", false), false);

        TabGroup group = store.createGroupForTabs(Arrays.asList("a", "b"), "Work", 0xFF34A853, false);

        assertNotNull(group);
        assertEquals(group.getGroupId(), store.findTabById("a").getGroupId());
        assertEquals(group.getGroupId(), store.findTabById("b").getGroupId());
        assertEquals(1, store.getGroups(false).size());
        assertEquals(2, store.getGroups(false).get(0).getTabCount());
    }

    @Test
    public void renameAndColorChangeGroupUpdatesEveryMemberTab() {
        BrowserStateStore store = new BrowserStateStore();
        store.addTab(tab("a", false), true);
        store.addTab(tab("b", false), false);
        TabGroup group = store.createGroupForTabs(Arrays.asList("a", "b"), "Work", 0xFF34A853, false);

        assertTrue(store.renameGroup(group.getGroupId(), "Research"));
        assertTrue(store.changeGroupColor(group.getGroupId(), 0xFF1A73E8));

        assertEquals("Research", store.getGroups(false).get(0).getGroupName());
        assertEquals(0xFF1A73E8, store.getGroups(false).get(0).getGroupColor());
        assertEquals("Research", store.findTabById("a").getGroupName());
        assertEquals("Research", store.findTabById("b").getGroupName());
        assertEquals(0xFF1A73E8, store.findTabById("a").getGroupColor());
        assertEquals(0xFF1A73E8, store.findTabById("b").getGroupColor());
    }

    @Test
    public void removingTabFromTwoTabGroupDissolvesGroup() {
        BrowserStateStore store = new BrowserStateStore();
        store.addTab(tab("a", false), true);
        store.addTab(tab("b", false), false);
        TabGroup group = store.createGroupForTabs(Arrays.asList("a", "b"), "Work", 0xFF34A853, false);

        assertTrue(store.removeTabFromGroup("a"));

        assertNull(store.findTabById("a").getGroupId());
        assertNull(store.findTabById("b").getGroupId());
        assertTrue(store.getGroups(false).isEmpty());
        assertNull(store.snapshot().getActiveGroupId());
    }

    @Test
    public void ungroupClearsGroupMetadataForAllMemberTabs() {
        BrowserStateStore store = new BrowserStateStore();
        store.addTab(tab("a", false), true);
        store.addTab(tab("b", false), false);
        store.addTab(tab("c", false), false);
        TabGroup group = store.createGroupForTabs(Arrays.asList("a", "b", "c"), "Work", 0xFF34A853, false);

        assertTrue(store.ungroup(group.getGroupId()));

        assertTrue(store.getGroups(false).isEmpty());
        assertNull(store.findTabById("a").getGroupId());
        assertNull(store.findTabById("b").getGroupId());
        assertNull(store.findTabById("c").getGroupId());
    }

    @Test
    public void closeGroupRemovesOnlyGroupMemberTabsAndUpdatesActiveTab() {
        BrowserStateStore store = new BrowserStateStore();
        store.addTab(tab("standalone", false), true);
        store.addTab(tab("a", false), true);
        store.addTab(tab("b", false), true);
        TabGroup group = store.createGroupForTabs(Arrays.asList("a", "b"), "Work", 0xFF34A853, false);

        assertEquals(2, store.closeGroup(group.getGroupId()).size());

        assertEquals(1, store.getRegularTabs().size());
        assertEquals("standalone", store.getRegularTabs().get(0).getId());
        assertEquals("standalone", store.snapshot().getActiveRegularTabId());
        assertEquals("standalone", store.getCurrentTab().getId());
        assertTrue(store.getGroups(false).isEmpty());
    }

    @Test
    public void privateGroupsRemainSeparateFromRegularGroups() {
        BrowserStateStore store = new BrowserStateStore();
        store.addTab(tab("p1", true), true);
        store.addTab(tab("p2", true), false);

        TabGroup group = store.createGroupForTabs(Arrays.asList("p1", "p2"), "Private", 0xFF1A73E8, true);

        assertNotNull(group);
        assertTrue(store.getGroups(false).isEmpty());
        assertEquals(1, store.getGroups(true).size());
        assertTrue(store.getGroups(true).get(0).isPrivate());
    }

    @Test
    public void loadRegularStateIgnoresPrivateTabs() {
        BrowserStateStore store = new BrowserStateStore();
        store.loadRegularState(
                Arrays.asList(tab("regular", false), tab("private", true)),
                Collections.emptyList(),
                "private");

        assertEquals(1, store.getRegularTabs().size());
        assertTrue(store.getPrivateTabs().isEmpty());
        assertEquals("regular", store.snapshot().getActiveRegularTabId());
    }

    @Test
    public void loadRegularStateClearsInvalidGroupMetadata() {
        BrowserStateStore store = new BrowserStateStore();
        Tab staleName = tab("staleName", false);
        staleName.setGroupName("Old Group");
        staleName.setGroupColor(0xFF34A853);
        Tab orphanedGroup = tab("orphanedGroup", false);
        orphanedGroup.setGroupId("missing-group");
        orphanedGroup.setGroupName("Missing Group");
        orphanedGroup.setGroupColor(0xFF1A73E8);

        store.loadRegularState(Arrays.asList(staleName, orphanedGroup),
                Collections.emptyList(), "staleName");

        assertNull(store.findTabById("staleName").getGroupId());
        assertNull(store.findTabById("staleName").getGroupName());
        assertEquals(0, store.findTabById("staleName").getGroupColor());
        assertNull(store.findTabById("orphanedGroup").getGroupId());
        assertNull(store.findTabById("orphanedGroup").getGroupName());
        assertEquals(0, store.findTabById("orphanedGroup").getGroupColor());
        assertTrue(store.getGroups(false).isEmpty());
    }

    @Test
    public void loadRegularStateHandlesMissingPartialSnapshot() {
        BrowserStateStore store = new BrowserStateStore();
        store.addTab(tab("previous-regular", false), true);
        store.addTab(tab("private", true), true);

        store.loadRegularState(null, null, "missing");

        BrowserState state = store.snapshot();
        assertEquals(0, state.getRegularTabCount());
        assertNull(state.getActiveRegularTabId());
        assertTrue(state.isPrivateMode());
        assertEquals("private", state.getActiveTabId());
        assertEquals("private", store.getCurrentTab().getId());
    }

    @Test
    public void loadRegularStateNormalizesCorruptPartialGroupSnapshot() {
        BrowserStateStore store = new BrowserStateStore();
        Tab singleMember = groupedTab("single", "single-group", "Single", 0xFF34A853);
        Tab validA = groupedTab("valid-a", "valid-group", "Valid", 0xFF1A73E8);
        Tab validB = groupedTab("valid-b", "valid-group", "Valid", 0xFF1A73E8);
        Tab wrongMode = groupedTab("wrong-mode", "private-group", "Private", 0xFFFBBC04);
        TabGroup singleGroup = new TabGroup("single-group", "Single", 0xFF34A853,
                false, 1000L, 1000L);
        TabGroup validGroup = new TabGroup("valid-group", "Valid", 0xFF1A73E8,
                false, 1000L, 1000L);
        TabGroup emptyGroup = new TabGroup("empty-group", "Empty", 0xFFEA4335,
                false, 1000L, 1000L);
        TabGroup privateGroup = new TabGroup("private-group", "Private", 0xFFFBBC04,
                true, 1000L, 1000L);

        store.loadRegularState(Arrays.asList(null, singleMember, validA, validB, wrongMode),
                Arrays.asList(null, singleGroup, validGroup, emptyGroup, privateGroup),
                "missing-active");

        BrowserState state = store.snapshot();
        assertEquals(4, state.getRegularTabCount());
        assertEquals("wrong-mode", state.getActiveRegularTabId());
        assertNull(store.findTabById("single").getGroupId());
        assertNull(store.findTabById("single").getGroupName());
        assertEquals(0, store.findTabById("single").getGroupColor());
        assertNull(store.findTabById("wrong-mode").getGroupId());
        assertNull(store.findTabById("wrong-mode").getGroupName());
        assertEquals(0, store.findTabById("wrong-mode").getGroupColor());
        assertEquals("valid-group", store.findTabById("valid-a").getGroupId());
        assertEquals("valid-group", store.findTabById("valid-b").getGroupId());
        assertEquals(1, store.getGroups(false).size());
        assertEquals("valid-group", store.getGroups(false).get(0).getGroupId());
        assertEquals(2, store.getGroups(false).get(0).getTabCount());
        assertTrue(store.getGroups(true).isEmpty());
    }

    @Test
    public void loadRegularStatePreservesActivePrivateTab() {
        BrowserStateStore store = new BrowserStateStore();
        store.addTab(tab("regular", false), true);
        store.addTab(tab("private", true), true);

        store.loadRegularState(Collections.singletonList(tab("regular", false)),
                Collections.emptyList(), "regular");

        BrowserState state = store.snapshot();
        assertTrue(state.isPrivateMode());
        assertEquals("private", state.getActiveTabId());
        assertEquals("regular", state.getActiveRegularTabId());
    }

    @Test
    public void loadRegularStateRestoresStandaloneGroupedPinnedAndInactiveTabs() {
        BrowserStateStore store = new BrowserStateStore();
        Tab pinned = tab("pinned", false);
        pinned.setPinned(true);
        pinned.setPosition(0);
        Tab standalone = tab("standalone", false);
        standalone.setPosition(1);
        Tab groupedA = groupedTab("grouped-a", "group", "Work", 0xFF34A853);
        groupedA.setPosition(2);
        Tab groupedB = groupedTab("grouped-b", "group", "Work", 0xFF34A853);
        groupedB.setPosition(3);
        TabGroup group = new TabGroup("group", "Work", 0xFF34A853, false, 1000L, 1000L);

        store.loadRegularState(Arrays.asList(pinned, standalone, groupedA, groupedB),
                Collections.singletonList(group), "grouped-b");

        BrowserState state = store.snapshot();
        assertEquals(4, state.getRegularTabCount());
        assertEquals("grouped-b", state.getActiveRegularTabId());
        assertEquals("grouped-b", store.getCurrentTab().getId());
        assertTrue(store.findTabById("pinned").isPinned());
        assertNull(store.findTabById("standalone").getGroupId());
        assertEquals("group", store.findTabById("grouped-a").getGroupId());
        assertEquals("group", store.findTabById("grouped-b").getGroupId());
        assertEquals(1, store.getGroups(false).size());
        assertEquals(2, store.getGroups(false).get(0).getTabCount());
    }

    @Test
    public void reorderTabsUpdatesOrderWithinState() {
        BrowserStateStore store = new BrowserStateStore();
        store.addTab(tab("a", false), true);
        store.addTab(tab("b", false), false);
        store.addTab(tab("c", false), false);

        assertTrue(store.reorderTabs(Arrays.asList("c", "b"), false));

        assertEquals("a", store.getRegularTabs().get(0).getId());
        assertEquals("c", store.getRegularTabs().get(1).getId());
        assertEquals("b", store.getRegularTabs().get(2).getId());
        assertEquals(0, store.findTabById("a").getPosition());
        assertEquals(1, store.findTabById("c").getPosition());
        assertEquals(2, store.findTabById("b").getPosition());
    }

    @Test
    public void reorderTabsRejectsCrossModeIds() {
        BrowserStateStore store = new BrowserStateStore();
        store.addTab(tab("regular", false), true);
        store.addTab(tab("private", true), false);

        assertFalse(store.reorderTabs(Arrays.asList("regular", "private"), false));
        assertEquals("regular", store.getRegularTabs().get(0).getId());
        assertEquals("private", store.getPrivateTabs().get(0).getId());
    }

    @Test
    public void getPersistableTabsNormalizesStandaloneMetadataAndPositions() {
        BrowserStateStore store = new BrowserStateStore();
        Tab staleStandalone = tab("stale", false);
        staleStandalone.setGroupName("Stale Group");
        staleStandalone.setGroupColor(0xFF34A853);
        staleStandalone.setPosition(42);
        Tab next = tab("next", false);
        next.setPosition(7);
        store.addTab(staleStandalone, true);
        store.addTab(next, false);
        store.addTab(tab("private", true), true);

        assertEquals(2, store.getPersistableTabs().size());

        assertNull(store.findTabById("stale").getGroupId());
        assertNull(store.findTabById("stale").getGroupName());
        assertEquals(0, store.findTabById("stale").getGroupColor());
        assertEquals(0, store.findTabById("stale").getPosition());
        assertEquals(1, store.findTabById("next").getPosition());
        assertEquals(0, store.findTabById("private").getPosition());
    }

    @Test
    public void setTabPinnedMovesPinnedTabBeforeUnpinnedTabs() {
        BrowserStateStore store = new BrowserStateStore();
        store.addTab(tab("a", false), true);
        store.addTab(tab("b", false), false);
        store.addTab(tab("c", false), false);

        assertTrue(store.setTabPinned("b", true));

        assertEquals("b", store.getRegularTabs().get(0).getId());
        assertEquals("a", store.getRegularTabs().get(1).getId());
        assertEquals("c", store.getRegularTabs().get(2).getId());
        assertTrue(store.findTabById("b").isPinned());
        assertEquals(0, store.findTabById("b").getPosition());

        assertTrue(store.setTabPinned("b", false));

        assertEquals("a", store.getRegularTabs().get(0).getId());
        assertEquals("c", store.getRegularTabs().get(1).getId());
        assertEquals("b", store.getRegularTabs().get(2).getId());
        assertFalse(store.findTabById("b").isPinned());
        assertEquals(2, store.findTabById("b").getPosition());
    }

    private static Tab tab(String id, boolean isPrivate) {
        Tab tab = new Tab(id, null, id, "https://example.com/" + id, isPrivate);
        tab.setCreatedAt(1000L);
        tab.setLastAccessed(1000L);
        return tab;
    }

    private static Tab groupedTab(String id, String groupId, String groupName, int groupColor) {
        Tab tab = tab(id, false);
        tab.setGroupId(groupId);
        tab.setGroupName(groupName);
        tab.setGroupColor(groupColor);
        return tab;
    }
}
