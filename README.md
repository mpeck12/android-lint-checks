Android Lint Checks
===================

NOTE
----
These lint checks (and others) have been incorporated into the AOSP
studio-master-dev branch, including improvements suggested by the
reviewers (that are in AOSP but not here).
So the code in this repository is now obsolete.
See https://android-review.googlesource.com/#/q/owner:mpeck%2540mitre.org+status:merged+project:platform/tools/base+branch:studio-master-dev

Broadcast Receiver Security Lint Checks
---------------------------------------
We propose two new lint checks to detect insecure broadcast receivers that
fail to properly check the origin of received intents, potentially making
the receiver vulnerable to spoofed intents.
The first check is for broadcast receivers that have declared an intent-filter
for a protected-broadcast action string but fail to actually check the received
action string, as described by section 4.3 of [1].
The second check is for broadcast receivers that have declared an intent-filter
for the SMS_DELIVER or SMS_RECEIVED action string but fail to ensure
that the sender holds the BROADCAST_SMS permission [2] [3].
Note that neither of these checks address dynamically created receivers.
They only address receivers that are declared in the application manifest.

* [1] Chin, et al. Analyzing Inter-Application Communication in Android.
Mobisys '11. https://www.eecs.berkeley.edu/~daw/papers/intents-mobisys11.pdf
* [2] https://commonsware.com/blog/2013/10/06/secured-broadcasts-sms-clients.html
* [3] http://android-developers.blogspot.com/2013/10/getting-your-sms-apps-ready-for-kitkat.html

TLS TrustManager Lint Check
---------------------------
This lint check detects declarations of TLS TrustManagers that do
not properly check certificates. The check looks for empty
checkClientTrusted and checkServerTrusted methods as well as
getAcceptedIssuers methods that always return null or an empty
array. We do not check if or how the declared TrustManager
is actually used by the application.
As documented by numerous sources such as [1] [2] [3], Android
applications commonly fail to properly check certificates while
establishing TLS sessions, making them susceptible to MITM attacks.

* [1] Fahl, et al. Why Eve and Mallory Love Android: An Analysis of Android SSL (In)Security.
CCS '12.
* [2] Sounthiraraj, et al. Large Scale, Automated Detection of SSL/TLS
Man-in-the-Middle Vulnerabilities in Android Apps. NDSS '14.
* [3] FireEye. SSL Vulnerabilities: Who listens when Android applications talk?
https://www.fireeye.com/blog/threat-research/2014/08/ssl-vulnerabilities-who-listens-when-android-applications-talk.html

How to Compile and Install
--------------------------
General information on writing and compiling custom lint rules can be found at:
http://tools.android.com/tips/lint-custom-rules

The lint rules can be compiled using javac or by using
an IDE such as Eclipse (compiling as a standard Java project,
not an Android application).
tools/lib/lint-api.jar in the Android SDK must be included when
building.

The compiled class files should be placed in a .jar file with
a manifest file containing the text:
Manifest-Version: 1.0
Lint-Registry: org.mitre.androidlint.MyIssueRegistry

The jar file should be placed in the ~/.android/lint directory
(or Windows equivalent). The lint rules will then be used by
Android's lint checker. They should show up when running
"lint --list Security".

Example commands to compile and install the lint rules using
javac with the Android SDK installed in ~/sdk:
```
cd src/org/mitre/androidlint
javac -cp ~/sdk/tools/lib/lint-api.jar *.java
cd ../../..
jar cmf MANIFEST.MF mitrelint.jar org/mitre/androidlint/*.class
cp mitrelint.jar ~/.android/lint
```

How to Analyze APK Files
------------------------
Android's lint checker is primarily designed for use by developers
with access to application source. We've demonstrated that it
can also be used to analyze APKs. (However, only the lint checks
that analyze .class files or AndroidManifest.xml will produce
results. The lint checks that analyze .java files will not
produce any results since no .java files will be included
in the analysis.)

We provide an example script "lint-apk.sh" demonstrating how
to analyze APK files using the Android lint checker. It depends on:
https://github.com/google/enjarify
https://code.google.com/p/xml-apk-parser/downloads/detail?name=APKParser.jar

Note that many of the lint checks operate on .java files. Those lint checks
will not report any results since there would not be any .java files
(just class files and AndroidManifest.xml). Our lint checks operate
on the .class files so will produce results from "enjarified" APKs.

Copyright
=========
Copyright 2015 The MITRE Corporation, All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Other Information
=================
NOTICE

This software was produced for the U.S. Government under
Basic Contract No. W15P7T-13-C-A802, and is subject to the Rights
in Noncommercial Computer Software and Noncommercial Computer
Software Documentation Clause 252.227-7014 (FEB 2012)

Approved for public release, case 15-1324.

Please send comments or questions to mpeck@mitre.org
