package us.deathmarine.luyten;

import java.io.File;
import java.util.ArrayList;
import java.util.prefs.Preferences;

public class RecentFiles {

	public static ArrayList<String> paths = new ArrayList<>();
	private static Preferences prefs = Preferences.userNodeForPackage(RecentFiles.class);
	
	public static int load() {
		boolean saveNeeded = false;
		
		String serializedPaths = prefs.get("recentFiles", null);
		
		if (serializedPaths == null) return 0;
		
		// "test","asdf.txt","text2"
		for (String path : serializedPaths.split("\",\"")) {

			path = path.replace("\"", "");
			
			if (!path.trim().isEmpty() && !new File(path).exists()) saveNeeded = true;
			else paths.add(path);
		}
		
		if (saveNeeded) save();
		
		return paths.size();
	}
	
	public static void add(String path) {
		if (paths.contains(path)) {
			paths.remove(path);
			paths.add(path);
			return;
		}
		
		if (paths.size() >= 10) paths.remove(0);
		paths.add(path);
		
		save();
	}
	
	public static void save() {
		if (paths.size() == 0) {
			prefs.put("recentFiles", "");
			return;
		}
		
		StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i < paths.size(); i++) {
			if (i != 0) sb.append(',');
			
			/*if (!new File(paths.get(i)).exists()) {
				paths.remove(i);
				continue;
			}*/
			sb.append("\"").append(paths.get(i)).append("\"");
		}
		
		prefs.put("recentFiles", sb.toString());
	}
}