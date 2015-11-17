package merklebtree;

import java.io.*;
import java.util.*;

public class MerkleBTree
{
    public final Map<ByteArrayWrapper, byte[]> storage;
    public final int maxChildren;
    public TreeNode root;

    public MerkleBTree(TreeNode root, Map<ByteArrayWrapper, byte[]> storage, int maxChildren) {
        this.root = root;
        this.storage = storage;
        this.maxChildren = maxChildren;
        this.storage.put(root.hash(), root.serialize());
    }

    public MerkleBTree() {
        this(new TreeNode(new TreeSet<>()), new HashMap<>(), 16);
    }

    public byte[] get(byte[] rawKey) throws IOException {
        return root.get(new ByteArrayWrapper(rawKey), storage);
    }

    public TreeNode put(byte[] rawKey, byte[] value) throws IOException {
        root = root.put(new ByteArrayWrapper(rawKey), value, storage, maxChildren);
        storage.put(root.hash(), root.serialize());
        return root;
    }

    public TreeNode delete(byte[] rawKey) throws IOException {
        root = root.delete(new ByteArrayWrapper(rawKey), storage, maxChildren);
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
