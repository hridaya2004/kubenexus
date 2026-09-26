# Privacy Policy — KubeNexus

**Last updated: 26 September 2026**

> **Reviewer note (remove before publishing).** This is a starting point drafted
> from the app's actual code and from Google Play's User Data policy. It is not
> legal advice. Re-review it — with a lawyer, if you can afford one — before you
> launch and again before you turn on in-app purchases. Play requires this page to
> be live at a public, non-geofenced, **non-PDF** URL, and to match the Data safety
> form you submit. If the two disagree, expect a rejection.
>
> **Placeholders to fill in:** `[YOUR_LEGAL_NAME]`, `[PRIVACY_EMAIL]`, `[PUBLIC_URL]`

---

## 1. Who we are

KubeNexus ("the app") is published by **[YOUR_LEGAL_NAME]** ("we", "us"), an
independent developer. KubeNexus is **not** affiliated with, endorsed by, or
sponsored by The Linux Foundation, the Kubernetes® project, or the Cloud Native
Computing Foundation.

- **App name:** KubeNexus
- **Play package:** `dev.hridaya.kubenexus`
- **Source code:** <https://github.com/hridaya2004/kubenexus>
- **Privacy contact:** `[PRIVACY_EMAIL]`

We do not operate a server, an account system, or a telemetry service.

## 2. The short version

**Your cluster credentials and everything about your clusters stay on your
device.** The app talks directly from your phone to your Kubernetes API server.
Your data is never routed through us, because we do not exist as a service.

| | |
|---|---|
| Do we collect your data? | **No.** |
| Do we share your data? | **No.** |
| Do we run a server? | **No.** |
| Do we show ads? | **No.** |
| Do we use third-party analytics or crash SDKs? | **No.** |
| Do we ask you to create an account? | **No.** |

## 3. What the app stores on your device

KubeNexus keeps the following **locally**, in a private app sandbox on your
device only:

| Data | What it is | Why | Where it lives |
|---|---|---|---|
| **Cluster credentials** | The kubeconfig you paste or import — server URL, context, and authentication material such as client certificates or bearer tokens | To connect to your cluster | `kubenexus.db` (Room) |
| **Cluster cache** | Namespaces, pods, services, deployments and API schemas read from your cluster | To display your resources offline and quickly | `kubenexus.db` (Room) |
| **App logs** | Diagnostic log lines the app writes while running | For you to inspect and export when troubleshooting | On-device log buffer |
| **Preferences** | Theme (light/dark/system) | To remember your choice | Android DataStore |

Cluster credentials are **authentication information**, which Google Play treats
as personal and sensitive user data. Because you supply it deliberately, for the
app's core purpose, we treat it as the most sensitive thing we hold.

### What we specifically do *not* do

- We do **not** collect, transmit, or upload your kubeconfig, credentials, or
  cluster data. There is no destination for it to be sent to.
- We do **not** read your device's IMEI, IMSI, SIM serial, Android Advertising
  ID, or any persistent device identifier.
- We do **not** collect your contacts, photos, files, location, microphone,
  camera, SMS, or call logs.
- We do **not** build a fingerprint of you.
- We do **not** embed advertising, analytics, or crash-reporting SDKs.

## 4. Where your data goes on the network

When you use the app, your device connects **directly to the Kubernetes API
server you configured**. We are not in that path.

The app also connects to:

- **Your cluster's API server** — the address *you* configured. This is the whole
  point of the app.
- **Your container registry**, if you use the image-pull features, at the
  address configured by your cluster.

We set `android:networkSecurityConfig` to **refuse cleartext (unencrypted)
traffic**, so these connections must use HTTPS. User-installed CA certificates
are trusted in debug builds only, never in release builds.

If you are on a managed or corporate network, your administrator may be able to
see connection metadata. That is between you and them.

## 5. Port forwarding and background operation

KubeNexus can keep Kubernetes **port-forward tunnels** open while the app is in
the background, using an Android foreground service with a persistent
notification. While a tunnel is open:

- the tunnel carries traffic between your device and your cluster;
- **we do not observe, log, proxy, or record** any traffic that passes through it;
- the notification is visible and stoppable by you at any time.

Port forwarding is a first-party feature of the app for your own clusters. The
app is not a proxy for third parties, and we do not offer, sell, or broker
access to anyone else's networks.

## 6. Log export and sharing — you control it

The app can export diagnostic logs (plain text) **only when you explicitly ask
it to**, using the standard Android share sheet, so you choose the destination.
We never initiate a share, and we never receive the file. Files you share are
then governed by whichever app or service you send them to.

## 7. Purchases, if we add them

*KubeNexus is currently free and does not sell anything. This section describes
what will be true if in-app purchases are added, and must be updated to match
reality at the moment you add them.*

- Any paid feature sold **inside the app on Google Play** will be processed
  entirely by **Google Play Billing**.
- We never see, receive, or store your card number or full payment credentials.
  Google acts as the merchant of record and handles the transaction data.
- We receive only the minimum Google provides to confirm a purchase (such as a
  purchase token, package name, and an acknowledgement that an item was
  purchased or not).
- Your purchase is linked to your **Google Play account**, not to a KubeNexus
  account — we do not operate accounts.
- Pricing shown in the app matches Google Play's billing interface exactly.
- You can restore a purchase on a new device using your Google Play account, and
  you can request a refund through Google Play.
- We do not use alternative payment methods inside the app, and we do not link
  you to any external checkout.

## 8. Data security

- **Encryption in transit:** all network connections must use HTTPS; cleartext
  traffic is blocked by the app's network security configuration.
- **Storage:** credentials and cluster data are stored inside the Android
  application sandbox, which the operating system isolates from other apps.
- **Backups:** the credential database is **explicitly excluded** from Android
  cloud backup and from device-to-device transfer. Your kubeconfig will not be
  copied into Google's backup service or migrated to a new phone by the system.
- **Screenshots:** the app does not set `FLAG_SECURE`, so you can take
  screenshots. If you would prefer it not be possible to capture cluster
  credentials on screen, tell us and we will consider enabling it.
- **No third-party processors.** Because data never leaves your device, we have
  no sub-processors, no vendors, and no third parties with whom your data is
  shared.

## 9. Data retention and deletion

We retain nothing, because we receive nothing.

**Data on your device** is retained until you remove it. You can delete it at any
time:

- **Delete a single cluster** — removes its stored credentials and cached
  resources from your device.
- **Clear cached resources** — removes cached cluster data while keeping
  credentials.
- **Clear app logs** — removes the local diagnostic log buffer.
- **Uninstall KubeNexus** — removes all app data, including `kubenexus.db`.
- **Revoke access** — the fastest way to invalidate a leaked credential is to
  revoke the associated token or certificate in your cluster, from any client.

Because the exclusion rules above keep the database out of Android backup,
uninstalling also leaves nothing behind in a backup.

## 10. Account deletion

KubeNexus **does not create accounts** and holds no server-side user data, so
there is no account for us to delete. The equivalent user control is the local
deletion described in section 9, which is available at all times from within the
app. This satisfies the intent of Google Play's account-deletion requirement;
because no account exists, there is no associated server data to remove.

If a future version introduces a hosted account or sync feature, this section
**must** be rewritten before that feature ships, to provide in-app deletion plus
a public web deletion request form.

## 11. Children

KubeNexus is intended for adults operating infrastructure they are responsible
for. It is **not** directed at children, and we do not knowingly collect data
from children. If you believe a child has provided us with personal data,
contact `[PRIVACY_EMAIL]` and we will help.

## 12. Your rights

Depending on where you live, privacy law may give you rights to access, correct,
delete, or export your personal data, or to object to its processing.

In practice, because your data never leaves your device, **you already hold
complete control**: it is in an app sandbox only you can read, and you can erase
it at any time using the controls in section 9. That is a stronger position than
any request procedure could give you.

If you still have a question or a formal request, email `[PRIVACY_EMAIL]`. We
will respond within 30 days.

Because we do not operate a server and hold no records, we cannot retrieve data
"about you" from anywhere — there is nowhere it is held.

## 13. International transfers

We do not transfer personal data internationally, because we do not collect or
receive personal data at all. Your device talks to your own cluster, wherever
that cluster happens to be.

If you are in the EEA, UK, or Switzerland, note that the app's handling of data
on your device remains under your control and is not a transfer by us.

## 14. Third-party components

KubeNexus includes open-source software, including the Kubernetes Go client and
the Ghostty terminal emulator core. These components run **on your device** and
none of them transmit your data to their authors or to anyone else. See
[THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md) for the full list and licenses.

## 15. Changes to this policy

If we change this policy — for example because a new version of the app collects
something it does not collect today, or because in-app purchases are added — we
will:

1. update the "Last updated" date,
2. change the app version's description or release notes to note the change, and
3. if the change is material, notify you in the app before it takes effect.

Material changes will not be applied retroactively without notice.

## 16. Contact

- **Privacy questions:** `[PRIVACY_EMAIL]`
- **Security reports:** `[PRIVACY_EMAIL]`
- **Source code:** <https://github.com/hridaya2004/kubenexus>
- **Published at:** `[PUBLIC_URL]` (must be a public, non-geofenced, non-PDF URL)

---

## Appendix A — Google Play Data safety form (suggested answers)

Because the app collects nothing and shares nothing, the honest answers are
mostly negative. In the Data safety section select **"No data collected"** and
**"No data shared"** for every type.

One nuance Play's form handles awkwardly: data stored **only on the device and
never transmitted** is generally **not** "collection." The form asks about
collection and sharing, not local storage. The log-export feature is a
**user-initiated transfer** of a file the user already possesses to a destination
the user already chose — Play explicitly does not treat that as collection or as
a "sale."

If you later add any SDK that phones home, or a hosted sync feature, this table
becomes obsolete and you must redo it.

| Data type | Collected? | Shared? | Purpose | Encrypted in transit? |
|---|---|---|---|---|
| Credentials / auth info | No (local only) | No | App functionality | Yes, to your cluster |
| App interactions | No | No | App functionality (local logs) | Not applicable |
| Files and docs | No (user-initiated export only) | No | App functionality | Not applicable |
| Diagnostics | No | No | App functionality (local logs) | Not applicable |
| Financial info | No — Google Play Billing only, if purchases are added | No | App functionality | Yes, by Google Play |

## Appendix B — Play Console checklist

- [ ] Policy published at a public, active, non-geofenced, **non-PDF** URL
- [ ] URL entered in Play Console → Policy → App content → Privacy policy
- [ ] Privacy policy reachable **inside the app** (Settings → About → Privacy policy)
- [ ] Data safety form completed and consistent with section 3 above
- [ ] Developer/contact details in Play listing match section 1 here
- [ ] Target audience set; content rating questionnaire completed
- [ ] If IAP is added: section 7 updated **and** the listing states that payment
      is required for the paid features (Payments policy §6)
- [ ] If IAP is added: no external payment links or webview checkouts anywhere
      in the app (Payments policy §4)
