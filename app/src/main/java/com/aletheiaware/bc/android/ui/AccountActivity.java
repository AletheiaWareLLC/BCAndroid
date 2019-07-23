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

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.aletheiaware.bc.Cache;
import com.aletheiaware.bc.Crypto;
import com.aletheiaware.bc.Network;
import com.aletheiaware.bc.android.R;
import com.aletheiaware.bc.android.utils.BCAndroidUtils;
import com.aletheiaware.bc.android.utils.CopyToClipboardListener;
import com.aletheiaware.bc.utils.BCUtils;
import com.aletheiaware.common.android.utils.CommonAndroidUtils;
import com.aletheiaware.finance.utils.FinanceUtils;
import com.stripe.android.model.Token;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class AccountActivity extends AppCompatActivity {

    private TextView aliasText;
    private TextView publicKeyText;
    private Button exportButton;
    private Button switchButton;
    private Button deleteButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup UI
        setContentView(R.layout.activity_account);

        aliasText = findViewById(R.id.account_alias_text);
        aliasText.setOnClickListener(new CopyToClipboardListener(aliasText, "Alias"));
        publicKeyText = findViewById(R.id.account_public_key_text);
        publicKeyText.setOnClickListener(new CopyToClipboardListener(publicKeyText, "Public Key"));
        // TODO add button to backup keys, either by printing, or emailing.
        // TODO add button to manage payment sources
        exportButton = findViewById(R.id.account_export_button);
        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String alias = BCAndroidUtils.getAlias();
                new PasswordUnlockDialog(AccountActivity.this, alias) {
                    @Override
                    public void onUnlock(DialogInterface dialog, final char[] password) {
                        dialog.dismiss();
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    String website = BCAndroidUtils.getBCWebsite();
                                    KeyPair keys = BCAndroidUtils.getKeyPair();
                                    final byte[] accessCode = Crypto.generateSecretKey(Crypto.AES_KEY_SIZE_BYTES);
                                    Crypto.exportKeyPair(website, getFilesDir(), alias, password, keys, accessCode);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            new ExportKeysDialog(AccountActivity.this, alias, Base64.encodeToString(accessCode, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING)) {
                                            }.create();
                                        }
                                    });
                                } catch (BadPaddingException | IOException | IllegalBlockSizeException | InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e) {
                                    CommonAndroidUtils.showErrorDialog(AccountActivity.this, R.style.AlertDialogTheme, R.string.error_export_key_pair, e);
                                }
                            }
                        }.start();
                    }
                }.create();
            }
        });
        switchButton = findViewById(R.id.account_switch_button);
        switchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BCAndroidUtils.initialize(null, null, null);
                setResult(RESULT_OK);
                finish();
            }
        });
        deleteButton = findViewById(R.id.account_delete_button);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DeleteKeysDialog(AccountActivity.this) {
                    @Override
                    public void onDelete(DialogInterface dialog) {
                        if (Crypto.deleteRSAKeyPair(getFilesDir(), BCAndroidUtils.getAlias())) {
                            dialog.dismiss();
                            BCAndroidUtils.initialize(null, null, null);
                            setResult(RESULT_OK);
                            finish();
                        }
                    }
                }.create();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (BCAndroidUtils.isInitialized()) {
            final String alias = BCAndroidUtils.getAlias();
            aliasText.setText(alias);
            final KeyPair keys = BCAndroidUtils.getKeyPair();
            final PublicKey key = keys.getPublic();
            publicKeyText.setText(Base64.encodeToString(key.getEncoded(), Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP));
        } else {
            Intent intent = new Intent(this, AccessActivity.class);
            startActivityForResult(intent, BCAndroidUtils.ACCESS_ACTIVITY);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case BCAndroidUtils.ACCESS_ACTIVITY:
                switch (resultCode) {
                    case RESULT_OK:
                        break;
                    case RESULT_CANCELED:
                        setResult(RESULT_CANCELED);
                        finish();
                        break;
                    default:
                        break;
                }
                break;
        }
    }

}
