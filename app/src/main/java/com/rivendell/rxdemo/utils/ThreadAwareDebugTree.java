package com.rivendell.rxdemo.utils;


import timber.log.Timber;

/**
 * Created by Rivendell on 2017/8/16.
 */

public class ThreadAwareDebugTree extends Timber.DebugTree {
    @Override
    protected void log(int priority, String tag, String message, Throwable t) {
        if (tag != null) {
            String threadName = Thread.currentThread().getName();
            tag = "<" + threadName + "> " + tag;
        }
        super.log(priority, tag, message, t);
    }

    @Override
    protected String createStackElementTag(StackTraceElement element) {
        return super.createStackElementTag(element) + "(Line " + element.getLineNumber() + ")";
    }
}
