/*
 * Copyright (C) 2017-2019 The Dirty Unicorns Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nusantara.wings.fragments.lockscreen;

import android.content.Context;
import android.content.ContentResolver;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.widget.LockPatternUtils;

import com.android.internal.util.nad.NadUtils;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;

import com.nusantara.support.preferences.SystemSettingSwitchPreference;
import com.nusantara.support.preferences.SecureSettingSwitchPreference;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class FingerprintPrefs extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String FP_KEYSTORE = "fp_unlock_keystore";
    private static final String FOD_ANIMATION_CATEGORY = "fod_animations";
    private static final String FOD_ICON = "fod_icon";
    private static final String FINGERPRINT_POWER_BUTTON_PRESS = "fingerprint_power_button_press";

    private SystemSettingSwitchPreference mFingerprintUnlock;
    private SecureSettingSwitchPreference mFingerprintPowerButtonPress;
    private PreferenceCategory mFodIcon;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.nad_fingerprint_prefs);
        final ContentResolver resolver = getActivity().getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();

        mFingerprintUnlock = (SystemSettingSwitchPreference) findPreference(FP_KEYSTORE);

        if (mFingerprintUnlock != null) {
           if (LockPatternUtils.isDeviceEncryptionEnabled()) {
               mFingerprintUnlock.setEnabled(false);
               mFingerprintUnlock.setSummary(R.string.fp_encrypt_warning);
            } else {
               mFingerprintUnlock.setEnabled(true);
               mFingerprintUnlock.setSummary(R.string.fp_unlock_keystore_summary);
            }
        }


        mFodIcon = findPreference(FOD_ICON);
        if (mFodIcon != null
                && !getResources().getBoolean(com.android.internal.R.bool.config_supportsInDisplayFingerprint)) {
            prefScreen.removePreference(mFodIcon);
        }

        final PreferenceCategory fodCat = (PreferenceCategory) prefScreen
                .findPreference(FOD_ANIMATION_CATEGORY);
        final boolean isFodAnimationResources = NadUtils.isPackageInstalled(getContext(),
                      getResources().getString(com.android.internal.R.string.config_fodAnimationPackage));
        if (!isFodAnimationResources) {
            prefScreen.removePreference(fodCat);
        }

        mFingerprintPowerButtonPress = (SecureSettingSwitchPreference) findPreference(FINGERPRINT_POWER_BUTTON_PRESS);
        mFingerprintPowerButtonPress.setChecked((Settings.Secure.getIntForUser(resolver,
                Settings.Secure.FINGERPRINT_POWER_BUTTON_PRESS, 0, UserHandle.USER_CURRENT) == 1));
        mFingerprintPowerButtonPress.setOnPreferenceChangeListener(this);

        if (mFingerprintPowerButtonPress != null
                && !getResources().getBoolean(com.android.internal.R.bool.config_powerButtonFingerprint)) {
            mFingerprintPowerButtonPress.setVisible(false);
        }
        updateFpSummary();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mFingerprintPowerButtonPress) {
            boolean value = (Boolean) newValue;
            Settings.Secure.putIntForUser(getContext().getContentResolver(),
                    FINGERPRINT_POWER_BUTTON_PRESS, value ? 1 : 0, UserHandle.USER_CURRENT);
            updateFpSummary();
            return true;
        }
        return false;
    }

    private void updateFpSummary() {
        boolean fp = Settings.Secure.getIntForUser(getContentResolver(),
                Settings.Secure.FINGERPRINT_POWER_BUTTON_PRESS, 0,
                UserHandle.USER_CURRENT) == 1;
        if (!fp) {
            mFingerprintPowerButtonPress.setSummary(R.string.fingerprint_power_button_press_off_summary);
        } else {
            mFingerprintPowerButtonPress.setSummary(R.string.fingerprint_power_button_press_on_summary);
        }
    }


    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.NUSANTARA_PRJ;
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.nad_fingerprint_prefs;
                    result.add(sir);
                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    return keys;
        }
    };
}
