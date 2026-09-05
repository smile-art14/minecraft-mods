import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class PatchCarpetFallDamage {
    private static final String ENTITY_MIXIN = "carpet/mixins/EntityMixin.class";
    private static final String FAKE_PLAYER = "carpet/patches/EntityPlayerMPFake.class";
    private static final String FAKE_PLAYER_INTERNAL = "carpet/patches/EntityPlayerMPFake";

    private PatchCarpetFallDamage() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: PatchCarpetFallDamage <input.jar> <output.jar>");
        }

        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        Set<String> patched = new HashSet<>();

        try (ZipFile source = new ZipFile(input.toFile());
             OutputStream fileOut = Files.newOutputStream(output);
             ZipOutputStream target = new ZipOutputStream(fileOut)) {
            Enumeration<? extends ZipEntry> entries = source.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                ZipEntry replacement = new ZipEntry(entry.getName());
                replacement.setTime(entry.getTime());
                if (entry.getComment() != null) {
                    replacement.setComment(entry.getComment());
                }
                target.putNextEntry(replacement);
                if (!entry.isDirectory()) {
                    byte[] bytes;
                    try (InputStream in = source.getInputStream(entry)) {
                        bytes = in.readAllBytes();
                    }
                    if (ENTITY_MIXIN.equals(entry.getName())) {
                        bytes = patchEntityMixin(bytes);
                        patched.add(ENTITY_MIXIN);
                    } else if (FAKE_PLAYER.equals(entry.getName())) {
                        bytes = patchFakePlayer(bytes);
                        patched.add(FAKE_PLAYER);
                    }
                    target.write(bytes);
                }
                target.closeEntry();
            }
        }

        if (!patched.equals(Set.of(ENTITY_MIXIN, FAKE_PLAYER))) {
            Files.deleteIfExists(output);
            throw new IOException("Expected classes were not both patched: " + patched);
        }
    }

    private static byte[] patchFakePlayer(byte[] original) {
        ClassNode node = read(original);
        MethodNode die = node.methods.stream()
            .filter(m -> m.name.equals("method_6078")
                && m.desc.equals("(Lnet/minecraft/class_1282;)V"))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("EntityPlayerMPFake.die not found"));
        MethodInsnNode vanillaDie = null;
        for (AbstractInsnNode instruction : die.instructions) {
            if (instruction instanceof MethodInsnNode call
                && call.getOpcode() == Opcodes.INVOKESPECIAL
                && call.owner.equals("net/minecraft/class_3222")
                && call.name.equals("method_6078")
                && call.desc.equals("(Lnet/minecraft/class_1282;)V")) {
                vanillaDie = call;
                break;
            }
        }
        if (vanillaDie == null) {
            throw new IllegalStateException("EntityPlayerMPFake super.die call not found");
        }
        InsnList clearExperience = new InsnList();
        clearExperience.add(new VarInsnNode(Opcodes.ALOAD, 0));
        clearExperience.add(new InsnNode(Opcodes.ICONST_0));
        clearExperience.add(new FieldInsnNode(
            Opcodes.PUTFIELD,
            "net/minecraft/class_1657",
            "field_7520",
            "I"
        ));
        clearExperience.add(new VarInsnNode(Opcodes.ALOAD, 0));
        clearExperience.add(new InsnNode(Opcodes.ICONST_0));
        clearExperience.add(new FieldInsnNode(
            Opcodes.PUTFIELD,
            "net/minecraft/class_1657",
            "field_7495",
            "I"
        ));
        clearExperience.add(new VarInsnNode(Opcodes.ALOAD, 0));
        clearExperience.add(new InsnNode(Opcodes.FCONST_0));
        clearExperience.add(new FieldInsnNode(
            Opcodes.PUTFIELD,
            "net/minecraft/class_1657",
            "field_7510",
            "F"
        ));
        die.instructions.insert(vanillaDie, clearExperience);

        node.fields.add(new FieldNode(
            Opcodes.ACC_PRIVATE,
            "carpet$fallFixDistance",
            "F",
            null,
            null
        ));

        boolean removedBrokenFallOverride = node.methods.removeIf(m ->
            m.name.equals("method_5623")
                && m.desc.equals("(DZLnet/minecraft/class_2680;Lnet/minecraft/class_2338;)V"));
        if (!removedBrokenFallOverride) {
            throw new IllegalStateException("EntityPlayerMPFake fall-damage override not found");
        }

        MethodNode move = new MethodNode(
            Opcodes.ACC_PUBLIC,
            "method_5784",
            "(Lnet/minecraft/class_1313;Lnet/minecraft/class_243;)V",
            null,
            null
        );
        move.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        move.instructions.add(new MethodInsnNode(
            Opcodes.INVOKEVIRTUAL,
            FAKE_PLAYER_INTERNAL,
            "method_23318",
            "()D",
            false
        ));
        move.instructions.add(new VarInsnNode(Opcodes.DSTORE, 3));
        move.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        move.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        move.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        move.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESPECIAL,
            "net/minecraft/class_3222",
            "method_5784",
            "(Lnet/minecraft/class_1313;Lnet/minecraft/class_243;)V",
            false
        ));
        move.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        move.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        move.instructions.add(new MethodInsnNode(
            Opcodes.INVOKEVIRTUAL,
            FAKE_PLAYER_INTERNAL,
            "method_23318",
            "()D",
            false
        ));
        move.instructions.add(new VarInsnNode(Opcodes.DLOAD, 3));
        move.instructions.add(new InsnNode(Opcodes.DSUB));
        move.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        move.instructions.add(new MethodInsnNode(
            Opcodes.INVOKEVIRTUAL,
            FAKE_PLAYER_INTERNAL,
            "method_24828",
            "()Z",
            false
        ));
        move.instructions.add(new MethodInsnNode(
            Opcodes.INVOKESPECIAL,
            FAKE_PLAYER_INTERNAL,
            "carpet$trackFall",
            "(DZ)V",
            false
        ));
        move.instructions.add(new InsnNode(Opcodes.RETURN));
        move.maxStack = 6;
        move.maxLocals = 5;
        node.methods.add(move);

        MethodNode fall = new MethodNode(
            Opcodes.ACC_PRIVATE,
            "carpet$trackFall",
            "(DZ)V",
            null,
            null
        );
        LabelNode airborne = new LabelNode();
        LabelNode done = new LabelNode();
        fall.instructions.add(new VarInsnNode(Opcodes.ILOAD, 3));
        fall.instructions.add(new JumpInsnNode(Opcodes.IFEQ, airborne));
        fall.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        fall.instructions.add(new FieldInsnNode(
            Opcodes.GETFIELD,
            FAKE_PLAYER_INTERNAL,
            "carpet$fallFixDistance",
            "F"
        ));
        fall.instructions.add(new InsnNode(Opcodes.FCONST_0));
        fall.instructions.add(new InsnNode(Opcodes.FCMPG));
        fall.instructions.add(new JumpInsnNode(Opcodes.IFLE, done));
        fall.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        fall.instructions.add(new MethodInsnNode(
            Opcodes.INVOKEVIRTUAL,
            FAKE_PLAYER_INTERNAL,
            "method_24515",
            "()Lnet/minecraft/class_2338;",
            false
        ));
        fall.instructions.add(new VarInsnNode(Opcodes.ASTORE, 4));
        fall.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        fall.instructions.add(new MethodInsnNode(
            Opcodes.INVOKEVIRTUAL,
            FAKE_PLAYER_INTERNAL,
            "method_37908",
            "()Lnet/minecraft/class_1937;",
            false
        ));
        fall.instructions.add(new VarInsnNode(Opcodes.ALOAD, 4));
        fall.instructions.add(new MethodInsnNode(
            Opcodes.INVOKEVIRTUAL,
            "net/minecraft/class_1937",
            "method_8320",
            "(Lnet/minecraft/class_2338;)Lnet/minecraft/class_2680;",
            false
        ));
        fall.instructions.add(new VarInsnNode(Opcodes.ASTORE, 5));
        fall.instructions.add(new VarInsnNode(Opcodes.ALOAD, 5));
        fall.instructions.add(new MethodInsnNode(
            Opcodes.INVOKEVIRTUAL,
            "net/minecraft/class_2680",
            "method_26204",
            "()Lnet/minecraft/class_2248;",
            false
        ));
        fall.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        fall.instructions.add(new MethodInsnNode(
            Opcodes.INVOKEVIRTUAL,
            FAKE_PLAYER_INTERNAL,
            "method_37908",
            "()Lnet/minecraft/class_1937;",
            false
        ));
        fall.instructions.add(new VarInsnNode(Opcodes.ALOAD, 5));
        fall.instructions.add(new VarInsnNode(Opcodes.ALOAD, 4));
        fall.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        fall.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        fall.instructions.add(new FieldInsnNode(
            Opcodes.GETFIELD,
            FAKE_PLAYER_INTERNAL,
            "carpet$fallFixDistance",
            "F"
        ));
        fall.instructions.add(new MethodInsnNode(
            Opcodes.INVOKEVIRTUAL,
            "net/minecraft/class_2248",
            "method_9554",
            "(Lnet/minecraft/class_1937;Lnet/minecraft/class_2680;Lnet/minecraft/class_2338;Lnet/minecraft/class_1297;F)V",
            false
        ));
        fall.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        fall.instructions.add(new InsnNode(Opcodes.FCONST_0));
        fall.instructions.add(new FieldInsnNode(
            Opcodes.PUTFIELD,
            FAKE_PLAYER_INTERNAL,
            "carpet$fallFixDistance",
            "F"
        ));
        fall.instructions.add(new JumpInsnNode(Opcodes.GOTO, done));
        fall.instructions.add(airborne);
        fall.instructions.add(new VarInsnNode(Opcodes.DLOAD, 1));
        fall.instructions.add(new InsnNode(Opcodes.DCONST_0));
        fall.instructions.add(new InsnNode(Opcodes.DCMPG));
        fall.instructions.add(new JumpInsnNode(Opcodes.IFGE, done));
        fall.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        fall.instructions.add(new InsnNode(Opcodes.DUP));
        fall.instructions.add(new FieldInsnNode(
            Opcodes.GETFIELD,
            FAKE_PLAYER_INTERNAL,
            "carpet$fallFixDistance",
            "F"
        ));
        fall.instructions.add(new VarInsnNode(Opcodes.DLOAD, 1));
        fall.instructions.add(new InsnNode(Opcodes.D2F));
        fall.instructions.add(new InsnNode(Opcodes.FSUB));
        fall.instructions.add(new FieldInsnNode(
            Opcodes.PUTFIELD,
            FAKE_PLAYER_INTERNAL,
            "carpet$fallFixDistance",
            "F"
        ));
        fall.instructions.add(done);
        fall.instructions.add(new InsnNode(Opcodes.RETURN));
        fall.maxStack = 6;
        fall.maxLocals = 6;
        node.methods.add(fall);
        return write(node);
    }

    private static byte[] patchEntityMixin(byte[] original) {
        ClassNode node = read(original);
        MethodNode method = node.methods.stream()
            .filter(m -> m.name.equals("isFakePlayer"))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("EntityMixin.isFakePlayer not found"));

        TypeInsnNode existingCheck = null;
        for (AbstractInsnNode insn : method.instructions) {
            if (insn instanceof TypeInsnNode type
                && type.getOpcode() == Opcodes.INSTANCEOF
                && FAKE_PLAYER_INTERNAL.equals(type.desc)) {
                existingCheck = type;
                break;
            }
        }
        if (existingCheck == null || !(existingCheck.getNext() instanceof JumpInsnNode existingBranch)
            || existingBranch.getOpcode() != Opcodes.IFEQ) {
            throw new IllegalStateException("Unexpected EntityMixin.isFakePlayer bytecode");
        }

        AbstractInsnNode firstPlayerLookup = existingCheck.getPrevious();
        if (!(firstPlayerLookup instanceof MethodInsnNode)) {
            throw new IllegalStateException("Expected controlling-passenger lookup before instanceof");
        }
        firstPlayerLookup = firstPlayerLookup.getPrevious();
        if (!(firstPlayerLookup instanceof VarInsnNode loadThis)
            || loadThis.getOpcode() != Opcodes.ALOAD || loadThis.var != 0) {
            throw new IllegalStateException("Expected ALOAD 0 before controlling-passenger lookup");
        }

        LabelNode fakePlayerMatch = new LabelNode();
        method.instructions.insert(existingBranch, fakePlayerMatch);
        InsnList directCheck = new InsnList();
        directCheck.add(new VarInsnNode(Opcodes.ALOAD, 0));
        directCheck.add(new TypeInsnNode(Opcodes.INSTANCEOF, FAKE_PLAYER_INTERNAL));
        directCheck.add(new JumpInsnNode(Opcodes.IFNE, fakePlayerMatch));
        method.instructions.insertBefore(firstPlayerLookup, directCheck);
        return write(node);
    }

    private static ClassNode read(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    private static byte[] write(ClassNode node) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }
}
