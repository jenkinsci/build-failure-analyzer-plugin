package com.sonyericsson.jenkins.plugins.bfa;

import hudson.model.Result;

public final class ResultFilter {
    private static boolean doNotAnalyzeAbortedJobs = false;

    private ResultFilter() {
    }

    public static void setDoNotAnalyzeAbortedJobs(boolean doNotAnalyzeAbortedJobs) {
        ResultFilter.doNotAnalyzeAbortedJobs = doNotAnalyzeAbortedJobs;
    }

    public static boolean needToAnalyze(Result result)  {
        if (doNotAnalyzeAbortedJobs) {
            return result != Result.SUCCESS && result != Result.ABORTED;
        }   else {
            return result != Result.SUCCESS;
        }
    }
}
