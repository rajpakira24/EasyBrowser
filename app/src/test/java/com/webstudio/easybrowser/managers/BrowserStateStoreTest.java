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
    public void reorderTabsUpdatesOrderWithinState() {
        BrowserStateStore store = new BrowserStateStore();
        store.addTab(tab("a", false), true);
        store.addTab(tab("b", false), false);
        store.addTab(tab("c", false), false);

        assertTrue(store.reorderTabs(Arrays.asList("c", "b"), false));

        assertEquals("a", store.getRegularTabs().get(0).getId());
        assertEquals("c", store.getRegularTabs().get(1).getId());
        assertEquals("b", store.getRegularTabs().get(2).getId());
        assertEquals(0, store.findTabById("c").getPosition());
        assertEquals(1, store.findTabById("b").getPosition());
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

    private static Tab tab(String id, boolean isPrivate) {
        Tab tab = new Tab(id, null, id, "https://example.com/" + id, isPrivate);
        tab.setCreatedAt(1000L);
        tab.setLastAccessed(1000L);
        return tab;
    }
}
