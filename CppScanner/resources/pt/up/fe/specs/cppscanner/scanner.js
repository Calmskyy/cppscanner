laraImport("weaver.Query")
laraImport("weaver.WeaverOptions")
laraImport("lara.Io")
laraImport("clava.analysis.analysers.BoundsAnalyserNew")
laraImport("clava.analysis.analysers.DoubleFreeAnalyserNew")
laraImport("clava.Clava")
laraImport("clava.analysis_new.analysers.CWE.AnalyserCWE457");
laraImport("clava.analysis_new.analysers.CWE.Analysers");
laraImport("clava.analysis_new.analysers.Scanner.ScanCode");
laraImport("clava.analysis_new.analysers.VariablesManager.VariableStorage");

var exceptions = 0

for (const $jp of Query.search("file")) {
    try {
        if ($jp.name.slice(-1) == "c") {
            var message = "";

            for (const f of Query.searchFrom($jp, "function")) {
                var analyser2 = new BoundsAnalyserNew()
                var result2 = analyser2.analyse(f)
                if (result2 !== undefined) {
                    message += $jp.name + ",CWE120\n"
                }
            }

            for (const f of Query.searchFrom($jp, "function")) {
                var analyser3 = new DoubleFreeAnalyserNew()
                var result3 = analyser3.analyse(f)
                if (result3 !== undefined) {
                    message += $jp.name + ",CWE415\n"
                }
            }
            if (message != "") {
                var analysisFileName = Io.getPath(Clava.getData().getContextFolder(), "/AnalysisReports/" + $jp.name + "_old-report.txt");
                Io.writeFile(analysisFileName, message);
            }

            let scanner = new ScanCode()
            VariableStorage.clear()
            scanner.launch($jp);

            for (const varia of VariableStorage.getTabOfPointer()) {

                analysers = new Analysers()
                analysers.addAnlayser(new AnalyserCWE457())

                varia.goThroughGraph(analysers)
                analysers.result()
            }
            MessageGenerator.generateReport($jp.name)

        }
    } catch (e) {
        println("Exception")
        exceptions++;
        continue;
    }
}
println("Exceptions: " + exceptions)