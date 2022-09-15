/**
 * Copyright 2022 SPeCS.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.cppscanner;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import org.lara.interpreter.joptions.config.interpreter.LaraiKeys;
import org.lara.interpreter.joptions.config.interpreter.VerboseLevel;
import org.lara.interpreter.joptions.keys.FileList;
import org.suikasoft.jOptions.JOptionsUtils;
import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.app.App;
import org.suikasoft.jOptions.persistence.XmlPersistence;
import org.suikasoft.jOptions.storedefinition.StoreDefinition;

import larai.LaraI;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.tupatcher.TUPatcherConfig;
import pt.up.fe.specs.tupatcher.TUPatcherLauncher;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.specs.util.utilities.StringList;

public class CppScannerLauncher {

    public static void main(String[] args) {
        SpecsSystem.programStandardInit();

        var definition = StoreDefinition.newInstanceFromInterface(CppScannerOptions.class);
        var app = App.newInstance("CppScanner", definition, new XmlPersistence(definition),
                CppScannerLauncher::execute);

        JOptionsUtils.executeApp(app, Arrays.asList(args));
    }

    private static int execute(DataStore options) {

        var paths = options.get(CppScannerOptions.PATHS);

        // var sources = SpecsIo.getFiles(paths.getFiles(), true, Arrays.asList("c", "cpp"));
        var sources = paths.getFiles();

        System.out.println("Sources: " + sources.size());

        var patchedFolders = new ArrayList<File>();

        for (var source : sources) {
            SpecsLogs.info("Scanning " + source.getAbsolutePath());

            var outputFolder = SpecsIo.getTempFolder("cppscanner_" + UUID.randomUUID());
            patchedFolders.add(outputFolder);

            var tuConfig = DataStore.newInstance(TUPatcherConfig.getDefinition());
            tuConfig.put(TUPatcherConfig.SOURCE_PATHS,
                    StringList.newInstance(SpecsIo.normalizePath(source.getAbsoluteFile())));
            tuConfig.put(TUPatcherConfig.OUTPUT_FOLDER, outputFolder.getAbsoluteFile());
            tuConfig.put(TUPatcherConfig.PARALLEL, false);
            tuConfig.put(TUPatcherConfig.MAX_FILES, 30000);

            new TUPatcherLauncher(new TUPatcherConfig(tuConfig)).execute();

        }

        System.out.println("Patched folders: " + patchedFolders);

        for (var patchedFolder : patchedFolders) {

            var script = SpecsIo.getResource("pt/up/fe/specs/cppscanner/scanner.js");
            var scriptFile = SpecsIo.getTempFile("test_adasd", "js");
            SpecsIo.write(scriptFile, script);

            var clavaConfig = DataStore.newInstance(CxxWeaver.getWeaverDefinition()); // change

            clavaConfig.put(LaraiKeys.WORKSPACE_FOLDER, FileList.newInstance(patchedFolder));
            clavaConfig.put(LaraiKeys.LARA_FILE, scriptFile);
            clavaConfig.put(LaraiKeys.VERBOSE, VerboseLevel.none);
            clavaConfig.put(LaraiKeys.EXTERNAL_DEPENDENCIES,
                    StringList.newInstance("https://github.com/Calmskyy/Clava-Analysis.git"));

            LaraI.exec(clavaConfig, new CxxWeaver());

            // ClavaWeaverLauncher
            // .execute(Arrays.asList(scriptFile.getAbsolutePath(), "-b", "2", "-av",
            // patchedFolder.getAbsolutePath(), "-dep",
            // "https://github.com/Calmskyy/Clava-Analysis.git"));

        }

        return 0;

    }

}
