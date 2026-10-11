package classicmining;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Exact, fail-closed patch for the GregTech version shipped in GTNH 2.9.0-RC-2. */
public final class OreTransformer implements IClassTransformer {
    public static int patches;
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !"gregtech.common.GTWorldgenerator$WorldGenContainer".equals(transformedName)) return bytes;
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        int hooks = 0, gates = 0;
        for (MethodNode method : node.methods) {
            if (!method.name.equals("run") || !method.desc.equals("()V")) continue;
            int clocks = 0;
            for (AbstractInsnNode insn : method.instructions.toArray()) {
                if (!(insn instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) insn;
                if (call.owner.equals("java/lang/System") && call.name.equals("nanoTime") && ++clocks == 2) {
                    InsnList hook = new InsnList();
                    hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "classicmining/MiningHooks", "generate", "(Ljava/lang/Object;)V", false));
                    method.instructions.insertBefore(call, hook);
                    hooks++;
                }
                if (call.owner.equals("gregtech/common/GTWorldgenerator") && call.name.equals("isOreChunk") && call.desc.equals("(II)Z")) {
                    method.instructions.insertBefore(call, new VarInsnNode(Opcodes.ALOAD, 0));
                    call.owner = "classicmining/MiningHooks";
                    call.name = "useOriginalGrid";
                    call.desc = "(IILjava/lang/Object;)Z";
                    gates++;
                }
            }
        }
        if (hooks != 1 || gates != 1) throw new IllegalStateException("Classic Mining: incompatible GregTech world generator, hooks=" + hooks + " gates=" + gates);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        patches++;
        System.out.println("[ClassicMining] Patched real GregTech WorldGenContainer: one placement hook, one grid gate.");
        return writer.toByteArray();
    }
}
