/*	Copyright 2021 Elastic Path Software Inc.

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/

package com.elasticpath.tools.mavenminimal.util;

import java.io.PrintStream;

/**
 * A simple logging class.
 */
public class Logger {

    private static PrintStream out = System.out;

    /**
     * Initialise the logger to print to the provided PrintStream.
     * @param printStream the PrintStream to output to
     */
    public static void init(final PrintStream printStream) {
        out = printStream;
    }

    /**
     * Print a debug message.
     * @param message the message to output
     */
    public static void debug(final Object message) {
        if (isDebugEnabled()) {
            out.println(message);
        }
    }

    /**
     * Print a debug message, with a stack trace.
     * @param message the message to output
     * @param t a throwable related to the message
     */
    public static void debug(final Object message, final Throwable t) {
        if (isDebugEnabled()) {
            out.println(message);
            if (t != null) {
                t.printStackTrace(System.out);
            }
        }
    }

    /**
     * The environment variable "DEBUG=true" enables debug logging.
     * @return true if debug is enabled, false otherwise.
     */
    public static boolean isDebugEnabled() {
        return Boolean.parseBoolean(System.getenv("DEBUG"));
    }

}
