package us.deathmarine.luyten;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarEntryFilter {

    private final JarFile jarFile;

    public JarEntryFilter() {
        this(null);
    }

    public JarEntryFilter(JarFile jarFile) {
        this.jarFile = jarFile;
    }

    public List<String> getAllEntriesFromJar() {
        List<String> mass = new ArrayList<>();
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry e = entries.nextElement();
            if (!e.isDirectory()) {
                mass.add(e.getName());
            }
        }
        return mass;
    }

    public List<String> getEntriesWithoutInnerClasses() {
        List<String> mass = new ArrayList<>();
        Enumeration<JarEntry> entries = jarFile.entries();
        Set<String> possibleInnerClasses = new HashSet<>();
        Set<String> baseClasses = new HashSet<>();

        while (entries.hasMoreElements()) {
            JarEntry e = entries.nextElement();
            if (!e.isDirectory()) {
                String entryName = e.getName();

                if (!entryName.trim().isEmpty()) {
                    entryName = entryName.trim();

                    if (!entryName.endsWith(".class")) {
                        mass.add(entryName);

                        // com/acme/Model$16.class
                    } else if (entryName.matches(".*[^(/|\\\\)]+\\$[^(/|\\\\)]+$")) {
                        possibleInnerClasses.add(entryName);

                    } else {
                        baseClasses.add(entryName);
                        mass.add(entryName);
                    }
                }
            }
        }

        // keep Badly$Named but not inner classes
        for (String inner : possibleInnerClasses) {

            // com/acme/Connection$Conn$1.class -> com/acme/Connection
            String innerWithoutTail = inner.replaceAll("\\$[^(/|\\\\)]+\\.class$", "");
            if (!baseClasses.contains(innerWithoutTail + ".class")) {
                mass.add(inner);
            }
        }
        return mass;
    }

}
