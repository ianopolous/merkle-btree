package merklebtree;

import java.io.*;
import java.util.*;

public class MerkleBTree
{
    public static final int MAX_CHILDREN = 16;
    public static final int KEY_SIZE = 32;
    public static final ByteArrayWrapper MIN_KEY = new ByteArrayWrapper(new byte[KEY_SIZE]);

    public final Map<ByteArrayWrapper, byte[]> storage;
    public TreeNode root;

    public MerkleBTree(TreeNode root, Map<ByteArrayWrapper, byte[]> storage) {
        this.root = root;
        this.storage = storage;
    }

    public MerkleBTree() {
        this.root = new TreeNode(new TreeSet<>());
        this.storage = new HashMap<>();
        this.storage.put(root.hash(), root.serialize());
    }

    public ByteArrayWrapper get(byte[] rawKey) throws IOException {
        return root.get(new ByteArrayWrapper(rawKey), storage);
    }

    public TreeNode put(byte[] rawKey, Hashable value) throws IOException {
        root = root.put(new ByteArrayWrapper(rawKey), value, storage);
        storage.put(root.hash(), root.serialize());
        return root;
    }

    public TreeNode delete(byte[] rawKey) throws IOException {
        root = root.delete(new ByteArrayWrapper(rawKey), storage);
        storage.put(root.hash(), root.serialize());
        return root;
    }

    public int size() throws IOException {
        return root.size(storage);
    }

    public void print(PrintStream w) throws IOException {
        root.print(w, 0, storage);
    }
}
