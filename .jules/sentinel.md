## 2026-01-23 - Network Security Config Cleartext
**Vulnerability:** Global `cleartextTrafficPermitted="true"` in `network_security_config.xml` allowed potential insecure communication with backend APIs if URL schemes were downgraded.
**Learning:** News reader apps often require global cleartext for WebView compatibility, but this shouldn't compromise the app's own API security.
**Prevention:** Use `<domain-config>` to enforce `cleartextTrafficPermitted="false"` for known API domains while keeping the global fallback for browsing.
