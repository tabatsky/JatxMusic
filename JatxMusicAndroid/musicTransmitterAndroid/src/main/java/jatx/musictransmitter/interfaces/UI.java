package jatx.musictransmitter.interfaces;

import java.io.File;
import java.util.List;

import jatx.musictransmitter.commons.TrackInfo;

/**
 * Created by jatx on 21.06.17.
 */

public interface UI extends UIController {
    String UPDATE_TRACK_LIST = "jatx.musictransmitter.android.setWifiStatus";

    void updateTrackList(List<TrackInfo> trackList, List<File> fileList);
}
