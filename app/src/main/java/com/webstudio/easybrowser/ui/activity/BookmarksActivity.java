package com.webstudio.easybrowser.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.adapters.BookmarksAdapter;
import com.webstudio.easybrowser.models.Bookmark;
import com.webstudio.easybrowser.repository.BookmarkRepository;
import com.webstudio.easybrowser.utils.ThemeEngine;

import java.util.ArrayList;
import java.util.List;

public class BookmarksActivity extends AppCompatActivity implements BookmarksAdapter.OnBookmarkClickListener {
    private RecyclerView bookmarksRecycler;
    private BookmarksAdapter adapter;
    private ImageButton btnViewType;
    private FloatingActionButton fabAddBookmark;
    private BookmarkRepository repository;
    private List<Bookmark> allBookmarks;
    private List<Bookmark> filteredBookmarks;
    private boolean isGridView = false;
    private SearchView searchView;
    private TextView emptyView;
    private ChipGroup folderChipGroup;
    private String selectedFolder = null; // null = All

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmarks);

        setupToolbar();
        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        loadBookmarks();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.bookmarks);
        }
        ThemeEngine.applyChrome(this, toolbar);
    }

    private void initializeViews() {
        bookmarksRecycler = findViewById(R.id.bookmarks_recycler);
        btnViewType = findViewById(R.id.btn_view_type);
        fabAddBookmark = findViewById(R.id.fab_add_bookmark);
        emptyView = findViewById(R.id.empty_view);
        folderChipGroup = findViewById(R.id.folder_chip_group);

        repository = new BookmarkRepository(this);
        allBookmarks = new ArrayList<>();
        filteredBookmarks = new ArrayList<>();
    }

    private void setupRecyclerView() {
        adapter = new BookmarksAdapter(filteredBookmarks, this);

        // Set initial layout manager
        updateLayoutManager();

        bookmarksRecycler.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnViewType.setOnClickListener(v -> toggleViewType());

        fabAddBookmark.setOnClickListener(v -> showAddBookmarkDialog());

        // Setup search
        setupSearch();
    }

    private void setupSearch() {
        // Assuming there's a SearchView in the toolbar
        searchView = findViewById(R.id.search_view);
        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    filterBookmarks(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    filterBookmarks(newText);
                    return true;
                }
            });
        }
    }

    private void toggleViewType() {
        isGridView = !isGridView;
        updateLayoutManager();
        updateViewTypeIcon();
    }

    private void updateLayoutManager() {
        if (isGridView) {
            bookmarksRecycler.setLayoutManager(new GridLayoutManager(this, 2));
        } else {
            bookmarksRecycler.setLayoutManager(new LinearLayoutManager(this));
        }

        if (adapter != null) {
            adapter.setViewType(isGridView ? BookmarksAdapter.VIEW_TYPE_GRID : BookmarksAdapter.VIEW_TYPE_LIST);
        }
    }

    private void updateViewTypeIcon() {
        if (isGridView) {
            btnViewType.setImageResource(R.drawable.ic_view_list);
        } else {
            btnViewType.setImageResource(R.drawable.ic_view_grid);
        }
    }

    private void loadBookmarks() {
        repository.getAllBookmarks(new BookmarkRepository.BookmarkCallback() {
            @Override
            public void onBookmarksLoaded(List<Bookmark> bookmarks) {
                runOnUiThread(() -> {
                    allBookmarks.clear();
                    allBookmarks.addAll(bookmarks);
                    rebuildFolderChips();
                    filterBookmarks(searchView != null ? searchView.getQuery().toString() : "");
                    updateEmptyView();
                });
            }

            @Override
            public void onBookmarkAdded(Bookmark bookmark) {
                runOnUiThread(() -> {
                    allBookmarks.add(bookmark);
                    filterBookmarks(searchView != null ? searchView.getQuery().toString() : "");
                    updateEmptyView();
                });
            }

            @Override
            public void onBookmarkRemoved(Bookmark bookmark) {
                runOnUiThread(() -> {
                    allBookmarks.remove(bookmark);
                    filterBookmarks(searchView != null ? searchView.getQuery().toString() : "");
                    updateEmptyView();
                });
            }
        });
    }

    private void filterBookmarks(String query) {
        filteredBookmarks.clear();

        String lowered = query == null ? "" : query.toLowerCase();
        for (Bookmark bookmark : allBookmarks) {
            if (selectedFolder != null) {
                String f = bookmark.getFolder();
                if (f == null || !f.equals(selectedFolder)) {
                    continue;
                }
            }
            if (!lowered.isEmpty()) {
                String title = bookmark.getTitle() != null ? bookmark.getTitle().toLowerCase() : "";
                String url = bookmark.getUrl() != null ? bookmark.getUrl().toLowerCase() : "";
                if (!title.contains(lowered) && !url.contains(lowered)) {
                    continue;
                }
            }
            filteredBookmarks.add(bookmark);
        }

        adapter.notifyDataSetChanged();
        updateEmptyView();
    }

    private void rebuildFolderChips() {
        if (folderChipGroup == null) return;
        View scrollContainer = findViewById(R.id.folder_chip_scroll);

        java.util.LinkedHashSet<String> folders = new java.util.LinkedHashSet<>();
        for (Bookmark b : allBookmarks) {
            if (b.getFolder() != null && !b.getFolder().trim().isEmpty()) {
                folders.add(b.getFolder().trim());
            }
        }

        if (folders.isEmpty()) {
            if (scrollContainer != null) scrollContainer.setVisibility(View.GONE);
            folderChipGroup.removeAllViews();
            selectedFolder = null;
            return;
        }

        if (scrollContainer != null) scrollContainer.setVisibility(View.VISIBLE);
        folderChipGroup.removeAllViews();

        Chip allChip = new Chip(this);
        allChip.setText(R.string.folder_all);
        allChip.setCheckable(true);
        allChip.setChecked(selectedFolder == null);
        allChip.setOnClickListener(v -> {
            selectedFolder = null;
            filterBookmarks(searchView != null ? searchView.getQuery().toString() : "");
        });
        folderChipGroup.addView(allChip);

        for (String folder : folders) {
            Chip chip = new Chip(this);
            chip.setText(folder);
            chip.setCheckable(true);
            chip.setChecked(folder.equals(selectedFolder));
            chip.setOnClickListener(v -> {
                selectedFolder = folder;
                filterBookmarks(searchView != null ? searchView.getQuery().toString() : "");
            });
            folderChipGroup.addView(chip);
        }
    }

    private void updateEmptyView() {
        if (filteredBookmarks.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            bookmarksRecycler.setVisibility(View.GONE);
            emptyView.setText(allBookmarks.isEmpty() ? R.string.no_bookmarks : R.string.no_search_results);
        } else {
            emptyView.setVisibility(View.GONE);
            bookmarksRecycler.setVisibility(View.VISIBLE);
        }
    }

    private void showAddBookmarkDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_bookmark, null);

        TextInputEditText titleInput = dialogView.findViewById(R.id.bookmark_title_input);
        TextInputEditText urlInput = dialogView.findViewById(R.id.bookmark_url_input);
        MaterialAutoCompleteTextView folderInput = dialogView.findViewById(R.id.bookmark_folder_input);
        populateFolderDropdown(folderInput, null);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.add_bookmark)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String title = titleInput.getText().toString().trim();
                    String url = urlInput.getText().toString().trim();
                    String folder = folderInput.getText() != null
                            ? folderInput.getText().toString().trim() : "";

                    if (!title.isEmpty() && !url.isEmpty()) {
                        addBookmark(title, url, folder);
                    } else {
                        Toast.makeText(this, R.string.error_invalid_url, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void addBookmark(String title, String url, String folder) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        Bookmark bookmark = new Bookmark(title, url);
        bookmark.setFolder(folder == null ? "" : folder);
        repository.addBookmark(bookmark, new BookmarkRepository.BookmarkCallback() {
            @Override
            public void onBookmarksLoaded(List<Bookmark> bookmarks) {}

            @Override
            public void onBookmarkAdded(Bookmark bookmark) {
                runOnUiThread(() -> {
                    allBookmarks.add(bookmark);
                    rebuildFolderChips();
                    filterBookmarks(searchView != null ? searchView.getQuery().toString() : "");
                    updateEmptyView();
                    Toast.makeText(BookmarksActivity.this, R.string.bookmark_added_message, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onBookmarkRemoved(Bookmark bookmark) {}
        });
    }

    private void populateFolderDropdown(MaterialAutoCompleteTextView folderInput, String preset) {
        if (preset != null) folderInput.setText(preset);
        repository.getFolderNames(names -> runOnUiThread(() -> {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    BookmarksActivity.this,
                    android.R.layout.simple_list_item_1,
                    names);
            folderInput.setAdapter(adapter);
        }));
    }

    @Override
    public void onBookmarkClick(Bookmark bookmark) {
        Intent intent = new Intent(this, BrowserActivity.class);
        intent.putExtra("url", bookmark.getUrl());
        startActivity(intent);
    }

    @Override
    public void onBookmarkLongClick(Bookmark bookmark) {
        String[] options = new String[]{
                getString(R.string.edit_bookmark),
                getString(R.string.remove_bookmark),
                getString(R.string.share)
        };

        new MaterialAlertDialogBuilder(this)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Edit
                            showEditBookmarkDialog(bookmark);
                            break;
                        case 1: // Delete
                            confirmDeleteBookmark(bookmark);
                            break;
                        case 2: // Share
                            shareBookmark(bookmark);
                            break;
                    }
                })
                .show();
    }

    private void showEditBookmarkDialog(Bookmark bookmark) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_bookmark, null);

        TextInputEditText titleInput = dialogView.findViewById(R.id.bookmark_title_input);
        TextInputEditText urlInput = dialogView.findViewById(R.id.bookmark_url_input);
        MaterialAutoCompleteTextView folderInput = dialogView.findViewById(R.id.bookmark_folder_input);

        titleInput.setText(bookmark.getTitle());
        urlInput.setText(bookmark.getUrl());
        populateFolderDropdown(folderInput, bookmark.getFolder());

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.edit_bookmark)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String title = titleInput.getText().toString().trim();
                    String url = urlInput.getText().toString().trim();
                    String folder = folderInput.getText() != null
                            ? folderInput.getText().toString().trim() : "";

                    if (!title.isEmpty() && !url.isEmpty()) {
                        bookmark.setTitle(title);
                        bookmark.setUrl(url);
                        bookmark.setFolder(folder);
                        updateBookmark(bookmark);
                    } else {
                        Toast.makeText(this, R.string.error_invalid_url, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void updateBookmark(Bookmark bookmark) {
        repository.updateBookmark(bookmark, new BookmarkRepository.BookmarkCallback() {
            @Override
            public void onBookmarksLoaded(List<Bookmark> bookmarks) {}

            @Override
            public void onBookmarkAdded(Bookmark updatedBookmark) {
                runOnUiThread(() -> {
                    loadBookmarks();
                    Toast.makeText(BookmarksActivity.this, R.string.bookmark_updated, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onBookmarkRemoved(Bookmark bookmark) {}
        });
    }

    private void confirmDeleteBookmark(Bookmark bookmark) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.remove_bookmark)
                .setMessage(getString(R.string.dialog_message_delete))
                .setPositiveButton(R.string.dialog_delete, (dialog, which) -> deleteBookmark(bookmark))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteBookmark(Bookmark bookmark) {
        repository.removeBookmark(bookmark, new BookmarkRepository.BookmarkCallback() {
            @Override
            public void onBookmarksLoaded(List<Bookmark> bookmarks) {}

            @Override
            public void onBookmarkAdded(Bookmark bookmark) {}

            @Override
            public void onBookmarkRemoved(Bookmark bookmark) {
                runOnUiThread(() -> {
                    allBookmarks.remove(bookmark);
                    filterBookmarks(searchView != null ? searchView.getQuery().toString() : "");
                    updateEmptyView();
                    Toast.makeText(BookmarksActivity.this, R.string.bookmark_removed_message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void shareBookmark(Bookmark bookmark) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, bookmark.getTitle() + "\n" + bookmark.getUrl());
        startActivity(Intent.createChooser(intent, getString(R.string.share_link)));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
