package com.moulberry.axiom.mixinplugin;

import com.moulberry.axiom.Axiom;
import com.moulberry.axiom.utils.Authorization;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforgespi.language.IModInfo;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Mixin config plugin. Ported from the Fabric build: MixinExtras is provided/initialised by NeoForge,
 * and the mixinconstraints dependency is replaced by an inline evaluator that reads the recreated
 * {@code @IfModLoaded}/{@code @IfModAbsent} class annotations (via ASM) and checks NeoForge's
 * {@link LoadingModList}. (Method-level conditions are a runtime refinement handled per-mixin.)
 */
public class AxiomMixinPlugin implements IMixinConfigPlugin {

    private static final String IF_MOD_LOADED = "Lcom/moulberry/mixinconstraints/annotations/IfModLoaded;";
    private static final String IF_MOD_ABSENT = "Lcom/moulberry/mixinconstraints/annotations/IfModAbsent;";

    private String mixinPackage;

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (this.mixinPackage != null && !mixinClassName.startsWith(this.mixinPackage)) {
            return true;
        }
        return evaluateClassConditions(mixinClassName);
    }

    private boolean evaluateClassConditions(String mixinClassName) {
        String resource = "/" + mixinClassName.replace('.', '/') + ".class";
        try (InputStream is = AxiomMixinPlugin.class.getResourceAsStream(resource)) {
            if (is == null) {
                return true;
            }
            ClassReader reader = new ClassReader(is);
            boolean[] apply = {true};
            reader.accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    if (IF_MOD_LOADED.equals(descriptor)) {
                        return conditionVisitor(true, apply);
                    } else if (IF_MOD_ABSENT.equals(descriptor)) {
                        return conditionVisitor(false, apply);
                    }
                    return null;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return apply[0];
        } catch (Exception e) {
            return true;
        }
    }

    private AnnotationVisitor conditionVisitor(boolean requireLoaded, boolean[] apply) {
        return new AnnotationVisitor(Opcodes.ASM9) {
            String value;
            String minVersion = "";
            String maxVersion = "";
            final List<String> aliases = new ArrayList<>();

            @Override
            public void visit(String name, Object v) {
                if ("value".equals(name)) {
                    value = (String) v;
                } else if ("minVersion".equals(name)) {
                    minVersion = (String) v;
                } else if ("maxVersion".equals(name)) {
                    maxVersion = (String) v;
                }
            }

            @Override
            public AnnotationVisitor visitArray(String name) {
                if ("aliases".equals(name)) {
                    return new AnnotationVisitor(Opcodes.ASM9) {
                        @Override
                        public void visit(String n, Object v) {
                            aliases.add((String) v);
                        }
                    };
                }
                return null;
            }

            @Override
            public void visitEnd() {
                boolean loaded = isLoaded(value, minVersion, maxVersion);
                for (String alias : aliases) {
                    loaded = loaded || isLoaded(alias, minVersion, maxVersion);
                }
                if (requireLoaded != loaded) {
                    apply[0] = false;
                }
            }
        };
    }

    private static boolean isLoaded(String modId, String minVersion, String maxVersion) {
        LoadingModList list = LoadingModList.get();
        if (list == null || modId == null) {
            return false;
        }
        for (IModInfo mod : list.getMods()) {
            if (mod.getModId().equals(modId)) {
                ArtifactVersion version = mod.getVersion();
                if (!minVersion.isEmpty() && version.compareTo(new DefaultArtifactVersion(minVersion)) < 0) {
                    return false;
                }
                if (!maxVersion.isEmpty() && version.compareTo(new DefaultArtifactVersion(maxVersion)) > 0) {
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void onLoad(String mixinPackage) {
        this.mixinPackage = mixinPackage;
        Axiom.LOGGER.info("Loading Mixin Plugin for " + Authorization.getUserAgent());
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
