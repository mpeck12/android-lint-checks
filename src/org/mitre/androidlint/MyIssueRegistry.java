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
import java.util.Arrays;

import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.detector.api.Issue;

public class MyIssueRegistry extends IssueRegistry {
	public MyIssueRegistry() {
	}

	@Override
	public List<Issue> getIssues() {
		return Arrays.asList(UnsafeNativeCodeDetector.LOAD);
	}

}
