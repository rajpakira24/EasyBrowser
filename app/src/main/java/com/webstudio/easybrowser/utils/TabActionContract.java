package com.webstudio.easybrowser.utils;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TabActionContract {
    public static final String EXTRA_ACTIONS = "tab_actions_v1";

    public static final String TYPE_CLOSE_TABS = "close_tabs";
    public static final String TYPE_RESTORE_INACTIVE_TABS = "restore_inactive_tabs";
    public static final String TYPE_CREATE_TAB = "create_tab";
    public static final String TYPE_RESTORE_URL = "restore_url";
    public static final String TYPE_CREATE_PRIVATE_GROUP = "create_private_group";
    public static final String TYPE_SET_GROUP = "set_group";
    public static final String TYPE_REORDER_PRIVATE_TABS = "reorder_private_tabs";
    public static final String TYPE_SET_PINNED = "set_pinned";
    public static final String TYPE_SET_LOCKED = "set_locked";
    public static final String TYPE_GROUPS_CHANGED = "groups_changed";
    public static final String TYPE_SELECT_TAB = "select_tab";

    private static final String KEY_TYPE = "type";
    private static final String KEY_TAB_ID = "tab_id";
    private static final String KEY_TAB_IDS = "tab_ids";
    private static final String KEY_PRIVATE = "private";
    private static final String KEY_URL = "url";
    private static final String KEY_GROUP_ID = "group_id";
    private static final String KEY_GROUP_NAME = "group_name";
    private static final String KEY_GROUP_COLOR = "group_color";
    private static final String KEY_PINNED = "pinned";
    private static final String KEY_LOCKED = "locked";

    private static final String ACTION_SEPARATOR = "\n";
    private static final String FIELD_SEPARATOR = "&";
    private static final String KEY_VALUE_SEPARATOR = "=";
    private static final String LIST_SEPARATOR = ",";
    private static final String UTF_8 = "UTF-8";

    private TabActionContract() {
    }

    public static String serialize(List<Action> actions) {
        StringBuilder builder = new StringBuilder();
        if (actions != null) {
            for (Action action : actions) {
                if (action == null || action.getType().trim().isEmpty()) {
                    continue;
                }
                if (builder.length() > 0) {
                    builder.append(ACTION_SEPARATOR);
                }
                builder.append(action.serialize());
            }
        }
        return builder.toString();
    }

    public static List<Action> parse(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList<Action> actions = new ArrayList<>();
        String[] lines = payload.split(ACTION_SEPARATOR);
        for (String line : lines) {
            Action action = Action.parse(line);
            if (action != null && !action.getType().trim().isEmpty()) {
                actions.add(action);
            }
        }
        return actions;
    }

    public static Action closeTabs(List<String> tabIds) {
        return action(TYPE_CLOSE_TABS).putStrings(KEY_TAB_IDS, tabIds);
    }

    public static Action restoreInactiveTabs(List<String> tabIds) {
        return action(TYPE_RESTORE_INACTIVE_TABS).putStrings(KEY_TAB_IDS, tabIds);
    }

    public static Action createTab(boolean isPrivate, String groupId, String groupName,
                                   int groupColor) {
        return action(TYPE_CREATE_TAB)
                .putBoolean(KEY_PRIVATE, isPrivate)
                .putString(KEY_GROUP_ID, groupId)
                .putString(KEY_GROUP_NAME, groupName)
                .putInt(KEY_GROUP_COLOR, groupColor);
    }

    public static Action restoreUrl(String url, boolean isPrivate) {
        return action(TYPE_RESTORE_URL)
                .putString(KEY_URL, url)
                .putBoolean(KEY_PRIVATE, isPrivate);
    }

    public static Action createPrivateGroup(String groupId, String groupName, int groupColor,
                                            List<String> tabIds) {
        return action(TYPE_CREATE_PRIVATE_GROUP)
                .putString(KEY_GROUP_ID, groupId)
                .putString(KEY_GROUP_NAME, groupName)
                .putInt(KEY_GROUP_COLOR, groupColor)
                .putStrings(KEY_TAB_IDS, tabIds);
    }

    public static Action setGroup(String tabId, String groupId, String groupName, int groupColor) {
        return action(TYPE_SET_GROUP)
                .putString(KEY_TAB_ID, tabId)
                .putString(KEY_GROUP_ID, groupId)
                .putString(KEY_GROUP_NAME, groupName)
                .putInt(KEY_GROUP_COLOR, groupColor);
    }

    public static Action reorderPrivateTabs(List<String> tabIds) {
        return action(TYPE_REORDER_PRIVATE_TABS).putStrings(KEY_TAB_IDS, tabIds);
    }

    public static Action setPinned(List<String> tabIds, boolean pinned) {
        return action(TYPE_SET_PINNED)
                .putStrings(KEY_TAB_IDS, tabIds)
                .putBoolean(KEY_PINNED, pinned);
    }

    public static Action setLocked(List<String> tabIds, boolean locked) {
        return action(TYPE_SET_LOCKED)
                .putStrings(KEY_TAB_IDS, tabIds)
                .putBoolean(KEY_LOCKED, locked);
    }

    public static Action groupsChanged() {
        return action(TYPE_GROUPS_CHANGED);
    }

    public static Action selectTab(String tabId) {
        return action(TYPE_SELECT_TAB).putString(KEY_TAB_ID, tabId);
    }

    private static Action action(String type) {
        return new Action().putString(KEY_TYPE, type);
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value != null ? value : "", UTF_8);
        } catch (java.io.UnsupportedEncodingException e) {
            return "";
        }
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value != null ? value : "", UTF_8);
        } catch (IllegalArgumentException | java.io.UnsupportedEncodingException e) {
            return "";
        }
    }

    public static final class Action {
        private final LinkedHashMap<String, String> values = new LinkedHashMap<>();

        private Action() {
        }

        public String getType() {
            return getValue(KEY_TYPE, "");
        }

        public String getTabId() {
            return getValue(KEY_TAB_ID, "");
        }

        public List<String> getTabIds() {
            return getStringList(KEY_TAB_IDS);
        }

        public boolean isPrivate() {
            return Boolean.parseBoolean(getValue(KEY_PRIVATE, "false"));
        }

        public String getUrl() {
            return getValue(KEY_URL, "");
        }

        public String getGroupId() {
            return getValue(KEY_GROUP_ID, "");
        }

        public String getGroupName() {
            return getValue(KEY_GROUP_NAME, "");
        }

        public int getGroupColor(int defaultColor) {
            try {
                return Integer.parseInt(getValue(KEY_GROUP_COLOR, ""));
            } catch (NumberFormatException ignored) {
                return defaultColor;
            }
        }

        public boolean isPinned() {
            return Boolean.parseBoolean(getValue(KEY_PINNED, "false"));
        }

        public boolean isLocked() {
            return Boolean.parseBoolean(getValue(KEY_LOCKED, "false"));
        }

        private String getValue(String key, String fallback) {
            String value = values.get(key);
            return value != null ? value : fallback;
        }

        private static Action parse(String line) {
            if (line == null || line.trim().isEmpty()) {
                return null;
            }
            Action action = new Action();
            String[] fields = line.split(FIELD_SEPARATOR);
            for (String field : fields) {
                int separator = field.indexOf(KEY_VALUE_SEPARATOR);
                if (separator <= 0) {
                    continue;
                }
                String key = decode(field.substring(0, separator));
                String value = decode(field.substring(separator + 1));
                if (!key.trim().isEmpty()) {
                    action.values.put(key, value);
                }
            }
            return action;
        }

        private String serialize() {
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<String, String> entry : values.entrySet()) {
                if (builder.length() > 0) {
                    builder.append(FIELD_SEPARATOR);
                }
                builder.append(encode(entry.getKey()))
                        .append(KEY_VALUE_SEPARATOR)
                        .append(encode(entry.getValue()));
            }
            return builder.toString();
        }

        private List<String> getStringList(String key) {
            String encodedList = values.get(key);
            if (encodedList == null || encodedList.trim().isEmpty()) {
                return Collections.emptyList();
            }
            String[] parts = encodedList.split(LIST_SEPARATOR);
            ArrayList<String> decoded = new ArrayList<>();
            for (String part : parts) {
                String value = decode(part);
                if (!value.trim().isEmpty()) {
                    decoded.add(value);
                }
            }
            return decoded;
        }

        private Action putString(String key, String value) {
            values.put(key, value != null ? value : "");
            return this;
        }

        private Action putStrings(String key, List<String> listValues) {
            StringBuilder builder = new StringBuilder();
            if (listValues != null) {
                for (String value : listValues) {
                    if (value == null || value.trim().isEmpty()) {
                        continue;
                    }
                    if (builder.length() > 0) {
                        builder.append(LIST_SEPARATOR);
                    }
                    builder.append(encode(value));
                }
            }
            values.put(key, builder.toString());
            return this;
        }

        private Action putBoolean(String key, boolean value) {
            values.put(key, String.valueOf(value));
            return this;
        }

        private Action putInt(String key, int value) {
            values.put(key, String.valueOf(value));
            return this;
        }
    }
}
