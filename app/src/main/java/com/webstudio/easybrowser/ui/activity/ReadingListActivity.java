package com.webstudio.easybrowser.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.adapters.ReadingListAdapter;
import com.webstudio.easybrowser.models.ReadingListItem;
import com.webstudio.easybrowser.repository.ReadingListRepository;
import com.webstudio.easybrowser.utils.ThemeEngine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ReadingListActivity extends AppCompatActivity implements ReadingListAdapter.OnItemClickListener {
    private RecyclerView recyclerView;
    private ReadingListAdapter adapter;
    private TextView emptyView;
    private ReadingListRepository repository;
    private List<ReadingListItem> items = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reading_list);
        setupToolbar();
        initializeViews();
        loadItems();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.reading_list);
        }
        ThemeEngine.applyChrome(this, toolbar);
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.reading_list_recycler);
        emptyView = findViewById(R.id.empty_view);
        repository = new ReadingListRepository(this);
        adapter = new ReadingListAdapter(items, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadItems() {
        repository.getAll(new ReadingListRepository.ReadingListCallback() {
            @Override public void onItemsLoaded(List<ReadingListItem> loaded) {
                runOnUiThread(() -> {
                    items.clear();
                    items.addAll(loaded);
                    adapter.setItems(new ArrayList<>(items));
                    emptyView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                    recyclerView.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
                });
            }
            @Override public void onItemSaved() {}
            @Override public void onItemDeleted() { runOnUiThread(ReadingListActivity.this::loadItems); }
        });
    }

    @Override
    public void onItemClick(ReadingListItem item) {
        if (item.getContentPath() != null) {
            showOpenDialog(item);
        } else {
            openInBrowser(item.getUrl());
        }
    }

    @Override
    public void onItemLongClick(ReadingListItem item) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(item.getTitle() != null ? item.getTitle() : item.getUrl())
                .setItems(new CharSequence[]{"Open in browser", "Delete"}, (dialog, which) -> {
                    if (which == 0) openInBrowser(item.getUrl());
                    else deleteItem(item);
                })
                .show();
    }

    private void showOpenDialog(ReadingListItem item) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(item.getTitle() != null ? item.getTitle() : item.getUrl())
                .setItems(new CharSequence[]{"Open saved copy (PDF)", "Open live page"}, (dialog, which) -> {
                    if (which == 0) openSavedPdf(item);
                    else openInBrowser(item.getUrl());
                })
                .show();
    }

    private void openSavedPdf(ReadingListItem item) {
        File file = new File(item.getContentPath());
        if (!file.exists()) {
            Toast.makeText(this, R.string.reading_list_file_missing, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, BrowserActivity.class);
        intent.putExtra("url", "file://" + file.getAbsolutePath());
        startActivity(intent);
    }

    private void openInBrowser(String url) {
        Intent intent = new Intent(this, BrowserActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
        finish();
    }

    private void deleteItem(ReadingListItem item) {
        if (item.getContentPath() != null) {
            new File(item.getContentPath()).delete();
        }
        repository.delete(item, new ReadingListRepository.ReadingListCallback() {
            @Override public void onItemsLoaded(List<ReadingListItem> items) {}
            @Override public void onItemSaved() {}
            @Override public void onItemDeleted() { runOnUiThread(ReadingListActivity.this::loadItems); }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
