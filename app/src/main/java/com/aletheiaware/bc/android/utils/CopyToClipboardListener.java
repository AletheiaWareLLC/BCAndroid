/*
 * Copyright 2019 Aletheia Ware LLC
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

package com.aletheiaware.bc.android.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class CopyToClipboardListener implements View.OnClickListener {
    private final TextView view;
    private final String label;

    public CopyToClipboardListener(TextView view, String label) {
        this.view = view;
        this.label = label;
    }

    @Override
    public void onClick(View v) {
        Context context = view.getContext();
        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText(label, view.getText()));
        Toast.makeText(context, label + " copied to clipboard", Toast.LENGTH_SHORT).show();
    }
}
