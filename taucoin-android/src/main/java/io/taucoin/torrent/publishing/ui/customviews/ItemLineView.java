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

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;

import androidx.databinding.DataBindingUtil;
import io.taucoin.torrent.publishing.R;
import io.taucoin.torrent.publishing.core.utils.StringUtil;
import io.taucoin.torrent.publishing.databinding.ViewItemLineBinding;

public class ItemLineView extends RelativeLayout {
    private String leftText;
    private String rightText;
    private int rightTextColor;
    private int leftImage;
    private ViewItemLineBinding binding;

    public ItemLineView(Context context) {
        this(context, null);
    }

    public ItemLineView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ItemLineView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initData(attrs);

    }

    private void initData(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ItemLineView);
        this.leftImage = a.getResourceId(R.styleable.ItemLineView_leftImage, -1);
        this.leftText = a.getString(R.styleable.ItemLineView_leftText);
        this.rightText = a.getString(R.styleable.ItemLineView_rightText);
        this.rightTextColor = a.getColor(R.styleable.ItemLineView_rightTextColor, getResources().getColor(R.color.color_black));
        a.recycle();
        loadView();
    }

    private void loadView() {
        binding = DataBindingUtil.inflate(LayoutInflater.from(getContext()), R.layout.view_item_line, this, true);
        if(leftImage != -1){
            binding.ivLeft.setImageResource(leftImage);
        }
        if(StringUtil.isNotEmpty(leftText)){
            binding.tvLeft.setText(leftText);
        }
        if(StringUtil.isNotEmpty(rightText)){
            binding.tvRight.setText(rightText);
        }
        binding.tvRight.setTextColor(rightTextColor);
    }

    public void setRightText(CharSequence rightText) {
        this.rightText = rightText.toString();
       if(binding != null){
           binding.tvRight.setText(rightText);
       }
    }
}
