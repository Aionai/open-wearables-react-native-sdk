import ExpoModulesCore
import OpenWearablesHealthSDK
import UIKit
import os

private let owDiagLog = Logger(subsystem: "com.aionai.owdiag", category: "launch")

public class OpenWearablesAppDelegateSubscriber: ExpoAppDelegateSubscriber {
    public func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        
        // Trigger background task registration
        let sdk = OpenWearablesHealthSDK.shared

        // Diagnostic (umbrella#191): NSLog from this app is not persisted to
        // the unified log on current iOS, so route the SDK's own breadcrumbs
        // ("Restored N tracked types" / "Auto-restoring background sync..." /
        // "Cannot auto-restore: no session") through a public os_log. In cold
        // background launches JS never boots, so this onLog handler stays
        // installed for the whole window we need to observe; in foreground
        // launches the Expo module's OnCreate replaces it, which is fine.
        sdk.logLevel = .always
        sdk.onLog = { message in
            owDiagLog.notice("[SDK] \(message, privacy: .public)")
        }

        let appState = application.applicationState
        owDiagLog.notice(
            "didFinishLaunching state=\(appState.rawValue) sessionValid=\(sdk.isSessionValid) syncActive=\(sdk.isSyncActive)"
        )

        // The SDK keeps `host` in memory only; restore the persisted value so
        // background relaunches (URLSession/BGTask wakes that fire before JS
        // calls configure()) can build apiBaseUrl and refresh expired tokens.
        let defaults = UserDefaults(suiteName: "com.openwearables.healthsdk.config") ?? .standard
        if let host = defaults.string(forKey: "host") {
            owDiagLog.notice("restoring host at launch")
            sdk.configure(host: host)
        } else {
            owDiagLog.notice("no persisted host found at launch")
        }

        return true
    }

    public func application(
        _ application: UIApplication,
        handleEventsForBackgroundURLSession identifier: String,
        completionHandler: @escaping () -> Void
    ) {
        OpenWearablesHealthSDK.setBackgroundCompletionHandler(completionHandler)
    }
}