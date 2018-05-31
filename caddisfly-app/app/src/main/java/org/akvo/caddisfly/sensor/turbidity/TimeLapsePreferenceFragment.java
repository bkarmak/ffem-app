package org.akvo.caddisfly.sensor.turbidity;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.View;
import android.widget.ListView;

import org.akvo.caddisfly.R;
import org.akvo.caddisfly.util.ListViewUtil;

import java.util.Arrays;

/**
 * A simple {@link Fragment} subclass.
 */
public class TimeLapsePreferenceFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int MAX_SAMPLE_NUMBER = 100;
    private ListView list;

    public TimeLapsePreferenceFragment() {
        // Required empty public constructor
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {

        String[] keys = {
                getString(R.string.colif_brothMediaKey),
                getString(R.string.colif_volumeKey),
                getString(R.string.colif_testDescriptionKey)};

        String format = "%s";
        if (Arrays.asList(keys).contains(key)) {
            Preference connectionPref = findPreference(key);
            if (key.equals(getString(R.string.colif_volumeKey))) {
                format = "%s ml";
            }
            connectionPref.setSummary(String.format(format, sharedPreferences.getString(key, "")));
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_turbidity);

        final EditTextPreference sampleIntervalPreference =
                (EditTextPreference) findPreference(getString(R.string.colif_IntervalMinutesKey));
        if (sampleIntervalPreference != null) {

            sampleIntervalPreference.setSummary(String.format("Every %s minutes", sampleIntervalPreference.getText()));

            sampleIntervalPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                try {
                    if (Integer.parseInt(String.valueOf(newValue)) > 360) {
                        newValue = 360;
                    }

                    if (Integer.parseInt(String.valueOf(newValue)) < 1) {
                        newValue = 1;
                    }
                } catch (Exception e) {
                    newValue = 2;
                }

                sampleIntervalPreference.setText(String.valueOf(newValue));
                sampleIntervalPreference.setSummary(String.format("Every %s minutes", newValue));
                return false;
            });
        }

        final EditTextPreference samplesPreference =
                (EditTextPreference) findPreference(getString(R.string.colif_NumberOfSamplesKey));
        if (samplesPreference != null) {
            samplesPreference.setSummary(samplesPreference.getText());

            samplesPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                try {
                    if (Integer.parseInt(String.valueOf(newValue)) > MAX_SAMPLE_NUMBER) {
                        newValue = MAX_SAMPLE_NUMBER;
                    }

                    if (Integer.parseInt(String.valueOf(newValue)) < 1) {
                        newValue = 1;
                    }
                } catch (Exception e) {
                    newValue = 1;
                }
                samplesPreference.setText(String.valueOf(newValue));
                samplesPreference.setSummary(String.valueOf(newValue));
                return false;
            });
        }

        final EditTextPreference brothPreference =
                (EditTextPreference) findPreference(getString(R.string.colif_brothMediaKey));
        if (brothPreference != null) {
            brothPreference.setSummary(brothPreference.getText());
        }

        final EditTextPreference volumePreference =
                (EditTextPreference) findPreference(getString(R.string.colif_volumeKey));

        if (volumePreference != null && volumePreference.getText() != null) {
            volumePreference.setSummary(String.format("%s ml", volumePreference.getText()));
        }

        final EditTextPreference testDescription =
                (EditTextPreference) findPreference(getString(R.string.colif_testDescriptionKey));
        if (testDescription != null) {
            testDescription.setSummary(testDescription.getText());
        }

    }

//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        View rootView = inflater.inflate(R.layout.card_row, container, false);
//
//        final EditTextPreference sampleIntervalPreference =
//                (EditTextPreference) findPreference(getString(R.string.colif_IntervalMinutesKey));
//        if (sampleIntervalPreference != null) {
//
//            sampleIntervalPreference.setSummary(String.format("Every %s minutes", sampleIntervalPreference.getText()));
//
//            sampleIntervalPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
//                @Override
//                public boolean onPreferenceChange(Preference preference, Object newValue) {
//                    try {
//                        if (Integer.parseInt(String.valueOf(newValue)) > 360) {
//                            newValue = 360;
//                        }
//
//                        if (Integer.parseInt(String.valueOf(newValue)) < 1) {
//                            newValue = 1;
//                        }
//                    } catch (Exception e) {
//                        newValue = 2;
//                    }
//
//                    sampleIntervalPreference.setText(String.valueOf(newValue));
//                    sampleIntervalPreference.setSummary(String.format("Every %s minutes", newValue));
//                    return false;
//                }
//            });
//        }
//
//        final EditTextPreference samplesPreference =
//                (EditTextPreference) findPreference(getString(R.string.colif_NumberOfSamplesKey));
//        if (samplesPreference != null) {
//            samplesPreference.setSummary(samplesPreference.getText());
//
//            samplesPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
//                @Override
//                public boolean onPreferenceChange(Preference preference, Object newValue) {
//                    try {
//                        if (Integer.parseInt(String.valueOf(newValue)) > 50) {
//                            newValue = 50;
//                        }
//
//                        if (Integer.parseInt(String.valueOf(newValue)) < 1) {
//                            newValue = 1;
//                        }
//                    } catch (Exception e) {
//                        newValue = 1;
//                    }
//                    samplesPreference.setText(String.valueOf(newValue));
//                    samplesPreference.setSummary(String.valueOf(newValue));
//                    return false;
//                }
//            });
//        }
//
//        final EditTextPreference rgbPreference =
//                (EditTextPreference) findPreference(getString(R.string.turbidityLedRgbKey));
//        if (rgbPreference != null) {
//            rgbPreference.setSummary(rgbPreference.getText());
//        }
//
//        final EditTextPreference testIdPreference =
//                (EditTextPreference) findPreference(getString(R.string.colif_TestIdKey));
//        if (testIdPreference != null) {
//            testIdPreference.setSummary(testIdPreference.getText());
//        }
//
//        final EditTextPreference phoneIdPreference =
//                (EditTextPreference) findPreference(getString(R.string.colif_PhoneIdKey));
//        if (phoneIdPreference != null) {
//            phoneIdPreference.setSummary(phoneIdPreference.getText());
//        }
//
//        final EditTextPreference chamberPreference =
//                (EditTextPreference) findPreference(getString(R.string.colif_chamberVersionKey));
//        if (chamberPreference != null) {
//            chamberPreference.setSummary(chamberPreference.getText());
//        }
//
//        final EditTextPreference brothPreference =
//                (EditTextPreference) findPreference(getString(R.string.colif_brothMediumKey));
//        if (brothPreference != null) {
//            brothPreference.setSummary(brothPreference.getText());
//        }
//
//        final EditTextPreference testDescription =
//                (EditTextPreference) findPreference(getString(R.string.colif_testDescriptionKey));
//        if (testDescription != null) {
//            testDescription.setSummary(testDescription.getText());
//        }
//
//        return rootView;
//    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        list = view.findViewById(android.R.id.list);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ListViewUtil.setListViewHeightBasedOnChildren(list, 0);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }
}
