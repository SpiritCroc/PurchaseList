/*
 * Copyright (C) 2017 SpiritCroc
 * Email: spiritcroc@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.spiritcroc.remotepurchaselist.preference;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import de.spiritcroc.remotepurchaselist.R;

public class SimpleDateFormatPreference extends DialogPreference {

    private static final String DEFAULT_VALUE = "";

    private String mEmptyReplacementValue = null;

    private String mCurrentValue = DEFAULT_VALUE;

    private View mDialogView;

    /*
    public SimpleDateFormatPreference(Context context) {
        super(context);
        init(null);
    }
    */

    public SimpleDateFormatPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public SimpleDateFormatPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    /*
    public SimpleDateFormatPreference(Context context, AttributeSet attrs, int defStyleAttr,
                                      int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }
    */

    private void init(AttributeSet attrs) {
        setDialogLayoutResource(R.layout.sdfp_dialog);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        setDialogIcon(null);
        if (attrs != null) {
            final TypedArray ta = getContext().obtainStyledAttributes(attrs,
                    R.styleable.SimpleDateFormatPreference);
            int id = ta.getResourceId(
                    R.styleable.SimpleDateFormatPreference_emptyReplacementValue, 0);
            if (id > 0) {
                mEmptyReplacementValue = getContext().getString(id);
            }
            ta.recycle();
        }
        setPreviewToSummary();
    }

    @Override
    protected View onCreateDialogView() {
        mDialogView = super.onCreateDialogView();
        setupDialogView();

        return mDialogView;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        EditText editText = (EditText) mDialogView.findViewById(R.id.preference_edit);
        if (positiveResult) {
            mCurrentValue = editText.getText().toString();
            persistString(mCurrentValue);
        }
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            // Restore existing state
            mCurrentValue = this.getPersistedString(DEFAULT_VALUE);
        } else {
            // Set default state from the XML attribute
            mCurrentValue = (String) defaultValue;
            persistString(mCurrentValue);
        }
        setPreviewToSummary();
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        String result = a.getString(index);
        return result == null ? DEFAULT_VALUE : result;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        // Check whether this Preference is persistent (continually saved)
        if (isPersistent()) {
            // No need to save instance state since it's persistent,
            // use superclass state
            return superState;
        }

        final SavedState state = new SavedState(superState);
        state.value = mCurrentValue;
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save the state, so call superclass
            super.onRestoreInstanceState(state);
            return;
        }

        // Cast state to custom BaseSavedState and pass to superclass
        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
    }

    @Override
    protected boolean persistString(String value) {
        if (super.persistString(value)) {
            setPreviewToSummary();
            return true;
        } else {
            return false;
        }
    }

    public void setEmptyReplacementValue(String replacementValue) {
        mEmptyReplacementValue = replacementValue;
        setPreviewToSummary();
    }

    public String getEmptyReplacementValue() {
        return mEmptyReplacementValue;
    }

    private View setupDialogView() {
        final TextView previewView = (TextView) mDialogView.findViewById(R.id.preference_preview);
        final EditText editView = (EditText) mDialogView.findViewById(R.id.preference_edit);
        View addView = mDialogView.findViewById(R.id.preference_add);
        final View clearView = mDialogView.findViewById(R.id.preference_clear);

        editView.setText(mCurrentValue);
        editView.setSelection(mCurrentValue.length());
        if (mEmptyReplacementValue != null) {
            editView.setHint(mEmptyReplacementValue);
        }
        setPreview(previewView, mCurrentValue);

        // Show keyboard on dialog open (editText gets focus automatically)
        editView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) {
                    getDialog().getWindow().setSoftInputMode(
                            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });

        addView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSuggestions(view, editView);
            }
        });
        clearView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editView.setText("");
            }
        });
        clearView.setVisibility(TextUtils.isEmpty(mCurrentValue) ? View.GONE : View.VISIBLE);

        editView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                clearView.setVisibility(TextUtils.isEmpty(charSequence) ? View.GONE : View.VISIBLE);
                setPreview(previewView, charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        return mDialogView;
    }

    private void setPreview(TextView previewView, String formatString) {
        try {
            String preview = getPreview(formatString);
            previewView.setText(TextUtils.isEmpty(preview) ? ""
                    : getContext().getString(R.string.sdfp_preview, preview));
        } catch (IllegalArgumentException e) {
            previewView.setText(e.getLocalizedMessage());
        }
    }

    private void setPreviewToSummary() {
        try {
            if (mEmptyReplacementValue == null) {
                setSummary(getPreview(mCurrentValue));
            } else {
                setSummary(getContext().getString("".equals(mCurrentValue)
                        ? R.string.sdfp_summary_default
                        : R.string.sdfp_summary_custom,
                        getPreview(mCurrentValue)));
            }
        } catch (IllegalArgumentException e) {
            setSummary(R.string.sdfp_summary_invalid_pattern);
        }
    }

    /**
     * @throws IllegalArgumentException
     * From SimpleDateFormat: invalid format
     */
    private String getPreview(String formatString) throws IllegalArgumentException {
        if (mEmptyReplacementValue != null && TextUtils.isEmpty(formatString)) {
            formatString = mEmptyReplacementValue;
        }
        return new SimpleDateFormat(formatString).format(new Date());
    }

    private void showSuggestions(View popupAnchor, final EditText editText) {
        PopupMenu popupMenu = new PopupMenu(getContext(), popupAnchor);
        final Menu menu = popupMenu.getMenu();
        Resources res = getContext().getResources();
        final String[] entries = res.getStringArray(R.array.sdfp_patterns_entries);
        final String[] values = res.getStringArray(R.array.sdfp_patterns_values);
        final HashMap<String, String> valueMap = new HashMap<>();
        for (int i = 0; i < entries.length; i++) {
            menu.add(entries[i]);
            valueMap.put(entries[i], values[i]);
        }
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                String patternName = menuItem.getTitle().toString();
                String currentInput = editText.getText().toString();
                String text = currentInput.substring(0, editText.getSelectionStart());
                // Add a separator to the previous pattern if not already there
                for (String pattern: values) {
                    if (text.endsWith(pattern)) {
                        text += getContext().getString(R.string.sdfp_pattern_separator);
                        break;
                    }
                }
                text += valueMap.get(patternName);
                int newSelection = text.length();
                text += currentInput.substring(editText.getSelectionEnd());
                editText.setText(text);
                editText.setSelection(newSelection);
                return false;
            }
        });
        popupMenu.show();
    }

    private static class SavedState extends BaseSavedState {
        // Member that holds the setting's value
        // Change this data type to match the type saved by your Preference
        String value;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);
            value = source.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(value);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {

                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
