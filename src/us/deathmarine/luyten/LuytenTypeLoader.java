package us.deathmarine.luyten;

import com.strobel.assembler.InputTypeLoader;
import com.strobel.assembler.metadata.Buffer;
import com.strobel.assembler.metadata.ITypeLoader;

import java.util.ArrayList;
import java.util.List;

public final class LuytenTypeLoader implements ITypeLoader {
	private final List<ITypeLoader> _typeLoaders;

	public LuytenTypeLoader() {
		_typeLoaders = new ArrayList<ITypeLoader>();
		_typeLoaders.add(new InputTypeLoader());
	}

	public final List<ITypeLoader> getTypeLoaders() {
		return _typeLoaders;
	}

	@Override
	public boolean tryLoadType(final String internalName, final Buffer buffer) {
		for (final ITypeLoader typeLoader : _typeLoaders) {
			if (typeLoader.tryLoadType(internalName, buffer)) {
				return true;
			}

			buffer.reset();
		}

		return false;
	}
}