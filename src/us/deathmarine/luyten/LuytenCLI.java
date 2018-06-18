package us.deathmarine.luyten;

import java.io.File;
import java.util.Map;

import net.sourceforge.argparse4j.inf.Argument;
import net.sourceforge.argparse4j.inf.ArgumentAction;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;


public class LuytenCLI {

	
	public static class OpenAndExtractAction implements ArgumentAction {

	    @Override
	    public void run(ArgumentParser parser, Argument arg,
	            Map<String, Object> attrs, String flag, Object value)
	            throws ArgumentParserException {
	    	System.out.printf("FooAction");
	        System.out.printf("%s '%s' %s\n", attrs, value, flag);
	        attrs.put(arg.getDest(), value);
	        String inFileName = (String)attrs.get("file");
	        File inFile=new File(inFileName);
	        System.out.printf("extracto to %s \n",value);
	        File outFile=new File((String)value);
	        FileSaver fileSaver=new FileSaver(null, null);
	        fileSaver.saveAllDecompiledCli(inFile, outFile);
	        
	    }

	    @Override
	    public void onAttach(Argument arg) {
	    }

	    @Override
	    public boolean consumeArgument() {
	        return true;
	    }
	}
}
