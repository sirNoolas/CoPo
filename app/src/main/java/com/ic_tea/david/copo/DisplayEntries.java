/*
 * Copyright (c) 2015. This was programmed by IC-Tea (@sirNoolas - David Vonk): Do not copy!
 */

package com.ic_tea.david.copo;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

public class DisplayEntries extends AppCompatActivity {
    public static final String INTENT_TYPE_EXTRA = "com.ic_tea.david.copo.TYPEFOREDIT";
    public static final String INTENT_ENTRY_ID_EXTRA = "com.ic_tea.david.copo.ENTRY";
    public static final int INTENT_REQUEST_EDIT = 1;
    private final String TAG = DisplayEntries.class.getSimpleName();
    ArrayList<Entry> entries;
    ListView listView;
    TextView instructions;
    DBHelper dbHelper;
    String typeStr;
    private int type;

    public static ArrayList<String> getInfo(ArrayList<Entry> entries) {
        ArrayList<String> info = new ArrayList<>();

        for (Entry entry : entries) {
            String name = entry.getName();

            // Format date from 'YYYYMMDDHHMM' to 'DD-MM-YYYY'
            String date = entry.getDateOfLastEdit(); // YYYYMMDDHHMM
            date = date.substring(6, 8) + "-" + date.substring(4, 6) + "-" + date.substring(0, 4);

            info.add(name + "  |  " + date);
        }
        return info;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        type = getIntent().getIntExtra(MainActivity.INTENT_TYPE_EXTRA, 0);
        typeStr = getResources().getStringArray(R.array.competences)[type];
        setTitle(typeStr);

        setContentView(R.layout.activity_display_entries);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        dbHelper = DBHelper.getInstance(this); // this may be laggy
        instructions = (TextView) findViewById(R.id.instructions);
        refreshListView();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startAddEntryActivity();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void refreshListView() {
        entries = dbHelper.getMultipleEntries(type);
        if (entries.size() > 0) {
            instructions.setVisibility(View.GONE);
        } else {
            instructions.setVisibility(View.VISIBLE);
        }
        setupListView(getInfo(entries));
    }

    private void startAddEntryActivity() { startAddEntryActivity(-1); }
    private void startAddEntryActivity(int id) {
        Intent intent = new Intent(this, EditEntry.class);
        intent.putExtra(INTENT_TYPE_EXTRA, type);
        intent.putExtra(INTENT_ENTRY_ID_EXTRA, id);
        startActivityForResult(intent, INTENT_REQUEST_EDIT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == INTENT_REQUEST_EDIT && resultCode != RESULT_CANCELED) {
            refreshListView();
            String message = getString(R.string.confirm_save_toast);
            Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    private void setupListView (ArrayList<String> info) {
        listView = (ListView) findViewById(R.id.entriesListView);
        final EntryAdapter entryAdapter =
                new EntryAdapter(this, R.layout.entry_item, info);

        listView.setAdapter(entryAdapter);

        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        // Capture listview item click
        listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode mode,
                                                  int position, long id, boolean checked) {
                mode.setTitle(listView.getCheckedItemCount() + " " +
                        getString(R.string.selected_shareselect));
                entryAdapter.toggleSelection(position);
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.getMenuInflater().inflate(R.menu.menu_display_entries, menu);
                getSupportActionBar().hide();
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                SparseBooleanArray selected;

                switch (item.getItemId()) {
                    case R.id.delete_option:
                        // Calls getSelectedIds method from EntryListViewAdapter Class
                        selected = entryAdapter.getSelectedIds();

                        // Captures all selected ids with a loop
                        for (int i = (selected.size() - 1); i >= 0; i--) {
                            if (selected.valueAt(i)) {
                                int key = selected.keyAt(i);

                                // remove from adapter
                                String selectedItem = entryAdapter.getItem(key);
                                entryAdapter.remove(selectedItem);

                                // remove from db
                                dbHelper.deleteEntry(entries.get(key));
                                entries.remove(key);
                            }
                        }

                        // Close CAB (Contextual action bar)
                        mode.finish();
                        return true;

                    case R.id.share_option:
                        // Calls getSelectedIds method from EntryListViewAdapter Class
                        selected = entryAdapter.getSelectedIds();

                        // isolate the selected entries
                        ArrayList<Entry> sharableEntries = new ArrayList<>();
                        for (int i = (selected.size() - 1); i >= 0; i--) {
                            if (selected.valueAt(i)) {
                                sharableEntries.add(entries.get(selected.keyAt(i)));
                            }
                        }
                        startShareIntent(sharableEntries);
                        mode.finish();
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                entryAdapter.removeSelection();
                getSupportActionBar().show();
            }
        });


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                startAddEntryActivity(entries.get(position).getId());
            }
        });
    }

    private void startShareIntent(ArrayList<Entry> sharableEntries) {
        // if no sharable entries were found
        if (!Entry.share(this, sharableEntries)) {
            String noEntriesMessage = getString(R.string.no_sharable_entries_message);
            Toast toast = Toast.makeText(this, noEntriesMessage, Toast.LENGTH_SHORT);
            toast.show();
        }
    }

}
