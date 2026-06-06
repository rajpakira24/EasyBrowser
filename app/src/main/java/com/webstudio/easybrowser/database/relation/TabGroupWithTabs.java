package com.webstudio.easybrowser.database.relation;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.webstudio.easybrowser.database.entity.TabEntity;
import com.webstudio.easybrowser.database.entity.TabGroupEntity;

import java.util.List;

public class TabGroupWithTabs {
    @Embedded
    public TabGroupEntity group;

    @Relation(parentColumn = "groupId", entityColumn = "groupId")
    public List<TabEntity> tabs;
}
