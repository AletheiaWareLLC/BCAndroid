/*
 * Copyright 2018 Aletheia Ware LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aletheiaware.bc.android.ui;

import android.app.Activity;
import androidx.appcompat.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.EditText;

import com.aletheiaware.bc.android.R;
import com.aletheiaware.common.android.utils.CommonAndroidUtils;

public abstract class ImportKeysDialog {

    private final Activity activity;
    private AlertDialog dialog;

    public ImportKeysDialog(Activity activity) {
        this.activity = activity;
    }

    public void create() {
        View importView = View.inflate(activity, R.layout.dialog_import, null);
        final EditText aliasText = importView.findViewById(R.id.import_alias_text);
        aliasText.setFocusable(true);
        aliasText.setFocusableInTouchMode(true);
        final EditText accessCodeText = importView.findViewById(R.id.import_access_code_text);
        accessCodeText.setFocusableInTouchMode(true);
        AlertDialog.Builder ab = new AlertDialog.Builder(activity, R.style.AlertDialogTheme);
        ab.setTitle(R.string.import_keys);
        ab.setView(importView);
        ab.setPositiveButton(R.string.import_keys_action, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                String alias = aliasText.getText().toString();
                // TODO this is bad error handling - dialog will still be dismissed
                if (alias.isEmpty()) {
                    CommonAndroidUtils.showErrorDialog(activity, R.style.AlertDialogTheme, activity.getString(R.string.error_alias_invalid));
                    return;
                }
                String accessCode = accessCodeText.getText().toString();
                if (accessCode.isEmpty()) {
                    CommonAndroidUtils.showErrorDialog(activity, R.style.AlertDialogTheme, activity.getString(R.string.error_access_code_invalid));
                    return;
                }
                onImport(dialog, alias, accessCode);
            }
        });
        dialog = ab.show();
    }

    public abstract void onImport(DialogInterface dialog, String alias, String accessCode);

    public AlertDialog getDialog() {
        return dialog;
    }
}