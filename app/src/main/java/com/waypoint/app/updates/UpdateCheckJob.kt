package com.waypoint.app

import android.app.job.JobParameters
import android.app.job.JobService
import kotlinx.coroutines.*
import okhttp3.Call
import okhttp3.OkHttpClient
import java.io.IOException
import java.util.concurrent.TimeUnit

/** System-bound job; no foreground service, wake lock, account or push server. */
class UpdateCheckJob:JobService() {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private var running:Job?=null
    private var request:Call?=null
    private var active:JobParameters?=null
    private var generation=0L
    private val client=OkHttpClient.Builder().connectTimeout(10,TimeUnit.SECONDS)
        .readTimeout(15,TimeUnit.SECONDS).callTimeout(30,TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).build()

    override fun onStartJob(params:JobParameters):Boolean {
        cancelCurrent()
        val app=application as WaypointApplication
        if(!app.updateAlerts.allowed() || app.navigation.isDriving)return false
        val ticket=generation
        active=params
        val call=releaseCheckCall(client)
        request=call
        running=scope.launch {
            var retry=false
            try {
                val release=withContext(Dispatchers.IO) { readReleaseCheck(call) }
                ensureActive()
                if(generation==ticket && active!=null)release?.let(app.updateAlerts::notifyIfNew)
            } catch(e:CancellationException) { throw e }
            catch(_:IOException) { retry=true }
            catch(_:Exception) { /* Invalid release metadata is retried at the next regular check. */ }
            finally {
                if(generation==ticket && active!=null) {
                    active=null;request=null
                    jobFinished(params,retry)
                }
            }
        }
        return true
    }
    override fun onStopJob(params:JobParameters):Boolean {
        // Android may deliver a different JobParameters instance for the same job.
        if(active?.jobId==params.jobId)cancelCurrent()
        return true
    }
    private fun cancelCurrent() {
        generation++
        active=null
        request?.cancel();request=null
        running?.cancel();running=null
    }
    override fun onDestroy() {
        cancelCurrent();scope.cancel()
        super.onDestroy()
    }
}
