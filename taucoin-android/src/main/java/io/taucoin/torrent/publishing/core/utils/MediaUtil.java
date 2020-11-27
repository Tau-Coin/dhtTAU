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
package io.taucoin.torrent.publishing.core.utils;

import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.language.LanguageConfig;
import com.luck.picture.lib.tools.PictureFileUtils;

import androidx.fragment.app.FragmentActivity;

/**
 *
 * Multimedia related: photo, video, recording, album
 *
 * */
public class MediaUtil {

    public static void startOpenCamera(FragmentActivity activity) {
        PictureSelector.create(activity)
                .openCamera(PictureMimeType.ofImage())
                .setLanguage(LanguageConfig.ENGLISH)
                .loadImageEngine(GlideEngine.createGlideEngine())
                .selectionMode(PictureConfig.SINGLE)
                .isSingleDirectReturn(true)
                .compress(false)
                .previewImage(false)
                .forResult(PictureConfig.REQUEST_CAMERA);
    }

    /**
     * 打开相册
     */
    public static void startOpenGallery(FragmentActivity activity){
        PictureSelector.create(activity)
                .openGallery(PictureMimeType.ofImage())
                .setLanguage(LanguageConfig.ENGLISH)
                .selectionMode(PictureConfig.SINGLE)
                .isSingleDirectReturn(true)
                .isCamera(false)
                .compress(false)
                .loadImageEngine(GlideEngine.createGlideEngine())
                .forResult(PictureConfig.CHOOSE_REQUEST);
    }

    public static void startOpenCameraVideo(FragmentActivity activity) {
        PictureSelector.create(activity)
                .openCamera(PictureMimeType.ofVideo())
                .setLanguage(LanguageConfig.ENGLISH)
                .loadImageEngine(GlideEngine.createGlideEngine())
                .selectionMode(PictureConfig.SINGLE)
                .isSingleDirectReturn(true)
                .previewVideo(false)
                .recordVideoSecond(43200)
                .forResult(PictureConfig.REQUEST_CAMERA);
    }

    public static void libraryClick(FragmentActivity activity) {
        PictureFileUtils.deleteAllCacheDirFile(activity);
        PictureSelector.create(activity)
                .openGallery(PictureMimeType.ofAll())
                .setLanguage(LanguageConfig.ENGLISH)
                .selectionMode(PictureConfig.SINGLE)
                .isSingleDirectReturn(true)
                .isCamera(false)
                .compress(false)
                .loadImageEngine(GlideEngine.createGlideEngine())
                .forResult(PictureConfig.CHOOSE_REQUEST);
    }

    public static void voiceClick(FragmentActivity activity) {
        PictureSelector.create(activity)
                .openGallery(PictureMimeType.ofAudio())
                .setLanguage(LanguageConfig.ENGLISH)
                .selectionMode(PictureConfig.SINGLE)
                .isSingleDirectReturn(true)
                .enablePreviewAudio(false)
                .isSingleDirectReturn(true)
                .loadImageEngine(GlideEngine.createGlideEngine())
                .forResult(PictureConfig.REQUEST_CAMERA);
    }

}