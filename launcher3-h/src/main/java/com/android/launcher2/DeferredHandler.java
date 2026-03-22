package com.android.launcher2;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import java.util.LinkedList;

public class DeferredHandler {
    private Impl mHandler = new Impl();
    private MessageQueue mMessageQueue = Looper.myQueue();
    /* access modifiers changed from: private */
    public LinkedList<Runnable> mQueue = new LinkedList<>();

    private class Impl extends Handler implements MessageQueue.IdleHandler {
        private Impl() {
        }

        public void handleMessage(Message msg) {
            synchronized (DeferredHandler.this.mQueue) {
                if (DeferredHandler.this.mQueue.size() != 0) {
                    Runnable r = (Runnable) DeferredHandler.this.mQueue.removeFirst();
                    r.run();
                    synchronized (DeferredHandler.this.mQueue) {
                        DeferredHandler.this.scheduleNextLocked();
                    }
                }
            }
        }

        public boolean queueIdle() {
            handleMessage((Message) null);
            return false;
        }
    }

    private class IdleRunnable implements Runnable {
        Runnable mRunnable;

        IdleRunnable(Runnable r) {
            this.mRunnable = r;
        }

        public void run() {
            this.mRunnable.run();
        }
    }

    public void post(Runnable runnable) {
        synchronized (this.mQueue) {
            this.mQueue.add(runnable);
            if (this.mQueue.size() == 1) {
                scheduleNextLocked();
            }
        }
    }

    public void postIdle(Runnable runnable) {
        post(new IdleRunnable(runnable));
    }

    /* access modifiers changed from: package-private */
    public void scheduleNextLocked() {
        if (this.mQueue.size() <= 0) {
            return;
        }
        if (this.mQueue.getFirst() instanceof IdleRunnable) {
            this.mMessageQueue.addIdleHandler(this.mHandler);
        } else {
            this.mHandler.sendEmptyMessage(1);
        }
    }
}
