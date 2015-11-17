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

    /**
     *
     * @param rawKey
     * @return value stored under rawKey
     * @throws IOException
     */
    public byte[] get(byte[] rawKey) throws IOException {
        return root.get(new ByteArrayWrapper(rawKey), storage);
    }

    /**
     *
     * @param rawKey
     * @param value
     * @return hash of new tree root
     * @throws IOException
     */
    public byte[] put(byte[] rawKey, byte[] value) throws IOException {
        root = root.put(new ByteArrayWrapper(rawKey), value, storage, maxChildren);
        storage.put(root.hash(), root.serialize());
        return root.hash().data;
    }

    /**
     *
     * @param rawKey
     * @return hash of new tree root
     * @throws IOException
     */
    public byte[] delete(byte[] rawKey) throws IOException {
        root = root.delete(new ByteArrayWrapper(rawKey), storage, maxChildren);
        storage.put(root.hash(), root.serialize());
        return root.hash().data;
    }

    /**
     *
     * @return number of keys stored in tree
     * @throws IOException
     */
    public int size() throws IOException {
        return root.size(storage);
    }

    public void print(PrintStream w) throws IOException {
        root.print(w, 0, storage);
    }
}
