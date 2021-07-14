/**
 * Copyright 2018 Taucoin Core Developers.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.taucoin.torrent.publishing.ui.customviews;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.StringUtil;

public class CommonDialog extends Dialog {

    public CommonDialog(Context context) {
        super(context);
    }

    public CommonDialog(Context context, int theme) {
        super(context, theme);
    }

    public static class Builder {
        private Context context;
        private String positiveButtonText;
        private String negativeButtonText;
        private boolean isCanCancel = true;
        private boolean isEnabledPositive = true;
        private boolean enableWarpWidth = false;
        private boolean isEnabledNegative = true;
        private boolean isExchange = false;
        private int btnWidth;
        private int positiveBgResource = -1;
        private int negativeBgResource = -1;
        private boolean horizontal = false;
        private OnClickListener positiveButtonClickListener;
        private OnClickListener negativeButtonClickListener;
        private View mContentView;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder setPositiveButton(int positiveButtonText,
                                         OnClickListener listener) {
            this.positiveButtonText = (String) context
                    .getText(positiveButtonText);
            this.positiveButtonClickListener = listener;
            return this;
        }

        public Builder setPositiveButton(OnClickListener listener) {
            this.positiveButtonClickListener = listener;
            return this;
        }

        public Builder setPositiveButton(String positiveButtonText,
                                         OnClickListener listener) {
            this.positiveButtonText = positiveButtonText;
            this.positiveButtonClickListener = listener;
            return this;
        }

        public Builder setButtonWidth(int dpWidth) {
            this.btnWidth = context.getResources().getDimensionPixelSize(dpWidth);
            return this;
        }

        public Builder setExchange(boolean isExchange) {
            this.isExchange = isExchange;
            return this;
        }

        public Builder isEnabledPositive(boolean isEnabled) {
            this.isEnabledNegative = isEnabled;
            return this;
        }

        public Builder isEnabledNegative(boolean isEnabled) {
            this.isEnabledNegative = isEnabled;
            return this;
        }

        public Builder setCanceledOnTouchOutside(boolean cancel) {
            this.isCanCancel = cancel;
            return this;
        }

        public Builder setContentView(View view) {
            this.mContentView = view;
            return this;
        }

        public Builder setNegativeButton(int negativeButtonText,
                                         OnClickListener listener) {
            this.negativeButtonText = (String) context
                    .getText(negativeButtonText);
            this.negativeButtonClickListener = listener;
            return this;
        }

        public Builder setNegativeButton(String negativeButtonText,
                                         OnClickListener listener) {
            this.negativeButtonText = negativeButtonText;
            this.negativeButtonClickListener = listener;
            return this;
        }
        public Builder setHorizontal() {
            this.horizontal = true;
            return this;
        }
        public Builder setPositiveBgResource(int positiveBgResource) {
            this.positiveBgResource = positiveBgResource;
            return this;
        }

        public Builder setNegativeBgResource(int negativeBgResource) {
            this.negativeBgResource = negativeBgResource;
            return this;
        }

        public Builder enableWarpWidth(boolean enableWarpWidth) {
            this.enableWarpWidth = enableWarpWidth;
            return this;
        }

        public CommonDialog create() {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final CommonDialog dialog = new CommonDialog(context, R.style.CommonDialog);
            ViewGroup layout;
            if(horizontal){
                layout = (ViewGroup) inflater.inflate(R.layout.dialog_common_layout_hor, null);
            }else{
                layout = (ViewGroup) inflater.inflate(R.layout.dialog_common_layout, null);
            }
            dialog.addContentView(layout, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            if(mContentView != null){
                layout.addView(mContentView, 0);
            }
            resetDialogWidth(layout, dialog, enableWarpWidth);
            ViewHolder viewHolder = new ViewHolder(layout);

            viewHolder.positiveButton.setEnabled(isEnabledPositive);
            viewHolder.negativeButton.setEnabled(isEnabledNegative);

            if(positiveBgResource != -1){
                viewHolder.positiveButton.setBackgroundResource(positiveBgResource);
            }
            if(negativeBgResource != -1){
                viewHolder.negativeButton.setBackgroundResource(negativeBgResource);
            }

            if(!isEnabledPositive){
                viewHolder.positiveButton.setBackgroundResource(R.drawable.grey_rect_round_bg);
            }else{
                if(isExchange){
                    viewHolder.positiveButton.setBackgroundResource(R.drawable.red_rect_round_bg);
                }
            }
            if(!isEnabledNegative){
                viewHolder.negativeButton.setBackgroundResource(R.drawable.grey_rect_round_bg);
            }else{
                if(isExchange){
                    viewHolder.negativeButton.setBackgroundResource(R.drawable.primary_rect_round_bg);
                }
            }
            if(isExchange){
                viewHolder.negativeButton.setText(positiveButtonText);
                viewHolder.positiveButton.setText(negativeButtonText);
            }else{
                viewHolder.negativeButton.setText(negativeButtonText);
                viewHolder.positiveButton.setText(positiveButtonText);
            }

            if (StringUtil.isEmpty(positiveButtonText)) {
                viewHolder.positiveButton.setVisibility(View.GONE);
            }
            if (StringUtil.isEmpty(negativeButtonText)) {
                viewHolder.negativeButton.setVisibility(View.GONE);
            }

            if (positiveButtonClickListener != null) {
                viewHolder.positiveButton.setOnClickListener(v -> positiveButtonClickListener.onClick(dialog, DialogInterface.BUTTON_POSITIVE));
                if(isExchange){
                    viewHolder.positiveButton.setOnClickListener(v -> negativeButtonClickListener.onClick(dialog, DialogInterface.BUTTON_POSITIVE));
                }
            }
            if (negativeButtonClickListener != null) {
                viewHolder.negativeButton.setOnClickListener(v -> negativeButtonClickListener.onClick(dialog, DialogInterface.BUTTON_NEGATIVE));
                if(isExchange){
                    viewHolder.negativeButton.setOnClickListener(v -> positiveButtonClickListener.onClick(dialog, DialogInterface.BUTTON_NEGATIVE));
                }
            }

            if(btnWidth > 0){
                viewHolder.negativeButton.setWidth(btnWidth);
                viewHolder.positiveButton.setWidth(btnWidth);
            }
            dialog.setCanceledOnTouchOutside(isCanCancel);
            return dialog;
        }

        private void resetDialogWidth(ViewGroup layout, CommonDialog dialog, boolean enableWarpWidth) {
            WindowManager windowManager = (WindowManager) context
                    .getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();

            if(enableWarpWidth){
                int padding = layout.getContext().getResources().getDimensionPixelSize(R.dimen.widget_size_10);
                layout.setPadding(padding, padding, padding, padding);
                Window dialogWindow = dialog.getWindow();
                if(dialogWindow != null){
                    WindowManager.LayoutParams lp = dialogWindow.getAttributes();
                    lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
                    dialogWindow.setAttributes(lp);
                }
            }else {
                layout.setMinimumWidth((int) (display.getWidth() * 0.85));
            }
        }

        static class ViewHolder {
            TextView positiveButton;
            TextView negativeButton;

            ViewHolder(View view) {
                positiveButton = view.findViewById(R.id.positiveButton);
                negativeButton = view.findViewById(R.id.negativeButton);
            }
        }
    }

    public void closeDialog(){
        dismiss();
    }
}