package dev.trang.notikeeper;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;

/**
 * The system runs this ~1 s after the no-freeze list changes, so no process waits in between.
 * Content-trigger jobs fire once and can't be persisted: we re-arm after each run and at boot.
 */
public class GuardJob extends JobService {
    private static final int ID = 1;

    static void arm(Context c, boolean on) {
        JobScheduler js = c.getSystemService(JobScheduler.class);
        if (!on || !XiaomiGuard.IS_XIAOMI) {
            js.cancel(ID);
        } else if (js.getPendingJob(ID) == null) { // replacing a pending job would drop its trigger
            schedule(c, js);
        }
    }

    private static void schedule(Context c, JobScheduler js) {
        js.schedule(new JobInfo.Builder(ID, new ComponentName(c, GuardJob.class))
                .addTriggerContentUri(new JobInfo.TriggerContentUri(XiaomiGuard.URI, 0))
                .setTriggerContentUpdateDelay(500)
                .setTriggerContentMaxDelay(1000)
                .build());
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        // Our own write triggers one more run, which then finds GMS protected and does nothing.
        XiaomiGuard.repair(this);
        schedule(this, getSystemService(JobScheduler.class));
        KeepAlive.exitSoon();
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
