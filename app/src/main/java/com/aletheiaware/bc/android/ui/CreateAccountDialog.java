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
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.aletheiaware.alias.utils.AliasUtils;
import com.aletheiaware.bc.BC.Channel;
import com.aletheiaware.bc.android.R;
import com.aletheiaware.bc.android.utils.BCAndroidUtils;
import com.aletheiaware.bc.utils.BCUtils;
import com.stripe.android.Stripe;
import com.stripe.android.TokenCallback;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;
import com.stripe.android.view.CardInputWidget;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public abstract class CreateAccountDialog {

    private final Activity activity;
    private AlertDialog createAccountDialog;

    private ProgressBar progressBar;
    private AlertDialog progressDialog;

    private Channel aliases;

    protected CreateAccountDialog(final Activity activity) {
        this.activity = activity;

        new Thread() {
            @Override
            public void run() {
                aliases = new Channel(AliasUtils.ALIAS_CHANNEL, BCUtils.THRESHOLD_STANDARD, activity.getCacheDir(), BCAndroidUtils.getBCHost());
                try {
                    aliases.sync();
                } catch (IOException | NoSuchAlgorithmException e) {
                    /* Ignored */
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void create() {
        View createAccountView = View.inflate(activity, R.layout.dialog_create_account, null);
        final EditText aliasText = createAccountView.findViewById(R.id.create_account_alias_text);
        aliasText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                final String alias = s.toString();
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            final boolean unique = AliasUtils.isUnique(aliases, alias);
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (unique) {
                                        aliasText.setError(null);
                                    } else {
                                        aliasText.setError(activity.getString(R.string.error_alias_taken));
                                    }
                                }
                            });
                        } catch (IOException e) {
                            BCAndroidUtils.showErrorDialog(activity, R.string.error_read_alias_failed, e);
                        }
                    }
                }.start();
            }
        });
        final EditText emailText = createAccountView.findViewById(R.id.create_account_email_text);
        final EditText newPasswordText = createAccountView.findViewById(R.id.create_account_new_password_text);
        final EditText confirmPasswordText = createAccountView.findViewById(R.id.create_account_confirm_password_text);
        final CardInputWidget cardWidget = createAccountView.findViewById(R.id.create_account_card_widget);
        TextView legaleseLabel = createAccountView.findViewById(R.id.create_account_legalese_label);
        legaleseLabel.setMovementMethod(LinkMovementMethod.getInstance());
        final CheckBox termsCheck = createAccountView.findViewById(R.id.create_account_terms_of_service_check);
        final CheckBox policyCheck = createAccountView.findViewById(R.id.create_account_privacy_policy_check);
        TextView legaleseBetaLabel = createAccountView.findViewById(R.id.create_account_legalese_beta_label);
        legaleseBetaLabel.setMovementMethod(LinkMovementMethod.getInstance());
        final CheckBox betaCheck = createAccountView.findViewById(R.id.create_account_beta_test_agreement_check);
        AlertDialog.Builder ab = new AlertDialog.Builder(activity, R.style.AlertDialogTheme);
        ab.setTitle(R.string.title_dialog_create_account);
        ab.setView(createAccountView);
        ab.setPositiveButton(R.string.create_account_action, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                // Alias
                final String alias = aliasText.getText().toString();
                // TODO ensure alias is valid
                if (alias.isEmpty()) {
                    BCAndroidUtils.showErrorDialog(activity, activity.getString(R.string.error_alias_invalid));
                    return;
                }

                // Email
                final String email = emailText.getText().toString();
                // TODO ensure email is valid
                if (email.isEmpty()) {
                    BCAndroidUtils.showErrorDialog(activity, activity.getString(R.string.error_email_invalid));
                    return;
                }

                // Password
                // TODO ensure password meets minimum security
                final int passwordLength = newPasswordText.length();
                if (passwordLength < 12) {
                    BCAndroidUtils.showErrorDialog(activity, activity.getString(R.string.error_password_short));
                    return;
                }
                if (passwordLength != confirmPasswordText.length()) {
                    BCAndroidUtils.showErrorDialog(activity, activity.getString(R.string.error_password_lengths_differ));
                    return;
                }

                final char[] newPassword = new char[passwordLength];
                final char[] confirmPassword = new char[passwordLength];
                newPasswordText.getText().getChars(0, passwordLength, newPassword, 0);
                confirmPasswordText.getText().getChars(0, passwordLength, confirmPassword, 0);
                if (!Arrays.equals(newPassword, confirmPassword)) {
                    BCAndroidUtils.showErrorDialog(activity, activity.getString(R.string.error_passwords_differ));
                    return;
                }

                // Payment
                final Card card = cardWidget.getCard();
                if (card == null || !card.validateCard()) {
                    BCAndroidUtils.showErrorDialog(activity, activity.getString(R.string.error_stripe_invalid_payment));
                    return;
                }

                // Legal
                if (!termsCheck.isChecked()) {
                    BCAndroidUtils.showErrorDialog(activity, activity.getString(R.string.error_terms_of_service_required));
                    return;
                }
                if (!policyCheck.isChecked()) {
                    BCAndroidUtils.showErrorDialog(activity, activity.getString(R.string.error_privacy_policy_required));
                    return;
                }
                if (!betaCheck.isChecked()) {
                    BCAndroidUtils.showErrorDialog(activity, activity.getString(R.string.error_beta_test_agreement_required));
                    return;
                }

                View progressView = View.inflate(activity, R.layout.dialog_progress, null);
                progressBar = progressView.findViewById(R.id.progress);
                progressDialog = new AlertDialog.Builder(activity, R.style.AlertDialogTheme)
                        .setTitle(R.string.title_dialog_creating_account)
                        .setCancelable(false)
                        .setView(progressBar)
                        .show();
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            setProgressBar(1);
                            final KeyPair keyPair = BCUtils.createRSAKeyPair(activity.getFilesDir(), alias, newPassword);
                            setProgressBar(2);
                            AliasUtils.registerAlias(BCAndroidUtils.getBCWebsite(), alias, keyPair);
                            setProgressBar(3);
                            // TODO mine terms of service agreement into blockchain
                            setProgressBar(4);
                            // TODO mine privacy policy agreement into blockchain
                            setProgressBar(5);
                            // TODO mine beta test agreement into blockchain
                            Stripe stripe = new Stripe(activity, activity.getString(R.string.stripe_publishable_key));
                            setProgressBar(6);
                            stripe.createToken(card, new TokenCallback() {
                                @Override
                                public void onError(Exception error) {
                                    BCAndroidUtils.showErrorDialog(activity, R.string.error_create_account, error);
                                }

                                @Override
                                public void onSuccess(Token token) {
                                    String website = activity.getString(R.string.register_account_address);
                                    String customerId = null;
                                    try {
                                        customerId = BCUtils.register(website, alias, email, token.getId());
                                    } catch (IOException e) {
                                        BCAndroidUtils.showErrorDialog(activity, R.string.error_registering, e);
                                    }
                                    Log.d(BCUtils.TAG, "Customer ID: " + customerId);
                                }
                            });
                            setProgressBar(7);
                            BCAndroidUtils.initialize(alias, keyPair);
                            setProgressBar(8);
                            // TODO show user the generated key pair, explain public vs private key, and provide options to backup keys
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (createAccountDialog != null && createAccountDialog.isShowing()) {
                                        createAccountDialog.dismiss();
                                    }
                                }
                            });
                        } catch (BadPaddingException | IOException | IllegalBlockSizeException | InvalidAlgorithmParameterException | InvalidKeyException | InvalidKeySpecException | InvalidParameterSpecException | NoSuchAlgorithmException | NoSuchPaddingException | SignatureException e) {
                            BCAndroidUtils.showErrorDialog(activity, R.string.error_create_account, e);
                        } finally {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (progressDialog != null && progressDialog.isShowing()) {
                                        progressDialog.dismiss();
                                    }
                                }
                            });
                        }
                    }
                }.start();
            }
        });
        createAccountDialog = ab.show();
        aliasText.requestFocus();
    }

    public AlertDialog getDialog() {
        return createAccountDialog;
    }

    private void setProgressBar(final int v) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setProgress(v);
            }
        });
    }
}
