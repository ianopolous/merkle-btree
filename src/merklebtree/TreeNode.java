package merklebtree;

import java.io.*;
import java.security.*;
import java.util.*;

public class TreeNode implements Hashable {
    public final SortedSet<KeyElement> keys;

    public TreeNode(byte[] leftChildHash, SortedSet<KeyElement> keys) {
        this.keys = new TreeSet<>();
        this.keys.addAll(keys);
        this.keys.add(new KeyElement(new ByteArrayWrapper(new byte[0]), new byte[0], leftChildHash));
    }

    public TreeNode(SortedSet<KeyElement> keys) {
        this(new byte[0], keys);
    }

    public ByteArrayWrapper get(ByteArrayWrapper key, Map<ByteArrayWrapper, byte[]> storage) throws IOException {
        KeyElement dummy = new KeyElement(key, new byte[0], new byte[0]);
        SortedSet<KeyElement> tailSet = keys.tailSet(dummy);
        KeyElement nextSmallest;
        if (tailSet.size() == 0) {
            nextSmallest = keys.last();
        } else {
            nextSmallest = tailSet.first();
            if (!nextSmallest.key.equals(key))
                nextSmallest = keys.headSet(dummy).last();
        }
        if (nextSmallest.key.equals(key))
            return new ByteArrayWrapper(storage.get(new ByteArrayWrapper(nextSmallest.valueHash)));
        if (nextSmallest.targetHash.length == 0)
            return null;
        return TreeNode.deserialize(storage.get(new ByteArrayWrapper(nextSmallest.targetHash))).get(key, storage);
    }

    public TreeNode put(ByteArrayWrapper key, Hashable value, Map<ByteArrayWrapper, byte[]> storage) throws IOException {
        KeyElement dummy = new KeyElement(key, null, null);
        SortedSet<KeyElement> tailSet = keys.tailSet(dummy);
        KeyElement nextSmallest;
        if (tailSet.size() == 0) {
            nextSmallest = keys.last();
        } else {
            nextSmallest = tailSet.first();
            if (!nextSmallest.key.equals(key)) {
                SortedSet<KeyElement> headSet = keys.headSet(dummy);
                nextSmallest = headSet.last();
            }
        }
        if (nextSmallest.key.equals(key)) {
            KeyElement modified = new KeyElement(key, value.hash().data, nextSmallest.targetHash);
            keys.remove(nextSmallest);
            keys.add(modified);
            // commit this node to storage
            storage.put(this.hash(), this.serialize());
            return this;
        }
        if (nextSmallest.targetHash.length == 0) {
            if (keys.size() < MerkleBTree.MAX_CHILDREN) {
                keys.add(new KeyElement(key, value.hash().data, new byte[0]));
                // commit this node to storage
                storage.put(this.hash(), this.serialize());
                return this;
            }
            // split into two and make new parent
            keys.add(new KeyElement(key, value.hash().data, new byte[0]));
            KeyElement[] tmp = new KeyElement[keys.size()];
            KeyElement median = keys.toArray(tmp)[keys.size()/2];
            // commit left child
            SortedSet<KeyElement> left = keys.headSet(median);
            TreeNode leftChild = new TreeNode(left);
            storage.put(leftChild.hash(), leftChild.serialize());

            // commit right child
            SortedSet<KeyElement> right = keys.tailSet(median);
            right.remove(right.first());
            TreeNode rightChild = new TreeNode(right);
            storage.put(rightChild.hash(), rightChild.serialize());

            // now add median to parent
            TreeSet holder = new TreeSet<>();
            KeyElement newParent = new KeyElement(median.key, median.valueHash, rightChild.hash().data);
            holder.add(newParent);
            return new TreeNode(leftChild.hash().data, holder);
        }

        TreeNode modifiedChild = TreeNode.deserialize(storage.get(new ByteArrayWrapper(nextSmallest.targetHash))).put(key, value, storage);
        if (modifiedChild.keys.size() == 2) {
            // we split a child and need to add the median to our keys
            if (keys.size() < MerkleBTree.MAX_CHILDREN) {
                KeyElement replacementNextSmallest = new KeyElement(nextSmallest.key, nextSmallest.valueHash, modifiedChild.keys.first().targetHash);
                keys.remove(nextSmallest);
                keys.add(replacementNextSmallest);
                keys.add(modifiedChild.keys.last());
                storage.put(this.hash(), this.serialize());
                return this;
            }
            // we need to split as well, merge in new key and two pointers first
            KeyElement nonZero = modifiedChild.keys.last();
            keys.add(nonZero);
            KeyElement updated = new KeyElement(nextSmallest.key, nextSmallest.valueHash, modifiedChild.keys.first().targetHash);
            keys.remove(nextSmallest);
            keys.add(updated);

            // now split
            KeyElement[] tmp = new KeyElement[keys.size()];
            KeyElement median = keys.toArray(tmp)[keys.size()/2];
            // commit left child
            SortedSet<KeyElement> left = keys.headSet(median);
            TreeNode leftChild = new TreeNode(left);
            storage.put(leftChild.hash(), leftChild.serialize());

            // commit right child
            SortedSet<KeyElement> right = keys.tailSet(median);
            right.remove(right.first());
            TreeNode rightChild = new TreeNode(right);
            storage.put(rightChild.hash(), rightChild.serialize());

            // now add median to parent
            TreeSet holder = new TreeSet<>();
            KeyElement newParent = new KeyElement(median.key, median.valueHash, rightChild.hash().data);
            holder.add(newParent);
            return new TreeNode(leftChild.hash().data, holder);
        }
        // update pointer to child (child element wasn't split)
        KeyElement updated = new KeyElement(nextSmallest.key, nextSmallest.valueHash, modifiedChild.hash().data);
        keys.remove(nextSmallest);
        keys.add(updated);
        storage.put(this.hash(), this.serialize());
        return this;
    }

    public byte[] serialize() {
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            DataOutputStream dout = new DataOutputStream(bout);
            dout.writeInt(keys.size());
            for (KeyElement e : keys) {
                dout.writeInt(e.key.data.length);
                dout.write(e.key.data);
                dout.writeInt(e.valueHash.length);
                dout.write(e.valueHash);
                dout.writeInt(e.targetHash.length);
                dout.write(e.targetHash);
            }
            return bout.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public ByteArrayWrapper hash() {
        return new ByteArrayWrapper(hash(serialize()));
    }

    public static TreeNode deserialize(byte[] raw) throws IOException {
        if (raw == null)
            throw new IllegalArgumentException("Null byte[]!");
        DataInputStream din = new DataInputStream(new ByteArrayInputStream(raw));
        int n = din.readInt();
        SortedSet<KeyElement> keys = new TreeSet<>();
        for (int i=0; i < n; i++) {
            byte[] key = new byte[din.readInt()];
            din.readFully(key);
            byte[] valueHash = new byte[din.readInt()];
            din.readFully(valueHash);
            byte[] targetHash = new byte[din.readInt()];
            din.readFully(targetHash);
            keys.add(new KeyElement(new ByteArrayWrapper(key), valueHash, targetHash));
        }
        return new TreeNode(keys);
    }

    public static class KeyElement implements Comparable<KeyElement> {
        public final ByteArrayWrapper key;
        public final byte[] valueHash;
        public final byte[] targetHash;

        public KeyElement(ByteArrayWrapper key, byte[] valueHash, byte[] targetHash) {
            this.key = key;
            this.valueHash = valueHash;
            this.targetHash = targetHash;
        }

        @Override
        public int compareTo(KeyElement that) {
            return key.compareTo(that.key);
        }
    }

    public static byte[] hash(byte[] input)
    {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(input);
            return md.digest();
        } catch (NoSuchAlgorithmException e)
        {
            throw new IllegalStateException("couldn't find hash algorithm");
        }
    }
}
