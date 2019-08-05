/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.upgrade.utils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.File;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author dbernstein
 * @since 2019-08-05
 */
public class UpgradeUtilDriver {

    private static final org.slf4j.Logger logger = getLogger(UpgradeUtilDriver.class);

    private UpgradeUtilDriver() {
        // Prevent public instantiation
    }

    /**
     * The main entry point
     *
     * @param args from the command line
     */
    public static void main(final String[] args) {
        final UpgradeUtilDriver driver = new UpgradeUtilDriver();

        try {
            driver.run(args);

        } catch (final Exception e) {
            logger.error("Error performing upgrade: {}", e.getMessage());
            logger.debug("Stacktrace: ", e);
        }
    }

    private void run(final String[] args) {

        // Help option
        final Options configOptions = new org.apache.commons.cli.Options();

        configOptions.addOption(Option.builder("h")
                .longOpt("help")
                .hasArg(false)
                .desc("Print these options")
                .required(false)
                .build());

        configOptions.addOption(Option.builder("i")
                .longOpt("input-dir")
                .hasArg(true)
                .desc("The path to the directory containing a 4.7.x export")
                .required(true)
                .build());

        // first see if they've specified a config file
        final CommandLineParser parser = new DefaultParser();
        final CommandLine cmd;
        try {
            cmd = parser.parse(configOptions, args);
        } catch (final ParseException e) {
            printHelpAndExit(e.getMessage(), configOptions);
            return;
        }

        final File inputDir = new File(cmd.getOptionValue("i"));

        if (!inputDir.exists()) {
            printHelpAndExit("input directory " + inputDir.getAbsolutePath() + " does not exist.", configOptions);
        }

        final String outputDirStr = cmd.getOptionValue("o");

        final File outputDir;

        if (outputDirStr == null) {
            outputDir = new File("output");
            outputDir.mkdirs();
        } else {
            outputDir = new File(outputDirStr);
            if (!outputDir.exists()) {
                printHelpAndExit("output directory " + outputDir.getAbsolutePath() + " does not exist.", configOptions);
            }
        }


        try {
            //run driver
            new UpgradeUtil(inputDir, outputDir).run();
        } catch (final Exception e) {
            logger.error("Upgrade failed.", e);
            printHelpAndExit("Upgrade failed due to " + e.getMessage() + ".  See log for details.", configOptions);
        }
    }

    private static void printHelpAndExit(final String errorMessage, final Options options) {
        final HelpFormatter formatter = new HelpFormatter();
        System.err.println(errorMessage);
        formatter.printHelp("fcepo-upgrade-util", options);
        System.exit(1);

    }
}
