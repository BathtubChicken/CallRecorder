package net.synapticweb.callrecorder.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import com.codekidlabs.storagechooser.Content;
import com.codekidlabs.storagechooser.StorageChooser;

import net.synapticweb.callrecorder.CrApp;
import net.synapticweb.callrecorder.R;
import net.synapticweb.callrecorder.TemplateActivity;

import java.io.File;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

public class SettingsFragment extends PreferenceFragmentCompat {
    //aceste valori vor fi dublate în res/xml/preferences.xml
    public static final String AUTOMMATICALLY_RECORD_PRIVATE_CALLS = "auth_record_priv";
    public static final String PARANOID_MODE = "paranoid";
    public static final String APP_THEME = "theme";
    public static final String STORAGE = "storage";
    public static final String STORAGE_PATH = "public_storage_path";
    public static final String SPEAKER_USE = "put_on_speaker";
    public static final String FORMAT = "format";
    public static final String MODE = "mode";
    public static final String GAIN = "gain";

    @Override
    public void onResume() {
        super.onResume();
        Preference storagePath = findPreference(STORAGE_PATH);
        ListPreference storage = (ListPreference) findPreference(STORAGE);
        manageStoragePathSummary(null, storage, storagePath);
    }

    private void manageStoragePathSummary(String newValue, ListPreference storage, Preference storagePath) {
        String storageValue = newValue == null ? storage.getValue() : newValue;
        if(storageValue.equals("public")) {
            storagePath.setEnabled(true);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            String path = preferences.getString(STORAGE_PATH, null);
            if(path == null) {
                File externalDir = getActivity().getExternalFilesDir(null);
                if(externalDir != null)
                    path = externalDir.getAbsolutePath();
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(STORAGE_PATH, path);
                editor.apply();
            }
            storagePath.setSummary(path);
        }
        else {
            storagePath.setEnabled(false);
            storagePath.setSummary(CrApp.getInstance().getResources().getString(R.string.private_storage));
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //cu androidx este nevoie ca dividerul să fie setat explicit.
        RecyclerView recycler = getListView();
        recycler.addItemDecoration(new DividerItemDecoration(getContext(),
                DividerItemDecoration.VERTICAL));
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);

        Preference themeOption = findPreference(APP_THEME);
        Preference paranoidMode = findPreference(PARANOID_MODE);
        Preference format = findPreference(FORMAT);
        Preference mode = findPreference(MODE);
        Preference storage = findPreference(STORAGE);
        final Preference storagePath = findPreference(STORAGE_PATH);
        Preference gain = findPreference(GAIN);

        storagePath.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                Content content = new Content();
                content.setOverviewHeading(CrApp.getInstance().getResources().getString(R.string.choose_recordings_storage));
                StorageChooser.Theme theme = new StorageChooser.Theme(getActivity());
                theme.setScheme(getResources().getIntArray(R.array.storage_chooser_theme));

                StorageChooser chooser = new StorageChooser.Builder()
                        .withActivity(getActivity())
                        .withFragmentManager(getActivity().getFragmentManager())
                        .allowCustomPath(true)
                        .setType(StorageChooser.DIRECTORY_CHOOSER)
                        .withMemoryBar(true)
                        .allowAddFolder(true)
                        .showHidden(true)
                        .withContent(content)
                        .setTheme(((SettingsActivity) getActivity()).getSettedTheme().equals(TemplateActivity.DARK_THEME) ?
                                theme : null)
                        .build();

                chooser.show();

                chooser.setOnSelectListener(new StorageChooser.OnSelectListener() {
                    @Override
                    public void onSelect(String path) {
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString(STORAGE_PATH, path);
                        editor.apply();
                        preference.setSummary(path);
                    }
                });
                return true;
            }
        });

        storage.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                manageStoragePathSummary((String) newValue, (ListPreference) preference, storagePath);
                return true;
            }
        });

        storage.setSummaryProvider(new Preference.SummaryProvider<ListPreference>() {
            @Override
            public CharSequence provideSummary(ListPreference preference) {
                return preference.getEntry();
            }
        });

        paranoidMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Preference autoPrivCalls = findPreference(AUTOMMATICALLY_RECORD_PRIVATE_CALLS);
                autoPrivCalls.setEnabled(!autoPrivCalls.isEnabled());
                return true;
            }
        });

        themeOption.setSummaryProvider(new Preference.SummaryProvider<ListPreference>() {
            @Override
            public CharSequence provideSummary(ListPreference preference) {
                return preference.getEntry();
            }
        });

        gain.setSummaryProvider(new Preference.SummaryProvider<ListPreference>() {
            @Override
            public CharSequence provideSummary(ListPreference preference) {
                return preference.getEntry();
            }
        });

        themeOption.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                getActivity().recreate();
                return true;
            }
        });

        format.setSummaryProvider(new Preference.SummaryProvider<ListPreference>() {
            @Override
            public CharSequence provideSummary(ListPreference preference) {
                return preference.getEntry();
            }
        });

        mode.setSummaryProvider(new Preference.SummaryProvider<ListPreference>() {
            @Override
            public CharSequence provideSummary(ListPreference preference) {
                return preference.getEntry();
            }
        });

    }
}