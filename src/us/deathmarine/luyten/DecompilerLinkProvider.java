package us.deathmarine.luyten;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.strobel.assembler.metadata.FieldDefinition;
import com.strobel.assembler.metadata.FieldReference;
import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.assembler.metadata.MethodDefinition;
import com.strobel.assembler.metadata.MethodReference;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.core.StringUtilities;
import com.strobel.decompiler.DecompilationOptions;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;

public class DecompilerLinkProvider implements LinkProvider {

	private Map<String, Selection> definitionToSelectionMap = new HashMap<>();
	private Map<String, Set<Selection>> referenceToSelectionsMap = new HashMap<>();
	private boolean isSelectionMapsPopulated = false;

	private MetadataSystem metadataSystem;
	private DecompilerSettings settings;
	private DecompilationOptions decompilationOptions;
	private TypeDefinition type;

	private String currentTypeQualifiedName;
	private String textContent = "";

	@Override
	public void generateContent() {
		definitionToSelectionMap = new HashMap<>();
		referenceToSelectionsMap = new HashMap<>();
		currentTypeQualifiedName = type.getPackageName() + "." + type.getName();
		final StringWriter stringwriter = new StringWriter();
		PlainTextOutput plainTextOutput = new PlainTextOutput(stringwriter) {
			@Override
			public void writeDefinition(String text, Object definition, boolean isLocal) {
				super.writeDefinition(text, definition, isLocal);
				try {
					if (text != null && definition != null) {
						String uniqueStr = createUniqueStrForReference(definition);
						if (uniqueStr != null) {
							// fix link's underline length: _java.util.HashSet_
							// -> _HashSet_
							text = text.replaceAll("[^\\.]*\\.", "");
							int from = stringwriter.getBuffer().length() - text.length();
							int to = stringwriter.getBuffer().length();
							definitionToSelectionMap.put(uniqueStr, new Selection(from, to));
						}
					}
				} catch (Exception e) {
					Luyten.showExceptionDialog("Exception!", e);
				}
			}

			@Override
			public void writeReference(String text, Object reference, boolean isLocal) {
				super.writeReference(text, reference, isLocal);
				try {
					if (text != null && reference != null) {
						String uniqueStr = createUniqueStrForReference(reference);
						if (uniqueStr != null) {
							text = text.replaceAll("[^\\.]*\\.", "");
							int from = stringwriter.getBuffer().length() - text.length();
							int to = stringwriter.getBuffer().length();
							if (reference instanceof FieldReference) {
								// fix enum definition links (note: could not fix enum reference links)
								if (((FieldReference) reference).isDefinition()) {
									definitionToSelectionMap.put(uniqueStr, new Selection(from, to));
									return;
								}
							}
							if (referenceToSelectionsMap.containsKey(uniqueStr)) {
								Set<Selection> selectionsSet = referenceToSelectionsMap.get(uniqueStr);
								if (selectionsSet != null) {
									selectionsSet.add(new Selection(from, to));
								}
							} else {
								Set<Selection> selectionsSet = new HashSet<>();
								selectionsSet.add(new Selection(from, to));
								referenceToSelectionsMap.put(uniqueStr, selectionsSet);
							}
						}
					}
				} catch (Exception e) {
					Luyten.showExceptionDialog("Exception!", e);
				}
			}
		};
		plainTextOutput.setUnicodeOutputEnabled(decompilationOptions.getSettings().isUnicodeOutputEnabled());
		settings.getLanguage().decompileType(type, plainTextOutput, decompilationOptions);
		textContent = stringwriter.toString();
		isSelectionMapsPopulated = true;
	}

	private String createUniqueStrForReference(Object reference) {
		String uniqueStr = null;
		if (reference instanceof TypeReference) {
			TypeReference type = (TypeReference) reference;
			String pathAndTypeStr = getPathAndTypeStr(type);
			if (pathAndTypeStr != null) {
				uniqueStr = "type|" + pathAndTypeStr;
			}
		} else if (reference instanceof MethodReference) {
			MethodReference method = (MethodReference) reference;
			String pathAndTypeStr = getPathAndTypeStr(method.getDeclaringType());
			if (pathAndTypeStr != null) {
				uniqueStr = "method|" + pathAndTypeStr + "|" + method.getName() + "|" + method.getErasedSignature();
			}
		} else if (reference instanceof FieldReference) {
			FieldReference field = (FieldReference) reference;
			String pathAndTypeStr = getPathAndTypeStr(field.getDeclaringType());
			if (pathAndTypeStr != null) {
				uniqueStr = "field|" + pathAndTypeStr + "|" + field.getName();
			}
		}
		return uniqueStr;
	}

	private String getPathAndTypeStr(TypeReference typeRef) {
		String name = typeRef.getName();
		String packageStr = typeRef.getPackageName();
		TypeReference mostOuterTypeRef = getMostOuterTypeRef(typeRef);
		String mostOuterTypeName = mostOuterTypeRef.getName();
		if (name != null && packageStr != null && mostOuterTypeName != null && name.trim().length() > 0
				&& mostOuterTypeName.trim().length() > 0) {
			String pathStr = packageStr.replaceAll("\\.", "/") + "/" + mostOuterTypeName;
			String typeStr = packageStr + "." + name.replace(".", "$");
			return pathStr + "|" + typeStr;
		}
		return null;
	}

	private TypeReference getMostOuterTypeRef(TypeReference typeRef) {
		int maxDecraringDepth = typeRef.getFullName().split("(\\.|\\$)").length;
		for (int i = 0; i < maxDecraringDepth; i++) {
			TypeReference declaringTypeRef = typeRef.getDeclaringType();
			if (declaringTypeRef == null) {
				break;
			} else {
				typeRef = declaringTypeRef;
			}
		}
		if (typeRef.getName().contains("$")) {
			return getMostOuterTypeRefBySlowLookuping(typeRef);
		}
		return typeRef;
	}

	private TypeReference getMostOuterTypeRefBySlowLookuping(TypeReference typeRef) {
		String name = typeRef.getName();
		if (name == null)
			return typeRef;
		String packageName = typeRef.getPackageName();
		if (packageName == null)
			return typeRef;
		String[] nameParts = name.split("\\$");
		String newName = "";
		String sep = "";
		for (int i = 0; i < nameParts.length - 1; i++) {
			newName = newName + sep + nameParts[i];
			sep = "$";
			String newInternalName = packageName.replaceAll("\\.", "/") + "/" + newName;
			TypeReference newTypeRef = metadataSystem.lookupType(newInternalName);
			if (newTypeRef != null) {
				TypeDefinition newTypeDef = newTypeRef.resolve();
				if (newTypeDef != null) {
					return newTypeRef;
				}
			}
		}
		return typeRef;
	}

	@Override
	public String getTextContent() {
		return textContent;
	}

	@Override
	public void processLinks() {
	}

	@Override
	public Map<String, Selection> getDefinitionToSelectionMap() {
		return definitionToSelectionMap;
	}

	@Override
	public Map<String, Set<Selection>> getReferenceToSelectionsMap() {
		return referenceToSelectionsMap;
	}

	@Override
	public boolean isLinkNavigable(String uniqueStr) {
		if (isSelectionMapsPopulated && definitionToSelectionMap.containsKey(uniqueStr))
			return true;
		if (uniqueStr == null)
			return false;
		String[] linkParts = uniqueStr.split("\\|");
		if (linkParts.length < 3)
			return false;
		String typeStr = linkParts[2];
		if (typeStr.trim().length() <= 0)
			return false;
		TypeReference typeRef = metadataSystem.lookupType(typeStr.replaceAll("\\.", "/"));
		if (typeRef == null)
			return false;
		TypeDefinition typeDef = typeRef.resolve();
		if (typeDef == null)
			return false;
		if (typeDef.isSynthetic())
			return false;

		if (isSelectionMapsPopulated) {
			// current type's navigable definitions checked already, now it's erroneous
			if (currentTypeQualifiedName == null || currentTypeQualifiedName.trim().length() <= 0)
				return false;
			if (typeStr.equals(currentTypeQualifiedName) || typeStr.startsWith(currentTypeQualifiedName + ".")
					|| typeStr.startsWith(currentTypeQualifiedName + "$"))
				return false;
		}

		// check linked field/method exists
		if (uniqueStr.startsWith("method")) {
			if (findMethodInType(typeDef, uniqueStr) == null) {
				return false;
			}
		} else if (uniqueStr.startsWith("field")) {
			if (findFieldInType(typeDef, uniqueStr) == null) {
				return false;
			}
		}
		return true;
	}

	private MethodDefinition findMethodInType(TypeDefinition typeDef, String uniqueStr) {
		String[] linkParts = uniqueStr.split("\\|");
		if (linkParts.length != 5)
			return null;
		String methodName = linkParts[3];
		String methodErasedSignature = linkParts[4];
		if (methodName.trim().length() <= 0 || methodErasedSignature.trim().length() <= 0)
			return null;
		List<MethodDefinition> declaredMethods = typeDef.getDeclaredMethods();
		if (declaredMethods == null)
			return null;
		boolean isFound = false;
		for (MethodDefinition declaredMethod : declaredMethods) {
			isFound = (declaredMethod != null && methodName.equals(declaredMethod.getName()));
			isFound = (isFound && methodErasedSignature.equals(declaredMethod.getErasedSignature()));
			if (isFound) {
				if (declaredMethod.isSynthetic() && !settings.getShowSyntheticMembers()) {
					return null;
				} else {
					return declaredMethod;
				}
			}
		}
		return null;
	}

	private FieldDefinition findFieldInType(TypeDefinition typeDef, String uniqueStr) {
		String[] linkParts = uniqueStr.split("\\|");
		if (linkParts.length != 4)
			return null;
		String fieldName = linkParts[3];
		if (fieldName.trim().length() <= 0)
			return null;
		List<FieldDefinition> declaredFields = typeDef.getDeclaredFields();
		if (declaredFields == null)
			return null;
		boolean isFound = false;
		for (FieldDefinition declaredField : declaredFields) {
			isFound = (declaredField != null && fieldName.equals(declaredField.getName()));
			if (isFound) {
				if (declaredField.isSynthetic()) {
					return null;
				} else {
					return declaredField;
				}
			}
		}
		return null;
	}

	@Override
	public String getLinkDescription(String uniqueStr) {
		String readableLink = null;
		try {
			if (uniqueStr == null)
				return null;
			String[] linkParts = uniqueStr.split("\\|");
			if (linkParts.length < 3)
				return null;
			String typeStr = linkParts[2];
			TypeReference typeRef = metadataSystem.lookupType(typeStr.replaceAll("\\.", "/"));
			if (typeRef == null)
				return null;
			TypeDefinition typeDef = typeRef.resolve();
			if (typeDef == null)
				return null;

			String declaredSuffix = "";
			String mostOuterTypeStr = linkParts[1].replaceAll("/", ".");
			boolean isOwnFile = mostOuterTypeStr.equals(currentTypeQualifiedName);
			if (!isOwnFile) {
				declaredSuffix = " - Declared: " + mostOuterTypeStr;
			}

			if (uniqueStr.startsWith("type")) {
				String desc = typeDef.getBriefDescription();
				if (desc != null && desc.trim().length() > 0) {
					readableLink = desc;
				}
			} else if (uniqueStr.startsWith("method")) {
				MethodDefinition methodDef = findMethodInType(typeDef, uniqueStr);
				if (methodDef == null)
					return null;
				String desc = methodDef.getBriefDescription();
				if (desc != null && desc.trim().length() > 0) {

					if (desc.contains("void <init>")) {
						String constructorName = typeDef.getName();
						TypeReference declaringTypeRef = typeRef.getDeclaringType();
						if (declaringTypeRef != null) {
							TypeDefinition declaringTypeDef = declaringTypeRef.resolve();
							if (declaringTypeDef != null) {
								String declaringTypeName = declaringTypeDef.getName();
								if (declaringTypeName != null) {
									constructorName = StringUtilities.removeLeft(constructorName, declaringTypeName);
									constructorName = constructorName.replaceAll("^(\\.|\\$)", "");
								}
							}
						}
						desc = desc.replace("void <init>", constructorName);
						readableLink = "Constructor: " + erasePackageInfoFromDesc(desc) + declaredSuffix;
					} else {
						readableLink = erasePackageInfoFromDesc(desc) + declaredSuffix;
					}
				}
			} else if (uniqueStr.startsWith("field")) {
				FieldDefinition fieldDef = findFieldInType(typeDef, uniqueStr);
				if (fieldDef == null)
					return null;
				String desc = fieldDef.getBriefDescription();
				if (desc != null && desc.trim().length() > 0) {
					readableLink = erasePackageInfoFromDesc(desc) + declaredSuffix;
				}

			}
			if (readableLink != null) {
				readableLink = readableLink.replace("$", ".");
			}
		} catch (Exception e) {
			readableLink = null;
			Luyten.showExceptionDialog("Exception!", e);
		}
		return readableLink;
	}

	private String erasePackageInfoFromDesc(String desc) {
		String limiters = "\\(\\)\\<\\>\\[\\]\\?\\s,";
		desc = desc.replaceAll("(?<=[^" + limiters + "]*)([^" + limiters + "]*)\\.", "");
		return desc;
	}

	public void setDecompilerReferences(MetadataSystem metadataSystem, DecompilerSettings settings,
			DecompilationOptions decompilationOptions) {
		this.metadataSystem = metadataSystem;
		this.settings = settings;
		this.decompilationOptions = decompilationOptions;
	}

	public void setType(TypeDefinition type) {
		this.type = type;
	}
}
