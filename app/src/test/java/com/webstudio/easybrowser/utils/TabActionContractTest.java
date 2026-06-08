package com.webstudio.easybrowser.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class TabActionContractTest {
    @Test
    public void serializeAndParsePreservesOrderedActions() {
        String payload = TabActionContract.serialize(Arrays.asList(
                TabActionContract.closeTabs(Arrays.asList("tab-1", "tab-2")),
                TabActionContract.createTab(true, "group-1", "Private group", 123),
                TabActionContract.setPinned(Arrays.asList("tab-3"), true),
                TabActionContract.selectTab("tab-3")));

        List<TabActionContract.Action> actions = TabActionContract.parse(payload);

        assertEquals(4, actions.size());
        assertEquals(TabActionContract.TYPE_CLOSE_TABS, actions.get(0).getType());
        assertEquals(Arrays.asList("tab-1", "tab-2"), actions.get(0).getTabIds());
        assertEquals(TabActionContract.TYPE_CREATE_TAB, actions.get(1).getType());
        assertTrue(actions.get(1).isPrivate());
        assertEquals("group-1", actions.get(1).getGroupId());
        assertEquals("Private group", actions.get(1).getGroupName());
        assertEquals(123, actions.get(1).getGroupColor(0));
        assertEquals(TabActionContract.TYPE_SET_PINNED, actions.get(2).getType());
        assertTrue(actions.get(2).isPinned());
        assertEquals(TabActionContract.TYPE_SELECT_TAB, actions.get(3).getType());
        assertEquals("tab-3", actions.get(3).getTabId());
    }

    @Test
    public void parseIgnoresInvalidPayloads() {
        assertTrue(TabActionContract.parse(null).isEmpty());
        assertTrue(TabActionContract.parse("").isEmpty());
        assertTrue(TabActionContract.parse("{not-json").isEmpty());
    }

    @Test
    public void booleanActionsUseExplicitFlags() {
        String payload = TabActionContract.serialize(Arrays.asList(
                TabActionContract.setLocked(Arrays.asList("tab-1"), false),
                TabActionContract.restoreUrl("https://example.com", true)));

        List<TabActionContract.Action> actions = TabActionContract.parse(payload);

        assertEquals(2, actions.size());
        assertFalse(actions.get(0).isLocked());
        assertEquals(Arrays.asList("tab-1"), actions.get(0).getTabIds());
        assertEquals("https://example.com", actions.get(1).getUrl());
        assertTrue(actions.get(1).isPrivate());
    }

    @Test
    public void tabsActivityActionsRoundTripTogether() {
        String payload = TabActionContract.serialize(Arrays.asList(
                TabActionContract.closeTabs(Arrays.asList("closed-1")),
                TabActionContract.restoreUrl("https://example.org", false),
                TabActionContract.createTab(true, null, null, 0),
                TabActionContract.groupsChanged(),
                TabActionContract.selectTab("selected-1")));

        List<TabActionContract.Action> actions = TabActionContract.parse(payload);

        assertEquals(5, actions.size());
        assertEquals(TabActionContract.TYPE_CLOSE_TABS, actions.get(0).getType());
        assertEquals(Arrays.asList("closed-1"), actions.get(0).getTabIds());
        assertEquals(TabActionContract.TYPE_RESTORE_URL, actions.get(1).getType());
        assertEquals("https://example.org", actions.get(1).getUrl());
        assertFalse(actions.get(1).isPrivate());
        assertEquals(TabActionContract.TYPE_CREATE_TAB, actions.get(2).getType());
        assertTrue(actions.get(2).isPrivate());
        assertEquals(TabActionContract.TYPE_GROUPS_CHANGED, actions.get(3).getType());
        assertEquals(TabActionContract.TYPE_SELECT_TAB, actions.get(4).getType());
        assertEquals("selected-1", actions.get(4).getTabId());
    }
}
