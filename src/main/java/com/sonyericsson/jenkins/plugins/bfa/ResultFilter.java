package com.sonyericsson.jenkins.plugins.bfa;

import hudson.model.Result;

public class ResultFilter {
    private static boolean doNotAnalyzeAbortedJobs = false;

    public static void setDoNotAnalyzeAbortedJobs(boolean doNotAnalyzeAbortedJobs) {
        ResultFilter.doNotAnalyzeAbortedJobs = doNotAnalyzeAbortedJobs;
    }

    public static boolean needToAnalyze(Result result)  {
        if (doNotAnalyzeAbortedJobs) {
            return result != Result.SUCCESS && result != Result.ABORTED;
        }
        else {
            return result != Result.SUCCESS;
        }
    }
}
