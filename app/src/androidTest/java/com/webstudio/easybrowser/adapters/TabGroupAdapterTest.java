package com.webstudio.easybrowser.adapters;

import static org.junit.Assert.assertEquals;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.webstudio.easybrowser.models.Tab;
import com.webstudio.easybrowser.models.TabGroup;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;

@RunWith(AndroidJUnit4.class)
public class TabGroupAdapterTest {
    @Test
    public void findPositionForTabIdReturnsStandaloneTabPosition() {
        Tab first = tab("first", 0);
        Tab second = tab("second", 1);
        TabGroup group = group("group", tab("group-first", 2), tab("group-second", 3));
        TabGroupAdapter adapter = new TabGroupAdapter(new NoOpListener());

        adapter.submitOverview(Collections.singletonList(group), Arrays.asList(first, second));

        assertEquals(1, adapter.findPositionForTabId("second"));
    }

    @Test
    public void findPositionForTabIdReturnsGroupCardPositionForGroupedTab() {
        Tab first = tab("first", 0);
        TabGroup group = group("group", tab("group-first", 1), tab("group-second", 2));
        Tab last = tab("last", 3);
        TabGroupAdapter adapter = new TabGroupAdapter(new NoOpListener());

        adapter.submitOverview(Collections.singletonList(group), Arrays.asList(first, last));

        assertEquals(1, adapter.findPositionForTabId("group-second"));
    }

    @Test
    public void findPositionForTabIdReturnsNoPositionWhenMissing() {
        TabGroupAdapter adapter = new TabGroupAdapter(new NoOpListener());
        adapter.submitOverview(null, Collections.singletonList(tab("first", 0)));

        assertEquals(RecyclerView.NO_POSITION, adapter.findPositionForTabId("missing"));
    }

    private static Tab tab(String id, int position) {
        Tab tab = new Tab(id, null, id, "https://example.com/" + id, false);
        tab.setPosition(position);
        tab.setLastAccessed(1000L + position);
        return tab;
    }

    private static TabGroup group(String id, Tab first, Tab second) {
        TabGroup group = new TabGroup(id, "Group", 0xFF34A853, false, 1000L, 1000L);
        group.setTabs(Arrays.asList(first, second));
        first.setGroupId(id);
        second.setGroupId(id);
        return group;
    }

    private static final class NoOpListener implements TabGroupAdapter.Listener {
        @Override public void onOpenGroup(TabGroup group, View sourceView) {}
        @Override public void onOpenTab(Tab tab) {}
        @Override public void onRenameGroup(TabGroup group) {}
        @Override public void onDeleteGroup(TabGroup group) {}
        @Override public void onChangeGroupColor(TabGroup group) {}
        @Override public void onMoveTabToGroup(Tab tab, TabGroup targetGroup) {}
        @Override public void onCreateGroupFromTabs(Tab firstTab, Tab secondTab) {}
        @Override public void onCloseGroup(TabGroup group) {}
        @Override public void onCloseTab(Tab tab) {}
        @Override public void onGroupLongClick(TabGroup group) {}
        @Override public void onTabLongClick(Tab tab, View anchor) {}
        @Override public void onAddTabToGroup(Tab tab) {}
        @Override public void onRemoveTabFromGroup(Tab tab) {}
        @Override public void onBookmarkTab(Tab tab) {}
        @Override public void onShareTab(Tab tab) {}
        @Override public void onDuplicateTab(Tab tab) {}
        @Override public void onPinTab(Tab tab) {}
    }
}
