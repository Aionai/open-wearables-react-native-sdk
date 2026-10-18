package expo.modules.openwearables

import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.kotlin.functions.Coroutine
import com.openwearables.health.sdk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

public class OpenWearablesModule : Module() {
  private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  public override fun definition() = ModuleDefinition {
    Name("OpenWearablesHealthSDK")

    // MARK: - Callbacks (Events)
    Events("onLog", "onAuthError")

    // MARK: - Lifecycle
    OnCreate {
      OpenWearablesHealthSDK.initialize(appContext.reactContext!!)
      
      appContext.activityProvider?.currentActivity?.let { activity ->
        OpenWearablesHealthSDK.getInstance().setActivity(activity)
      }

      OpenWearablesHealthSDK.getInstance().logListener = { message ->
        sendEvent(
          "onLog",
          mapOf(
            "message" to message
          )
        )
      }

      OpenWearablesHealthSDK.getInstance().authErrorListener = { statusCode, message ->
        sendEvent(
          "onAuthError",
          mapOf(
            "statusCode" to statusCode,
            "message" to message
          )
        )
      }
    }

    OnActivityEntersForeground {
      appContext.activityProvider?.currentActivity?.let { activity ->
        OpenWearablesHealthSDK.getInstance().setActivity(activity)
      }

      OpenWearablesHealthSDK.getInstance().onForeground()
    }

    OnActivityEntersBackground {
      OpenWearablesHealthSDK.getInstance().onBackground()
    }

    OnActivityDestroys {
      OpenWearablesHealthSDK.getInstance().unregisterPermissionLauncher()
    }

    // MARK: - Configure
    Function("configure") { host: String, customSyncURL: String? ->
      OpenWearablesHealthSDK.getInstance().configure(host, customSyncURL)
    }

    // MARK: - Auth
    AsyncFunction("signIn") Coroutine { userId: String, accessToken: String?, refreshToken: String?, apiKey: String? ->
      OpenWearablesHealthSDK.getInstance().signIn(
        userId,
        accessToken,
        refreshToken,
        apiKey
      )
    }

    AsyncFunction("signOut") Coroutine { _: Unit? ->
      OpenWearablesHealthSDK.getInstance().signOut()
    }

    Function("updateTokens") { accessToken: String, refreshToken: String ->
      OpenWearablesHealthSDK.getInstance().updateTokens(
        accessToken,
        refreshToken
      )
    }

    Function("restoreSession") {
      OpenWearablesHealthSDK.getInstance().restoreSession()
    }

    Function("isSessionValid") {
      OpenWearablesHealthSDK.getInstance().isSessionValid()
    }

    // MARK: - Authorization
    AsyncFunction("requestAuthorization") Coroutine { types: List<String> ->
      return@Coroutine OpenWearablesHealthSDK.getInstance().requestAuthorization(types)
    }

    // MARK: - Sync
    Function("setSyncInterval") { minutes: Long ->
      OpenWearablesHealthSDK.getInstance().setSyncInterval(minutes)
    }

    // Since 0.13.0 the native startBackgroundSync also runs the first sync inline, which
    // can be a whole historical export. Resolve once the session is checked and let the
    // upload continue here, so callers are not blocked for the length of the export.
    AsyncFunction("startBackgroundSync") { syncDaysBack: Int? ->
      val sdk = OpenWearablesHealthSDK.getInstance()
      if (!sdk.isSessionValid()) {
        sendEvent("onLog", mapOf("message" to "Cannot start sync: not signed in"))
        return@AsyncFunction false
      }
      syncScope.launch {
        try {
          sdk.startBackgroundSync(syncDaysBack)
        } catch (e: IllegalStateException) {
          sendEvent("onLog", mapOf("message" to "Cannot start sync: ${e.message}"))
        }
      }
      return@AsyncFunction true
    }

    AsyncFunction("stopBackgroundSync") Coroutine { _: Unit? ->
      OpenWearablesHealthSDK.getInstance().stopBackgroundSync()
    }

    // The native SDK dropped syncNow in 0.11.2. startBackgroundSync now uploads immediately
    // and keeps the periodic worker (KEEP policy), so it is the "upload now" call; it is
    // awaited here so the promise settles once the upload round has finished.
    AsyncFunction("syncNow") Coroutine { _: Unit? ->
      val sdk = OpenWearablesHealthSDK.getInstance()
      if (!sdk.isSyncActive()) throw IllegalStateException("Background sync is not active")
      sdk.startBackgroundSync(null)
    }

    // iOS resolves false when there is nothing to resume; the native here throws instead.
    AsyncFunction("resumeSync") Coroutine { _: Unit? ->
      try {
        OpenWearablesHealthSDK.getInstance().resumeSync()
        return@Coroutine true
      } catch (e: IllegalStateException) {
        sendEvent("onLog", mapOf("message" to "Cannot resume sync: ${e.message}"))
        return@Coroutine false
      }
    }

    Function("isSyncActive") {
      OpenWearablesHealthSDK.getInstance().isSyncActive()
    }

    Function("getSyncStatus") {
      OpenWearablesHealthSDK.getInstance().getSyncStatus()
    }

    Function("resetAnchors") {
      OpenWearablesHealthSDK.getInstance().resetAnchors()
    }

    Function("getStoredCredentials") {
      OpenWearablesHealthSDK.getInstance().getStoredCredentials()
    }

    // MARK: - Providers
    Function("getAvailableProviders") {
      OpenWearablesHealthSDK.getInstance().getAvailableProviders()
    }

    Function("setProvider") { providerId: String ->
      OpenWearablesHealthSDK.getInstance().setProvider(providerId)
    }
    
    // MARK: - Logs
    Function("setLogLevel") { levelId: Int ->
      val level = OWLogLevel.entries.getOrElse(levelId) {
          OWLogLevel.DEBUG
      }
      OpenWearablesHealthSDK.getInstance().logLevel = level
    }

    Function("getLogLevel") {
      OpenWearablesHealthSDK.getInstance().logLevel.ordinal
    }    
  }
}