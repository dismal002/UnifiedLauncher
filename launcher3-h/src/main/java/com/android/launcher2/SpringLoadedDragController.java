package com.android.launcher2;

public class SpringLoadedDragController implements OnAlarmListener {
    final long ENTER_SPRING_LOAD_HOVER_TIME = 1000;
    final long EXIT_SPRING_LOAD_HOVER_TIME = 200;
    Alarm mAlarm;
    boolean mFinishedAnimation = false;
    private Launcher mLauncher;
    private CellLayout mScreen;
    boolean mWaitingToReenter = false;

    public SpringLoadedDragController(Launcher launcher) {
        this.mLauncher = launcher;
        this.mAlarm = new Alarm();
        this.mAlarm.setOnAlarmListener(this);
    }

    public void onDragEnter(CellLayout cl, boolean isSpringLoaded) {
        this.mScreen = cl;
        this.mAlarm.setAlarm(1000);
        this.mFinishedAnimation = isSpringLoaded;
        this.mWaitingToReenter = false;
    }

    public void onEnterSpringLoadedMode(boolean waitToReenter) {
        this.mFinishedAnimation = true;
        this.mWaitingToReenter = waitToReenter;
    }

    public void onDragExit() {
        if (this.mScreen != null) {
            this.mScreen.onDragExit();
        }
        this.mScreen = null;
        if (this.mFinishedAnimation && !this.mWaitingToReenter) {
            this.mAlarm.setAlarm(200);
        }
    }

    public void onAlarm(Alarm alarm) {
        if (this.mScreen != null) {
            this.mLauncher.enterSpringLoadedDragMode(this.mScreen);
        } else {
            this.mLauncher.exitSpringLoadedDragMode();
        }
    }
}
