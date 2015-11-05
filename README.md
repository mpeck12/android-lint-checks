Android Lint Checks
===================

NOTE
----
Most of our proposed lint checks have been incorporated into the AOSP
studio-master-dev branch so have been removed from here.
See https://android-review.googlesource.com/#/q/owner:mpeck%2540mitre.org+status:merged+project:platform/tools/base+branch:studio-master-dev
They should appear in a future release of the Android SDK.
In the mean time, they can still be separately compiled and included in the current
version of the Android SDK as a jar plugin through the following steps:

* Download the source code for the desired lint checks from the Android Open Source Project, e.g. from https://android.googlesource.com/platform/tools/base/+log/studio-master-dev/lint/libs/lint-checks/src/main/java/com/android/tools/lint/checks
* Place the source code in its own directory tree, e.g. in a directory called “androidlint”
* Change the package names in the source code to reflect the created directory tree (e.g. change the package entry at the top of each source code file to a value such as “package androidlint;”)
* Create a MyIssueRegistry.java with contents similar to the below, with an entry in the array for each Issue declared in the lint check source code.
```
package androidlint;

import java.util.List;
import java.util.Arrays;

import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.detector.api.Issue;

public class MyIssueRegistry extends IssueRegistry {
	@Override
	public List<Issue> getIssues() {
        return Arrays.asList(
            TrustAllX509TrustManagerDetector.ISSUE,
            UnsafeBroadcastReceiverDetector.ACTION_STRING);
	}
}
```
* Compile the lint checks and issue registry, e.g. ```javac –cp <sdk-path>/tools/lib/lint-api.jar *.java``` where <sdk-path> is the installed location of the Android SDK
* Create a MANIFEST.MF file with contents similar to the below:
```
Manifest-Version: 1.0
Lint-Registry: androidlint.MyIssueRegistry
```
* Bundle the compiled lint checks, issue registry, and MANIFEST.MF file into a jar file by running: ```jar cmf MANIFEST.MF custom.jar androidlint/*.class```
* Create an .android/lint directory under the user’s home directory and copy the jar file to it, e.g. ```cp custom.jar /home/<username>/.android/lint/custom.jar``` on most Linux distributions, or ```copy custom.jar C:\Users\<username>\.android\.lint\custom.jar``` on Windows
* Run ```lint –list Security``` and verify that the new lint checks appear in the list. They will now be used by default when running lint from the command line (e.g. with ```lint``` or ```gradlew lint```). Unfortunately, additional steps are needed to integrate the lint checks directly into the Android Studio UI.

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
