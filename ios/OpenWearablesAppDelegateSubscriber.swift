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

        // The SDK keeps `host` in memory only; restore the persisted value so
        // background relaunches (URLSession/BGTask wakes that fire before JS
        // calls configure()) can build apiBaseUrl and refresh expired tokens.
        let defaults = UserDefaults(suiteName: "com.openwearables.healthsdk.config") ?? .standard
        if let host = defaults.string(forKey: "host") {
            sdk.configure(host: host)
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