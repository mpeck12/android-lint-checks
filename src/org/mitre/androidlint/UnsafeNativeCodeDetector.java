/*
 * Copyright (C) 2015 The MITRE Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * NOTICE
 * This software was produced for the U.S. Government under
 * Basic Contract No. W15P7T-13-C-A802, and is subject to the Rights
 * in Noncommercial Computer Software and Noncommercial Computer
 * Software Documentation Clause 252.227-7014 (FEB 2012)
 *
 * Approved for public release, case 15-1324.
 *
 */

//package com.android.tools.lint.checks;
package org.mitre.androidlint;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.client.api.JavaParser.ResolvedClass;
import com.android.tools.lint.client.api.JavaParser.ResolvedMethod;
import com.android.tools.lint.client.api.JavaParser.ResolvedNode;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import lombok.ast.AstVisitor;
import lombok.ast.MethodInvocation;

public class UnsafeNativeCodeDetector extends Detector
        implements Detector.JavaScanner, Detector.OtherFileScanner {

    private static final Implementation IMPLEMENTATION_JAVA = new Implementation(
            UnsafeNativeCodeDetector.class,
            Scope.JAVA_FILE_SCOPE);

    private static final Implementation IMPLEMENTATION_OTHER = new Implementation(
            UnsafeNativeCodeDetector.class,
            Scope.OTHER_SCOPE);

    public static final Issue LOAD = Issue.create(
            "UnsafeDynamicallyLoadedCode",
            "`load` used to dynamically load code",
            "Dynamically loading code from locations other than the application's library " +
            "directory or the Android platform's built-in library directories is dangerous, " +
            "as there is an increased risk that the code could have been tampered with. " +
            "Applications should use `loadLibrary` when possible, which provides increased " +
            "assurance that libraries are loaded from one of these safer locations. " +
            "Application developers should use the features of their development " +
            "environment to place application native libraries into the lib directory " +
            "of their compiled APKs.",
            Category.SECURITY,
            4,
            Severity.WARNING,
            IMPLEMENTATION_JAVA);

    public static final Issue UNSAFE_NATIVE_CODE_LOCATION = Issue.create(
            "UnsafeNativeCodeLocation", //$NON-NLS-1$
            "Native code outside library directory",
            "In general, application native code should only be placed in the application's " +
            "library directory, not in other locations such as the res or assets directories. " +
            "Placing the code in the library directory provides increased assurance that the " +
            "code will not be tampered with after application installation. Application " +
            "developers should use the features of their development environment to place " +
            "application native libraries into the lib directory of their compiled " +
            "APKs. Embedding non-shared library native executables into applications should " +
            "be avoided when possible.",
            Category.SECURITY,
            4,
            Severity.WARNING,
            IMPLEMENTATION_OTHER);

    private static final String RUNTIME_CLASS = "java.lang.Runtime"; //$NON-NLS-1$
    private static final String SYSTEM_CLASS = "java.lang.System"; //$NON-NLS-1$

    private static final byte[] ELF_MAGIC_VALUE = { (byte) 0x7F, (byte) 0x45, (byte) 0x4C, (byte) 0x46 };

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.NORMAL;
    }

    // ---- Implements Detector.JavaScanner ----

    @Override
    public List<String> getApplicableMethodNames() {
        // Identify calls to Runtime.load() and System.load()
        return Arrays.asList("load");
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @Nullable AstVisitor visitor,
            @NonNull MethodInvocation node) {
        ResolvedNode resolved = context.resolve(node);
        if (resolved instanceof ResolvedMethod) {
            String methodName = node.astName().astValue();
            ResolvedClass resolvedClass = ((ResolvedMethod) resolved).getContainingClass();
            if ((resolvedClass.isSubclassOf(RUNTIME_CLASS, false)) ||
                    (resolvedClass.isSubclassOf(SYSTEM_CLASS, false))) {
                // Report calls to Runtime.load() and System.load()
                if ("load".equals(methodName)) {
                    context.report(LOAD, node, context.getLocation(node),
                            "Dynamically loading code using `load` is risky, please use " +
                                    "`loadLibrary` instead when possible");
                    return;
                }
            }
        }
    }

    // ---- Implements Detector.OtherFileScanner ----

    private static boolean isNativeCode(File file) {
        if (!file.isFile()) {
            return false;
        }

        String path = file.getPath().toLowerCase();

        // TODO: Currently this method never gets invoked on
        // files in the assets directory (at least when testing
        // with ./gradlew lint). Need to investigate why and
        // fix.
        if (!path.contains("res") && !path.contains("assets")) {
            return false;
        }

        try {
            FileInputStream fis = new FileInputStream(file);
            byte[] bytes = new byte[4];
            int length = fis.read(bytes);
            fis.close();
            if ((length == 4) && (Arrays.equals(ELF_MAGIC_VALUE, bytes))) {
                return true;
            } else {
                return false;
            }
        } catch (IOException ex) {
            return false;
        }
    }

    @NonNull
    @Override
    public EnumSet<Scope> getApplicableFiles() {
        return Scope.OTHER_SCOPE;
    }

    @Override
    public void run(@NonNull Context context) {
        if (!context.getProject().getReportIssues()) {
            // If this is a library project not being analyzed, ignore it
            return;
        }

        File file = context.file;
        if (isNativeCode(file)) {
            if (LintUtils.endsWith(file.getPath(), "so")) {
                context.report(UNSAFE_NATIVE_CODE_LOCATION, Location.create(file),
                        "Shared libraries should not be placed in the res or assets " +
                        "directories. Please use the features of your development " +
                        "environment to place shared libraries in the lib directory of " +
                        "the compiled APK.");
            } else {
                context.report(UNSAFE_NATIVE_CODE_LOCATION, Location.create(file),
                        "Embedding non-shared library native executables into applications " +
                        "should be avoided when possible, as there is an increased risk that " +
                        "the executables could be tampered with after installation. Instead, " +
                        "native code should be placed in a shared library, and the features of " +
                        "the development environment should be used to place the shared library " +
                        "in the lib directory of the compiled APK.");
            }
        }
    }

}
