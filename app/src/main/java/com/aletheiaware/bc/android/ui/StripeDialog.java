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
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.aletheiaware.bc.android.R;
import com.aletheiaware.bc.android.utils.BCAndroidUtils;
import com.stripe.android.Stripe;
import com.stripe.android.TokenCallback;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;
import com.stripe.android.view.CardInputWidget;

public abstract class StripeDialog {

    private final Activity activity;
    private final String description;
    private final String amount;
    private AlertDialog dialog;

    public StripeDialog(Activity activity, String description, String amount) {
        this.activity = activity;
        this.description = description;
        this.amount = amount;
    }

    public void create() {
        View stripeView = View.inflate(activity, R.layout.dialog_stripe, null);
        final CheckBox termsCheck = stripeView.findViewById(R.id.stripe_terms_of_service_check);
        final CheckBox policyCheck = stripeView.findViewById(R.id.stripe_privacy_policy_check);
        TextView legaleseBetaLabel = stripeView.findViewById(R.id.stripe_legalese_beta_label);
        legaleseBetaLabel.setMovementMethod(LinkMovementMethod.getInstance());
        final CheckBox betaCheck = stripeView.findViewById(R.id.stripe_beta_test_agreement_check);
        TextView descriptionLabel = stripeView.findViewById(R.id.stripe_description_label);
        TextView descriptionText = stripeView.findViewById(R.id.stripe_description_text);
        if (description != null && !description.isEmpty()) {
            descriptionLabel.setVisibility(View.VISIBLE);
            descriptionText.setVisibility(View.VISIBLE);
            descriptionText.setText(description);
        } else {
            descriptionLabel.setVisibility(View.GONE);
            descriptionText.setVisibility(View.GONE);
        }
        TextView amountLabel = stripeView.findViewById(R.id.stripe_amount_label);
        TextView amountText = stripeView.findViewById(R.id.stripe_amount_text);
        if (amount != null && !amount.isEmpty()) {
            amountLabel.setVisibility(View.VISIBLE);
            amountText.setVisibility(View.VISIBLE);
            amountText.setText(amount);
        } else {
            amountLabel.setVisibility(View.GONE);
            amountText.setVisibility(View.GONE);
        }
        final EditText emailText = stripeView.findViewById(R.id.stripe_email_text);
        emailText.setFocusable(true);
        emailText.setFocusableInTouchMode(true);
        final CardInputWidget cardWidget = stripeView.findViewById(R.id.stripe_card_widget);
        TextView legaleseLabel = stripeView.findViewById(R.id.stripe_legalese_label);
        legaleseLabel.setMovementMethod(LinkMovementMethod.getInstance());
        AlertDialog.Builder ab = new AlertDialog.Builder(activity, R.style.AlertDialogTheme);
        ab.setTitle(R.string.title_dialog_stripe);
        ab.setView(stripeView);
        ab.setPositiveButton(R.string.stripe_action, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
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

                // Email
                final String email = emailText.getText().toString();
                // TODO ensure email is valid
                if (email.isEmpty()) {
                    BCAndroidUtils.showErrorDialog(activity, activity.getString(R.string.error_invalid_email));
                    return;
                }

                // TODO mine terms of service agreement into blockchain
                // TODO mine privacy policy agreement into blockchain
                // TODO mine beta test agreement into blockchain

                Card card = cardWidget.getCard();
                if (card != null) {
                    card.setName(email);
                    Stripe stripe = new Stripe(activity, activity.getString(R.string.stripe_publishable_key));
                    stripe.createToken(card, new TokenCallback() {
                        @Override
                        public void onError(Exception error) {
                            BCAndroidUtils.showErrorDialog(activity, R.string.error_stripe_invalid_payment, error);
                        }

                        @Override
                        public void onSuccess(Token token) {
                            onSubmit(email, token);
                        }
                    });
                }
            }
        });
        dialog = ab.show();
    }

    public AlertDialog getDialog() {
        return dialog;
    }

    public abstract void onSubmit(String email, Token token);

}
