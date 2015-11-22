package merklebtree;

import java.io.*;
import java.util.*;

public class MerkleBTree
{
    public final ContentAddressedStorage storage;
    public final int maxChildren;
    public TreeNode.TreeNodeAndHash root;

    public MerkleBTree(TreeNode root, ContentAddressedStorage storage, int maxChildren) {
        this.root = new TreeNode.TreeNodeAndHash(root);
        this.storage = storage;
        this.maxChildren = maxChildren;
        this.storage.put(root.serialize());
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
        return root.node.get(new ByteArrayWrapper(rawKey), storage);
    }

    /**
     *
     * @param rawKey
     * @param value
     * @return hash of new tree root
     * @throws IOException
     */
    public byte[] put(byte[] rawKey, byte[] value) throws IOException {
        root = root.node.put(new ByteArrayWrapper(rawKey), value, storage, maxChildren);
        if (!root.hash.isPresent()) {
            root = new TreeNode.TreeNodeAndHash(root.node, storage.put(root.node.serialize()));
        }
        return root.hash.get();
    }

    /**
     *
     * @param rawKey
     * @return hash of new tree root
     * @throws IOException
     */
    public byte[] delete(byte[] rawKey) throws IOException {
        root = root.node.delete(new ByteArrayWrapper(rawKey), storage, maxChildren);
        if (!root.hash.isPresent()) {
            root = new TreeNode.TreeNodeAndHash(root.node, storage.put(root.node.serialize()));
        }
        return root.hash.get();
    }

    /**
     *
     * @return number of keys stored in tree
     * @throws IOException
     */
    public int size() throws IOException {
        return root.node.size(storage);
    }

    public void print(PrintStream w) throws IOException {
        root.node.print(w, 0, storage);
    }
}
