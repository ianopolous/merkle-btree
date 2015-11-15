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
        Random r = new Random(1);
        long t1 = System.currentTimeMillis();
        for (int i=0; i < 1481; i++) {
            byte[] key1 = new byte[32];
            r.nextBytes(key1);
            byte[] data = new byte[32];
            r.nextBytes(data);
            ByteArrayHashable value1 = new ByteArrayHashable(data);
            tree.storage.put(value1.hash(), value1.data);
            tree.put(key1, value1);
//            ByteArrayWrapper res1 = tree.get(key1);
//            if (!res1.equals(value1))
//                throw new IllegalStateException("Results not equal");
        }
        tree.print(System.out);
        long t2 = System.currentTimeMillis();
        System.out.printf("Put+get rate = %f /s\n", 1000000.0 / (t2 - t1) * 1000);
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
