package com.donut.launcher;

/** Static holder for the Donut launcher's model. */
public class DonutState {
    private static LauncherModel sModel;

    public static void init(LauncherModel model) { sModel = model; }
    public static LauncherModel getModel() { return sModel; }
}
