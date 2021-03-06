/*
 * Copyright 2018 the original author or authors.
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
 */

package dev.gradleplugins.integtests.fixtures.nativeplatform

import dev.gradleplugins.test.fixtures.file.TestFile
import org.gradle.internal.os.OperatingSystem
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

trait HostPlatform {
	dev.gradleplugins.integtests.fixtures.nativeplatform.NativeInstallationFixture installation(Object installDir, OperatingSystem os = OperatingSystem.current()) {
		if (installDir instanceof TestFile) {
			return new dev.gradleplugins.integtests.fixtures.nativeplatform.NativeInstallationFixture((TestFile) installDir, os)
		}
		return new dev.gradleplugins.integtests.fixtures.nativeplatform.NativeInstallationFixture(file(installDir), os)
	}

	String getOsName() {
		return System.getProperty("os.name")
	}

	String getArchName() {
		return DefaultNativePlatform.currentArchitecture.name
	}
}
