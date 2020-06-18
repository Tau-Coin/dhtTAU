/*
 * Copyright (C) 2018, 2019 Yaroslav Pronin <proninyaroslav@mail.ru>
 *
 * This file is part of LibreTorrent.
 *
 * LibreTorrent is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LibreTorrent is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LibreTorrent.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.taucoin.torrent.publishing.ui.video;


import androidx.annotation.NonNull;
import io.taucoin.torrent.publishing.core.model.data.VideoInfo;

/*
 * Wrapper of TorrentInfo class for TorrentListAdapter, that override Object::equals method
 * Necessary for other behavior in case if item was selected (see SelectionTracker).
 */

public class VideoListItem extends VideoInfo
{
    public VideoListItem(@NonNull VideoInfo state) {
        super(state.videoId, state.name, state.progress,
              state.receivedBytes, state.uploadedBytes, state.totalBytes,
              state.downloadSpeed, state.uploadSpeed, state.ETA, state.dateAdded,
              state.totalPeers, state.peers, state.error, state.sequentialDownload);
    }

    @Override
    public int hashCode()
    {
        return videoId.hashCode();
    }

    /*
     * Compare objects by their content
     */

    public boolean equalsContent(VideoListItem item)
    {
        return super.equals(item);
    }

    /*
     * Compare objects by torrent id
     */

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof VideoListItem))
            return false;

        if (o == this)
            return true;

        return videoId.equals(((VideoListItem)o).videoId);
    }
}
