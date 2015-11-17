package merklebtree;

import org.junit.*;

import java.io.*;
import java.util.*;

public class Tests {

    @Test
    public void basic() throws IOException {
        MerkleBTree tree = new MerkleBTree();
        byte[] key1 = new byte[]{0, 1, 2, 3};
        ByteArrayHashable value1 = new ByteArrayHashable(new byte[]{1, 1, 1, 1});
        tree.storage.put(value1.hash(), value1.data);
        tree.put(key1, value1);
        ByteArrayWrapper res1 = tree.get(key1);
        if (!res1.equals(value1))
            throw new IllegalStateException("Results not equal");
    }

    @Test
    public void basic2() throws IOException {
        MerkleBTree tree = new MerkleBTree();
        for (int i=0; i < 16; i++) {
            byte[] key1 = new byte[]{0, 1, 2, (byte)i};
            ByteArrayHashable value1 = new ByteArrayHashable(new byte[]{1, 1, 1, (byte)i});
            tree.storage.put(value1.hash(), value1.data);
            tree.put(key1, value1);
            ByteArrayWrapper res1 = tree.get(key1);
            if (!res1.equals(value1))
                throw new IllegalStateException("Results not equal");
        }
        if (tree.root.keys.size() != 2)
            throw new IllegalStateException("New root should have two children!");
    }

    @Test
    public void huge() throws IOException {
        MerkleBTree tree = new MerkleBTree();
        long t1 = System.currentTimeMillis();
        for (int i=0; i < 1000000; i++) {
            byte[] key1 = new byte[]{0, 1, 2, (byte)i};
            ByteArrayHashable value1 = new ByteArrayHashable(new byte[]{1, 1, 1, (byte)i});
            tree.storage.put(value1.hash(), value1.data);
            tree.put(key1, value1);
            ByteArrayWrapper res1 = tree.get(key1);
            if (!res1.equals(value1))
                throw new IllegalStateException("Results not equal");
        }
        long t2 = System.currentTimeMillis();
        for (int i=0; i < 1000000; i++) {
            byte[] key1 = new byte[]{0, 1, 2, (byte)i};
            ByteArrayHashable value1 = new ByteArrayHashable(new byte[]{1, 1, 1, (byte)i});
            ByteArrayWrapper res1 = tree.get(key1);
            if (!res1.equals(value1))
                throw new IllegalStateException("Results not equal");
        }
        System.out.printf("Put+get rate = %f /s\n", 1000000.0 / (t2 - t1) * 1000);
    }

    @Test
    public void random() throws IOException {
        MerkleBTree tree = new MerkleBTree();
        int keylen = 32;

        long t1 = System.currentTimeMillis();
        Random r = new Random(1);
        int lim = 140000;
        for (int i = 0; i < lim; i++) {
            if (i % (lim/10) == 0)
                System.out.println((10*i/lim)+"0 %");
            byte[] key1 = new byte[keylen];
            r.nextBytes(key1);
            byte[] data = new byte[keylen];
            r.nextBytes(data);
            ByteArrayHashable value1 = new ByteArrayHashable(data);
            tree.storage.put(value1.hash(), value1.data);
            tree.put(key1, value1);

            ByteArrayWrapper res1 = tree.get(key1);
            if (!res1.equals(value1))
                throw new IllegalStateException("Results not equal");
        }
        long t2 = System.currentTimeMillis();
        System.out.printf("Put+get rate = %f /s\n", (double)lim / (t2 - t1) * 1000);
    }

    @Test
    public void delete() throws IOException {
        MerkleBTree tree = new MerkleBTree();
        int keylen = 32;

        Random r = new Random(1);
        SortedSet<ByteArrayWrapper> keys = new TreeSet<>();
        int lim = 10000;
        for (int i = 0; i < lim; i++) {
            if (i % (lim/10) == 0)
                System.out.println((10*i/lim)+"0 % of building");
            byte[] key1 = new byte[keylen];
            keys.add(new ByteArrayWrapper(key1));
            r.nextBytes(key1);
            byte[] data = new byte[keylen];
            r.nextBytes(data);
            ByteArrayHashable value1 = new ByteArrayHashable(data);
            tree.storage.put(value1.hash(), value1.data);
            tree.put(key1, value1);

            ByteArrayWrapper res1 = tree.get(key1);
            if (!res1.equals(value1))
                throw new IllegalStateException("Results not equal");
        }

        ByteArrayWrapper[] keysArray = keys.toArray(new ByteArrayWrapper[keys.size()]);
        long t1 = System.currentTimeMillis();
        for (int i = 0; i < lim; i++) {
            if (i % (lim / 10) == 0)
                System.out.println((10 * i / lim) + "0 % of deleting");
            if (tree.size() != lim)
                throw new IllegalStateException("Missing keys from tree!");
            ByteArrayWrapper key = keysArray[r.nextInt(keysArray.length)];
            ByteArrayWrapper value = tree.get(key.data);
            if (value == null)
                throw new IllegalStateException("Key not present!");
            tree.delete(key.data);
            if (tree.get(key.data) != null)
                throw new IllegalStateException("Key still present!");
            tree.put(key.data, new ByteArrayHashable(value.data));
        }
        long t2 = System.currentTimeMillis();
        System.out.printf("size+get+delete+get+put rate = %f /s\n", (double)lim / (t2 - t1) * 1000);
    }

    public static class ByteArrayHashable extends ByteArrayWrapper implements Hashable {
        public ByteArrayHashable(byte[] data) {
            super(data);
        }

        @Override
        public ByteArrayWrapper hash() {
            return new ByteArrayWrapper(TreeNode.hash(data));
        }
    }
}
