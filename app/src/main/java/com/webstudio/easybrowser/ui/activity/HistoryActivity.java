package com.webstudio.easybrowser.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.webstudio.easybrowser.R;
import com.webstudio.easybrowser.adapters.HistoryAdapter;
import com.webstudio.easybrowser.models.HistoryItem;
import com.webstudio.easybrowser.repository.HistoryRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity implements HistoryAdapter.OnHistoryClickListener {
    private RecyclerView historyRecycler;
    private HistoryAdapter adapter;
    private TextView emptyView;
    private SearchView searchView;
    private HistoryRepository repository;
    private List<HistoryItem> allHistoryItems;
    private List<HistoryAdapter.Item> filteredHistoryItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Browsing history is privacy-sensitive — keep it out of screenshots and
        // the recent-apps thumbnail.
        getWindow().setFlags(
                android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_history);

        setupToolbar();
        initializeViews();
        setupRecyclerView();
        setupSearch();
        loadHistory();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.history);
        }
    }

    private void initializeViews() {
        historyRecycler = findViewById(R.id.history_recycler);
        emptyView = findViewById(R.id.empty_view);
        searchView = findViewById(R.id.search_view);

        repository = new HistoryRepository(this);
        allHistoryItems = new ArrayList<>();
        filteredHistoryItems = new ArrayList<>();
    }

    private void setupRecyclerView() {
        adapter = new HistoryAdapter(filteredHistoryItems, this);
        historyRecycler.setLayoutManager(new LinearLayoutManager(this));
        historyRecycler.setAdapter(adapter);
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterHistory(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterHistory(newText);
                return true;
            }
        });
    }

    private void loadHistory() {
        repository.getAllHistory(new HistoryRepository.HistoryCallback() {
            @Override
            public void onHistoryLoaded(List<HistoryItem> historyItems) {
                runOnUiThread(() -> {
                    allHistoryItems.clear();
                    allHistoryItems.addAll(historyItems);
                    filterHistory("");
                    updateEmptyView();
                });
            }

            @Override
            public void onHistoryItemAdded(HistoryItem item) {
                runOnUiThread(() -> {
                    allHistoryItems.add(0, item);
                    filterHistory(searchView.getQuery().toString());
                    updateEmptyView();
                });
            }

            @Override
            public void onHistoryCleared() {
                runOnUiThread(() -> {
                    allHistoryItems.clear();
                    filteredHistoryItems.clear();
                    adapter.notifyDataSetChanged();
                    updateEmptyView();
                });
            }
        });
    }

    private void filterHistory(String query) {
        filteredHistoryItems.clear();

        if (query.isEmpty()) {
            for (HistoryItem item : allHistoryItems) {
                filteredHistoryItems.add(HistoryAdapter.Item.history(item));
            }
        } else {
            query = query.toLowerCase();
            for (HistoryItem item : allHistoryItems) {
                if (item.getTitle().toLowerCase().contains(query) ||
                        item.getUrl().toLowerCase().contains(query)) {
                    filteredHistoryItems.add(HistoryAdapter.Item.history(item));
                }
            }
        }

        // Group by date
        List<HistoryAdapter.Item> groupedItems = groupHistoryByDate(filteredHistoryItems);
        filteredHistoryItems.clear();
        filteredHistoryItems.addAll(groupedItems);

        adapter.notifyDataSetChanged();
        updateEmptyView();
    }

    private List<HistoryAdapter.Item> groupHistoryByDate(List<HistoryAdapter.Item> items) {
        List<HistoryAdapter.Item> grouped = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        String currentDateHeader = "";

        for (HistoryAdapter.Item row : items) {
            HistoryItem item = row.getHistoryItem();
            String itemDate = dateFormat.format(new Date(item.getVisitTime()));

            if (!itemDate.equals(currentDateHeader)) {
                grouped.add(HistoryAdapter.Item.header(itemDate));
                currentDateHeader = itemDate;
            }

            grouped.add(row);
        }

        return grouped;
    }

    private void updateEmptyView() {
        if (filteredHistoryItems.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            historyRecycler.setVisibility(View.GONE);
            if (allHistoryItems.isEmpty()) {
                emptyView.setText(R.string.no_history);
            } else {
                emptyView.setText(R.string.no_search_results);
            }
        } else {
            emptyView.setVisibility(View.GONE);
            historyRecycler.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onHistoryClick(HistoryItem item) {
        Intent intent = new Intent(this, BrowserActivity.class);
        intent.putExtra("url", item.getUrl());
        startActivity(intent);
    }

    @Override
    public void onHistoryLongClick(HistoryItem item) {
        showHistoryContextMenu(item);
    }

    private void showHistoryContextMenu(HistoryItem item) {
        String[] options = new String[]{
                getString(R.string.open_in_new_tab),
                getString(R.string.share),
                getString(R.string.copy_link),
                getString(R.string.delete)
        };

        new MaterialAlertDialogBuilder(this)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Open in new tab
                            // Implement open in new tab logic
                            Intent intent = new Intent(this, BrowserActivity.class);
                            intent.putExtra("url", item.getUrl());
                            intent.putExtra("new_tab", true);
                            startActivity(intent);
                            break;
                        case 1: // Share
                            shareHistory(item);
                            break;
                        case 2: // Copy link
                            copyToClipboard(item.getUrl());
                            break;
                        case 3: // Delete
                            confirmDeleteHistory(item);
                            break;
                    }
                })
                .show();
    }

    private void shareHistory(HistoryItem item) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, item.getTitle() + "\n" + item.getUrl());
        startActivity(Intent.createChooser(intent, getString(R.string.share)));
    }

    private void copyToClipboard(String text) {
        android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("URL", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, R.string.link_copied, Toast.LENGTH_SHORT).show();
    }

    private void confirmDeleteHistory(HistoryItem item) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_history_item)
                .setMessage(getString(R.string.confirm_delete_history, item.getTitle()))
                .setPositiveButton(R.string.dialog_delete, (dialog, which) -> deleteHistoryItem(item))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteHistoryItem(HistoryItem item) {
        repository.deleteHistoryItem(item);
        allHistoryItems.remove(item);
        filterHistory(searchView.getQuery().toString());
        Toast.makeText(this, R.string.history_item_deleted, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.history_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_clear_history) {
            confirmClearAllHistory();
            return true;
        } else if (id == R.id.action_clear_today) {
            confirmClearTodayHistory();
            return true;
        } else if (id == R.id.action_clear_week) {
            confirmClearWeekHistory();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmClearAllHistory() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.clear_history)
                .setMessage(R.string.confirm_clear_all_history)
                .setPositiveButton(R.string.dialog_clear, (dialog, which) -> {
                    repository.clearHistory(new HistoryRepository.HistoryCallback() {
                        @Override
                        public void onHistoryLoaded(List<HistoryItem> historyItems) {}

                        @Override
                        public void onHistoryItemAdded(HistoryItem item) {}

                        @Override
                        public void onHistoryCleared() {
                            loadHistory();
                            Toast.makeText(HistoryActivity.this, R.string.history_cleared, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmClearTodayHistory() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.clear_today_history)
                .setMessage(R.string.confirm_clear_today_history)
                .setPositiveButton(R.string.dialog_clear, (dialog, which) -> clearHistoryRange(getTodayStart(), System.currentTimeMillis()))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmClearWeekHistory() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.clear_week_history)
                .setMessage(R.string.confirm_clear_week_history)
                .setPositiveButton(R.string.dialog_clear, (dialog, which) -> clearHistoryRange(getWeekStart(), System.currentTimeMillis()))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void clearHistoryRange(long startTime, long endTime) {
        repository.clearHistoryBetweenTimes(startTime, endTime, new HistoryRepository.HistoryCallback() {
            @Override
            public void onHistoryLoaded(List<HistoryItem> historyItems) {}

            @Override
            public void onHistoryItemAdded(HistoryItem item) {}

            @Override
            public void onHistoryCleared() {
                runOnUiThread(() -> {
                    loadHistory();
                    Toast.makeText(HistoryActivity.this, R.string.history_cleared, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private long getTodayStart() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private long getWeekStart() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
