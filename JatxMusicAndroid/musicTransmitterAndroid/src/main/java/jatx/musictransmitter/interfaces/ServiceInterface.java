package jatx.musictransmitter.interfaces;

import java.io.File;
import java.util.List;

/**
 * Created by jatx on 21.06.17.
 */

public interface ServiceInterface {
    String TP_SET_POSITION = "jatx.musictransmitter.android.tpSetPosition";
    String TP_PLAY = "jatx.musictransmitter.android.tpPlay";
    String TP_PAUSE = "jatx.musictransmitter.android.tpPause";
    String TP_SET_FILE_LIST = "jatx.musictransmitter.android.tpSetFileList";
    String TC_PLAY = "jatx.musictransmitter.android.tcPlay";
    String TC_PAUSE = "jatx.musictransmitter.android.tpPause";
    String TC_SET_VOLUME = "jatx.musictransmitter.android.tcSetVolume";

    void tpSetPosition(int position);
    void tpPlay();
    void tpPause();
    void tpSetFileList(List<File> fileList);
    void tcPlay();
    void tcPause();
    void tcSetVolume(int volume);
}
