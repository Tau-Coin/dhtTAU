package io.taucoin.torrent.publishing.core.utils;

import android.text.InputFilter;
import android.text.Spanned;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * EditText抑制输入
 */
public class EditTextInhibitInput implements InputFilter {
    public static final String WELL_REGEX = "^[#]+$";
    private String regex;

    public EditTextInhibitInput(String regex){
        this.regex = regex;
    }
    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        if(StringUtil.isNotEmpty(regex)){
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(source.toString());
            if(matcher.find()){
                return "";
            }
        }
        return null;
    }
}
