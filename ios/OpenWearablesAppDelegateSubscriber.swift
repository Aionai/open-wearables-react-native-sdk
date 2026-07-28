import ExpoModulesCore
import OpenWearablesHealthSDK
import UIKit

public class OpenWearablesAppDelegateSubscriber: ExpoAppDelegateSubscriber {
    public func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        
        // Trigger background task registration
        let sdk = OpenWearablesHealthSDK.shared

        // Diagnostic (umbrella#191): surface the SDK's own launch breadcrumbs
        // ("Restored N tracked types" / "Auto-restoring background sync..." /
        // "Cannot auto-restore: no session") in the unified log so cold
        // background launches can be inspected via `log collect`.
        sdk.logLevel = .always

        let appState = application.applicationState
        NSLog(
            "[OWDiag] didFinishLaunching state=%ld sessionValid=%d syncActive=%d",
            appState.rawValue,
            sdk.isSessionValid ? 1 : 0,
            sdk.isSyncActive ? 1 : 0
        )

        // The SDK keeps `host` in memory only; restore the persisted value so
        // background relaunches (URLSession/BGTask wakes that fire before JS
        // calls configure()) can build apiBaseUrl and refresh expired tokens.
        let defaults = UserDefaults(suiteName: "com.openwearables.healthsdk.config") ?? .standard
        if let host = defaults.string(forKey: "host") {
            NSLog("[OWDiag] restoring host at launch")
            sdk.configure(host: host)
        } else {
            NSLog("[OWDiag] no persisted host found at launch")
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