package us.deathmarine.luyten;

import com.strobel.assembler.InputTypeLoader;
import com.strobel.assembler.metadata.Buffer;
import com.strobel.assembler.metadata.ITypeLoader;
import java.util.ArrayList;
import java.util.List;

public final class LuytenTypeLoader implements ITypeLoader {

    private final List<ITypeLoader> typeLoaders;

    public LuytenTypeLoader() {
        typeLoaders = new ArrayList<>();
        typeLoaders.add(new InputTypeLoader());
    }

    public List<ITypeLoader> getTypeLoaders() {
        return typeLoaders;
    }

    @Override
    public boolean tryLoadType(final String internalName, final Buffer buffer) {
        for (final ITypeLoader typeLoader : typeLoaders) {
            if (typeLoader.tryLoadType(internalName, buffer)) {
                return true;
            }

            buffer.reset();
        }

        return false;
    }

}
