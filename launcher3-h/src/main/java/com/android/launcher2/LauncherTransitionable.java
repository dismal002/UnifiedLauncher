package com.android.launcher2;

import android.animation.Animator;

/* compiled from: Launcher */
interface LauncherTransitionable {
    void onLauncherTransitionEnd(Animator animator);

    void onLauncherTransitionStart(Animator animator);
}
