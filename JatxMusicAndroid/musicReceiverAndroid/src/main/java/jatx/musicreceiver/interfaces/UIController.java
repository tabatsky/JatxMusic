package jatx.musicreceiver.interfaces;

/**
 * Created by jatx on 21.06.17.
 */

public interface UIController {
    String START_JOB = "jatx.musicreceiver.android.uiStartJob";
    String STOP_JOB = "jatx.musicreceiver.android.uiStopJob";

    void uiStartJob();
    void uiStopJob();
}
