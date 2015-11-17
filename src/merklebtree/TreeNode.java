package merklebtree;

import java.io.*;
import java.security.*;
import java.util.*;

public class TreeNode implements Hashable {
    public final SortedSet<KeyElement> keys;

    public TreeNode(byte[] leftChildHash, SortedSet<KeyElement> keys) {
        this.keys = new TreeSet<>();
        this.keys.addAll(keys);
        KeyElement zero = new KeyElement(new ByteArrayWrapper(new byte[0]), new byte[0], leftChildHash);
        if (!keys.contains(zero))
            this.keys.add(zero);
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
            TreeNode rightChild = new TreeNode(median.targetHash, right);
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
            TreeNode rightChild = new TreeNode(median.targetHash, right);
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

    public int size(Map<ByteArrayWrapper, byte[]> storage) throws IOException {
        int total = 0;
        for (KeyElement e : keys)
            if (e.targetHash.length > 0)
                total += TreeNode.deserialize(storage.get(new ByteArrayWrapper(e.targetHash))).size(storage);
        total += keys.size() - 1;
        return total;
    }

    private KeyElement smallestNonZeroKey() {
        return keys.tailSet(new KeyElement(new ByteArrayWrapper(new byte[]{0}), new byte[0], new byte[0])).first();
    }

    public ByteArrayWrapper smallestKey(Map<ByteArrayWrapper, byte[]> storage) throws IOException {
        if (keys.first().targetHash.length == 0)
            return keys.toArray(new KeyElement[keys.size()])[1].key;
        return TreeNode.deserialize(storage.get(new ByteArrayWrapper(keys.first().targetHash))).smallestKey(storage);
    }

    public TreeNode delete(ByteArrayWrapper key, Map<ByteArrayWrapper, byte[]> storage) throws IOException {
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
        if (nextSmallest.key.equals(key)) {
            if (nextSmallest.targetHash.length == 0) {
                // we are a leaf
                keys.remove(nextSmallest);
                if (keys.size() >= MerkleBTree.MAX_CHILDREN/2)
                    storage.put(this.hash(), this.serialize());
                return this;
            } else {
                keys.remove(nextSmallest);
                TreeNode child = TreeNode.deserialize(storage.get(new ByteArrayWrapper(nextSmallest.targetHash)));
                // take the subtree's smallest value (in a leaf) delete it and promote it to the separator here
                ByteArrayWrapper smallestKey = child.smallestKey(storage);
                ByteArrayWrapper value = child.get(smallestKey, storage);
                TreeNode newChild = child.delete(smallestKey, storage);
                KeyElement replacement = new KeyElement(smallestKey, hash(value.data), newChild.hash().data);
                keys.add(replacement);
                if (newChild.keys.size() >= MerkleBTree.MAX_CHILDREN/2) {
                    storage.put(this.hash(), this.serialize());
                    return this;
                } else {
                    // re-balance
                    return rebalance(this, newChild, storage);
                }
            }
        }
        if (nextSmallest.targetHash.length == 0)
            return this;
        TreeNode child = TreeNode.deserialize(storage.get(new ByteArrayWrapper(nextSmallest.targetHash))).delete(key, storage);
        // update pointer
        keys.remove(nextSmallest);
        keys.add(new KeyElement(nextSmallest.key, nextSmallest.valueHash, child.hash().data));
        if (child.keys.size() < MerkleBTree.MAX_CHILDREN/2) {
            // re-balance
            return rebalance(this, child, storage);
        }
        storage.put(this.hash(), this.serialize());
        return this;
    }

    private static TreeNode rebalance(TreeNode parent, TreeNode child, Map<ByteArrayWrapper, byte[]> storage) throws IOException {
        // child has too few children
        ByteArrayWrapper childHash = child.hash();
        KeyElement[] parentKeys = parent.keys.toArray(new KeyElement[parent.keys.size()]);
        int i = 0;
        while (i < parentKeys.length && !(new ByteArrayWrapper(parentKeys[i].targetHash)).equals(childHash))
            i++;

        KeyElement centerKey = parentKeys[i];
        Optional<KeyElement> leftKey = i > 0 ? Optional.of(parentKeys[i-1]) : Optional.empty();
        Optional<KeyElement> rightKey = i + 1 < parentKeys.length ? Optional.of(parentKeys[i+1]) : Optional.empty();
        Optional<TreeNode> leftSibling = leftKey.isPresent() ? Optional.of(TreeNode.deserialize(storage.get(new ByteArrayWrapper(leftKey.get().targetHash)))) : Optional.empty();
        Optional<TreeNode> rightSibling = rightKey.isPresent() ? Optional.of(TreeNode.deserialize(storage.get(new ByteArrayWrapper(rightKey.get().targetHash)))) : Optional.empty();
        if (rightSibling.isPresent() && rightSibling.get().keys.size() > MerkleBTree.MAX_CHILDREN/2) {
            // rotate left
            TreeNode right = rightSibling.get();
            KeyElement newSeparator = right.smallestNonZeroKey();
            parent.keys.remove(centerKey);

            child.keys.add(new KeyElement(rightKey.get().key, rightKey.get().valueHash, right.keys.first().targetHash));
            storage.put(child.hash(), child.serialize());

            right.keys.remove(newSeparator);
            right.keys.remove(new KeyElement(new ByteArrayWrapper(new byte[0]), new byte[0], new byte[0]));
            right = new TreeNode(newSeparator.targetHash, right.keys);
            storage.put(right.hash(), right.serialize());

            parent.keys.remove(rightKey.get());
            parent.keys.add(new KeyElement(centerKey.key, centerKey.valueHash, child.hash().data));
            parent.keys.add(new KeyElement(newSeparator.key, newSeparator.valueHash, right.hash().data));
            storage.put(parent.hash(), parent.serialize());
            return parent;
        } else if (leftSibling.isPresent() && leftSibling.get().keys.size() > MerkleBTree.MAX_CHILDREN/2) {
            // rotate right
            TreeNode left = leftSibling.get();
            KeyElement newSeparator = left.keys.last();
            parent.keys.remove(centerKey);

            left.keys.remove(newSeparator);
            storage.put(left.hash(), left.serialize());

            child.keys.add(new KeyElement(centerKey.key, centerKey.valueHash, child.keys.first().targetHash));
            child.keys.remove(new KeyElement(new ByteArrayWrapper(new byte[0]), new byte[0], new byte[0]));
            child.keys.add(new KeyElement(new ByteArrayWrapper(new byte[0]), new byte[0], newSeparator.targetHash));
            storage.put(child.hash(), child.serialize());

            parent.keys.remove(leftKey.get());
            parent.keys.add(new KeyElement(leftKey.get().key, leftKey.get().valueHash, left.hash().data));
            parent.keys.add(new KeyElement(newSeparator.key, newSeparator.valueHash, child.hash().data));
            storage.put(parent.hash(), parent.serialize());
            return parent;
        } else {
            if (rightSibling.isPresent()) {
                // merge with right sibling and separator
                SortedSet<KeyElement> combinedKeys = new TreeSet<>();
                combinedKeys.addAll(child.keys);
                combinedKeys.addAll(rightSibling.get().keys);
                combinedKeys.add(new KeyElement(rightKey.get().key, rightKey.get().valueHash, rightSibling.get().keys.first().targetHash));
                TreeNode combined = new TreeNode(combinedKeys);
                storage.put(combined.hash(), combined.serialize());

                parent.keys.remove(rightKey.get());
                parent.keys.remove(centerKey);
                parent.keys.add(new KeyElement(centerKey.key, centerKey.valueHash, combined.hash().data));
                if (parent.keys.size() >= MerkleBTree.MAX_CHILDREN/2)
                    storage.put(parent.hash(), parent.serialize());
                return parent;
            } else {
                // merge with left sibling and separator
                SortedSet<KeyElement> combinedKeys = new TreeSet<>();
                combinedKeys.addAll(child.keys);
                combinedKeys.addAll(leftSibling.get().keys);
                combinedKeys.add(new KeyElement(centerKey.key, centerKey.valueHash, child.keys.first().targetHash));
                TreeNode combined = new TreeNode(combinedKeys);
                storage.put(combined.hash(), combined.serialize());

                parent.keys.remove(leftKey.get());
                parent.keys.remove(centerKey);
                parent.keys.add(new KeyElement(leftKey.get().key, leftKey.get().valueHash, combined.hash().data));
                if (parent.keys.size() >= MerkleBTree.MAX_CHILDREN/2)
                    storage.put(parent.hash(), parent.serialize());
                return parent;
            }
        }
    }

    public void print(PrintStream w, int depth, Map<ByteArrayWrapper, byte[]> storage) throws IOException {
        int index = 0;
        for (KeyElement e: keys) {
            String tab = "";
            for (int i=0; i < depth; i++)
                tab += "   ";
            w.print(String.format(tab + "[%d/%d] %s : %s\n", index++, keys.size(), e.key.toString(), new ByteArrayWrapper(e.valueHash).toString()));
            if (e.targetHash.length > 0)
                TreeNode.deserialize(storage.get(new ByteArrayWrapper(e.targetHash))).print(w, depth + 1, storage);
        }
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

        @Override
        public String toString() {
            return key.toString() + " -> " + new ByteArrayWrapper(valueHash) +" : "+new ByteArrayWrapper(targetHash);
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
