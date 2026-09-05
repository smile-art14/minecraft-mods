import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class PatchGcaResident {
    private static final String EXTENSION = "dev/dubhe/gugle/carpet/GcaExtension.class";
    private static final String SETTING = "dev/dubhe/gugle/carpet/GcaSetting.class";

    private PatchGcaResident() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: PatchGcaResident <input.jar> <output.jar>");
        }

        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        boolean extensionPatched = false;
        boolean settingPatched = false;
        try (ZipFile source = new ZipFile(input.toFile());
             OutputStream fileOut = Files.newOutputStream(output);
             ZipOutputStream target = new ZipOutputStream(fileOut)) {
            Enumeration<? extends ZipEntry> entries = source.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                ZipEntry replacement = new ZipEntry(entry.getName());
                replacement.setTime(entry.getTime());
                target.putNextEntry(replacement);
                if (!entry.isDirectory()) {
                    byte[] bytes;
                    try (InputStream in = source.getInputStream(entry)) {
                        bytes = in.readAllBytes();
                    }
                    if (EXTENSION.equals(entry.getName())) {
                        bytes = patchExtension(bytes);
                        extensionPatched = true;
                    } else if (SETTING.equals(entry.getName())) {
                        bytes = patchSettingDefaults(bytes);
                        settingPatched = true;
                    }
                    target.write(bytes);
                }
                target.closeEntry();
            }
        }
        if (!extensionPatched || !settingPatched) {
            Files.deleteIfExists(output);
            throw new IllegalStateException(
                "Required classes were not found: extension=" + extensionPatched
                    + ", setting=" + settingPatched);
        }
    }

    private static byte[] patchExtension(byte[] original) {
        ClassNode node = new ClassNode();
        new ClassReader(original).accept(node, 0);
        MethodNode method = node.methods.stream()
            .filter(m -> m.name.equals("onServerClosed")
                && m.desc.equals("(Lnet/minecraft/server/MinecraftServer;)V"))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("GcaExtension.onServerClosed not found"));

        LabelNode loopHead = null;
        LabelNode loopExit = null;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                && call.owner.equals("java/util/Iterator")
                && call.name.equals("hasNext")
                && call.desc.equals("()Z")) {
                AbstractInsnNode cursor = instruction.getPrevious();
                while (cursor != null && !(cursor instanceof LabelNode)) {
                    cursor = cursor.getPrevious();
                }
                loopHead = (LabelNode) cursor;
                if (instruction.getNext() instanceof JumpInsnNode branch
                    && branch.getOpcode() == Opcodes.IFEQ) {
                    loopExit = branch.label;
                }
                break;
            }
        }
        if (loopHead == null || loopExit == null) {
            throw new IllegalStateException("Resident save loop was not recognized");
        }

        int replaced = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction == loopExit) {
                break;
            }
            if (instruction instanceof InsnNode && instruction.getOpcode() == Opcodes.RETURN) {
                method.instructions.set(instruction, new JumpInsnNode(Opcodes.GOTO, loopHead));
                replaced++;
            }
        }
        if (replaced != 2) {
            throw new IllegalStateException("Expected two early returns, replaced " + replaced);
        }

        MethodInsnNode writerPathCall = null;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                && call.owner.equals("java/io/File")
                && call.name.equals("toPath")
                && call.desc.equals("()Ljava/nio/file/Path;")) {
                writerPathCall = call;
                break;
            }
        }
        if (writerPathCall == null
            || !(writerPathCall.getPrevious() instanceof VarInsnNode fileLoad)
            || fileLoad.getOpcode() != Opcodes.ALOAD) {
            throw new IllegalStateException("Resident output file write was not recognized");
        }
        LabelNode writeFile = new LabelNode();
        InsnList preserveEarlierSave = new InsnList();
        preserveEarlierSave.add(new VarInsnNode(Opcodes.ALOAD, 2));
        preserveEarlierSave.add(new MethodInsnNode(
            Opcodes.INVOKEVIRTUAL,
            "com/google/gson/JsonObject",
            "size",
            "()I",
            false));
        preserveEarlierSave.add(new JumpInsnNode(Opcodes.IFNE, writeFile));
        preserveEarlierSave.add(new VarInsnNode(Opcodes.ALOAD, fileLoad.var));
        preserveEarlierSave.add(new MethodInsnNode(
            Opcodes.INVOKEVIRTUAL,
            "java/io/File",
            "isFile",
            "()Z",
            false));
        preserveEarlierSave.add(new JumpInsnNode(Opcodes.IFEQ, writeFile));
        preserveEarlierSave.add(new InsnNode(Opcodes.RETURN));
        preserveEarlierSave.add(writeFile);
        method.instructions.insertBefore(fileLoad, preserveEarlierSave);

        if (node.methods.stream().anyMatch(m -> m.name.equals("onPlayerLoggedOut")
            && m.desc.equals("(Lnet/minecraft/class_3222;)V"))) {
            throw new IllegalStateException("GcaExtension.onPlayerLoggedOut already exists");
        }
        MethodNode logout = new MethodNode(
            Opcodes.ACC_PUBLIC,
            "onPlayerLoggedOut",
            "(Lnet/minecraft/class_3222;)V",
            null,
            null);
        LabelNode logoutEnd = new LabelNode();
        logout.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        logout.instructions.add(new TypeInsnNode(
            Opcodes.INSTANCEOF,
            "carpet/patches/EntityPlayerMPFake"));
        logout.instructions.add(new JumpInsnNode(Opcodes.IFNE, logoutEnd));
        logout.instructions.add(new FieldInsnNode(
            Opcodes.GETSTATIC,
            "dev/dubhe/gugle/carpet/GcaSetting",
            "fakePlayerResident",
            "Z"));
        logout.instructions.add(new JumpInsnNode(Opcodes.IFEQ, logoutEnd));
        logout.instructions.add(new FieldInsnNode(
            Opcodes.GETSTATIC,
            "carpet/CarpetServer",
            "minecraft_server",
            "Lnet/minecraft/server/MinecraftServer;"));
        logout.instructions.add(new JumpInsnNode(Opcodes.IFNULL, logoutEnd));
        logout.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        logout.instructions.add(new FieldInsnNode(
            Opcodes.GETSTATIC,
            "carpet/CarpetServer",
            "minecraft_server",
            "Lnet/minecraft/server/MinecraftServer;"));
        logout.instructions.add(new MethodInsnNode(
            Opcodes.INVOKEVIRTUAL,
            "dev/dubhe/gugle/carpet/GcaExtension",
            "onServerClosed",
            "(Lnet/minecraft/server/MinecraftServer;)V",
            false));
        logout.instructions.add(logoutEnd);
        logout.instructions.add(new InsnNode(Opcodes.RETURN));
        node.methods.add(logout);

        MethodNode loadWorlds = node.methods.stream()
            .filter(m -> m.name.equals("onServerLoadedWorlds")
                && m.desc.equals("(Lnet/minecraft/server/MinecraftServer;)V"))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "GcaExtension.onServerLoadedWorlds not found"));
        InsnList forceResidentRules = new InsnList();
        forceResidentRules.add(new InsnNode(Opcodes.ICONST_1));
        forceResidentRules.add(new FieldInsnNode(
            Opcodes.PUTSTATIC,
            "dev/dubhe/gugle/carpet/GcaSetting",
            "fakePlayerResident",
            "Z"));
        forceResidentRules.add(new InsnNode(Opcodes.ICONST_1));
        forceResidentRules.add(new FieldInsnNode(
            Opcodes.PUTSTATIC,
            "dev/dubhe/gugle/carpet/GcaSetting",
            "fakePlayerReloadAction",
            "Z"));
        loadWorlds.instructions.insert(forceResidentRules);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static byte[] patchSettingDefaults(byte[] original) {
        ClassNode node = new ClassNode();
        new ClassReader(original).accept(node, 0);
        MethodNode initializer = node.methods.stream()
            .filter(m -> m.name.equals("<clinit>") && m.desc.equals("()V"))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("GcaSetting.<clinit> not found"));

        int replaced = 0;
        for (AbstractInsnNode instruction : initializer.instructions.toArray()) {
            if (instruction instanceof FieldInsnNode field
                && field.getOpcode() == Opcodes.PUTSTATIC
                && field.owner.equals("dev/dubhe/gugle/carpet/GcaSetting")
                && field.desc.equals("Z")
                && (field.name.equals("fakePlayerResident")
                    || field.name.equals("fakePlayerReloadAction"))) {
                AbstractInsnNode value = instruction.getPrevious();
                if (value == null || value.getOpcode() != Opcodes.ICONST_0) {
                    throw new IllegalStateException(
                        "Expected false initializer for " + field.name);
                }
                initializer.instructions.set(value, new InsnNode(Opcodes.ICONST_1));
                replaced++;
            }
        }
        if (replaced != 2) {
            throw new IllegalStateException(
                "Expected two resident defaults, replaced " + replaced);
        }

        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }
}
