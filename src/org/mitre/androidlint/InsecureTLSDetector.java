/**
* Copyright 2015 The MITRE Corporation. All rights reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
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
**/

package org.mitre.androidlint;

import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.ClassContext;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.Detector.ClassScanner;

public class InsecureTLSDetector extends Detector implements
        ClassScanner {
    public static final Issue ISSUE = Issue.create("InsecureTLS",
        "Insecure certificate validation",
        "This check looks for an X509TrustManager that contains a getAcceptedIssuers, " +
        "checkServerTrusted, or checkClientTrusted method that simply returns " +
        "null (or empty array in the case of getAcceptedIssuers) " +
        "which could result in the app improperly trusting unauthorized " +
        "certificates.",
        Category.SECURITY,
        6,
        Severity.WARNING,
        new Implementation(InsecureTLSDetector.class, Scope.CLASS_FILE_SCOPE));

    public InsecureTLSDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.SLOW;
    }

    @SuppressWarnings("rawtypes")
    public void checkClass(@NonNull final ClassContext context,
            @NonNull ClassNode classNode) {
        if(!classNode.interfaces.contains("javax/net/ssl/X509TrustManager")) {
            return;
        }
        List methodList = classNode.methods;
        for (Object m : methodList) {
            MethodNode method = (MethodNode) m;
            if (method.name.equals("getAcceptedIssuers")) {
                InsnList nodes = method.instructions;
                boolean emptyMethod = true; // Stays true if method doesn't perform any "real"
                                            // operations (just returns empty array or null)
                for (int i = 0, n = nodes.size(); i < n; i++) {
                    AbstractInsnNode instruction = nodes.get(i);
                    int type = instruction.getType();
                    if (type != AbstractInsnNode.LABEL && type != AbstractInsnNode.LINE
                            && !(type == AbstractInsnNode.INSN
                                    && instruction.getOpcode() == Opcodes.ICONST_0)
                            && !(type == AbstractInsnNode.TYPE_INSN
                                    && instruction.getOpcode() == Opcodes.ANEWARRAY)
                            && !(type == AbstractInsnNode.INSN
                                    && instruction.getOpcode() == Opcodes.ACONST_NULL)
                            && !(type == AbstractInsnNode.INSN
                                    && instruction.getOpcode() == Opcodes.ARETURN)) {
                        emptyMethod = false;
                    }
                }
                // If emptyMethod is true, raise issue.
                if (emptyMethod) {
                    Location location = context.getLocation(method, classNode);
                    context.report(ISSUE, location, method.name + " always returns an empty " +
                        "array or null, which could cause vulnerable TLS certificate checking");
                }
            } else if (method.name.equals("checkServerTrusted")
                    || method.name.equals("checkClientTrusted")) {
                InsnList nodes = method.instructions;
                boolean emptyMethod = true; // Stays true if method doesn't perform any "real" 
                                            // operations (just returns null)
                for (int i = 0, n = nodes.size(); i < n; i++) {
                    AbstractInsnNode instruction = nodes.get(i);
                    int type = instruction.getType();
                    if (type != AbstractInsnNode.LABEL && type != AbstractInsnNode.LINE
                            && !(type == AbstractInsnNode.INSN
                                    && instruction.getOpcode() == Opcodes.RETURN)) {
                        emptyMethod = false;
                    }
                }
                // if emptyMethod is true, raise issue.
                if (emptyMethod) {
                    Location location = context.getLocation(method, classNode);
                    context.report(ISSUE, location, method.name + " is empty, which could cause " +
                        "vulnerable TLS certificate checking");
                }
            }
        }
    }
}
