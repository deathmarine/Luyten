package us.deathmarine.luyten;

import java.io.File;

public final class LuytenCLI {

    private LuytenCLI() {
        throw new UnsupportedOperationException();
    }

    /**
     * Execute the CLI version of Luyten using the given arguments.
     *
     * @param input  The argument given by {@code input} in the command line.
     * @param output The argument given by {@code output} in the command line.
     * @return {@code true} if the CLI was executed successfully, {@code false} otherwise.
     */
    public static boolean execute(File input, File output) {
        if (input != null && output != null) {
            FileSaver fileSaver = new FileSaver(null, null);
            fileSaver.saveAllDecompiledCLI(input, output);
            return true;
        } else if (input == null && output != null) {
            System.err.println("Input file not specified!");
            return true;
        } else if (input != null) {
            System.err.println("Output file not specified!");
            return true;
        }
        return false;
    }

}
