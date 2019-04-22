package merklebtree;

import java.io.*;
import java.util.*;

public class MerkleBTree
{
    public final ContentAddressedStorage storage;
    public final int maxChildren;
    public TreeNode root;

    public MerkleBTree(TreeNode root, ContentAddressedStorage storage, int maxChildren) {
        this.storage = storage;
        byte[] hash = this.storage.put(root.serialize());
        this.root = new TreeNode(root.keys, hash);
        this.maxChildren = maxChildren;
    }

    public MerkleBTree() {
        this(new TreeNode(new TreeSet<>()), new RAMStorage(), 16);
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

    public byte[] get(byte[] rawKey, TreeNode.Nodes nodes) throws IOException {
        nodes.root = root.hash.get();
        return root.getWithNodeHistory(new ByteArrayWrapper(rawKey), storage, nodes);
    }

    /**
     *
     * @param rawKey
     * @param value
     * @return hash of new tree root
     * @throws IOException
     */
    public byte[] put(byte[] rawKey, byte[] value) throws IOException {
        TreeNode newRoot = root.put(new ByteArrayWrapper(rawKey), value, storage, maxChildren);
        if (root.hash.isPresent())
            storage.remove(root.hash.get());
        if (!newRoot.hash.isPresent()) {
            root = new TreeNode(newRoot.keys, storage.put(newRoot.serialize()));
        } else
            root = newRoot;
        return root.hash.get();
    }

    /**
     *
     * @param rawKey
     * @return hash of new tree root
     * @throws IOException
     */
    public byte[] delete(byte[] rawKey) throws IOException {
        TreeNode newRoot = root.delete(new ByteArrayWrapper(rawKey), storage, maxChildren);
        if (root.hash.isPresent())
            storage.remove(root.hash.get());
        if (!newRoot.hash.isPresent()) {
            root = new TreeNode(newRoot.keys, storage.put(newRoot.serialize()));
        } else
            root = newRoot;
        return root.hash.get();
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
